# Spec Delta

## Purpose

Lets the editor show which versions of a project are published, without holding Reposilite credentials of its own, and lets a developer download the local build of any published version. Every endpoint identifies the project by its backend id; the generator resolves the Maven artifact id from the backend.

## ADDED Requirements

### Requirement: Project id validation and key resolution
The system SHALL accept a `projectId` path parameter only when it is a UUID. Any other value SHALL be answered with `400` and neither the backend nor a repository SHALL be contacted. The system SHALL resolve the project key of the project id from the backend. An unknown project SHALL be answered with `422`, and a backend that cannot be reached with `502`. A key that does not match `^[a-z][a-z0-9-]*$` SHALL be answered with `422` and no repository SHALL be contacted.

#### Scenario: Project id not a UUID
- **WHEN** the path parameter is `Eldoria`
- **THEN** the response status is `400` and neither the backend nor a repository is contacted

#### Scenario: Unknown project
- **WHEN** the backend does not know the project id
- **THEN** the response status is `422` and no repository is contacted

#### Scenario: Invalid key from the backend
- **WHEN** the backend returns the key `Eldoria` for the project id
- **THEN** the response status is `422` and no repository is contacted

### Requirement: Published version list
The system SHALL answer `GET /projects/{projectId}/releases` with every version of the resolved project key that the releases repository and the snapshots repository hold. Each entry SHALL carry `version`, `channel` (`release` for `X.Y.Z`, `snapshot` for `X.Y.Z-SNAPSHOT`), the Maven coordinates, the URL of the repository it lives in and the URL of its data JAR. Each snapshot version SHALL appear once, because a republish replaces it.

#### Scenario: Versions in both repositories
- **WHEN** the releases repository holds `1.0.0` and `1.1.0` and the snapshots repository holds `1.1.0-SNAPSHOT`
- **THEN** the list has three entries with their channels and coordinates

#### Scenario: Project never published
- **WHEN** both repositories answer `404` for the project's metadata
- **THEN** the response status is `200` with an empty list

### Requirement: Listing credentials stay server side
The system SHALL read the versions from the `maven-metadata.xml` of each repository with the generator's own credentials, and SHALL NOT return any credential or authorisation header in a response.

#### Scenario: Credentials stay server side
- **WHEN** the list is returned
- **THEN** no username, password or authorisation header appears in the response

### Requirement: Latest version per channel
The system SHALL answer with `latest`, holding the newest release version and the newest snapshot version. Versions SHALL be compared by their numeric parts, so `1.10.0` is newer than `1.9.0`. A channel without versions SHALL be `null`.

#### Scenario: Newest by version number
- **WHEN** the releases repository holds `1.10.0` and `1.9.0`
- **THEN** `latest.release` is `1.10.0`

#### Scenario: Newest of both channels
- **WHEN** the releases repository holds `1.1.0` and the snapshots repository holds `1.1.0-SNAPSHOT`
- **THEN** `latest.release` is `1.1.0` and `latest.snapshot` is `1.1.0-SNAPSHOT`

#### Scenario: Empty channel
- **WHEN** the snapshots repository holds no version
- **THEN** `latest.snapshot` is `null`

### Requirement: Repository unavailable is an error
The system SHALL answer `502` with a message that names the repository when either repository cannot be reached, answers with a status other than `200` or `404`, or returns metadata that cannot be read. The system MUST NOT return a partial list in that case.

#### Scenario: Snapshots repository down
- **WHEN** the snapshots repository cannot be reached
- **THEN** the response status is `502`, the message names the snapshots repository, and no list is returned

#### Scenario: Unreadable metadata
- **WHEN** a repository returns metadata that is not valid XML
- **THEN** the response status is `502`

### Requirement: Version path validation
The system SHALL accept a version path parameter only when it is `X.Y.Z` or `X.Y.Z-SNAPSHOT` with no leading zeros in the numeric parts. Any other value SHALL be answered with `400` before a repository is contacted.

#### Scenario: Malformed version path
- **WHEN** the version path is `1.0`
- **THEN** the response status is `400` and no repository is contacted

### Requirement: Local build of a published version
The system SHALL answer `GET /projects/{projectId}/releases/{version}/local-build` with a zip of the published version. The zip SHALL have the media type `application/zip`, the file name `<projectKey>-<version>.zip` and the layout of the `local-build-download` capability: the entries `vulpes/<projectKey>/release.json` and `vulpes/<projectKey>/snapshot.json` and nothing else. The system SHALL answer `404` when the version is not published in its channel.

#### Scenario: Published version downloaded
- **WHEN** the version `1.1.0` is published and its local build is requested
- **THEN** the response status is `200` and the zip has the layout of a local build

#### Scenario: Unknown version
- **WHEN** the version `9.9.9` is not published
- **THEN** the response status is `404`

### Requirement: Published JSON copied unchanged
The system SHALL copy `release.json` and `snapshot.json` of a published version byte for byte from its data JAR into the zip. The system MUST NOT rewrite the `release.json` of a published version.

#### Scenario: Bytes unchanged
- **WHEN** the local build of a published version is returned
- **THEN** the bytes of both JSON entries equal the bytes of the same entries in the published JAR

### Requirement: Published JAR integrity
The system SHALL check that the SHA-256 of the `snapshot.json` in a published JAR equals the `revision` in its `release.json` before it returns a zip. When the check fails, the system SHALL answer `502` and SHALL NOT return the zip.

#### Scenario: Corrupt published JAR
- **WHEN** the revision in `release.json` does not match the digest of `snapshot.json`
- **THEN** the response status is `502` and no zip is returned
