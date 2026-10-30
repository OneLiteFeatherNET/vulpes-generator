# Tasks

Test notes for all sections:
- Unit tests build minimal format-1 snapshot bytes in the test itself and use JUnit `@TempDir`. The contract fixtures of the plugin repository are not copied into this repository.
- The integration test uses `@MicronautTest` with an in-process fake backend and a fake Maven repository, both as controllers. No real network.
- The byte-identical JAR test builds the same release twice and compares the bytes.
- No `Thread.sleep` and no system clock: `createdAt` comes from the request, the release path reads no clock at all.

## 1. Request validation

- [ ] 1.1 Write the failing unit tests for the release request: a valid body is accepted, `version` with a leading zero (`1.02.0`, `1.0.0-dev.01`) is rejected, `project` `Eldoria` is rejected, `revision` without the `sha256:` prefix is rejected
- [ ] 1.2 Implement the request record and its validation until the tests pass; invalid input maps to `400` without any collaborator being called

## 2. Snapshot source and revision check

- [ ] 2.1 Write the failing unit tests for `ReleaseService` with a fake `ReleaseSnapshotSource`: a matching SHA-256 passes, a mismatching digest gives `422`, `format` `2` gives `422`, a foreign `projectId` gives `422`, a backend `404` for the project gives `422`
- [ ] 2.2 Implement the `ReleaseSnapshotSource` port and the digest and format checks until the tests pass
- [ ] 2.3 Write the failing test for the HTTP adapter: it requests `GET {vulpes.backend.url}/project/{projectId}/snapshot` and sends the bearer header only when `vulpes.backend.token` is set (fake backend controller)
- [ ] 2.4 Implement the HTTP adapter; a connection failure maps to `502`
- [ ] 2.5 Write the failing test that the snapshot bytes are kept unchanged and that a backend connection error publishes nothing
- [ ] 2.6 Implement the byte handling so the received bytes reach the JAR builder untouched

## 3. release.json

- [ ] 3.1 Write the failing unit test for the content of `release.json`: format `1`, `packs` empty, `provenance.createdAt` taken from the request, `provenance.generator` from the generator version
- [ ] 3.2 Implement the release metadata writer with sorted keys, no insignificant whitespace and UTF-8 until the tests pass

## 4. Reproducible JAR

- [ ] 4.1 Write the failing test for the entry layout: exactly `META-INF/MANIFEST.MF`, `META-INF/vulpes/release.json`, `META-INF/vulpes/snapshot.json` in this order, `MANIFEST.MF` containing only `Manifest-Version: 1.0`
- [ ] 4.2 Implement the JAR builder with a fixed entry timestamp and no extra fields until the tests pass
- [ ] 4.3 Write the failing test that building the same request and snapshot twice yields byte-identical JARs
- [ ] 4.4 Implement the fixes needed for byte identity until the test passes
- [ ] 4.5 Write the failing tests that the temporary build directory is fresh, empty before the build and deleted after success and after a failing build
- [ ] 4.6 Implement the build in a `@TempDir`-style directory with deletion in a `finally` block until the tests pass

## 5. Publishing: routing and 409

- [ ] 5.1 Write the failing test that `1.2.0` is routed to the releases repository and `1.2.0-dev.3` to the snapshots repository (fake Maven repository controller)
- [ ] 5.2 Implement the Maven Resolver publisher and the routing with the configurable URLs until the test passes
- [ ] 5.3 Write the failing test that the published set is the JAR, the POM without dependencies, the `.module` file and the md5, sha1, sha256 and sha512 checksums
- [ ] 5.4 Implement the artifact set until the test passes
- [ ] 5.5 Write the failing test that an existing version gives `409` and that nothing is uploaded
- [ ] 5.6 Implement the existence check before upload until the test passes
- [ ] 5.7 Write the failing test that the publisher authenticates with the configured `vulpes.content.maven.username` and `vulpes.content.maven.password` (set as test properties, no real environment variables)
- [ ] 5.8 Bind the credentials to these keys with the defaults `${ONELITEFEATHER_MAVEN_USERNAME}` and `${ONELITEFEATHER_MAVEN_PASSWORD}` until the test passes

## 6. Endpoint and integration test

- [ ] 6.1 Write the failing integration test for `POST /releases`: `201` with coordinates and revision, `422` on a revision mismatch, `400` on invalid input, `502` when the fake backend is down, `409` on a repeated version
- [ ] 6.2 Implement `ReleaseController` as a thin layer over `ReleaseService` until the test passes
- [ ] 6.3 Write the failing test that `ReleaseCompletionListener` is called exactly once after a successful publish and not after a rejected one
- [ ] 6.4 Implement the logging listener and the call after publish until the test passes
- [ ] 6.5 Add the configuration keys `vulpes.backend.url`, `vulpes.backend.token`, `vulpes.content.maven.username`, `vulpes.content.maven.password`, `vulpes.content.maven.releases-url` and `vulpes.content.maven.snapshots-url` to `application.yml`, and the Helm values for them

## 7. Authentication

- [ ] 7.1 Write the failing integration tests for `POST /releases`: a missing, wrong, Basic-scheme or unconfigured `vulpes.release.token` answers `401` without contacting the fake backend or the fake Maven repository, and `/health` stays open without a token
- [ ] 7.2 Implement the server filter for `/releases` with constant-time token comparison and fail-closed behaviour (an empty token answers `401`) until the tests pass
- [ ] 7.3 Add the OpenAPI bearer security scheme for `POST /releases` and the Helm secret reference for `vulpes.release.token`

## 8. Verification and pull request

- [ ] 8.1 Run `./gradlew test --max-workers=2` and `./gradlew build --max-workers=2`, and fix all failures
- [ ] 8.2 Open the pull request titled `feat(release): publish content data jar to reposilite`
