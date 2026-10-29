# Proposal

**Conventional Commits:** `refactor(generator)!: remove java generation and git integration`

## Why

The generator still carries two ways of getting content into Minecraft that the content-release concept replaces. It generates Java sources from the live Postgres database with JavaPoet and commits them into a clone of `vulpes-base` through JGit and the GitHub API. Both paths couple every content change to a generator run, a Git repository and a database connection.

The design "2026-10-28 — Vulpes: Content-Releases ohne Git (Design)" (https://outline.onelitefeather.dev/doc/2026-10-28-vulpes-content-releases-ohne-git-design-7oIaSsfvbz) replaces them with a data JAR built from a frozen snapshot (change `add-content-release-data-jar`). Code generation moves into the Gradle plugin, the contract stays in the plugin repository (https://github.com/OneLiteFeatherNET/vulpes-gradle-plugin/pull/1). Once the release flow exists, the old code is only a maintenance burden and a dependency on a retired database and repository.

## What Changes

**Prerequisite (before any deletion):** the Gradle plugin repository (`OneLiteFeatherNET/vulpes-gradle-plugin`) has taken over code generation in a merged commit. That commit is linked in the pull request.

- Remove the Java code generation under `src/main/java/net/onelitefeather/vulpes/generator/`: `generation/java/*`, `generation/AbstractCodeGenerator`, `generation/JavaIdentifiers`, `generation/JavaStructure`, `generation/Generator`, `generation/FileGenerator`, `generation/component/*`, the DB-backed `generation/json/FontJsonGenerator` and `generation/json/SoundJsonGenerator`, `registry/*` and `listener/VulpesStartupListener`, together with their tests and the test fixture `src/test/resources/vulpes-base/`.
- Remove the Git and GitHub integration: `git/*`, `domain/client/GithubClient`, `domain/client/GithubService`, `domain/release/*`, `controller/git/*`, `util/BranchFilter`, `domain/configuration/{GithubConfiguration,CommitConfiguration,BranchFilterConfiguration}`, `controller/CodeGenerationHandler` (`/generator/generate`), `controller/download/VulpesDownloadController` (`/download`), `util/FileHelper` and the obsolete entries of `util/Constants`, together with their tests. `util/package-info.java` goes with `util` once it is empty.
- Remove the dependencies JavaPoet, JGit, the vulpes-model dependency, Micronaut Data JPA and Hibernate, the JDBC and Hikari pool, the PostgreSQL driver and the JDBC tracing module. Guava, commons-io and commons-compress are removed only when no remaining code uses them; this is checked during apply.
- Remove the configuration `datasources`, `jpa`, `github`, `commit` and `filter`, and `micronaut.http.services.github` from `application.yml`. Remove the related keys from `application-test.yml`.
- Remove the database and GitHub related values and examples from the Helm chart (`charts/vulpes-generator`) and the Postgres service from `docker/docker-compose.yml`. If no service remains in that file, the file is deleted.

**Kept for the later pack-build change:** `generation/font/BitmapFontProviderCodec`, the `generation/sound/*` codecs, `generation/sound/SoundEntryCodecTest` and `gson/*`. In their current form they still depend on vulpes-model: four of them import vulpes-model types (`BitmapFontProviderCodec`, `SoundEntry`, `SoundEventDefinition`, `SoundEventKey`), so this change removes exactly the methods that map database entities to them: `BitmapFontProviderCodec.fromFont` and `namespaceOf`, `SoundEntry.from`, `SoundEventDefinition.from` and `SoundEventKey.of`, together with their imports and Javadoc links. The codec logic and the record types stay unchanged. After that, the kept files compile without vulpes-model and without the removed code, which is what allows the vulpes-model dependency to go.

**Breaking change.** Commit footer:

```
BREAKING CHANGE: GET /download, GET /git/branches, GET /build/data and GET /generator/generate are removed. Stelaris must switch to the release flow (POST /releases).
```

## Capabilities

### New Capabilities
- `generator-inputs`: The generator reads content only from a release snapshot and has no runtime dependency on a database or on Git and GitHub.

### Modified Capabilities
<!-- None: the capability is introduced here. The removed endpoints are described as removed in the new capability. -->

## Non-goals

- No new functionality of any kind.
- No pack build. The kept codecs are not wired into a build yet.
- No change to the data-JAR path introduced by `add-content-release-data-jar`.

## Impact

- **API**: the endpoints `GET /download`, `GET /git/branches`, `GET /build/data` and `GET /generator/generate` are removed. Stelaris must switch to the release flow. OpenAPI loses these operations.
- **Code**: the removals listed above. The kept codecs change only by the removal of the entity-mapping methods listed above.
- **Dependencies**: JavaPoet, JGit, vulpes-model, Micronaut Data JPA, Hibernate, JDBC, Hikari, PostgreSQL driver and the JDBC tracing module are removed from `build.gradle.kts`. Minestom stays, because the kept codecs use it.
- **Deployment**: the Helm chart loses its database and GitHub examples, and the compose file loses the Postgres service. The Helm templates and the CI workflows are checked for Postgres or GitHub references during apply.
- **Stelaris**: loses the endpoints it uses today (`/download`, `/git/branches`, `/build/data`) and switches to the release flow in its own change; this is accepted as a breaking change.
