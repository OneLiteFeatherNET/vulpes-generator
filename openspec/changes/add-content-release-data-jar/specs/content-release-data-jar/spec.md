# Spec Delta

## Purpose

Lets a caller turn one frozen project snapshot into a versioned, reproducible content data JAR and publish it to Reposilite, so that Minecraft servers consume content as a versioned artifact instead of generated Java sources.

## ADDED Requirements

### Requirement: Release authentication
The system SHALL require the header `Authorization: Bearer <token>` on `POST /releases`, where the token is configured as `vulpes.release.token`. The system SHALL answer `401` with the header `WWW-Authenticate: Bearer` when the header is missing, uses another scheme or carries a different token, and SHALL then contact neither the backend nor the repository. When no token is configured, the system SHALL answer every release request with `401`. The system MUST NOT log the token.

#### Scenario: Missing token
- **WHEN** a client posts a release without an Authorization header
- **THEN** the response status is `401` and neither the backend nor the repository is contacted

#### Scenario: Wrong token
- **WHEN** a client posts a release with a bearer token that differs from `vulpes.release.token`
- **THEN** the response status is `401` and neither the backend nor the repository is contacted

#### Scenario: No token configured
- **WHEN** `vulpes.release.token` is empty
- **THEN** every release request is answered with `401`

#### Scenario: Valid token
- **WHEN** the request carries the configured token
- **THEN** the release is processed as specified below

### Requirement: Release request validation
The system SHALL expose `POST /releases`, which accepts a JSON body with `projectId` (a UUID), `project` (a Maven artifact id matching `^[a-z][a-z0-9-]*$`), `version` (`X.Y.Z` or `X.Y.Z-dev.N`, where the numbers have no leading zeros), `revision` (`sha256:` followed by 64 hexadecimal digits), `createdBy` (a string) and `createdAt` (an ISO-8601 instant in UTC). The system SHALL reject a body that violates any of these rules with `400` before it contacts the backend or the repository.

#### Scenario: Version with a leading zero
- **WHEN** a client posts a release with `version` `1.02.0`
- **THEN** the response status is `400` and the backend is not contacted

#### Scenario: Dev version with a leading zero
- **WHEN** a client posts a release with `version` `1.0.0-dev.01`
- **THEN** the response status is `400` and nothing is published

#### Scenario: Invalid project artifact id
- **WHEN** a client posts a release with `project` `Eldoria`
- **THEN** the response status is `400` and nothing is published

### Requirement: Snapshot source
The system SHALL obtain the content of a release only from the frozen snapshot, through the `ReleaseSnapshotSource` port. The first adapter SHALL request `GET {vulpes.backend.url}/project/{projectId}/snapshot` and SHALL send an `Authorization: Bearer` header with `vulpes.backend.token` when that token is configured. The system MUST NOT read the Vulpes database.

#### Scenario: Snapshot fetched from the backend
- **WHEN** a valid release request for project `P` is received
- **THEN** the system requests `GET {vulpes.backend.url}/project/P/snapshot` once and uses the received bytes as the snapshot

#### Scenario: Token configured
- **WHEN** `vulpes.backend.token` is configured
- **THEN** the snapshot request carries the header `Authorization: Bearer <token>`

### Requirement: Snapshot verification
The system SHALL compute the SHA-256 digest of the received snapshot bytes and SHALL reject the release with `422` when the digest differs from `revision`. The system SHALL reject the release with `422` when the snapshot `format` is not `1` or when the snapshot `projectId` differs from the request `projectId`. The system SHALL reject the release with `422` when the backend answers `404` for the project. A rejected release SHALL publish nothing.

#### Scenario: Revision mismatch
- **WHEN** the SHA-256 of the received snapshot bytes differs from the request `revision`
- **THEN** the response status is `422` and nothing is uploaded

#### Scenario: Unsupported snapshot format
- **WHEN** the received snapshot has `format` `2`
- **THEN** the response status is `422` and nothing is uploaded

#### Scenario: Unknown project
- **WHEN** the backend answers `404` for the requested `projectId`
- **THEN** the response status is `422` and nothing is uploaded

#### Scenario: Snapshot for another project
- **WHEN** the received snapshot has a `projectId` that differs from the request `projectId`
- **THEN** the response status is `422` and nothing is uploaded

### Requirement: Backend availability
The system SHALL answer `502` and publish nothing when the backend cannot be reached.

#### Scenario: Backend unreachable
- **WHEN** the snapshot request to the backend fails with a connection error
- **THEN** the response status is `502` and nothing is uploaded

### Requirement: Snapshot stored unchanged
The system SHALL store the received snapshot bytes unchanged as `META-INF/vulpes/snapshot.json` in the JAR and MUST NOT re-serialise them.

#### Scenario: Bytes preserved
- **WHEN** a release is built from a snapshot
- **THEN** the entry `META-INF/vulpes/snapshot.json` is byte-identical to the received snapshot

