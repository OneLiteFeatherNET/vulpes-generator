# Spec Delta

## Purpose

Lets a developer download the release data of a project as a local build, so that the Vulpes Gradle plugin can build the project against a `vulpes-base` branch on the developer's machine, without a published release and without any generated Java sources in the download.

## ADDED Requirements

### Requirement: Local build download
The system SHALL answer `GET /projects/{projectId}/local-build` with a zip file of the project's current snapshot. The response SHALL have the media type `application/zip` and a `Content-Disposition` header with the attachment file name `<project>-local.zip`, where `<project>` is the project key of the snapshot. The request SHALL accept the query parameter `baseBranch`, which follows the base branch selection of the `base-branch-build` capability.

#### Scenario: Download succeeds
- **WHEN** a client requests the local build of a known project
- **THEN** the response status is `200`, the media type is `application/zip` and the file name is `<project>-local.zip`

#### Scenario: Unknown project
- **WHEN** the backend does not know the project
- **THEN** the response status is `422` and no zip is returned

#### Scenario: Backend unreachable
- **WHEN** the backend cannot be reached
- **THEN** the response status is `502`

### Requirement: Local build layout
The zip of a local build SHALL contain exactly two entries: `vulpes/<project>/release.json` and `vulpes/<project>/snapshot.json`. The zip SHALL contain no `.git` entry, no Java source, no class file and no other file. `snapshot.json` SHALL hold the snapshot bytes unchanged.

#### Scenario: Entries of the zip
- **WHEN** a local build is downloaded
- **THEN** the zip lists exactly `vulpes/<project>/release.json` and `vulpes/<project>/snapshot.json`

#### Scenario: No sources and no git data
- **WHEN** a local build is generated
- **THEN** the zip contains no `.git` entry and no `.java` entry

### Requirement: Local build metadata
The `release.json` of a local build SHALL be in release format 2 with `version` `0.0.0-local` and `base` set to the repository, the requested branch and the resolved commit. Its `revision` SHALL be the SHA-256 of `snapshot.json`. Its `provenance.generator` SHALL be the generator's version, `provenance.createdBy` SHALL be `local` and `provenance.createdAt` SHALL come from the generator's clock.

#### Scenario: Local version
- **WHEN** a local build is downloaded
- **THEN** `release.json` contains `version` `0.0.0-local` and `base.commit` equal to the commit that was compiled

#### Scenario: Revision matches the snapshot
- **WHEN** the zip is opened and `snapshot.json` is hashed with SHA-256
- **THEN** the digest equals the `revision` in `release.json`

#### Scenario: Local provenance
- **WHEN** a local build is downloaded
- **THEN** `provenance.createdBy` is `local`

### Requirement: Local build refused on compilation failure
The system SHALL NOT return a zip of a local build when the compilation gate of the selected base branch refuses the project. The response SHALL be the `422` answer of the `base-branch-build` capability with its diagnostics.

#### Scenario: Refused build
- **WHEN** the project does not compile against the selected base branch
- **THEN** the response status is `422`, the body lists the diagnostics and no zip is returned

## REMOVED Requirements

### Requirement: Legacy source download
**Reason**: The zip contained a clone of `vulpes-base` with legacy generated Java and a `.git` folder. The Vulpes Gradle plugin cannot use it.

**Migration**: Use `GET /projects/{projectId}/local-build`, which returns `release.json` and `snapshot.json` only.

### Requirement: Legacy generation endpoint
**Reason**: The endpoint was a stub that answered success without building anything.

**Migration**: Use `GET /projects/{projectId}/local-build` for a local build and `POST /generator/release` for a release.

This is a breaking change for the legacy API. Only the Stelaris generator page used both endpoints, and that page switches in its own change.

#### Scenario: Removed download route
- **WHEN** a client requests `GET /download`
- **THEN** the response status is `404`

#### Scenario: Removed generation route
- **WHEN** a client requests `GET /generator/generate`
- **THEN** the response status is `404`
