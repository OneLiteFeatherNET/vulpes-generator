## MODIFIED Requirements

### Requirement: Snapshot from the backend api
The system SHALL assemble the format 1 snapshot of a release from one read of the backend content export `GET /project/{projectId}/content`. During a release it SHALL NOT request the list endpoints, the per-item, per-font or per-sound sub-resources, the asset metadata endpoints, or `GET /project/{projectId}/snapshot`. It SHALL read asset bytes only through `GET /project/{projectId}/asset/{assetId}/content`, once per packed asset, to build the resource pack. The mapping of the export to format 1 SHALL follow the requirements of this capability.

#### Scenario: Project with content
- **WHEN** a release is built for a project with items, attributes, fonts, sounds and expressions
- **THEN** the snapshot contains all of them in format 1, validates against `snapshot-1.schema.json`, and the backend receives one request `GET /project/{projectId}/content` plus one `GET /project/{projectId}/asset/{assetId}/content` per packed asset

#### Scenario: Many entries
- **WHEN** the export of a project holds 150 items
- **THEN** the snapshot contains all 150 items and no paging request is made

#### Scenario: Component values are JSON values
- **WHEN** an item has the component `stelaris:material` with the value `"minecraft:book"` and the component `minecraft:custom_name` with the value `{"text":"Statistics","color":"gold"}`
- **THEN** the snapshot item has the material `minecraft:book` and the component `minecraft:custom_name` with the same object, unchanged

#### Scenario: Output unchanged for the sample project
- **WHEN** the export of the sample project is mapped
- **THEN** the snapshot bytes equal the bytes of the snapshot that the per-endpoint reader produced for the same project before this change
