# Proposal

**Conventional Commits:** `feat(release): let a font mapper name a font in another namespace`

## Why

A font's mapper names the font file its glyphs land in. The generator always puts that file into the project namespace: `font` is `<project>:<mapper>`. Some fonts of a project live in a shared font of another namespace, though. In Cygnus, seven rank tag fonts belong to the font `olf:rank_tags` of the shared OneLiteFeather pack (keys `olf.rank_tags_player_rank_tags_*`). The generator can only write them as `cygnus:<mapper>`, so the game asks for a font the pack does not have and shows boxes instead of the tags.

The key of such a font already gets its namespace from `vulpes.release.foreign-namespaces` (`olf.rank_tags_…` becomes `olf:rank_tags_…`), because the backend cannot store a colon in a key. The mapper has no such restriction: it is free text in the backend and can carry a colon.

## What Changes

- A mapper that contains a colon is the full font key: mapper `olf:rank_tags` gives `font` `olf:rank_tags`.
- A mapper without a colon keeps today's behaviour: mapper `glitch` in project `cygnus` gives `font` `cygnus:glitch`. The dot rule of `foreign-namespaces` is not applied to mappers, so no existing mapper changes its font.
- A mapper with a colon must be a valid resource location (`^[a-z0-9_.-]+:[a-z0-9_./-]+$`, the `resourceLocation` pattern of the format 1 schema). Otherwise the release is refused with `422` (`UNSUPPORTED_CONTENT`) naming the font key, like the other font checks of the mapper. Nothing downstream catches this today: `ContentReader` does not check the pattern and `vulpes-codegen` writes the key into a string literal, so `olf:`, `a:b:c` or `OLF:x` would only fail in the game.
- A mapper in the reserved namespace `space` (for example `space:default`) needs no new check: `vulpes-codegen` already refuses it, and the release answers `422` (`GENERATION_REFUSED`) naming the font key. A test pins that answer.

### Compatibility

- **Format 1 / vulpes-format:** `font` is a `resourceLocation` with any namespace; the format does not tie it to the project namespace. No format change, no new format version.
- **Gradle plugin / vulpes-codegen:** the font constant is `Key.key(<namespace of font>, <path of font>)`, so `olf:rank_tags` becomes `Key.key("olf", "rank_tags")`. The only namespace rule is the `space` reservation, which still holds.
- **foreign-namespaces:** unchanged and still used for keys. A mapper with a colon is not limited to that list: the colon is explicit, so there is nothing to guess.

## Non-goals

- No change to the backend: the mapper field already accepts a colon.
- No change to how font keys, texture paths or assets are qualified.
- The pack build does not exist in this repository yet. Writing `assets/olf/font/rank_tags.json` from such fonts is the job of that later change, which has to merge providers of fonts from several projects into one shared font.
- The data fix in the dev backend (the seven Cygnus fonts get mapper `olf:rank_tags`) is done by hand after the deploy and is not part of the code.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `backend-release-snapshot`: the font mapping accepts a full font key as mapper.

## Impact

- `ProjectSnapshotMapper.fonts()` and `ProjectSnapshotMapperTest`, plus a codegen port test for the `space` refusal.
- Snapshots of existing projects stay byte-identical as long as no mapper contains a colon.
