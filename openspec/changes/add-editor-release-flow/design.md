# Design

## Context

See `proposal.md` for the editor's problem and `specs/` for the behaviour. This section describes what the code does today and what the design relies on.

- `POST /generator/release` (`EditorReleaseController`) fetches the snapshot from the backend (`BackendApiReleaseSnapshotSource`), computes its revision and publishes through `ReleaseService.releaseSnapshot`. It takes no base branch and compiles nothing. The creator is fixed to `stelaris`.
- `ReleaseService` refuses an existing version before the snapshot is fetched, but `EditorReleaseService` fetches the snapshot first and only then calls the refusal. The order required by `editor-release` (refusal of an existing release version before any clone or compile) needs the check moved up into `EditorReleaseService`. Snapshot versions are never refused for existing.
- `DataJarBuilder` writes `META-INF/MANIFEST.MF`, `META-INF/vulpes/release.json` and `META-INF/vulpes/snapshot.json`. `ReleaseJsonWriter` writes `release.json` by hand with sorted keys and format `1`.
- `MavenContentPublisher` checks existence with a `HEAD` of the POM and deploys JAR, POM and `.module` through Maven Resolver. It already sends the configured credentials; it has no read path for listings. Today the snapshot version is `-dev.N`; this change routes `-SNAPSHOT` to the snapshots repository.
- `VulpesDownloadController` clones `vulpes-base` into a temporary folder with `GitProjectWorker` and zips the whole folder, `.git` included. `CodeGenerationHandler` is a stub. Neither removes its temporary folder.
- `GithubService.getBranches()` returns an empty list when GitHub answers anything but `200`. `GithubClient` sends only a `User-Agent` header, so the branch list is unauthenticated (GitHub allows 60 unauthenticated requests per hour per address).
- The Vulpes Gradle plugin repository (`OneLiteFeatherNET/vulpes-gradle-plugin`) holds the code generator in `vulpes-codegen` (depends on `vulpes-format`). Today both are bundled into the plugin's shadow JAR and the published POM does not list them. The prerequisite `publish-codegen-library` publishes them as Maven artifacts with a public facade. `vulpes-codegen` generates Java against the API of `vulpes-base` (`net.onelitefeather.vulpes`), and `vulpes-base` builds with JDK 25 and compiles against `minestom` (compileOnly), `mycelium` and `evalex`.
- `release-format 1` is defined by `vulpes-format/src/main/resources/net/onelitefeather/vulpes/format/release-1.schema.json` in the plugin repository. Its top level has `additionalProperties: false` and `format` is a `const` of `1`.
- `charts/vulpes-generator/templates/deployment.yaml` already injects `VULPES_BACKEND_URL`, `VULPES_RELEASE_TOKEN`, `VULPES_BACKEND_TOKEN`, `ONELITEFEATHER_MAVEN_USERNAME` and `ONELITEFEATHER_MAVEN_PASSWORD` when the overlay names secrets, and `VULPES_RELEASE_WORK_DIR` when it sets a work directory. The chart has no key for `VULPES_RELEASE_EDITOR_ROUTE_ENABLED`. Production sets none of these values, so the backend URL falls back to `http://localhost:8081` and the Maven upload fails with `401`.

## Goals / Non-Goals

**Goals**
- One request path for local builds and releases, each verified against a named `vulpes-base` branch.
- One code generator: the one of the Vulpes Gradle plugin, consumed as a library.
- No request leaves a clone, a build directory or a zip behind.
- Errors carry a status code that tells the editor what to fix: `400` input, `404` disabled route, `409` existing release version, `422` content or compilation, `502` infrastructure.

**Non-Goals**
- No change to the snapshot format and no change to the codegen output. The generator consumes the codegen as a library and does not fork it.
- No sandbox beyond the pod. See Risks.
- No compile of the generated code into the data JAR. It is only a gate.

## Decisions

### 1. Where the compilation runs: in-process `javax.tools` against a classpath built once per commit

The compile gate needs the classes of a `vulpes-base` commit and its runtime classpath (`minestom`, the `mycelium` BOM, `evalex`, `slf4j`, and the rest of the resolved runtime dependencies).

