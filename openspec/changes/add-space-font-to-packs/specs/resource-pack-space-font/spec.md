## ADDED Requirements

### Requirement: Space font in every pack
The system SHALL write `assets/space/font/default.json` into every resource pack it builds. The file SHALL contain exactly one provider of type `space`. Its `advances` SHALL map `\uF001`..`\uF008` to -1..-8, `\uF009` to -16, `\uF00A` to -32, `\uF00B` to -64, `\uF00C` to -128, `\uF00D` to -256, `\uF00E` to 0, `\uF00F` to 1, `" "` to 1, `\uF010`..`\uF016` to 2..8, `\uF017` to 16, `\uF018` to 32, `\uF019` to 64, `\uF01A` to 128 and `\uF01B` to 256.

#### Scenario: Project without fonts
- **WHEN** a pack is built for a project that has no fonts
- **THEN** the pack contains `assets/space/font/default.json` with the `space` provider and its 28 advances

#### Scenario: Project with own fonts
- **WHEN** a pack is built for a project with its own fonts
- **THEN** the pack contains the project's font files and `assets/space/font/default.json`

### Requirement: Advances match vulpes-base
The built-in space font SHALL have the same advances as the `space:default` table in `font/font-widths.json` of `vulpes-base`. A test SHALL compare the two and fail on any difference.

#### Scenario: Table drifts
- **WHEN** the `space:default` table in `vulpes-base` contains an advance the built-in font does not have, or a different value
- **THEN** the generator's test suite fails and names the codepoint

### Requirement: Reserved space namespace
The system SHALL reject a release whose content contains a font or asset in the namespace `space`. It SHALL answer `POST /releases` with `422` and name the offending key.

#### Scenario: Project font in space namespace
- **WHEN** a release is requested for content that contains the font `space:default`
- **THEN** the response is `422`, it names `space:default`, and no pack or data JAR is published

### Requirement: Same format as project fonts
The system SHALL write the space font through the same font writer as project fonts, with the same structure, key order and encoding. The space font SHALL NOT be configurable.

#### Scenario: Written like a project font
- **WHEN** a pack contains the space font and a project font
- **THEN** both font files are written by the same font writer and differ only in their providers

### Requirement: Reproducible space font
The system SHALL write the space font with the same bytes for every build.

#### Scenario: Two builds
- **WHEN** two packs are built from the same release
- **THEN** both contain a byte-identical `assets/space/font/default.json`
