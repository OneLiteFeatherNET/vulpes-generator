## MODIFIED Requirements

### Requirement: Snapshot from the backend api
The system SHALL assemble the format 1 snapshot of a release from one read of the backend content export `GET /project/{projectId}/content`. During a release it SHALL NOT request the list endpoints, the per-item, per-font or per-sound sub-resources, the asset metadata endpoints, the asset bytes, or `GET /project/{projectId}/snapshot`. The mapping of the export to format 1 SHALL follow the requirements of this capability.

#### Scenario: Project with content
- **WHEN** a release is built for a project with items, attributes, fonts, sounds and expressions
- **THEN** the snapshot contains all of them in format 1, validates against `snapshot-1.schema.json`, and the backend receives exactly one request, `GET /project/{projectId}/content`

#### Scenario: Many entries
- **WHEN** the export of a project holds 150 items
- **THEN** the snapshot contains all 150 items and no paging request is made

#### Scenario: Component values are JSON values
- **WHEN** an item has the component `stelaris:material` with the value `"minecraft:book"` and the component `minecraft:custom_name` with the value `{"text":"Statistics","color":"gold"}`
- **THEN** the snapshot item has the material `minecraft:book` and the component `minecraft:custom_name` with the same object, unchanged

#### Scenario: Output unchanged for the sample project
- **WHEN** the export of the sample project is mapped
- **THEN** the snapshot bytes equal the bytes of the snapshot that the per-endpoint reader produced for the same project before this change

## ADDED Requirements

### Requirement: Content export failures
The system SHALL answer a `404` from the content export with the same rejection as an unknown project, `UNKNOWN_PROJECT` (`422`). It SHALL answer any other failure of the export, including `5xx`, an unreachable backend, a missing body or invalid JSON, with `SOURCE_UNAVAILABLE` (`502`). It SHALL NOT retry the export. Nothing SHALL be published for a rejected release.

#### Scenario: Unknown project
- **WHEN** the export of project `P` answers `404`
- **THEN** the release is refused with `UNKNOWN_PROJECT` (`422`) and nothing is uploaded

#### Scenario: Backend error
- **WHEN** the export answers `500`
- **THEN** the release is refused with `SOURCE_UNAVAILABLE` (`502`), the export is requested once, and nothing is uploaded

#### Scenario: Backend unreachable
- **WHEN** the backend cannot be reached while the export is read
- **THEN** the release is refused with `SOURCE_UNAVAILABLE` (`502`) and nothing is uploaded

#### Scenario: Key changed after the key was read
- **WHEN** the project key from `GET /project/{projectId}` is `cygnus` and the export's `project.key` is `cygnus-next`
- **THEN** the release is refused with `PROJECT_MISMATCH` (`422`) and nothing is uploaded

## REMOVED Requirements

### Requirement: Stable input
**Reason:** The content export is one read in one transaction, so the snapshot cannot mix two states. Assembling the snapshot twice and comparing the results is no longer needed.

**Migration:** None. The generator no longer answers `409` for a project that is edited during a release. `409` remains only for an existing release version.
