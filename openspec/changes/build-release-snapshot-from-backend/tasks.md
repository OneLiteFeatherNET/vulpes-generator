# Tasks

Test notes: unit tests with an in-process fake backend (no network, no sleeps, injected `Clock`, `@TempDir`). The assembled snapshot is validated against the schema from the Gradle plugin repository.

## 1. Snapshot assembly

- [ ] 1.1 Write the failing tests for paging, key namespaces (project and foreign) and the canonical encoding
- [ ] 1.2 Write the failing tests for the item, attribute, font, sound, expression function, expression and asset mapping
- [ ] 1.3 Write the failing tests for the double assembly: identical runs continue, three differing pairs answer `409` and publish nothing
- [ ] 1.4 Write the failing tests that a non-bitmap font and an advancement reject the release with `422`
- [ ] 1.5 Implement the adapter that reads the backend endpoints, with the double assembly, and replace the adapter for `GET /project/{projectId}/snapshot`
- [ ] 1.6 Add `vulpes.release.foreign-namespaces` with the default `minecraft`, `olf`

## 2. Release from the editor

- [ ] 2.1 Write the failing controller tests for `POST /generator/release`: `201` with the published coordinates, `409` for an existing version, `422` for an invalid version, `404` when `vulpes.release.editor-route-enabled` is off, no token needed when on
- [ ] 2.2 Implement the route on top of the existing release service

## 3. Automated releases

- [ ] 3.1 Write the failing test that `POST /releases` with a revision that differs from the assembled snapshot answers `422`
- [ ] 3.2 Adapt the revision check

## 4. Pull request

- [ ] 4.1 Run `./gradlew build`
- [ ] 4.2 Open the code pull request `feat(release): build the release snapshot from the backend api`
