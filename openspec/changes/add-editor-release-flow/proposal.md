# Proposal

**Conventional Commits:** `feat(release): build and publish content releases from the editor against a vulpes-base branch`

## Why

The Stelaris editor has a generator page for a Vulpes project, and it cannot finish its job. Its download hands out a zip of a `vulpes-base` clone with legacy generated Java and a `.git` folder, which the Vulpes Gradle plugin cannot use. Its build call (`/generator/generate`) is a stub that answers success without building anything. The backend already builds a real content data JAR in `POST /releases`, but the editor never calls it, and once a version is published there is no list of versions the editor can show, because Reposilite requires credentials the editor does not have. An editor user therefore cannot build a project for local testing, cannot publish it, and cannot see what was published.

The content data JAR replaces the generated sources, so the editor's output has to be the release data that the Gradle plugin reads: `release.json` and `snapshot.json`. Before a project is handed out, the snapshot has to be proven to work with a real `vulpes-base` branch, because a snapshot that does not compile against the base is a broken release. This change makes the editor's generator page work end to end for the Gradle plugin: build locally, release, and list what was released.

## What Changes

- **Base branch.** Every local build and every release names a `baseBranch` of `vulpes-base` (default `develop`, must be a branch that exists). The generator clones that branch shallowly, records the commit it resolved, generates the Java sources for the project's snapshot with the code generator of the Vulpes Gradle plugin, and compiles them against the checkout. A compile failure stops the request with `422` and the compiler diagnostics (file, line, message). Nothing is published.
- **Release metadata.** `release.json` records the base as `base: {repository, branch, commit}`. Because the format-1 contract is closed and its readers reject unknown format values, the format becomes `2`. See `design.md`, decision 6.
- **Local build download.** Add `GET /projects/{projectId}/local-build?baseBranch=...`, which returns `application/zip` named `<project>-local.zip` with exactly `vulpes/<project>/release.json` and `vulpes/<project>/snapshot.json`. The version in `release.json` is `0.0.0-local`. The zip contains no `.git` and no sources.
- **Editor release.** `POST /generator/release` takes `{projectId, version, baseBranch}`. A version `X.Y.Z` goes to the releases repository, `X.Y.Z-dev.N` to the snapshots repository. The response carries the coordinates, the revision, the base commit and the repository URL. An existing version answers `409`. The route stays switched by configuration in the cluster.
- **Published versions.** Add `GET /projects/{projectKey}/releases`, which lists every published version of both repositories with its channel, Maven coordinates, repository URL and JAR URL, plus the latest version per channel. Add `GET /projects/{projectKey}/releases/{version}/local-build`, which returns the same zip layout built from the JSON files of the published JAR. The generator reads Reposilite with its configured credentials. When a repository is unavailable the answer is `502`, never an empty list.
- **Cleanup.** Clone and build directories are deleted after each request, also when the request fails. Today they leak.
- **BREAKING** `GET /download` and `GET /generator/generate` are removed. Only the Stelaris generator page used them. Stelaris must switch to `local-build` and `POST /generator/release`.
- **Deployment.** The Kubernetes-FLUX values for the generator must set the backend URL, the Reposilite credentials and the editor route flag. Today production sets none of them, so publishing fails with `401` and the backend URL falls back to `localhost`.

## Capabilities

### New Capabilities
- `base-branch-build`: Verifies a project against a `vulpes-base` branch: branch selection, shallow clone, commit resolution, code generation, compilation against the checkout, diagnostics, cache per commit and cleanup of temporary directories.
- `local-build-download`: The zip a developer downloads to run a project locally, and the removal of the legacy download endpoints.
- `editor-release`: Starting a content release from the editor with a base branch, its version rules, its responses and its configuration switch.
- `published-content-releases`: Listing the versions Reposilite holds for a project and rebuilding a local zip from a published version.

### Modified Capabilities
- `content-release-data-jar`: Release metadata gains the `base` object and the format moves from `1` to `2`. The requirement text is taken from the change `add-content-release-data-jar`, which is not archived yet; this change applies after it.

## Non-goals

- No Stelaris changes. The editor switches its calls in its own change; this change only removes the endpoints that Stelaris will stop using.
- No resource packs. `packs` stays `[]`; the pack build is a separate change.
- No job queue, no SSE status and no asynchronous builds. Requests are synchronous.
- No backend `Release` object and no frozen or published state in the generator.
- No change to the snapshot format or to the release JSON schema of the Gradle plugin repository. The schema change is a prerequisite that lives there (`design.md`, Migration Plan).
- No publication of the generated Java sources. The generated sources are only compiled as a gate; the zip and the data JAR carry the snapshot.
- No editor login. Authorisation of the editor route stays the cluster boundary, as today.
- No pagination of the branch list beyond what the GitHub client returns today.

## Impact

- **Code**: new base build workflow with its cache and workspace handling; `local-build` and release-list controllers; extended `EditorReleaseRequest`, `ReleaseRequest` and `ReleaseResponse`; `ReleaseJsonWriter` writes format 2; the removal of `CodeGenerationHandler` and `VulpesDownloadController`.
- **Dependencies**: the code generator of the Vulpes Gradle plugin (`vulpes-codegen` and its format module) is needed as a library. It is not published yet; see `design.md`, Open Questions. The Gradle runtime and a JDK 25 for building `vulpes-base` must be present in the image.
- **API**: `GET /projects/{projectId}/local-build`, `GET /projects/{projectKey}/releases`, `GET /projects/{projectKey}/releases/{version}/local-build` are added. `POST /generator/release` gains `baseBranch` and an extended response. `GET /download` and `GET /generator/generate` are removed. OpenAPI changes accordingly.
- **Configuration**: `vulpes.base.*` keys for the clone URL, cache, timeouts and concurrency; `vulpes.release.editor-route-enabled` already exists; the Reposilite read credentials reuse `vulpes.content.maven.*`.
- **Deployment**: Helm values gain the editor route flag and the base cache volume. FLUX values must set the backend URL, the backend token, the release token and the Maven credentials as secret references.
- **Ordering**: this change runs after `add-content-release-data-jar` and after `remove-git-and-codegen`, and it amends that removal (`design.md`, Migration Plan).