- **Per commit, once.** The first request for a full commit SHA runs the cloned branch's own Gradle wrapper (`./gradlew`) with a cleared environment, through an init script that the generator owns. The build compiles `vulpes-base` and prints its runtime classpath. The generator copies the built `vulpes-base` jar and the dependency jars into `<cache>/<sha>/`. The cache holds no clone and no source.
- **Per request.** The generated sources are written into the request directory and compiled in-process with `ToolProvider.getSystemJavaCompiler()`, `--release 25`, the cached classpath, a `DiagnosticCollector` and `-d` into the request directory. A warm commit runs no Gradle at all, and diagnostics arrive as structured objects, so `file`, `line` and `message` come straight from the compiler.
- **Rejected.** Running the Gradle build of the clone for every request: a cold Gradle start per request, a Gradle JVM of around one gigabyte each time, diagnostics parsed from Gradle's text output, and no practical concurrency. Resolving the classpath statically from `gradle/libs.versions.toml`: it repeats the dependency wiring of `vulpes-base` by hand and drifts whenever the base build changes.

The cache:
- **Location.** `vulpes.base.cache-dir` (`VULPES_BASE_CACHE_DIR`). The cluster chooses an `emptyDir` (a pod restart rebuilds cold) or a PVC (the cache survives restarts). The code default is `<work>/base-cache`.
- **Key.** The full 40-character commit SHA only.
- **Eviction.** The cache keeps the `vulpes.base.cache-entries` most recently used commits (default `3`). Storing a new commit beyond that removes the least recently used entries.
- **Time limit.** `vulpes.base.clone-timeout` (default `2m`) for the clone and `vulpes.base.build-timeout` (default `10m`) for the base build. A build that exceeds its limit answers `BASE_UNAVAILABLE` (`502`) and leaves no cache entry.
- **Concurrency.** One base build at a time per pod. The cache lookup happens before that lock, so a warm request does not wait for the cold build of another commit.
- **Cold build.** The first build of a new commit is slower (Gradle start, dependency download, compilation of `vulpes-base`; minutes on a cold Gradle cache) and needs more memory than a warm request, which needs only `javac`. The pod's memory values in Kubernetes-FLUX must be raised for the cold build (section 9).

Per-request flow:
1. Clone `vulpes-base` at the branch with depth one into `<work>/<request>/clone` and resolve `HEAD` to its full SHA.
2. If `<cache>/<sha>/` exists, use it and mark it as used. Otherwise run the base build in the clone with the time limit, move the result into the cache and evict beyond `cache-entries`. The clone is not needed after this step.
3. Assemble the snapshot from the backend (existing `BackendApiReleaseSnapshotSource`).
4. Generate the sources with the codegen library into `<work>/<request>/generated`.
5. Compile with `javax.tools` into `<work>/<request>/classes` against the cached classpath. Diagnostics above 100 are cut.
6. On success build `release.json` (format 2) and the zip or the data JAR. Delete `<work>/<request>` in a `finally` block.

The clone is deleted at the end of every request, also on failure (`specs/base-branch-build`, Workspace cleanup). The base build runs with an environment that holds no generator secret (`specs/base-branch-build`, Base build environment).

### 2. Consuming the code generator: the published plugin library

The generator consumes `vulpes-codegen` and `vulpes-format` as Maven libraries, pinned in its version catalog. The plugin repository owns the generator; this repository never keeps a copy of it, and there is only one code generation implementation.

The prerequisite `publish-codegen-library` (task 0.1, in `OneLiteFeatherNET/vulpes-gradle-plugin`) publishes `vulpes-format` and `vulpes-codegen` as `net.onelitefeather.vulpes:<artifact>` and exposes one public facade. The facade takes a parsed snapshot, a package name and a project name, and returns the generated sources. The generator's codegen wrapper calls only that facade. Until the artifacts are published, the code pull request cannot be built.

Rejected: depending on the plugin's shadow JAR (it contains the Gradle API glue and a second copy of `vulpes-format`), and copying the codegen sources into this repository.

### 3. `release.json` format 2 with a `base` object

