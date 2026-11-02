# Proposal

**Conventional Commits:** `feat(release): build the release snapshot from the backend api`

## Why

The release flow (`add-content-release-data-jar`) builds the content data JAR from a format 1 snapshot that it fetches with `GET /project/{projectId}/snapshot`. The backend has no such endpoint, neither in production (3.3.0) nor on its main branch. No release can be built today. In the local full test with Cygnus, a hand-written bridge script read the normal backend endpoints and assembled the snapshot. That is the job this change gives to the generator.

The editors also have no way to start a release. Stelaris still offers its old build dialog, which picks a Git branch and calls `GET /generator/generate`. `POST /releases` needs a shared bearer token and a revision that only someone holding the snapshot can compute. The browser has neither, so the release has to fit the existing dialog: the editor enters a version and the generator does the rest.

## What Changes

- **Snapshot assembly:** a new `ReleaseSnapshotSource` adapter reads the backend's existing REST endpoints of a project and assembles a format 1 snapshot. It replaces the adapter that calls `GET /project/{projectId}/snapshot`. It reads the project, items with components and lore, attributes, fonts with their chars, sound events with their sources, expression functions, expressions and asset metadata. Every list endpoint is read page by page until `totalSize` entries are collected.
- **Mapping to format 1**, as defined by the contract in `OneLiteFeatherNET/vulpes-gradle-plugin` (`snapshot-1.schema.json`):
  - Namespace: the project `key`. An entry key without a namespace gets the project namespace (`statistics` -> `cygnus:statistics`). A key whose first dot-separated segment is listed in `vulpes.release.foreign-namespaces` (default `minecraft`, `olf`) gets that namespace (`minecraft.music.game` -> `minecraft:music.game`), because the backend cannot store a second namespace in a key.
  - Ids are the backend ids, `previousKeys` and `previousNames` are empty.
  - Items: `material` from the component `stelaris:material`, `amount` 1, every other component unchanged, lore lines in order as `minecraft:lore` text components `{"text": line}`.
  - Attributes: `displayName` from `uiName`, the values and `synced` unchanged, `translationKey` as stored.
  - Fonts: `font` is `<namespace>:<mapper>`, `file` is `texturePath`, `ascent` and `height` unchanged, `chars` in `orderIndex` order. A provider other than `bitmap` rejects the release.
  - Sound events: `subtitle` from `subTitle` (blank -> `null`), `replace` `false`, sources with their fields and `type` in lower case.
  - Expression functions and expressions: `name` from `name` or `uiName`, `description` from `comment`, variable and parameter types in lower case, `expression` from `body` or `expression`.
  - Assets: `path` from key and kind as in the resource pack (`assets/<ns>/textures/<path>.png`, `assets/<ns>/models/<path>.json`), `sha1`, `size` and `kind` in lower case, `resolution` `null`.
  - Pack: `description` `{"text": <project displayName>}`, `resolutions` `[16]`.
  - The snapshot is written canonically (sorted keys, no insignificant whitespace, arrays sorted by key or path), so the same backend state always yields the same bytes and the same revision.
- **Stable input instead of a frozen snapshot (interim step, consciously accepted):** the design expects a frozen snapshot, but the backend cannot provide one. The generator therefore assembles the snapshot twice in a row and uses it only when both runs are byte-identical. When they differ, because someone edited the project in between, it retries up to two more times and then rejects the release with `409` ("content changed during the release"). This narrows, but does not remove, the window for mixed states: an edit that is reverted between the two runs is not detected. A backend snapshot endpoint that reads in one transaction would replace this step later.
- **Unsupported content is rejected, not dropped:** a project with advancements answers `422` and names them, because the mapping of advancements is not part of this change.
- **Release from the editor:** a new route `POST /generator/release` takes `{projectId, version}`. The generator assembles the snapshot, computes the revision itself, sets `createdBy` to `stelaris` and `createdAt` from the injected clock, and runs the existing release (data JAR, publication, immutable versions, response). The project name of the coordinates is the project `key`. The route publishes immutable artifacts, but a browser cannot hold the shared release token, and the editor has no login yet. It is therefore off by default and only active when `vulpes.release.editor-route-enabled` is `true`. A deployment turns it on only when the generator is reachable from the internal network alone, the same trust level as the unauthenticated backend today. When it is off, the route answers `404`. Once Stelaris and the generator have a login (backend OIDC, OneLiteFeatherNET/Vulpes-Backend#200), the route requires it and the flag goes away.
- **`POST /releases` stays** for automation. Its `revision` is now checked against the assembled snapshot.

## Non-goals

- No backend change. The generator only uses endpoints that backend 3.3.0 already has.
- No atomic snapshot (see the double assembly above). The revision in `release.json` still describes exactly the bytes that were published.
- No login for the editor route in this change.
- The old Java generation, the Git integration and the routes `/generator/generate`, `/download` and `/git/branches` stay until Stelaris uses the new route (change `remove-git-and-codegen`).
- No pack build. `packs` stays empty.
- The Stelaris dialog itself is changed in the Stelaris repository.
- Lore styling: lines become plain text components. Styling such as colour or italics needs styled lore in the backend.
- `replace` of sound events: the backend cannot store it, so it is always `false`.

## Capabilities

### New Capabilities
- `backend-release-snapshot`: assembling the format 1 snapshot from the backend API and starting a release from the editor.

### Modified Capabilities
- None. This change supersedes the requirement "Snapshot source" of the not yet archived change `add-content-release-data-jar`.

## Impact

- Replaces `BackendReleaseSnapshotSource`. New HTTP calls to the backend, new controller route, new configuration `vulpes.release.foreign-namespaces` and `vulpes.release.editor-route-enabled`.
- Stelaris can start a release with only a project and a version.
- Projects whose fonts all share one `mapper` end up in one font file; the imported Cygnus data needs its font mappers set to the font names before its first release.
