# Proposal

**Conventional Commits:** `feat(pack): build the resource pack from the project's assets`

## Why

Authors upload textures, models and sounds to a Vulpes project, and define fonts and sound events there. The resource pack that players download still has to be built by hand: `cygnus-pack` carries copies of the font JSON and a hand-written `sounds.json`, and someone has to keep them in step with the content in the editor. A release says `"packs": []`, so the generated `ResourcePacks` class of a game is empty, and the game points at a pack URL that it configures on its own.

So a font change in the editor reaches the game's code with the next release, but the matching glyph texture reaches the players only after a separate commit to the pack repository. When the two get out of step, players see wrong glyphs or hear nothing, and nothing reports an error.

## What Changes

- Each release builds one resource pack ZIP from the project's content:
  - `pack.mcmeta` from the backend's resource pack settings (description, supported formats), and `pack.png` from its icon asset when set;
  - every texture, model and sound asset at its path from the snapshot's `assets`, with the bytes read from the backend (`GET /project/{projectId}/asset/{assetId}/content`) and checked against the SHA-1 of the export;
  - `assets/<namespace>/sounds.json` from the project's sound events;
  - `assets/<namespace>/font/<name>.json` from the project's bitmap fonts.
- The ZIP is deterministic: entries sorted by path, fixed timestamps, JSON written in one canonical form. The same content always gives the same bytes and the same SHA-1, so the client does not download the pack again when nothing changed.
- The ZIP is uploaded to the public S3 bucket that already serves `cygnus-pack`, under the immutable key `vulpes/<project>/<sha1>.zip`. An existing key is not uploaded again.
- The pack is uploaded before the data JAR is published. `release.json` lists it in `packs` (id, resolution, URL, SHA-1, size), so the Gradle plugin generates a usable `ResourcePacks` constant.
- A local build contains the same ZIP as `<project>-resourcepack.zip` next to the data, for testing in a client. It is not uploaded.
- If the bucket is not configured, releases are built without a pack (`"packs": []`) and the generator logs a `WARN` at startup. If the upload fails, the release is refused with the new reason `PACK_STORAGE_UNAVAILABLE` (`502`) and nothing is published.
- An asset whose bytes do not match the SHA-1 of the export refuses the release with `SOURCE_UNAVAILABLE` (`502`), because the content changed while the release was being built.

## Non-goals

- The hand-made parts of a game's pack stay in its own pack: shaders, post effects, HUD sprites and lang files. A game sends both packs; the client supports several packs since 1.20.3. Fonts and sounds that `cygnus-pack` still writes by hand move to Vulpes step by step, which is a change in `cygnus-pack`.
- No conversion of Blockbench models, no lang files, and one resolution only (16). More resolutions and lang files are later changes.
- No cleanup of old packs in the bucket. The keys are content-addressed, so old releases keep working.
- No change to the release JSON format or to the Gradle plugin; `packs` already exists in format 2.
- The bucket credentials for dev and prod are a separate change in Kubernetes-FLUX.

## Capabilities

### New Capabilities
- `resource-pack-build`: the generator builds a deterministic resource pack from the project's assets and content, uploads it to the public bucket and lists it in the release.

### Modified Capabilities
- `backend-release-snapshot`: a release now reads the asset bytes from the backend in addition to the content export.

## Impact

- New pack builder and S3 client in the generator. The existing font and sound codecs, kept for this change by `remove-git-and-codegen`, are used to write the font JSON and `sounds.json`.
- New configuration `vulpes.pack.storage.*` (endpoint, bucket, region, public URL, credentials), and the reason `PACK_STORAGE_UNAVAILABLE`.
- Depends on `validate-release-content`: only content whose references resolve can be packed.
- The change `add-space-font-to-packs` builds on this pack build.
- Cygnus can drop its hand-written `sounds.json` and font JSON from `cygnus-pack` once its releases carry the pack.