`base` is a new top-level property. Format 1 is closed (`additionalProperties: false`) and its consumers reject unknown `format` values, so a format-1 document with `base` would be rejected. The format therefore becomes `2`. The rules for format 2 are the format-1 rules plus `base` (`specs/content-release-data-jar`).

- Releases published in format 1 stay valid and are never rewritten. The local build of a published format-1 release returns its JSON unchanged (`specs/published-content-releases`).
- Ordering constraint: the plugin must accept format 2 before this generator publishes a format-2 JAR. The plugin's format-2 support is the change `add-local-content-override` (PR #22 in the plugin repository). Otherwise the plugin fails on every new release. This is task 0.3.

### 4. Versions and projects: snapshots are `X.Y.Z-SNAPSHOT`, and every endpoint is keyed by project id

- **Versions.** A release version is `X.Y.Z` and goes to the releases repository. A snapshot version is `X.Y.Z-SNAPSHOT` (Maven's snapshot convention, aligned with the plugin's `allow-snapshot-versions` change) and goes to the snapshots repository. `-dev.N` is no longer accepted anywhere in this change. Publishing a snapshot version again replaces the previous snapshot of that version; the existence check and the `409` apply to release versions only. The snapshots repository must accept such a redeploy, and the releases repository must refuse one. This is checked before the cluster is configured (task 0.4).
- **Project identity.** `GET /projects/{projectId}/local-build`, `GET /projects/{projectId}/releases`, `GET /projects/{projectId}/releases/{version}/local-build` and `POST /generator/release` all identify the project by its backend UUID. The generator resolves the project key (the Maven artifact id) from the backend for the listing and for the published-version local build. An unknown project is `422`, an unreachable backend is `502`, and a key that does not match `^[a-z][a-z0-9-]*$` is `422` with no repository contacted.
- **Listing.** `GET /projects/{projectId}/releases` reads `maven-metadata.xml` of the releases and snapshots repositories with `vulpes.content.maven.username/password`, the same credentials the publisher uses. The latest version per channel is computed from the version numbers, not from the `<latest>` and `<release>` tags, because those follow upload order in Reposilite. Any repository failure is `502`; a `404` for the project in both repositories is an empty list (`specs/published-content-releases`).
- **Published local build.** It downloads the JAR, checks that `sha256(snapshot.json)` equals `revision` in `release.json`, and copies both JSON files unchanged into the zip. The JAR is read into memory; its size is bounded by the publish path (`vulpes.backend.max-snapshot-size`, 64 MiB by default).

### 5. The local build zip is built in memory

The zip holds two entries of at most the snapshot limit plus a small JSON file. It is built in memory from the bytes the request already has, so no temporary zip file is written and nothing needs deleting after the response. The clone and the generated sources are the only files written to disk, and they are deleted in a `finally` block.

### 6. Branch list and shallow clone are kept: `remove-git-and-codegen` is amended

`GET /git/branches` (`GitBranchController`), `GithubService.getBranches()` and the shallow clone of `GitProjectWorker` provide the branch list and the clone that `baseBranch` needs. `remove-git-and-codegen` currently removes them. This change requires that removal to be amended so that it keeps `GET /git/branches`, `GithubClient`, `GithubService.getBranches()`, the shallow-clone part of `GitProjectWorker`, the `github` configuration and JGit. Everything else that change lists stays removed. The amendment is task 0.2 and is made in that change, not here; it must merge before section 3 of `tasks.md` can compile.

Two fixes are needed in the existing branch path. `getBranches()` must answer `502` instead of an empty list when GitHub fails, because an empty list would refuse every branch with `422`. And the GitHub client must send the configured token (`github.password`) as `Authorization`, because the unauthenticated rate limit is too small for an editor.

### 7. Error mapping

Existing `ReleaseRejectedException` reasons are mapped as today. New reasons: `UNKNOWN_BASE_BRANCH` (`422`), `BASE_UNAVAILABLE` (`502`, clone, branch list or time limit), `COMPILATION_FAILED` (`422`, carries the diagnostics), `GENERATION_REFUSED` (`422`) and `PUBLISHED_JAR_CORRUPT` (`502`). `ReleaseErrorResponse` gains an optional `diagnostics` array of `{file, line, message}`; it stays absent for other errors. The error body keeps `reason` and `message`.

