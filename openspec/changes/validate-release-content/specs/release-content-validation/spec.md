## ADDED Requirements

### Requirement: Uploaded sounds and Blockbench models
The system SHALL map an asset of kind `SOUND` with key `<namespace>:<path>` to an entry of `assets` with the path `assets/<namespace>/sounds/<path>.ogg` and kind `SOUND`. It SHALL leave assets of kind `BBMODEL` out of `assets`. Neither kind SHALL refuse a release or a local build.

#### Scenario: Project with an uploaded sound
- **WHEN** a project has the sound asset `cygnus:ambient/drone`
- **THEN** the snapshot's `assets` contains `assets/cygnus/sounds/ambient/drone.ogg` with kind `SOUND` and the asset's SHA-1 and size, and the release is not refused

#### Scenario: Project with a Blockbench model
- **WHEN** a project has the Blockbench model `cygnus:slender`
- **THEN** the snapshot's `assets` contains no entry for it, and the release is not refused

### Requirement: Content references resolve
The system SHALL check, before code generation, that every bitmap font `file`, every sound entry of type `file`, and every item model or texture reference names an entry of the snapshot's `assets`. References in the `minecraft` namespace SHALL NOT be checked. It SHALL collect all problems of one content in one pass.

#### Scenario: Sound file without an asset
- **WHEN** the sound `cygnus:vhs_static` has the entries `cygnus:vhs_static_1`, `cygnus:vhs_static_2` and `cygnus:vhs_static_3`, and none of them is an asset of the project
- **THEN** the validation reports three problems, each naming `sound cygnus:vhs_static` and the missing file

#### Scenario: Vanilla reference
- **WHEN** an item uses the model `minecraft:item/book`
- **THEN** the validation reports no problem for it

#### Scenario: Several problems at once
- **WHEN** a font refers to a texture that does not exist and two items share the key `cygnus:page`
- **THEN** the validation reports both problems in the same answer

#### Scenario: Valid content
- **WHEN** every reference resolves and every key is unique and valid
- **THEN** the validation reports no problems

### Requirement: Keys are unique and valid
The system SHALL check that asset paths, sound keys, font keys and item keys are unique and match `[a-z0-9_.-]+:[a-z0-9_./-]+`.

#### Scenario: Invalid key
- **WHEN** an item has the key `Cygnus:Page`
- **THEN** the validation reports one problem naming the item and the invalid key

### Requirement: Fonts are consistent
The system SHALL check that the glyph rows of a bitmap font have the same length and that no character is used twice under the same font key.

#### Scenario: Character used twice
- **WHEN** two bitmap providers of the font `cygnus:icons` both map the character ``
- **THEN** the validation reports one problem naming the font and the character

### Requirement: Invalid content refuses a release
The system SHALL refuse a release whose content has problems with the reason `INVALID_CONTENT` (`422`). The response SHALL list every problem as a diagnostic that names the content entry and the problem. Nothing SHALL be published for a refused release.

#### Scenario: Release with a missing sound file
- **WHEN** a release is requested for content in which the sound `cygnus:vhs_static` refers to a missing file
- **THEN** the release is refused with `INVALID_CONTENT` (`422`), the diagnostics name `sound cygnus:vhs_static` and the missing file, and nothing is uploaded to the repository

### Requirement: Local build with problems
The system SHALL deliver a local build whose content has problems. It SHALL log each problem at `WARN` and SHALL set the response header `Vulpes-Content-Problems` to their count. For valid content it SHALL set the header to `0`.

#### Scenario: Local build with a missing sound file
- **WHEN** a local build is requested for content in which three sound files are missing
- **THEN** the ZIP is delivered with `200`, the header `Vulpes-Content-Problems` is `3`, and the log has three `WARN` lines naming the sound

### Requirement: Validation without a build
The system SHALL answer `GET /projects/{projectId}/validation{?baseBranch}` with `200` and the list of problems of the project's current content, without building, compiling or publishing anything. An unknown project SHALL be answered with `UNKNOWN_PROJECT` (`422`), and an unavailable backend with `SOURCE_UNAVAILABLE` (`502`).

#### Scenario: Editor checks before a release
- **WHEN** the editor requests the validation of a project with one missing font texture
- **THEN** the answer is `200` with one problem naming the font and the missing texture, and nothing is built or uploaded

### Requirement: Refusals are logged
The system SHALL log every refused release and every refused local build at `WARN` with the project id, the reason and at most the first ten diagnostics. It SHALL NOT log credentials, tokens or content bodies.

#### Scenario: Backend unavailable during a local build
- **WHEN** a local build is refused with `SOURCE_UNAVAILABLE` because the content export answered `500`
- **THEN** the log contains one `WARN` line with the project id, `SOURCE_UNAVAILABLE` and the status of the export
