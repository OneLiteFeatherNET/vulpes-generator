# Tasks

Test notes for all sections:
- Unit tests use fakes for the branch lister, the base class builder, the codegen facade, the backend and the Maven repositories. No real network, no real GitHub, no real Reposilite.
- Clone tests use a local bare Git repository created in `@TempDir` with JGit. The clone URL is injected, so the production URL is never used in a test.
- Compilation tests compile against a small base class fixture in `@TempDir`, not against the real `vulpes-base` build.
- Integration tests use `@MicronautTest` with the in-process fake backend and the fake Maven repository controllers that already exist under `src/test/.../release/`.
- No `Thread.sleep`, no system clock: `Clock` is injected (`FixedClockFactory`); timeouts are injected as durations that a fake builder reports as expired.
- Every behaviour gets its failing test before the production code (red, green, refactor).

## 1. Base branch selection

- [ ] 1.1 Write the failing unit tests for the branch selection: an omitted branch resolves to `develop`, a listed branch is accepted, an unlisted branch gives `UNKNOWN_BASE_BRANCH` (`422`), and a failing branch list gives `BASE_UNAVAILABLE` (`502`) without assuming the branch exists; then implement the resolver until they pass (verify: `./gradlew test --tests '*BaseBranchResolverTest'` is green)
- [ ] 1.2 Write the failing test that `GithubService.getBranches()` answers `BASE_UNAVAILABLE` instead of an empty list on a non-`200` response, and that the GitHub client sends `github.password` as `Authorization` when it is set; then change the service and the client (verify: `GithubServiceTest` and a client test with a fake HTTP client are green)

## 2. Release metadata in format 2

- [ ] 2.1 Write the failing test in `ReleaseJsonWriterTest` that `release.json` has `format` `2`, a `base` object with `repository`, `branch` and `commit`, and sorted keys so that `base` comes first; then implement the writer change (verify: the test is green and the byte output equals the golden string in the test)
- [ ] 2.2 Write the failing test that `ReleaseRequest` rejects a `base.commit` that is not 40 lower-case hex characters and a branch name that is blank; then add the `base` fields to the request (verify: `ReleaseRequestTest` is green)

## 3. Shallow clone and resolved commit

- [ ] 3.1 Write the failing test that a shallow clone of a branch of a local bare repository in `@TempDir` resolves the branch head commit and returns it, and that an unreachable clone URL gives `BASE_UNAVAILABLE`; then implement `BaseCheckout` on the existing JGit worker (verify: `BaseCheckoutTest` is green with no network)
- [ ] 3.2 Write the failing test that the clone directory no longer exists after a successful request and after a failed one, using the checkout and a generator that throws; then implement the cleanup in `try`/`finally` (verify: the test asserts `Files.exists(clone)` is false in both cases)

## 4. Classpath cache per commit and build limits

- [ ] 4.1 Write the failing tests that two requests for the same commit trigger one base build, that the least recently used commit is evicted when the cache holds `cache-entries` commits, and that the cache directory contains no `.git` or source file; then implement `BaseClassCache` with the base builder as a port (verify: `BaseClassCacheTest` is green)
- [ ] 4.2 Write the failing test that a base build exceeding the injected `build-timeout` gives `BASE_UNAVAILABLE` and leaves no cache entry, using a fake builder that reports a timeout; then implement the timeout handling (verify: the test is green and no wall-clock wait happens)
- [ ] 4.3 Write the failing test that two base builds of different commits never run at the same time, using a fake builder with latches; then implement the single-permit lock that is taken after the cache lookup (verify: the test is green and the second build starts only after the first finishes)
- [ ] 4.4 Write the integration test that the real Gradle base build of a fixture checkout (a minimal Gradle project in `src/test/resources`) produces classes and a classpath file in the cache; mark it as an integration test and keep it out of the unit suite (verify: `./gradlew integrationTest --tests '*BaseBuildIntegrationTest'`); then implement the Gradle invocation with a cleared environment

## 5. Code generation and compilation gate

- [ ] 5.1 Write the failing test that sources compiled against the fixture base classes succeed, and that a source with a type error gives `COMPILATION_FAILED` with file, line and message of the compiler; then implement the compilation gate on `javax.tools` with a `DiagnosticCollector` and `--release 25` (verify: `CompilationGateTest` is green)
- [ ] 5.2 Write the failing test that more than 100 errors give exactly 100 diagnostics and `truncated` `true`; then implement the cap (verify: the test is green)
- [ ] 5.3 Write the failing test that a codegen refusal (fake facade throws the codegen exception) gives `GENERATION_REFUSED` with `422` and the message; then wrap the facade (verify: the test is green)
- [ ] 5.4 Write the integration test that the real codegen facade of the plugin artifact generates a fixture snapshot that compiles against the fixture base; mark it as integration (verify: `./gradlew integrationTest --tests '*CodegenIntegrationTest'`); this task needs the prerequisite publication of `vulpes-codegen` (design, Open Question 1)

## 6. Local build download

