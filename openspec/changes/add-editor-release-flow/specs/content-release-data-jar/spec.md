# Spec Delta

## MODIFIED Requirements

### Requirement: Release metadata
The system SHALL write `META-INF/vulpes/release.json` in release format 2 with the fields `format` (`2`), `base`, `project`, `projectId`, `version`, `revision`, `packs` (an empty array) and `provenance`. `base` SHALL be an object with `repository` (the `vulpes-base` repository, for example `OneLiteFeatherNET/vulpes-base`), `branch` (the base branch the release was compiled against) and `commit` (the full 40-character lower-case SHA-1 that the branch resolved to; abbreviated SHAs are never written). `provenance` SHALL contain `generator` (the generator's own version), `createdBy` and `createdAt` taken from the request. The system MUST NOT take `createdAt` from its own clock. The file SHALL be UTF-8 with sorted object keys and no insignificant whitespace.

#### Scenario: Provenance from the request
- **WHEN** a release is built from a request with `createdAt` `2026-10-28T12:00:00Z`
- **THEN** `release.json` contains `provenance.createdAt` `2026-10-28T12:00:00Z` regardless of the time the build runs

#### Scenario: Packs empty
- **WHEN** a release is built
- **THEN** `release.json` contains `packs` as an empty array

#### Scenario: Base recorded
- **WHEN** a release is built against the branch `develop` that resolved to the commit `0123456789abcdef0123456789abcdef01234567`
- **THEN** `release.json` contains `base` with `repository` `OneLiteFeatherNET/vulpes-base`, `branch` `develop` and `commit` `0123456789abcdef0123456789abcdef01234567`

#### Scenario: Canonical encoding
- **WHEN** `release.json` is written
- **THEN** its object keys are sorted, so `base` comes before `format`, and it contains no spaces or line breaks outside of string values

#### Scenario: Format 1 releases are not rewritten
- **WHEN** a data JAR published in release format 1 (no `base`) is downloaded as a local build
- **THEN** its `release.json` is returned byte for byte, without a `base` object and without format 2