### 8. Configuration

| Key | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `vulpes.backend.url` | `VULPES_BACKEND_URL` | `http://localhost:8081` | Backend snapshot and project endpoint. The default is wrong in the cluster. |
| `vulpes.backend.token` | `VULPES_BACKEND_TOKEN` | empty | Bearer token for the backend, if the backend requires one. |
| `vulpes.release.token` | `VULPES_RELEASE_TOKEN` | empty (refuses all) | Guards `POST /releases`. |
| `vulpes.release.editor-route-enabled` | `VULPES_RELEASE_EDITOR_ROUTE_ENABLED` | `false` | Serves `POST /generator/release`. Cluster sets `true`. |
| `vulpes.content.maven.username` / `.password` | `ONELITEFEATHER_MAVEN_USERNAME` / `_PASSWORD` | empty | Reposilite publish, existence check and listing. |
| `vulpes.content.maven.releases-url` / `.snapshots-url` | none | Reposilite URLs | Unchanged. |
| `vulpes.base.repository` | none | `OneLiteFeatherNET/vulpes-base` | Recorded in `base.repository`; the clone URL derives from it. |
| `vulpes.base.cache-dir` | `VULPES_BASE_CACHE_DIR` | `<work>/base-cache` | Built base and runtime classpath per full commit SHA. An `emptyDir` or a PVC. |
| `vulpes.base.cache-entries` | none | `3` | Most recently used commits kept in the cache. |
| `vulpes.base.clone-timeout` / `build-timeout` | none | `2m` / `10m` | Limits for the clone and for a base build. |
| `vulpes.base.maven.username` / `.password` | `VULPES_BASE_MAVEN_USERNAME` / `_PASSWORD` | empty | Read-only Reposilite token for the base build, passed to it as `ORG_GRADLE_PROJECT_OneLiteFeatherRepositoryUsername` / `_Password`. Never the publish credential. |
| `github.password` | `GITHUB_PASSWORD` | empty | Token for the branch list (the rate limit). |
| `vulpes.release.work-dir` | `VULPES_RELEASE_WORK_DIR` | JVM temp dir | Base for request directories. Should be an `emptyDir` of at least 2 GiB. |

The chart gets a `release.editorRouteEnabled` value that renders `VULPES_RELEASE_EDITOR_ROUTE_ENABLED`, a `release.baseCacheVolume` value for the cache and a `release.gradleCacheVolume` value for `GRADLE_USER_HOME`. The Kubernetes-FLUX overlay must set, through secret references: the backend URL (the in-cluster backend Service), the release token, the backend token if used, the Maven username and password, the read-only base build token, `GITHUB_PASSWORD`, the cache volume, the pod memory for cold builds, and `VULPES_RELEASE_EDITOR_ROUTE_ENABLED=true` for the cluster where the editor runs. Production sets none of them today. The change is made in a pull request against Kubernetes-FLUX; no manual `kubectl` change.

### 9. Pod resources and time limits (estimates, to be measured)

- Cold base build: Gradle JVM around 1 GiB heap, roughly 1–3 minutes on a cold Gradle cache and a clone of a few tens of megabytes. Warm requests need only javac, around 512 MiB, plus the snapshot in memory (up to 64 MiB, as the backend limit).
- The Gradle cache (`GRADLE_USER_HOME`) is a persistent volume shared by the base builds, because it is the only part that is slow to refill. The cache directory `vulpes.base.cache-dir` also belongs on a volume, an `emptyDir` or a PVC chosen by the cluster.
- The pod needs a memory limit that covers the Gradle JVM and javac together, estimated at 3 GiB for the cold build, and an `emptyDir` work directory of at least 2 GiB. The first build of a commit is the reason the limit is higher than the warm path needs. These numbers are estimates; task 11.1 measures them with a probe run before the FLUX values are set.
- Base builds are serialized per pod (one at a time). Releases and local builds that hit a warm cache do not wait for the Gradle build of another commit longer than the compile itself, because the cache lookup happens before the lock.
- The cold path can exceed the default ingress and gateway timeouts. The editor route is internal, but the timeouts of the route that serves it must allow the build timeout plus the clone timeout (about 12 minutes).

