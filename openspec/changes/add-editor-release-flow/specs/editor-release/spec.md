# Spec Delta

## Purpose

Lets the Stelaris editor publish a content release of a project against a `vulpes-base` branch. The editor names the project, the version and the base branch; the generator verifies the snapshot against the branch, publishes the content data JAR and reports where it went.

## ADDED Requirements

### Requirement: Editor release request
The system SHALL accept `POST /generator/release` with the JSON body `{projectId, version, baseBranch}`. `projectId` SHALL be a UUID. `version` SHALL be `X.Y.Z` or `X.Y.Z-dev.N` with no leading zeros. `baseBranch` SHALL follow the base branch selection of the `base-branch-build` capability and SHALL default to `develop` when omitted. An invalid body SHALL be answered with `400` and nothing SHALL be fetched or built.

#### Scenario: Valid request
- **WHEN** the body carries a project id, the version `1.2.0` and the base branch `feature/font`
- **THEN** the release is processed against the branch `feature/font`

#### Scenario: Invalid version
- **WHEN** the version is `1.02.0`
- **THEN** the response status is `400` and no snapshot is fetched

#### Scenario: Base branch omitted
- **WHEN** the body has no `baseBranch`
- **THEN** the release is processed against the branch `develop`

### Requirement: Repository by version
The system SHALL publish a version `X.Y.Z` to the releases repository and a version `X.Y.Z-dev.N` to the snapshots repository, as the existing content coordinates rules define. The group id SHALL be `net.onelitefeather.vulpes.content` and the artifact id SHALL be the project key.

#### Scenario: Release version
- **WHEN** the version is `2.0.0`
- **THEN** the artifact is published to the releases repository

#### Scenario: Snapshot version
- **WHEN** the version is `2.0.0-dev.4`
- **THEN** the artifact is published to the snapshots repository

### Requirement: Existing version refused before the build
The system SHALL check whether the version already exists in the target repository before it generates or compiles anything. When it exists, the system SHALL answer `409` and upload nothing.

#### Scenario: Version exists
- **WHEN** the version `2.0.0` already exists in the releases repository
- **THEN** the response status is `409`, and neither the base branch is cloned nor the sources are compiled

### Requirement: Editor release response
The system SHALL answer a successful release with `201` and the JSON body `{coordinates, revision, base, repositoryUrl}`. `coordinates` SHALL be the Maven coordinates `net.onelitefeather.vulpes.content:<project>:<version>`. `revision` SHALL be the SHA-256 revision of the snapshot. `base` SHALL be an object with `repository`, `branch` and `commit`, where `commit` is the commit that was compiled. `repositoryUrl` SHALL be the URL of the repository the version was published to.

#### Scenario: Successful release
- **WHEN** a release of `2.0.0` against `develop` is published
- **THEN** the response status is `201` and `base.commit` is the commit that the clone resolved

### Requirement: Compile failure answers before publication
The system SHALL refuse an editor release whose generated sources do not compile against the selected base branch. The system SHALL answer `422` with the diagnostics of the `base-branch-build` capability and SHALL upload nothing.

#### Scenario: Refused release
- **WHEN** the snapshot does not compile against `develop`
- **THEN** the response status is `422`, the body lists the diagnostics, and the repository receives no upload

### Requirement: Editor release error answers
The system SHALL answer `404` when the editor route is disabled, `409` when the version exists or the project changed while the snapshot was assembled, `422` when the backend refuses the project or its content, and `502` when the backend, the base repository or the Maven repository cannot be reached. Every error body SHALL carry a `reason` and a `message`.

#### Scenario: Route disabled
- **WHEN** `vulpes.release.editor-route-enabled` is `false`
- **THEN** `POST /generator/release` answers `404`

#### Scenario: Backend unreachable
- **WHEN** the backend cannot be reached
- **THEN** the response status is `502` and nothing is published

### Requirement: Editor route switch
The system SHALL serve `POST /generator/release` only when `vulpes.release.editor-route-enabled` is `true`. The cluster configuration SHALL switch the route on. The route SHALL NOT require a release token, because the editor has no login yet; it is reachable only from the internal network.

#### Scenario: Route enabled
- **WHEN** `vulpes.release.editor-route-enabled` is `true`
- **THEN** the route accepts requests without a bearer token
