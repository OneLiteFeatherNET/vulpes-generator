# Proposal

**Conventional Commits:** `feat(pack): ship the space font in every generated pack`

## Why

Games lay out HUDs, inventory titles and background bars with pixel offsets. They do this through the `space:default` font: private-use characters `\uF001`..`\uF01B` whose glyphs advance by -256..+256 pixels. `vulpes-base` already ships the runtime side (`net.onelitefeather.vulpes.font.SpaceFont`) and the advances it relies on (`space:default` in `src/main/resources/font/font-widths.json`, also used by `TextWidth`).

The resource pack side is copied by hand today: `cygnus-pack` and `ManisPack` each carry an identical `assets/space/font/default.json`. If a pack lacks the font or its advances differ from the table in `vulpes-base`, Minecraft neither fails nor warns. The offsets are just wrong on screen. Every project that uses `SpaceFont` needs the font, so it belongs in every pack the generator builds, not in each game's hand-made base pack.

## What Changes

- Every resource pack the generator builds contains `assets/space/font/default.json` with one `space` provider. Its `advances` map is exactly the `space:default` table of `vulpes-base` (`\uF001`..`\uF00D` -> -1..-8, -16, -32, -64, -128, -256; `\uF00E` -> 0; `\uF00F`..`\uF01B` -> 1..8, 16, 32, 64, 128, 256; `" "` -> 1). This holds even for a project with no fonts of its own.
- The generator holds the space font as a fixed, built-in font definition, not as a copied file. It writes the font through the same font writer as project fonts, so the file has the same shape, key order and encoding as every other generated font JSON. The definition is not stored in the backend and cannot be configured.
- A unit test reads the `space:default` table from the `vulpes-base` JAR (test dependency) and fails if the built-in advances differ, so code and pack cannot drift apart unnoticed.
- A project font or asset in the namespace `space` is rejected when the pack is built (HTTP 422 on `POST /releases`, naming the offending key), because it would overwrite or collide with the shipped font.

**Dependency:** this change extends the pack build that the later pack-build change introduces (see "Kept for the later pack-build change" in `remove-git-and-codegen`). It is applied together with or after that change. Until then, games keep the font in their base pack.

## Non-goals

- No change to `release.json`, `snapshot.json` or the data JAR layout. The snapshot carries no space font. The Gradle plugin generates the glyph constants on its own (change `add-space-font-constants` in OneLiteFeatherNET/vulpes-gradle-plugin), and games build offsets with `SpaceFont` from `vulpes-base`.
- The unused extras of today's hand-made packs are not shipped: the `splitter.png` bitmap provider (``) and the `space.<n>` / `newlayer` translations in `assets/space/lang/en_us.json`. No game code references them. A base pack that still needs them has to move them to another namespace, because the generated pack wins on a collision.
- The advances stay fixed. Projects cannot configure them.

## Capabilities

### New Capabilities
- `resource-pack-space-font`: every generated resource pack contains the `space:default` font matching `vulpes-base`.

### Modified Capabilities
- None.

## Impact

- New built-in font definition and pack-build step in the generator. `vulpes-base` becomes a test-only dependency.
- Games (Cygnus, Manis) can drop `assets/space/font/default.json` from their base packs once their packs come from the generator.
