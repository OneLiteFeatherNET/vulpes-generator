# Design

## Context

See `proposal.md` for the editor's problem and `specs/` for the behaviour. This section describes what the code does today and what the design relies on.

- `POST /generator/release` (`EditorReleaseController`) fetches the snapshot from the backend (`BackendApiReleaseSnapshotSource`), computes its revision and publishes through `ReleaseService.releaseSnapshot`. It takes no base branch and compiles nothing. The creator is fixed to `stelaris`.
- `ReleaseService` refuses an existing version before the snapshot is fetched, but `EditorReleaseService` fetches the snapshot first and only then calls the refusal. The order required by `editor-release` (refusal before any clone or compile) needs the check moved up into `EditorReleaseService`.
- `DataJarBuilder` writes `META-INF/MANIFEST.MF`, `META-INF/vulpes/release.json` and `META-INF/vulpes/snapshot.json`. `ReleaseJsonWriter` writes `release.json` by hand with sorted keys and format `1`.
- `MavenContentPublisher` checks existence with a `HEAD` of the POM and deploys JAR, POM and `.module` through Maven Resolver. It already sends the configured credentials; it has no read path for listings.
- `VulpesDownloadController` clones `vulpes-base` into a temporary folder with `GitProjectWorker` and zips the whole folder, `.git` included. `CodeGenerationHandler` is a stub. Neither removes its temporary folder.
- `GithubService.getBranches()` returns an empty list when GitHub answers anything but `200`. `GithubClient` sends only a `User-Agent` header, so the branch list is unauthenticated (GitHub allows 60 unauthenticated requests per hour per address).
- The Vulpes Gradle plugin repository (`OneLiteFeatherNET/vulpes-gradle-plugin`) holds the code generator in `vulpes-codegen` (depends on `vulpes-format`). Its root build bundles `vulpes-codegen` into the plugin's shadow JAR, and the published POM does not list it. `vulpes-codegen` generates Java against the API of `vulpes-base` (`net.onelitefeather.vulpes`), and `vulpes-base` builds with JDK 25 and compiles against `minestom` (compileOnly), `mycelium` and `evalex`.
- `release-format 1` is defined by `vulpes-format/src/main/resources/net/onelitefeather/vulpes/format/release-1.schema.json` in the plugin repository. Its top level has `additionalProperties: false` and `format` is a `const` of `1`.
- `charts/vulpes-generator/templates/deployment.yaml` already injects `VULPES_BACKEND_URL`, `VULPES_RELEASE_TOKEN`, `VULPES_BACKEND_TOKEN`, `ONELITEFEATHER_MAVEN_USERNAME` and `ONELITEFEATHER_MAVEN_PASSWORD` when the overlay names secrets, and `VULPES_RELEASE_WORK_DIR` when it sets a work directory. The chart has no key for `VULPES_RELEASE_EDITOR_ROUTE_ENABLED`. Production sets none of these values (product owner), so the backend URL falls back to `http://localhost:8081` and the Maven upload fails with `401`.

## Goals / Non-Goals

**Goals**
- One request path for local builds and releases, each verified against a named `vulpes-base` branch.
- No request leaves a clone, a build directory or a zip behind.
- Errors carry a status code that tells the editor what to fix: `400` input, `404` disabled route, `409` existing version, `422` content or compilation, `502` infrastructure.

**Non-Goals**
- No change to the snapshot format and no change to the codegen output. The generator consumes the codegen as a library and does not fork it.
- No sandbox beyond the pod. See Risks.
- No compile of the generated code into the data JAR. It is only a gate.

## Decisions

### 1. Where the compilation runs: in-process `javax.tools`, classpath from the base build once per commit

The compile gate needs the classes of the selected `vulpes-base` checkout and its compile classpath (`minestom`, `mycelium` BOM, `evalex`, `slf4j`). Three options were evaluated.

- **A. In-process `javax.tools` against a cached base classpath (chosen).** Per commit, once: run the clone's Gradle build (`compileJava` plus a printed compile classpath through an init script owned by the generator). Store the compiled base classes and the classpath under `<cache>/<commit>/`. Per request: write the generated sources into the request directory and call `ToolProvider.getSystemJavaCompiler()` with a `DiagnosticCollector`, `--release 25`, the cached classpath and `-d` into the request directory. Diagnostics arrive as structured objects, so `file`, `line` and `message` come straight from the API. A warm request needs no Gradle and no daemon, so it takes seconds.
- **B. Run the clone's Gradle build per request** (put the generated sources into `src/main/java` and run `compileJava`). This is the plugin's own build and needs no classpath work. It costs a cold Gradle start for every request (tens of seconds even warm, minutes cold), a Gradle JVM of around one gigabyte per request, and diagnostics that have to be parsed from Gradle's text output. It rejects concurrent requests in practice.
- **C. Resolve the classpath statically** from the branch's `gradle/libs.versions.toml` with Maven Resolver, without running Gradle. No code from the branch runs. The generator then has to repeat the dependency wiring of `vulpes-base` (its `compileOnly` and BOM rules) and it drifts whenever the base build changes.