### 10. Cleanup

Each request creates `<work>/<request-id>` and deletes it in a `finally` block with a recursive delete. A startup sweep removes `vulpes-*` directories older than one hour in the work directory, so that a killed pod does not leak them (the existing `TEMP_PREFIX` is `vulpes-`).

## Risks / Trade-offs

- **[Branch code runs in the pod]** The first build of a commit runs the branch's Gradle build, which can execute arbitrary build code. This is accepted by the product owner. → Mitigations, which are requirements of `specs/base-branch-build`: only branches of `OneLiteFeatherNET/vulpes-base` are accepted; the Gradle process gets a cleared environment (no `ONELITEFEATHER_MAVEN_*`, no `VULPES_*`, no `GITHUB_PASSWORD`) and only a read-only repository token, because Reposilite refuses anonymous reads; the pod runs as non-root with no service account token mounted; network egress is limited to Maven and GitHub.
- **[Codegen not published]** The code pull request cannot build until `publish-codegen-library` is merged. → Task 0.1.
- **[Snapshot redeploy]** Republishing `X.Y.Z-SNAPSHOT` relies on the snapshots repository accepting redeploys, and the `409` for release versions relies on the releases repository refusing them. Neither is verified here. → Task 0.4, before the FLUX values are set.
- **[Concurrent publishes of one version]** The existence check and the upload of a release version are not atomic; two editors publishing the same release version at the same time can both pass the check. → Existing risk of the publish path. The change adds no lock.
- **[Stale base cache]** The cache is keyed by the full commit SHA, so a branch that moves gets a new entry. A commit is immutable in Git, so no invalidation is needed.
- **[Memory]** A local build holds the snapshot and the zip in memory; with the 64 MiB limit, the Gradle JVM of the cold build and javac, the pod needs the memory of section 9.
- **[Backend project key]** The project key for a project id comes from the backend. The endpoint that returns it is confirmed in task 8.2; if the backend lacks it, the backend change is a prerequisite of section 8.

## Migration Plan

1. **Plugin repository (prerequisite, separate change).** `publish-codegen-library` (task 0.1): publish `vulpes-format` and `vulpes-codegen` as Maven artifacts and expose the public codegen facade. The format-2 reader (`add-local-content-override`, PR #22) and the `X.Y.Z-SNAPSHOT` rule (`allow-snapshot-versions`) must be merged before the generator publishes (task 0.3).
2. **Amend `remove-git-and-codegen`** (task 0.2, made in that change). Keep `GET /git/branches`, `GithubClient`, `GithubService.getBranches()`, the shallow-clone part of `GitProjectWorker`, the `github` configuration and JGit; remove only the rest it already lists. This must merge before this change can compile.
3. **Archive `add-content-release-data-jar`.** The MODIFIED delta of this change applies only to the archived `content-release-data-jar` spec.
4. **Verify the Reposilite policies** (task 0.4): snapshot redeploy accepted, release redeploy refused.
5. **Code PR** `feat(release): build and publish content releases from the editor against a vulpes-base branch`, which removes `/download` and `/generator/generate` and adds the new endpoints.
6. **Chart and Kubernetes-FLUX PR**: the values of section 8, including the editor route flag, the cache volume, and the memory of section 9.
7. **Stelaris** switches to `local-build` and `POST /generator/release` in its own change. Until it does, the editor page is broken for the removed endpoints; this is accepted as the product owner's breaking change.

Rollback: switch `VULPES_RELEASE_EDITOR_ROUTE_ENABLED` to `false` to stop releases from the editor at once. The removed download endpoints come back only by redeploying the previous image, so the editor must not be switched to the new endpoints before step 5 is in production.

## Open Questions

None. The product owner has decided codegen publication (Decision 2), the base build of branch code (Decision 1), the snapshot version rule (Decision 4), the project identity of every endpoint (Decision 4), the full commit SHA (`specs/base-branch-build`), the format-2 ordering (Decision 3) and the amendment of `remove-git-and-codegen` (Decision 6). Verification items that remain before the cluster is configured are tasks 0.4, 4.6 and 11.1.
