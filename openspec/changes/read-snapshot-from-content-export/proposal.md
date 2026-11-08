# Proposal

**Conventional Commits:** `fix(release): read the release snapshot from the backend content export`

## Why

The generator assembles the release snapshot from the backend's list and item endpoints. It reads every list page by page and then makes one request per item (components, lore), per font (chars) and per sound (sources). It does all of this twice, to notice a concurrent edit. A release therefore costs about N+1 requests, is slow, and is still not atomic: an edit that is made between the two reads and reverted before the second one goes unnoticed, and the release can publish a mix of two states.

The backend now offers the content of a project as one consistent read: `GET /project/{projectId}/content` (Vulpes-Backend #286, spec `add-project-content-export`, live on dev). The response holds the whole project, is read in one read-only transaction with REPEATABLE READ and needs no paging. On dev, the sample project Cygnus (18 items, 21 fonts, 32 sounds, 19 expressions) is about 36 KB and arrives in about 0.6 s.

## What Changes

- **One read per release:** `BackendApiReleaseSnapshotSource` reads `GET /project/{projectId}/content` once and fills the `ProjectData` that `ProjectSnapshotMapper` already maps. The double read and its retries are removed, and so are the list, item, font and sound requests and the asset metadata requests.
- **Mapping unchanged:** the mapping rules of `build-release-snapshot-from-backend` stay as they are: namespaces and foreign namespaces, lore as `minecraft:lore` text components, `material` from `stelaris:material`, asset paths, fonts, sound events, expression functions, the pack entry and the canonical output. `ProjectSnapshotMapper` needs no change. What it needs from the export, field by field, is listed under "Mapper input" below.
- **Component values:** the export returns each component `value` as a JSON value, not as a string. The mapper already reads it as a JSON node: the material must be a JSON string, every other component is copied unchanged, whether it is a string, an object or a number. The adapter hands the values over without conversion. A golden test pins the output to the bytes the current per-endpoint reader produces for the sample project, so the change is proven not to alter the snapshot.
- **Revision:** the snapshot `revision` stays the SHA-256 of the format 1 bytes, as today. The export's `revision` is a hash of the backend model and is not used. Its ETag is not used either (see Non-goals).
- **Errors:** a `404` from the export is the same answer as an unknown project today (`UNKNOWN_PROJECT`, `422`). Any other failure (`5xx`, unreachable backend, missing or invalid body) is the same `502` (`SOURCE_UNAVAILABLE`) as today. The generator does not retry. Nothing is published in either case.
- **Project key:** the release still reads the project key with `GET /project/{projectId}`, because the version check runs before the snapshot is assembled and needs the key. The export's `project.key` must equal that key. When it differs, the release is refused with `422` (`PROJECT_MISMATCH`) and nothing is published. This closes the gap that the two reads of the key and the snapshot would otherwise leave.
- **Removed:** the reason `CONTENT_CHANGED` leaves `ReleaseRejectedException.Reason`, its `409` mapping leaves `ReleaseRejectedExceptionHandler`, and the "project changed while the snapshot was assembled" wording leaves the OpenAPI descriptions of the editor and automated routes. `VERSION_EXISTS` keeps its `409`.
- **Asset bytes:** checked. The generator downloads no asset bytes in a release (no request to `/project/{p}/asset/{id}/content` exists in `src/main`). It writes the `sha1` and `size` of the export's asset metadata into the snapshot. There is nothing to verify against `sha1` in this change. Verifying downloaded bytes becomes relevant only with a step that downloads them, and is out of scope.

### Mapper input

`ProjectSnapshotMapper` reads the `ProjectData` record. The adapter fills it from the export as follows; field names are those of the existing endpoints, which the export keeps:

- `project`: `id`, `key`, `displayName`.
- `items`, and `components` and `lore` per item id: component `componentKey` and `value` (any JSON value), lore `text` and `orderIndex`.
- `attributes`: as today.
- `fonts`, and `glyphRows` per font id: `key`, `provider`, `mapper`, `texturePath`, `ascent`, `height`; chars `line` and `orderIndex`.
- `sounds`, and `sources` per sound id: `key`, `subTitle`; source `name`, `type`, `volume`, `pitch`, `weight`, `stream`, `attenuationDistance`, `preload`.
- `expressionFunctions`, `expressions`: `id`, `name`, `uiName`, `kind`, `returnType`, `body`, `expression`, `comment`, `parameters` and `variables` as `name` and `type`.
- `assets`: `key`, `kind`, `sha1`, `size`.
- `advancements`: `key`, so that the mapper keeps rejecting them with `422`.

Because the export holds the entries of each parent inside the parent, the adapter groups them by parent id. Its order is the export's stable order, and the mapper sorts as before.

### Test approach

The fake backend of the integration tests serves `GET /project/{id}/content` from a committed export fixture of the sample project. The golden snapshot is taken from the current reader before any code changes. Tests are unit or in-process, with no network.

## Non-goals

- No ETag caching and no `If-None-Match` in the generator. The export's `revision` is not stored. This is a later step.
- No mapping of `dimensions`, `packs` or `advancements`. Advancements keep answering `422`. Dimensions and packs are ignored, as today.
- Item `enchantments` are in the export. The mapper does not map them today and does not reject them either, so they are dropped silently. This is noted here and not changed. A follow-up should map them or reject them like advancements.
- No retry of the export request. A failed read is a `502`, as it is today.
- No backend change. No change to format 1 or to the generator's HTTP API.
- The project key lookup `GET /project/{projectId}` stays. It is not replaced by the export, so that the version check does not need the full content first.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `backend-release-snapshot`: the requirement "Snapshot from the backend api" is modified (one content export request instead of the list and item requests), the requirement "Stable input" is removed, and the requirement "Content export failures" is added. The requirements are defined in the unarchived change `build-release-snapshot-from-backend`. This change must be archived after that change, or both together in that order.

## Impact

- Code: `BackendApiReleaseSnapshotSource` (one read, no double read), `BackendApiClient` (the list and per-parent reads become unused and are removed), `ReleaseRejectedException` (`CONTENT_CHANGED` removed), `ReleaseRejectedExceptionHandler` (409 mapping removed), the OpenAPI descriptions of `ReleaseController` and `EditorReleaseController`, `EditorReleaseService` (key check).
- Tests: `BackendApiReleaseSnapshotSourceIntegrationTest` (the paging and stable-read tests are replaced), `EditorReleaseControllerIntegrationTest` (the "keeps changing" test is replaced), the fake backend and its fixtures, a new golden snapshot and export fixture.
- Runtime: about 36 KB and one request per release instead of N+1 requests. The production backend must run the content export before this generator version is deployed; a release against a backend without it answers `502`.
- API: the generator no longer answers `409` for concurrent edits. The Stelaris dialog does not depend on that answer.
