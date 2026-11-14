# Tasks

Test notes: unit tests with an in-process fake backend serving the content export and asset bytes, and an in-process fake S3 store (no network, no sleeps, injected `Clock`, `@TempDir`). Every behaviour starts with a failing test (red, green, refactor). Applied after `validate-release-content`.

## 1. Pack builder

- [ ] 1.1 Write the failing test that a project with a font and a sound gives the ZIP entries of the spec scenario, with `sounds.json` and the font JSON written by the existing codecs
- [ ] 1.2 Write the failing test that `pack.mcmeta` carries the backend's description and formats, and that `pack.png` is the icon asset
- [ ] 1.3 Write the failing test that two builds of the same content give the same bytes
- [ ] 1.4 Implement the pack builder (sorted entries, fixed timestamps, canonical JSON)

## 2. Asset bytes

- [ ] 2.1 Write the failing tests that asset bytes are read once per asset, and that a SHA-1 mismatch or failed read refuses the release with `SOURCE_UNAVAILABLE` naming the asset
- [ ] 2.2 Implement the byte read in `BackendApiClient`

## 3. Upload and release

- [ ] 3.1 Write the failing tests for the upload: key `vulpes/<project>/<sha1>.zip`, skipped when it exists, upload before the JAR, `PACK_STORAGE_UNAVAILABLE` on failure with no JAR published
- [ ] 3.2 Write the failing test that `release.json` lists the pack with id, resolution, URL, SHA-1 and size, and that an unconfigured bucket gives `"packs": []` with one startup `WARN`
- [ ] 3.3 Implement the S3 client, the `vulpes.pack.storage.*` configuration and the reason `PACK_STORAGE_UNAVAILABLE`, and fill `packs` in `ReleaseJsonWriter`

## 4. Local build

- [ ] 4.1 Write the failing test that the local build contains `<project>-resourcepack.zip` and the bucket receives no request
- [ ] 4.2 Implement it in `LocalBuildZip`

## 5. Pull request

- [ ] 5.1 Run `heavy ./gradlew build --max-workers=2`
- [ ] 5.2 Open the code pull request `feat(pack): build the resource pack from the project's assets`
