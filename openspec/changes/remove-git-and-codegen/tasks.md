# Tasks

Test notes for all sections:
- The new boundary is pinned by tests first (section 1); the deletions in sections 2 to 6 turn them green.
- Unit tests stay fast and need no database, no network and no GitHub token. Integration tests use `@MicronautTest` with the datasource and GitHub properties absent.
- No `Thread.sleep` and no system clock.

## 0. Prerequisite

- [ ] 0.1 Confirm that the Gradle plugin repository (`OneLiteFeatherNET/vulpes-gradle-plugin`) has merged the commit that takes over code generation, and record its link for the pull request

## 1. Pin the new boundary with tests

- [ ] 1.1 Write the failing integration test that the application starts with no datasource configuration and `GET /health` answers `200`
- [ ] 1.2 Write the failing integration test that the application starts with no GitHub configuration and makes no outbound request to GitHub (fake HTTP client that fails the test on any call)
- [ ] 1.3 Write the failing integration test that `GET /download`, `GET /git/branches`, `GET /build/data` and `GET /generator/generate` each answer `404`

## 2. Remove Java code generation

- [ ] 2.1 Delete `generation/java/*`, `generation/AbstractCodeGenerator`, `generation/JavaIdentifiers`, `generation/JavaStructure`, `generation/Generator`, `generation/FileGenerator` and `generation/component/*`, with their tests and `JavaSourceAssertions`
- [ ] 2.2 Delete the DB-backed `generation/json/FontJsonGenerator` and `generation/json/SoundJsonGenerator`, with their tests
- [ ] 2.3 Delete `registry/*` and `listener/VulpesStartupListener`, with their tests, and the fixture `src/test/resources/vulpes-base/`
- [ ] 2.4 Run `./gradlew compileJava compileTestJava --max-workers=2` and fix all remaining references

## 3. Remove the Git and GitHub integration

- [ ] 3.1 Delete `git/*`, `domain/client/GithubClient`, `domain/client/GithubService`, `domain/release/*` and `util/BranchFilter`, with their tests
- [ ] 3.2 Delete `domain/configuration/{GithubConfiguration,CommitConfiguration,BranchFilterConfiguration}`
- [ ] 3.3 Delete `util/FileHelper`, remove the obsolete entries of `util/Constants` (delete the file if nothing remains) and delete `util/package-info.java` if `util` is empty
- [ ] 3.4 Run `./gradlew compileJava compileTestJava --max-workers=2` and fix all remaining references

## 4. Remove the endpoints

- [ ] 4.1 Delete `controller/git/*` (`GitBranchController`, `GitReleaseController`) with their tests
- [ ] 4.2 Delete `controller/CodeGenerationHandler` and `controller/download/VulpesDownloadController`, with their tests
- [ ] 4.3 Run the test from 1.3 and confirm it is green

## 5. Keep the codecs for the pack-build change

- [ ] 5.1 Remove `BitmapFontProviderCodec.fromFont` and `namespaceOf`, the vulpes-model imports and the Javadoc links to them; keep the rest of the class unchanged
- [ ] 5.2 Remove `SoundEntry.from(SoundFileSource)`, `SoundEventDefinition.from(SoundEventEntity)` (with its TODO) and `SoundEventKey.of(SoundEventEntity)`, together with their imports; keep the records unchanged
- [ ] 5.3 Confirm that `generation/sound/SoundEntryCodecTest` and the `gson/*` classes are green without vulpes-model

## 6. Dependencies, configuration and deployment

- [ ] 6.1 Remove JavaPoet, JGit, the vulpes-model dependency, Micronaut Data JPA, Hibernate, the JDBC and Hikari pool, the PostgreSQL driver and the JDBC tracing module from `build.gradle.kts`; remove Guava, commons-io and commons-compress only if `git grep` shows no remaining use
- [ ] 6.2 Remove `datasources`, `jpa`, `github`, `commit`, `filter` and `micronaut.http.services.github` from `application.yml`, and the matching keys from `application-test.yml`
- [ ] 6.3 Remove the database and GitHub examples from `charts/vulpes-generator/values.yaml` (including the `secrets` example and the comment about cloning) and check the Helm templates for references
- [ ] 6.4 Remove the Postgres service from `docker/docker-compose.yml`; delete the file if no service remains
- [ ] 6.5 Check `.github/workflows` for Postgres service containers or GitHub token use that only served the removed code, and remove them

## 7. Verification and pull request

- [ ] 7.1 Run `git grep -nE "javapoet|jgit|vulpes\.api\.model|github|datasource" -- src charts docker` and confirm there are no hits outside the documented kept files
- [ ] 7.2 Run `./gradlew test --max-workers=2` and `./gradlew build --max-workers=2`, and fix all failures
- [ ] 7.3 Open the pull request titled `refactor(generator)!: remove java generation and git integration`, linking the plugin commit from 0.1 and including the BREAKING CHANGE footer