### Requirement: Release metadata
The system SHALL write `META-INF/vulpes/release.json` in release format 1 with the fields `format` (`1`), `project`, `projectId`, `version`, `revision`, `packs` (an empty array) and `provenance`. `provenance` SHALL contain `generator` (the generator's own version), `createdBy` and `createdAt` taken from the request. The system MUST NOT take `createdAt` from its own clock. The file SHALL be UTF-8 with sorted object keys and no insignificant whitespace.

#### Scenario: Provenance from the request
- **WHEN** a release is built from a request with `createdAt` `2026-10-28T12:00:00Z`
- **THEN** `release.json` contains `provenance.createdAt` `2026-10-28T12:00:00Z` regardless of the time the build runs

#### Scenario: Packs empty
- **WHEN** a release is built
- **THEN** `release.json` contains `packs` as an empty array

#### Scenario: Canonical encoding
- **WHEN** `release.json` is written
- **THEN** its object keys are sorted and it contains no spaces or line breaks outside of string values

### Requirement: Reproducible JAR
The system SHALL build the JAR with the entries `META-INF/MANIFEST.MF`, `META-INF/vulpes/release.json` and `META-INF/vulpes/snapshot.json`, in this fixed order, with a fixed entry timestamp and no extra fields. The manifest SHALL contain only `Manifest-Version: 1.0`. The same request and the same snapshot SHALL yield byte-identical JARs.

#### Scenario: Same input, same bytes
- **WHEN** the same request and the same snapshot are built twice
- **THEN** both JAR files are byte-identical

#### Scenario: Entry layout
- **WHEN** a JAR is built
- **THEN** it contains exactly the three entries in the fixed order, and `META-INF/MANIFEST.MF` contains only `Manifest-Version: 1.0`

### Requirement: Temporary build directory
The system SHALL run every build in a fresh, empty temporary directory and SHALL delete that directory afterwards, also when the build fails.

#### Scenario: Build succeeds
- **WHEN** a build completes successfully
- **THEN** its temporary directory no longer exists

#### Scenario: Build fails
- **WHEN** a build fails after the temporary directory was created
- **THEN** its temporary directory no longer exists

### Requirement: Publication
The system SHALL publish the release with Maven Resolver as `net.onelitefeather.vulpes.content:<project>:<version>`, including the JAR, a POM without dependencies, the Gradle `.module` metadata and checksum files for md5, sha1, sha256 and sha512. The system SHALL take the repository credentials from `vulpes.content.maven.username` and `vulpes.content.maven.password`, which default to the environment variables `ONELITEFEATHER_MAVEN_USERNAME` and `ONELITEFEATHER_MAVEN_PASSWORD`.

#### Scenario: Published artifact set
- **WHEN** a release `1.2.0` of project `eldoria` is published
- **THEN** the repository receives the JAR, the POM, the `.module` file and the four checksum files for each of them under `net/onelitefeather/vulpes/content/eldoria/1.2.0/`

### Requirement: Repository routing
The system SHALL publish versions `X.Y.Z` to the releases repository `https://repo.onelitefeather.dev/onelitefeather-releases` and versions `X.Y.Z-dev.N` to the snapshots repository `https://repo.onelitefeather.dev/onelitefeather-snapshots`. Both URLs SHALL be configurable through `vulpes.content.maven.releases-url` and `vulpes.content.maven.snapshots-url`.

#### Scenario: Release version
- **WHEN** version `1.2.0` is published
- **THEN** the artifacts are uploaded to the releases repository

#### Scenario: Dev version
- **WHEN** version `1.2.0-dev.3` is published
- **THEN** the artifacts are uploaded to the snapshots repository

### Requirement: Immutable versions
The system SHALL NOT overwrite an existing version in the target repository. When the version already exists, the system SHALL answer `409` and SHALL upload nothing.

#### Scenario: Version already published
- **WHEN** a release is requested for a version that already exists in the target repository
- **THEN** the response status is `409` and no file is uploaded

### Requirement: Release response
The system SHALL answer `201` after a successful publish with the response body containing the Maven coordinates and the `revision`.

#### Scenario: Successful release
- **WHEN** a release is published successfully
- **THEN** the response status is `201` and the body contains the coordinates `net.onelitefeather.vulpes.content:<project>:<version>` and the `revision`

#### Scenario: Repository unreachable
- **WHEN** the Maven repository cannot be reached during publish
- **THEN** the response status is `502` and no release is published

### Requirement: Completion notification
The system SHALL call the `ReleaseCompletionListener` port once after a successful publish. The first listener implementation SHALL only log the completed release. The listener MUST NOT be called when a release is rejected or fails.

#### Scenario: Successful publish
- **WHEN** a release is published successfully
- **THEN** the listener is called once with the coordinates and the revision

#### Scenario: Rejected release
- **WHEN** a release is rejected with `409`, `422` or `502`
- **THEN** the listener is not called
