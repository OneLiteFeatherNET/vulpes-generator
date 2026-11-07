# Spec Delta

## Purpose

Defines the inputs of the generator after the content-release change. The generator takes content only from a frozen release snapshot. It does not need a database at runtime, and the endpoints of the old generation flow are gone. The branch list and the base branch clone stay for `add-editor-release-flow`.

## ADDED Requirements

### Requirement: Content only from a release snapshot
The system SHALL read content only from a release snapshot and SHALL NOT connect to the Vulpes database.

#### Scenario: Starts without a datasource
- **WHEN** the application starts with no datasource configuration
- **THEN** the application starts and `GET /health` answers successfully

### Requirement: Git and GitHub only for the base branch list
The system SHALL NOT read content from Git or GitHub. It SHALL make an outbound call to GitHub only when a client requests the branch list (`GET /git/branches`) or when the base branch clone of `add-editor-release-flow` runs. It SHALL NOT make any outbound call to GitHub at startup.

#### Scenario: Starts without GitHub configuration
- **WHEN** the application starts with no GitHub token and no GitHub configuration
- **THEN** the application starts and makes no outbound request to GitHub

### Requirement: Removed generation endpoints
The system SHALL NOT serve the endpoints `GET /download`, `GET /build/data` and `GET /generator/generate`. Each of them SHALL answer `404`.

#### Scenario: Removed endpoints
- **WHEN** a client requests `GET /download`, `GET /build/data` or `GET /generator/generate`
- **THEN** the response status is `404` for each of them

### Requirement: Branch list kept
The system SHALL keep serving `GET /git/branches` for the editor release flow of `add-editor-release-flow`.

#### Scenario: Branch list still served
- **WHEN** a client requests `GET /git/branches`
- **THEN** the response status is not `404`
