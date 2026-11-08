# Tasks

Test notes: unit tests with an in-process fake backend (no network, no sleeps, injected `Clock`, `@TempDir`). The fake backend serves `GET /project/{id}/content` from a committed export fixture. The golden snapshot is taken from the current per-endpoint reader before any production code changes. Every behaviour starts with a failing test (red, green, refactor).

## 1. Golden baseline

- [ ] 1.1 Capture the snapshot bytes of the sample project from the current per-endpoint reader as a golden file, with a test that pins them (this test passes before any change)
- [ ] 1.2 Add the committed export fixture of the sample project (`GET /project/{id}/content`, captured from dev, shapes as in the backend spec `add-project-content-export`)

## 2. Single content read

- [ ] 2.1 Write the failing test that a release requests exactly one path, `/project/{id}/content`, besides the project key lookup
- [ ] 2.2 Write the failing test that the export maps to the golden bytes, including component values that are a string and an object
- [ ] 2.3 Write the failing test that a `404` from the export is `UNKNOWN_PROJECT` (`422`), and that `500` and an unreachable backend are `SOURCE_UNAVAILABLE` (`502`) with exactly one request
- [ ] 2.4 Write the failing test that a key in the export different from the key of the project lookup is `PROJECT_MISMATCH` (`422`) and uploads nothing
- [ ] 2.5 Write the failing test that an advancement in the export is still `UNSUPPORTED_CONTENT` (`422`) naming it
- [ ] 2.6 Implement the single read in `BackendApiReleaseSnapshotSource`, grouping the export's children by parent id into `ProjectData`, and remove the double read and its retries
- [ ] 2.7 Implement the key check in `EditorReleaseService`
- [ ] 2.8 Remove the list and per-parent reads of `BackendApiClient` that are no longer used

## 3. Remove the concurrent-edit answer

- [ ] 3.1 Write the failing test that an export that answers `200` once is used once, and that no `409` for content changes exists in the release path
- [ ] 3.2 Remove `CONTENT_CHANGED` from `ReleaseRejectedException.Reason`, its mapping in `ReleaseRejectedExceptionHandler` and the `409` wording of the OpenAPI descriptions of `ReleaseController` and `EditorReleaseController`
- [ ] 3.3 Replace the paging and stable-read tests of `BackendApiReleaseSnapshotSourceIntegrationTest` and the "keeps changing" test of `EditorReleaseControllerIntegrationTest` with the tests of 2.x

## 4. Pull request

- [ ] 4.1 Run `flock ~/.cache/claude-heavy.lock ./gradlew build`
- [ ] 4.2 Open the code pull request `fix(release): read the release snapshot from the backend content export`
