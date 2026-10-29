# Proposal

**Conventional Commits:** `feat(release): publish content data jar to reposilite`

## Why

Today the content of a Vulpes project reaches Minecraft through a chain that cannot be reproduced later: `vulpes-generator` reads the live Postgres database, emits Java sources with JavaPoet into a clone of `vulpes-base` and returns a zip from `GET /download`. Nothing in that chain pins a release to a revision, and a release cannot be rebuilt byte for byte.

The design "2026-10-28 — Vulpes: Content-Releases ohne Git (Design)" (https://outline.onelitefeather.dev/doc/2026-10-28-vulpes-content-releases-ohne-git-design-7oIaSsfvbz) moves content into a versioned data JAR. The JAR is built from one frozen snapshot of the backend, never from the database, and is published to Reposilite. This change adds the first half of that path: a synchronous endpoint that turns a snapshot into a published, reproducible JAR.

The snapshot format and the release format are owned by the Gradle plugin repository and are only linked, never copied: https://github.com/OneLiteFeatherNET/vulpes-gradle-plugin/pull/1 (`contract/snapshot-1.schema.json`, `contract/release-1.schema.json` and the fixtures under `contract/fixtures/`).

## What Changes

- Add `POST /releases`. The body carries `projectId` (UUID), `project` (Maven artifact id), `version` (`X.Y.Z` or `X.Y.Z-dev.N`), `revision` (`sha256:<64 hex>`), `createdBy` and `createdAt` (ISO-8601, UTC). Invalid input is rejected with `400`.
- The release input is only the frozen snapshot. A `ReleaseSnapshotSource` port fetches it; the first adapter reads `GET {vulpes.backend.url}/project/{projectId}/snapshot`. The generator never reads the database.
- The generator checks the snapshot before it builds anything: the SHA-256 of the received bytes must equal `revision`, the snapshot `format` must be `1` and its `projectId` must match the request. Otherwise the release is rejected with `422`.
- The snapshot bytes are stored unchanged as `META-INF/vulpes/snapshot.json`. They are never re-serialised.
- `META-INF/vulpes/release.json` follows release format 1: `format`, `project`, `projectId`, `version`, `revision`, `packs` (always `[]` in this change) and `provenance` (`generator` is the generator's own version; `createdBy` and `createdAt` come from the request, never from the generator's clock). UTF-8, sorted object keys, no insignificant whitespace.
- The JAR is reproducible: `META-INF/MANIFEST.MF` contains only `Manifest-Version: 1.0`, the entries are in a fixed order with a fixed timestamp and no extra fields. The same request and snapshot yield byte-identical JARs.
- Every build runs in a fresh, empty temporary directory that is deleted afterwards, also on failure.
- The JAR is published with Maven Resolver as `net.onelitefeather.vulpes.content:<project>:<version>` together with a POM without dependencies, the Gradle `.module` metadata and checksums (md5, sha1, sha256, sha512). `X.Y.Z` goes to `https://repo.onelitefeather.dev/onelitefeather-releases`, `X.Y.Z-dev.N` to `https://repo.onelitefeather.dev/onelitefeather-snapshots`. Credentials come from `ONELITEFEATHER_MAVEN_USERNAME` and `ONELITEFEATHER_MAVEN_PASSWORD`; both repository URLs are configurable.
- A version that already exists in the target repository is never overwritten: the endpoint answers `409` and uploads nothing.
- A project unknown to the backend (`404`) is rejected with `422`.
- A successful release answers `201` with the coordinates and the revision. An unreachable backend or repository answers `502` and publishes nothing.
- A `ReleaseCompletionListener` port is called after a successful publish. For now it only logs. Later it reports to the backend, which then marks the release as published.

## Capabilities

### New Capabilities
- `content-release-data-jar`: Synchronous release of one frozen project snapshot as a reproducible content data JAR, published to Reposilite.

### Modified Capabilities
<!-- None: no existing endpoint, DTO or configuration changes behaviour in this change. -->

## Non-goals

- No resource packs and no S3. The `packs` array stays empty until the packs change.
- No downscaling of assets.
- No backend `Release` object. `POST /releases` is the narrow interface that the backend Release object (frozen and published states, not built yet) will later call or replace. The build is synchronous.
- No job queue and no SSE status.
- No JSON-schema validation of the whole snapshot. This is a follow-up once the plugin publishes its `vulpes-format` module.
- No Stelaris changes.
- No removal of existing code. That is the separate change `remove-git-and-codegen`.

## Impact

- **Code**: new `ReleaseController`, `ReleaseService`, the ports `ReleaseSnapshotSource` and `ReleaseCompletionListener`, an HTTP adapter for the backend snapshot, a logging listener and a Maven Resolver publisher with repository routing.
- **Dependencies**: Maven Resolver is added. The backend must serve snapshot format 1 with a byte-identical revision. Until then only the fixtures exercise the path.
- **Configuration**: new keys `vulpes.backend.url`, `vulpes.backend.token` (optional bearer token), `vulpes.content.maven.username`, `vulpes.content.maven.password`, `vulpes.content.maven.releases-url` and `vulpes.content.maven.snapshots-url`. The Maven credentials default to the environment variables above.
- **Deployment**: Helm values gain the backend URL, the optional token and the Maven credentials as secret references.
- **API**: one new endpoint, `POST /releases`. OpenAPI gains one operation.
- **Unchanged**: the existing endpoints and the database access stay as they are until `remove-git-and-codegen` lands.
