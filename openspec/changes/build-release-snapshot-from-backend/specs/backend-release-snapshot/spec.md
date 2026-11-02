## ADDED Requirements

### Requirement: Snapshot from the backend api
The system SHALL assemble the format 1 snapshot of a release from the backend's REST endpoints of the project and SHALL NOT request `GET /project/{projectId}/snapshot`. It SHALL read every list endpoint page by page until `totalSize` entries are collected.

#### Scenario: Project with content
- **WHEN** a release is built for a project with items, attributes, fonts, sounds and expressions
- **THEN** the snapshot contains all of them in format 1 and validates against `snapshot-1.schema.json`

#### Scenario: More entries than one page
- **WHEN** the project has 150 items and the backend returns 100 per page
- **THEN** the snapshot contains all 150 items

### Requirement: Key namespaces
The system SHALL give an entry key without a namespace the project namespace. A key whose first dot-separated segment is listed in `vulpes.release.foreign-namespaces` SHALL get that segment as namespace and the rest as path. The default list SHALL be `minecraft` and `olf`.

#### Scenario: Project key
- **WHEN** project `cygnus` has the item key `statistics`
- **THEN** the snapshot item key is `cygnus:statistics`

#### Scenario: Foreign namespace
- **WHEN** project `cygnus` has the sound key `minecraft.music.game`
- **THEN** the snapshot sound key is `minecraft:music.game`

### Requirement: Item mapping
The system SHALL take the item `material` from the component `stelaris:material`, set `amount` to 1, keep every other component unchanged and write the lore lines in order as `minecraft:lore` text components.

#### Scenario: Item with lore
- **WHEN** an item has the material `minecraft:book` and the lore lines `a` and `b`
- **THEN** the snapshot item has `material` `minecraft:book` and `minecraft:lore` `[{"text":"a"},{"text":"b"}]`

### Requirement: Font mapping
The system SHALL map a font to a symbol with `font` `<namespace>:<mapper>`, `file` from `texturePath` and `chars` in `orderIndex` order. It SHALL reject the release with `422` when a font provider is not `bitmap`.

#### Scenario: Bitmap font
- **WHEN** project `cygnus` has a font with mapper `glitch` and chars ordered `b`, `a` by `orderIndex` 1 and 0
- **THEN** the symbol has `font` `cygnus:glitch` and `chars` `["a","b"]`

### Requirement: Stable input
The system SHALL assemble the snapshot twice in a row and SHALL use it only when both results are byte-identical. When they differ, it SHALL retry at most two more times and then reject the release with `409`. Nothing SHALL be published for a rejected release.

#### Scenario: Unchanged project
- **WHEN** the project does not change while the release is assembled
- **THEN** both runs are byte-identical and the release continues

#### Scenario: Project keeps changing
- **WHEN** every pair of runs differs
- **THEN** after three attempts the response is `409` and nothing is uploaded

### Requirement: Unsupported content
The system SHALL reject a release with `422` when the project has advancements, and the response SHALL name them. Nothing SHALL be published.

#### Scenario: Project with an advancement
- **WHEN** the project has the advancement `first_page`
- **THEN** the response is `422`, it names `first_page`, and nothing is uploaded

### Requirement: Canonical snapshot
The system SHALL write the snapshot with sorted object keys, no insignificant whitespace and arrays sorted by key or path, so that the same backend state yields byte-identical snapshots.

#### Scenario: Two releases of the same state
- **WHEN** the snapshot is assembled twice from an unchanged backend
- **THEN** both snapshots are byte-identical and have the same revision

### Requirement: Release from the editor
The system SHALL offer `POST /generator/release` with `projectId` and `version`. It SHALL assemble the snapshot, compute the revision from it, use the project `key` as project name, `stelaris` as `createdBy` and the injected clock for `createdAt`, and run the release with the existing validation, publication and responses. The route SHALL be active only when `vulpes.release.editor-route-enabled` is `true` and SHALL answer `404` otherwise. It SHALL NOT require the release token.

#### Scenario: Editor starts a release
- **WHEN** Stelaris posts `{projectId: P, version: "0.2.0"}` for project `cygnus`
- **THEN** the data JAR `net.onelitefeather.vulpes.content:cygnus:0.2.0` is published and the response is `201`

#### Scenario: Route disabled
- **WHEN** `vulpes.release.editor-route-enabled` is not set
- **THEN** `POST /generator/release` answers `404` and nothing is fetched or uploaded

#### Scenario: Existing version
- **WHEN** Stelaris posts a version that is already published
- **THEN** the response is `409` and nothing is uploaded

### Requirement: Revision of automated releases
For `POST /releases`, the system SHALL compare the request `revision` with the SHA-256 of the assembled snapshot and SHALL reject the release with `422` on a mismatch.

#### Scenario: Content changed after the revision was computed
- **WHEN** the request revision differs from the assembled snapshot
- **THEN** the response is `422` and nothing is uploaded
