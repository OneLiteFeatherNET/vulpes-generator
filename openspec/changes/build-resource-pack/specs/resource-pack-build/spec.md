## ADDED Requirements

### Requirement: Pack content
The system SHALL build one resource pack ZIP for each release. The ZIP SHALL contain `pack.mcmeta` from the project's resource pack settings, `pack.png` when the settings name an icon asset, every entry of the snapshot's `assets` at its path, one `assets/<namespace>/sounds.json` per namespace that has sound events, and one `assets/<namespace>/font/<name>.json` per bitmap font.

#### Scenario: Project with a font and a sound
- **WHEN** a release is built for a project with the font `cygnus:glitch` (texture `cygnus:font/glitch`) and the sound event `cygnus:ambient.drone` playing the sound asset `cygnus:ambient/drone`
- **THEN** the ZIP contains `pack.mcmeta`, `assets/cygnus/textures/font/glitch.png`, `assets/cygnus/font/glitch.json`, `assets/cygnus/sounds/ambient/drone.ogg` and `assets/cygnus/sounds.json` with the event `ambient.drone`

#### Scenario: Project without assets
- **WHEN** a release is built for a project without assets, fonts or sounds
- **THEN** the ZIP contains only `pack.mcmeta`

### Requirement: Asset bytes from the backend
The system SHALL read the bytes of every packed asset from `GET /project/{projectId}/asset/{assetId}/content` and SHALL compare their SHA-1 with the SHA-1 of the content export. A mismatch or a failed read SHALL refuse the release with `SOURCE_UNAVAILABLE` (`502`), and nothing SHALL be published.

#### Scenario: Asset changed during the release
- **WHEN** the bytes of an asset have a different SHA-1 than the export lists
- **THEN** the release is refused with `SOURCE_UNAVAILABLE` (`502`), naming the asset, and nothing is uploaded

### Requirement: Deterministic pack
The system SHALL write the ZIP with entries sorted by path, a fixed timestamp for every entry, and JSON in one canonical form. The same content SHALL give the same bytes.

#### Scenario: Same content twice
- **WHEN** two releases are built from content with the same revision
- **THEN** both ZIPs have the same bytes and the same SHA-1

### Requirement: Pack upload and listing
When the pack bucket is configured, the system SHALL upload the ZIP to the key `vulpes/<project>/<sha1>.zip` before it publishes the data JAR, SHALL skip the upload when that key already exists, and SHALL list the pack in `release.json` under `packs` with its id, resolution `16`, public URL, SHA-1 and size. A failed upload SHALL refuse the release with `PACK_STORAGE_UNAVAILABLE` (`502`), and nothing SHALL be published to the repository.

#### Scenario: Release with a configured bucket
- **WHEN** a release of `cygnus` is built and its pack has the SHA-1 `S`
- **THEN** the object `vulpes/cygnus/S.zip` exists in the bucket, and `release.json` lists one pack with the URL `<public-url>/vulpes/cygnus/S.zip`, the SHA-1 `S` and the size of the ZIP

#### Scenario: Pack already uploaded
- **WHEN** the key `vulpes/cygnus/S.zip` already exists
- **THEN** the pack is not uploaded again and the release lists the same URL

#### Scenario: Bucket unavailable
- **WHEN** the upload of the pack fails
- **THEN** the release is refused with `PACK_STORAGE_UNAVAILABLE` (`502`) and the data JAR is not published

#### Scenario: Bucket not configured
- **WHEN** no pack bucket is configured
- **THEN** the release is published with `"packs": []` and the generator logged one `WARN` about the missing bucket at startup

### Requirement: Pack in the local build
The system SHALL add the pack ZIP to the local build as `<project>-resourcepack.zip` and SHALL NOT upload it.

#### Scenario: Local build
- **WHEN** a local build of `cygnus` is requested
- **THEN** the local build ZIP contains `cygnus-resourcepack.zip` with the same bytes a release of the same content would upload, and the bucket receives no request
