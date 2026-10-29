# Spec Delta

## Purpose

Defines the inputs of the generator after the content-release change. The generator takes content only from a frozen release snapshot. It does not need a database or a Git repository at runtime, and the endpoints of the old generation and Git flow are gone.

## ADDED Requirements

### Requirement: Content only from a release snapshot
The system SHALL read content only from a release snapshot and SHALL NOT connect to the Vulpes database.

#### Scenario: Starts without a datasource
- **WHEN** the application starts with no datasource configuration
- **THEN** the application starts and `GET /health` answers successfully

### Requirement: No runtime dependency on Git or GitHub
The system SHALL NOT depend on Git or GitHub at runtime. It SHALL NOT make any outbound call to GitHub.

#### Scenario: Starts without GitHub configuration
- **WHEN** the application starts with no GitHub token and no GitHub configuration
- **THEN** the application starts and makes no outbound request to GitHub

### Requirement: Removed generation and Git endpoints
The system SHALL NOT serve the endpoints `GET /download`, `GET /git/branches`, `GET /build/data` and `GET /generator/generate`. Each of them SHALL answer `404`.

#### Scenario: Removed endpoints
- **WHEN** a client requests `GET /download`, `GET /git/branches`, `GET /build/data` or `GET /generator/generate`
- **THEN** the response status is `404` for each of them