A is the only option with structured diagnostics, a fast warm path and a bounded memory cost. Its weak point is that the first build of a commit executes the branch's Gradle build script (see Risks). Option C is the fallback if that risk is not accepted.

Per-request flow (once the classpath is cached):
1. Clone `vulpes-base` at the branch with depth one into `<work>/<request>/clone`.
2. Resolve `HEAD`; use the cached classes when `<cache>/<commit>/` exists, otherwise build them in the clone and move the result into the cache (one base build at a time per pod).
3. Assemble the snapshot from the backend (existing `BackendApiReleaseSnapshotSource`).
4. Generate the sources with the codegen facade into `<work>/<request>/generated`.
5. Compile with `javax.tools` into `<work>/<request>/classes`. Diagnostics above 100 are cut.
6. On success build `release.json` (format 2) and the zip or the data JAR. Then delete `<work>/<request>`.

The clone is deleted at the end of every request, also on failure (`specs/base-branch-build`, Workspace cleanup). Only compiled classes and the classpath live in the cache; the cache holds no clone and no source.

### 2. Consuming the code generator: a published library dependency that does not exist yet

The generator consumes `vulpes-codegen` and `vulpes-format` as Maven libraries, pinned in its version catalog. Today both are not published as their own artifacts: `vulpes-codegen` is only bundled into the plugin's shadow JAR, and `ContentGenerator` is package-private, so the generator has no public entry point.

Decision: the code PR depends on a prerequisite change in `vulpes-gradle-plugin` that publishes `vulpes-format` and `vulpes-codegen` as `net.onelitefeather.vulpes:<artifact>` and exposes one public facade that takes a parsed snapshot, a package name and a project name, and returns generated sources. Without that prerequisite, the code PR cannot be built. This is an open question below, not a guess in the code.

Rejected: depending on the plugin's shadow JAR (it contains the Gradle API glue and a second copy of `vulpes-format`) and copying the codegen sources into this repository (the contract says the plugin owns the generator, and this repository never keeps a copy).

### 3. `release.json` format 2 with a `base` object

`base` is a new top-level property. Format 1 is closed (`additionalProperties: false`) and its consumers reject unknown `format` values, so a format-1 document with `base` would be rejected. The format therefore becomes `2`. The rules for format 2 are the format-1 rules plus `base` (`specs/content-release-data-jar`).

- Releases published in format 1 stay valid and are never rewritten. The local build of a published format-1 release returns its JSON unchanged (`specs/published-content-releases`).
- The plugin must ship a format-2 reader before this generator publishes format 2. Otherwise the plugin fails on every new release. The prerequisite is part of Migration Plan step 1.

### 4. Release listing reads Reposilite with the generator's credentials

`GET /projects/{projectKey}/releases` reads `maven-metadata.xml` of the releases and snapshots repositories with `vulpes.content.maven.username/password`, the same credentials the publisher uses. It does not call the backend. The latest version per channel is computed from the version numbers, not from the `<latest>` and `<release>` tags, because those follow upload order in Reposilite. Any repository failure is `502`; a `404` for the project in both repositories is an empty list (`specs/published-content-releases`).

The local build of a published version downloads the JAR, checks that `sha256(snapshot.json)` equals `revision` in `release.json`, and then copies both JSON files unchanged into the zip. The JAR is read into memory; its size is bounded by the publish path (`vulpes.backend.max-snapshot-size`, 64 MiB by default).

### 5. The local build zip is built in memory

The zip holds two entries of at most the snapshot limit plus a small JSON file. It is built in memory from the bytes the request already has, so no temporary zip file is written and nothing needs deleting after the response. The clone and the generated sources are the only files written to disk, and they are deleted in a `finally` block.

### 6. Branch listing and clone use the existing GitHub and JGit code, which the other change removes

`GET /git/branches` (`GitBranchController`) and `GithubService.getBranches()` provide the list that `baseBranch` must be part of. The clone uses `GitProjectWorker.cloneBaseRepo`. Both are scheduled for removal by `remove-git-and-codegen`. This change therefore needs that removal amended to keep `GithubService.getBranches()`, `GitProjectWorker` (shallow clone only) and the `GET /git/branches` endpoint, and the amended removal must land before this change. See Migration Plan and Open Questions.