- [ ] 6.1 Write the failing test that the local build zip holds exactly `vulpes/<project>/release.json` and `vulpes/<project>/snapshot.json`, with no `.git` or `.java` entry, and that `release.json` has `version` `0.0.0-local`, `base.commit` of the compiled commit, `provenance.createdBy` `local`, and a `revision` equal to the SHA-256 of `snapshot.json`; then implement the in-memory assembler (verify: `LocalBuildZipTest` is green)
- [ ] 6.2 Write the failing `@MicronautTest` test for `GET /projects/{projectId}/local-build`: `200` with `application/zip` and `Content-Disposition` `attachment; filename="<project>-local.zip"`, `422` for an unknown project and `422` with diagnostics for a compile failure; then implement the controller and its OpenAPI annotations (verify: `LocalBuildControllerIntegrationTest` is green)
- [ ] 6.3 Write the failing test that `GET /download` and `GET /generator/generate` answer `404`; then delete `VulpesDownloadController`, `CodeGenerationHandler` and their tests (verify: the tests are green and `grep -r "VulpesDownloadController\|CodeGenerationHandler" src` finds nothing)

## 7. Editor release

- [ ] 7.1 Write the failing test in `EditorReleaseServiceTest` that an existing version is refused with `409` before the snapshot is fetched and before any base checkout is made, with the fake publisher reporting the version as existing and the fake source and checkout recording zero calls; then move the existence check in front of the fetch (verify: the test is green)
- [ ] 7.2 Write the failing test that an editor release against a non-compiling snapshot gives `422` with diagnostics and that the fake publisher receives no upload, and that a successful release returns coordinates, revision, `base` and `repositoryUrl`; then implement the flow with the base step (verify: `EditorReleaseServiceTest` is green)
- [ ] 7.3 Write the failing `@MicronautTest` test that `POST /generator/release` without `baseBranch` uses `develop`, that an invalid version gives `400` before the backend is called, and that the response carries `base` and `repositoryUrl`; then extend `EditorReleaseRequest` and `ReleaseResponse` (with `base` and `repositoryUrl`) and their OpenAPI annotations (verify: `EditorReleaseControllerIntegrationTest` is green)
- [ ] 7.4 Write the failing test that `POST /releases` accepts `baseBranch` with the same default and rejects an unknown branch with `422`; then add the field to `ReleaseRequest` and the controller (verify: `ReleaseControllerIntegrationTest` is green)
- [ ] 7.5 Confirm with `EditorReleaseDisabledIntegrationTest` that `POST /generator/release` answers `404` while `vulpes.release.editor-route-enabled` is `false` (verify: the existing test is still green after the changes)

## 8. Published content releases

- [ ] 8.1 Write the failing unit tests for the `maven-metadata.xml` reader: the version list is read from a fixture document, a malformed document gives `502`, and `latest` is chosen by numeric comparison so that `1.10.0` beats `1.9.0`; then implement the reader and the comparison (verify: `MavenMetadataTest` is green)
- [ ] 8.2 Write the failing `@MicronautTest` test for `GET /projects/{projectKey}/releases` against two fake repositories: both channels are listed with coordinates and URLs, `latest` is correct, both `404` gives an empty list, one repository answering `500` gives `502` naming that repository, and the response contains no credential; then implement the listing service and controller (verify: `ReleaseListingControllerIntegrationTest` is green)
- [ ] 8.3 Write the failing test that an invalid project key (`Eldoria`) and an invalid version path (`1.0`) give `400` and that the fake repositories receive zero requests; then implement the validation (verify: the test is green)
- [ ] 8.4 Write the failing test for `GET /projects/{projectKey}/releases/{version}/local-build`: the zip layout matches the local build, the two JSON entries are byte-identical to the published JAR, an unpublished version gives `404`, and a JAR whose revision does not match gives `502` with no zip; then implement it (verify: `PublishedLocalBuildTest` is green)

## 9. Cleanup of work directories

- [ ] 9.1 Write the failing test that the startup sweep removes `vulpes-*` directories older than one hour according to the fixed clock, keeps newer ones and leaves other names alone, all in `@TempDir`; then implement the sweep as a startup listener (verify: `WorkDirectorySweepTest` is green)

## 10. Configuration and Helm chart

- [ ] 10.1 Write the failing test that the new `vulpes.base.*` keys bind with their defaults and that `vulpes.release.editor-route-enabled` defaults to `false`; then add the configuration records and the entries in `application.yml` (verify: `ConfigurationBindingTest` is green)
- [ ] 10.2 Add the `release.editorRouteEnabled`, `release.baseCacheVolume` and `release.gradleCacheVolume` values and their templates in `charts/vulpes-generator`; verify with `helm template charts/vulpes-generator -f <overlay with all values set>` that `VULPES_RELEASE_EDITOR_ROUTE_ENABLED`, the volumes and the secret references are rendered, and with the defaults that the route is absent or `false`
- [ ] 10.3 Update the chart documentation comment and the `application.yml` comments for the release section so that they name every key in design section 8 (verify: `grep -n "vulpes.base" src/main/resources/application.yml` lists all new keys)

## 11. Measurement, deployment and code pull request

- [ ] 11.1 Run a probe of a cold base build and a warm request in a pod built from the new image; record the peak memory, the build time and the disk use of the work directory, and update design section 9 with the measured numbers (verify: the numbers appear in the pull request description and in `design.md`)
- [ ] 11.2 Open a pull request in Kubernetes-FLUX that sets the values of design section 8 through secret references, including `VULPES_BACKEND_URL` for the in-cluster backend and `VULPES_RELEASE_EDITOR_ROUTE_ENABLED`; the change is made through the pull request only, without `kubectl` (verify: the Flux pull request is linked and its rendered manifests show the variables)
- [ ] 11.3 Open the code pull request `feat(release): build and publish content releases from the editor against a vulpes-base branch` from the implementation branch, with the implementation only and no file under `openspec/` (verify: `gh pr create` returns the URL and the pull request template sections are filled)
