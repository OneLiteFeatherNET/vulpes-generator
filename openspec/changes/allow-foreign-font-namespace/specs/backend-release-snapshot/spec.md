## MODIFIED Requirements

### Requirement: Font mapping
The system SHALL map a font to a symbol with `file` from `texturePath` and `chars` in `orderIndex` order. When the mapper contains a colon, the system SHALL use the mapper unchanged as `font`. Otherwise `font` SHALL be `<namespace>:<mapper>` with the project namespace. A mapper with a colon SHALL match `^[a-z0-9_.-]+:[a-z0-9_./-]+$`; otherwise the system SHALL reject the release with `422`, naming the font key. It SHALL reject the release with `422` when a font provider is not `bitmap`.

#### Scenario: Bitmap font
- **WHEN** project `cygnus` has a font with mapper `glitch` and chars ordered `b`, `a` by `orderIndex` 1 and 0
- **THEN** the symbol has `font` `cygnus:glitch` and `chars` `["a","b"]`

#### Scenario: Font in another namespace
- **WHEN** project `cygnus` has a font with key `olf.rank_tags_player_rank_tags_admin` and mapper `olf:rank_tags`
- **THEN** the symbol has `key` `olf:rank_tags_player_rank_tags_admin` and `font` `olf:rank_tags`

#### Scenario: Mapper with a dot is not a namespace
- **WHEN** project `cygnus` has a font with mapper `olf.rank_tags`
- **THEN** the symbol has `font` `cygnus:olf.rank_tags`

#### Scenario: Invalid font key
- **WHEN** a font has the mapper `olf:`, `a:b:c` or `OLF:rank_tags`
- **THEN** the release is refused with `422` (`UNSUPPORTED_CONTENT`), the message names the font key, and nothing is published

#### Scenario: Reserved space namespace
- **WHEN** a font has the mapper `space:default`
- **THEN** the release is refused with `422` (`GENERATION_REFUSED`), the message names the font key, and nothing is published