Two fixes are needed in the existing branch path. `getBranches()` must answer `502` instead of an empty list when GitHub fails, because an empty list would refuse every branch with `422`. And the GitHub client must send the configured token (`github.password`) as `Authorization`, because the unauthenticated rate limit is too small for an editor.

### 7. Error mapping

Existing `ReleaseRejectedException` reasons are mapped as today. New reasons: `UNKNOWN_BASE_BRANCH` (`422`), `BASE_UNAVAILABLE` (`502`, clone, branch list or time limit), `COMPILATION_FAILED` (`422`, carries the diagnostics), `GENERATION_REFUSED` (`422`) and `PUBLISHED_JAR_CORRUPT` (`502`). `ReleaseErrorResponse` gains an optional `diagnostics` array of `{file, line, message}`; it stays absent for other errors. The error body keeps `reason` and `message`.

### 8. Configuration

| Key | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `vulpes.backend.url` | `VULPES_BACKEND_URL` | `http://localhost:8081` | Backend snapshot endpoint. The default is wrong in the cluster. |
| `vulpes.backend.token` | `VULPES_BACKEND_TOKEN` | empty | Bearer token for the backend, if the backend requires one. |
| `vulpes.release.token` | `VULPES_RELEASE_TOKEN` | empty (refuses all) | Guards `POST /releases`. |
| `vulpes.release.editor-route-enabled` | `VULPES_RELEASE_EDITOR_ROUTE_ENABLED` | `false` | Serves `POST /generator/release`. Cluster sets `true`. |
| `vulpes.content.maven.username` / `.password` | `ONELITEFEATHER_MAVEN_USERNAME` / `_PASSWORD` | empty | Reposilite publish, existence check and listing. |
| `vulpes.content.maven.releases-url` / `.snapshots-url` | none | Reposilite URLs | Unchanged. |
| `vulpes.base.repository` | none | `OneLiteFeatherNET/vulpes-base` | Recorded in `base.repository`; the clone URL derives from it. |
| `vulpes.base.cache-dir` | `VULPES_BASE_CACHE_DIR` | `<work>/base-cache` | Compiled base classes per commit. Should sit on a volume. |
| `vulpes.base.cache-entries` | none | `3` | Commits kept in the cache. |
| `vulpes.base.clone-timeout` / `build-timeout` | none | `2m` / `10m` | Limits for the clone and for a cold base build. |
| `github.password` | `GITHUB_PASSWORD` | empty | Token for the branch list (the rate limit). |
| `vulpes.release.work-dir` | `VULPES_RELEASE_WORK_DIR` | JVM temp dir | Base for request directories. Should be an `emptyDir` of at least 2 GiB. |

The chart gets a `release.editorRouteEnabled` value that renders `VULPES_RELEASE_EDITOR_ROUTE_ENABLED`, and a `release.baseCacheVolume` value for the cache. The Kubernetes-FLUX overlay must set, through secret references: the backend URL (the in-cluster backend Service), the release token, the backend token if used, the Maven username and password, `GITHUB_PASSWORD`, and `VULPES_RELEASE_EDITOR_ROUTE_ENABLED=true` for the cluster where the editor runs. Production sets none of them today. The change is made in a pull request against Kubernetes-FLUX; no manual `kubectl` change.

### 9. Pod resources and time limits (estimates to be measured)

- Cold base build: Gradle JVM around 1 GiB heap, roughly 1–3 minutes on a cold Gradle cache and a clone of a few tens of megabytes. Warm requests need only javac, around 512 MiB, plus the snapshot in memory (up to 64 MiB, as the backend limit).
- The Gradle cache (`GRADLE_USER_HOME`) is a persistent volume shared by the base builds, because it is the only part that is slow to refill. The cache directory `vulpes.base.cache-dir` also belongs on a volume.
- The pod needs a memory limit that covers the Gradle JVM and javac together, estimated at 3 GiB, and an `emptyDir` work directory of at least 2 GiB. These numbers are estimates; the task list measures them with a probe run before the FLUX values are set.
- Base builds are serialized per pod (one at a time). Releases and local builds that hit a warm cache do not wait for the Gradle build of another commit longer than the compile itself, because the cache lookup happens before the lock.
- The cold path can exceed the default ingress and gateway timeouts. The editor route is internal, but the timeouts of the route that serves it must allow the build timeout plus the clone timeout (about 12 minutes).

### 10. Cleanup

Each request creates `<work>/<request-id>` and deletes it in a `finally` block with a recursive delete. A startup sweep removes `vulpes-*` directories older than one hour in the work directory, so that a killed pod does not leak them (the existing `TEMP_PREFIX` is `vulpes-`).

