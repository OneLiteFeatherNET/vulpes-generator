## ADDED Requirements

### Requirement: Space font in every pack
The system SHALL write `assets/space/font/default.json` into every resource pack it builds. The file SHALL contain exactly one provider of type `space`. Its `advances` SHALL map ``..`` to -1..-8, `` to -16, `` to -32, `` to -64, `` to -128, `` to -256, `` to 0, `` to 1, `" "` to 1, ``..`` to 2..8, `` to 16, `` to 32, `` to 64, `` to 128 and `` to 256.

#### Scenario: Project without fonts
- **WHEN** a pack is built for a project that has no fonts
- **THEN** the pack contains `assets/space/font/default.json` with the `space` provider and its 28 advances

#### Scenario: Project with own fonts
- **WHEN** a pack is built for a project with its own fonts
- **THEN** the pack contains the project's font files and `assets/space/font/default.json`

### Requirement: Advances match vulpes-base
The bundled space font SHALL have the same advances as the `space:default` table in `font/font-widths.json` of `vulpes-base`. A test SHALL compare the two and fail on any difference.

#### Scenario: Table drifts
- **WHEN** the `space:default` table in `vulpes-base` contains an advance the bundled font does not have, or a different value
- **THEN** the generator's test suite fails and names the codepoint

### Requirement: Reserved space namespace
The system SHALL reject a release whose content contains a font or asset in the namespace `space`. It SHALL answer `POST /releases` with `422` and name the offending key.

#### Scenario: Project font in space namespace
- **WHEN** a release is requested for content that contains the font `space:default`
- **THEN** the response is `422`, it names `space:default`, and no pack or data JAR is published

### Requirement: Reproducible space font
The system SHALL write the space font with sorted object keys and the same bytes for every build.

#### Scenario: Two builds
- **WHEN** two packs are built from the same release
- **THEN** both contain a byte-identical `assets/space/font/default.json`