## Risks / Trade-offs

- **[Branch code runs in the pod]** The first build of a commit runs the branch's Gradle build, which can execute arbitrary build code. → Mitigations: only branches of `OneLiteFeatherNET/vulpes-base` are accepted; the Gradle process gets a cleared environment (no `ONELITEFEATHER_MAVEN_*`, no `VULPES_*`, no `GITHUB_PASSWORD`); the pod runs as non-root with no service account token mounted; network egress is limited to Maven and GitHub. Open question 2 asks whether this is acceptable or whether option C is used instead.
- **[Codegen not published]** The code PR cannot build until the plugin publishes `vulpes-codegen`. → Prerequisite change in the plugin repository, tracked as Migration Plan step 1.
- **[`-dev.N` versus `-SNAPSHOT`]** The plugin's pending change `allow-snapshot-versions` makes the release `version` of a snapshot `X.Y.Z-SNAPSHOT` and drops `-dev.N` from format 1. This generator publishes `X.Y.Z-dev.N`. → Open question 3; the version rule is kept as the product owner defined it until it is decided.
- **[Concurrent publishes of one version]** The existence check and the upload are not atomic; two editors publishing the same version at the same time can both pass the check. → Existing risk of the publish path. Whether Reposilite refuses redeploys of an existing version is not verified here and must be checked for the release repositories before the FLUX values are set. The change adds no lock.
- **[Stale base cache]** The cache is keyed by commit, so a branch that moves gets a new entry. A reused commit is immutable in Git, so no invalidation is needed.
- **[Memory]** A local build holds the snapshot and the zip in memory; with the 64 MiB limit and the Gradle JVM the pod needs the memory of section 9.

## Migration Plan

1. **Plugin repository (prerequisite, separate change).** Publish `vulpes-format` and `vulpes-codegen` as Maven artifacts; expose the public codegen facade; add the format-2 schema and reader with `base`; decide the snapshot version rule (open question 3).
2. **Amend `remove-git-and-codegen`.** Keep `GithubService.getBranches()`, the shallow-clone part of `GitProjectWorker` and `GET /git/branches`; remove only the generation, the database and the other endpoints it already lists. This must land before this change, or this change cannot compile.
3. **Archive `add-content-release-data-jar`.** The MODIFIED delta of this change applies only to the archived `content-release-data-jar` spec.
4. **Code PR** `feat(release): build and publish content releases from the editor against a vulpes-base branch`, which removes `/download` and `/generator/generate` and adds the new endpoints.
5. **Chart and Kubernetes-FLUX PR**: the values of section 8, including the editor route flag, and the volumes and memory of section 9.
6. **Stelaris** switches to `local-build` and `POST /generator/release` in its own change. Until it does, the editor page is broken for the removed endpoints; this is accepted as the product owner's breaking change.

Rollback: switch `VULPES_RELEASE_EDITOR_ROUTE_ENABLED` to `false` to stop releases from the editor at once. The removed download endpoints come back only by redeploying the previous image, so the editor must not be switched to the new endpoints before step 4 is in production.

## Open Questions

These must be answered before the code PR is opened. They do not change the proposal's requirements, but each one changes an approach or a dependency.

1. **Codegen publication.** Will the plugin repository publish `vulpes-format` and `vulpes-codegen` as separate Maven artifacts and expose a public facade? If not, the generator cannot compile the sources and the code PR is blocked.
2. **Executing branch build code.** Is it acceptable that the first build of a commit runs the branch's Gradle build inside the generator pod (section 1, option A), with the mitigations in Risks? If not, option C (static classpath) is used, and the compile classpath must be kept in step with the base build by hand.
3. **Version rule.** The plugin's `allow-snapshot-versions` change replaces `X.Y.Z-dev.N` with `X.Y.Z-SNAPSHOT` in `release.json`. Should this generator publish snapshots as `-SNAPSHOT` (Maven version) and drop `-dev.N`, or keep `-dev.N` as the product owner specified? The plugin's format-1 schema rejects `-dev.N` once that change lands.
4. **Format 2 and the plugin.** Does the plugin accept format 2 with `base` before this generator publishes? This is the same prerequisite as question 1.
5. **Legacy endpoint owner.** `remove-git-and-codegen` removes `GET /git/branches`, which this flow needs. Is it acceptable to amend that change as described in Migration Plan step 2, or should the branch list move to a new endpoint in this change?
6. **Project identifier.** The local build and the editor release identify the project by `projectId` (a backend UUID), while the release listing identifies it by `projectKey` (the Maven artifact id). The product owner specified both. Should the listing also accept `projectId` by resolving the key through the backend, or does the editor always know the key?
