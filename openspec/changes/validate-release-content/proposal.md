# Proposal

**Conventional Commits:** `fix(release): validate content references before a release`

## Why

A content release can be published while its content points at files that do not exist. The Cygnus sound `cygnus:vhs_static` plays `cygnus:vhs_static_1` to `_3`, but the project has no such assets. The generator published the release anyway. The problem only showed up later, in the Gradle plugin's local override, which aborted with "uses resource location 'cygnus:vhs_static_1' that has no asset entry". In the game, such a sound or glyph is silent or renders as a missing texture, without any error.

The opposite also happens: the generator refuses content that is fine. Since the backend accepts sound uploads (`.ogg`) and Blockbench models (`.bbmodel`), a project with a single uploaded sound cannot be released at all. Its assets are refused as `UNSUPPORTED_CONTENT`.

When a release or local build is refused, the editor gets the reason, but the generator's log does not. A `502` on dev could only be explained by repeating the request by hand.

The checks that do exist are spread over the mapper, the reader and the code generation, and each stops at the first problem. An author who fixes one problem only learns about the next on the following attempt.

## What Changes

- Sound assets become part of the snapshot's `assets` with the path `assets/<namespace>/sounds/<path>.ogg` and kind `SOUND`, instead of refusing the release. Blockbench models are editor sources: they are left out of `assets` and do not refuse the release.
- A content validation step runs on the assembled snapshot before code generation, for releases and local builds. It collects all problems in one pass, not just the first:
  - every bitmap font `file`, every sound entry of type `file`, and every item model or texture reference resolves to an entry of `assets` (references in the `minecraft` namespace are vanilla resources and are not checked);
  - asset paths, sound keys, font keys and item keys are unique and are valid resource locations (`[a-z0-9_.-]+:[a-z0-9_./-]+`);
  - a font's glyph rows have the same width, and no character is used by two fonts of the same font key.
- A release with problems is refused with the new reason `INVALID_CONTENT` (`422`). The response lists every problem as a diagnostic, naming the content entry (for example `sound cygnus:vhs_static`) and what is wrong. Nothing is published.
- A local build with problems is still delivered, so an author can keep working. The problems are logged at `WARN`, and the response carries the header `Vulpes-Content-Problems` with their count.
- New `GET /projects/{projectId}/validation{?baseBranch}`: runs the validation on the current content without building anything and answers `200` with the list of problems, which is empty when the content is valid. The editor can show the problems before the author starts a release.
- Every refused release or local build is logged at `WARN` with the project, the reason and the first diagnostics. Credentials and content bodies are never logged.

## Non-goals

- No check of file contents (PNG decoding, Ogg header, mono sound). The backend already checks them on upload.
- No change to the release JSON format, to the data JAR, or to the Gradle plugin. The plugin's own check of missing local assets stays as is.
- No resource pack build; that is the change `build-resource-pack`.
- No validation in the backend. Moving the checks into the backend is possible later. The generator is the last point before publishing, so it has to check either way.

## Capabilities

### New Capabilities
- `release-content-validation`: the generator validates the references, keys and fonts of the content, refuses invalid releases with all problems at once, and offers the check on its own.

### Modified Capabilities
- None.

## Impact

- New validation step between snapshot assembly and code generation, in `ReleaseService` and `LocalBuildService`.
- `ProjectSnapshotMapper` maps sound assets and skips Blockbench models.
- New reason `INVALID_CONTENT` in `ReleaseRejectedException` and its `422` mapping, and a new controller for `GET /projects/{projectId}/validation`.
- The editor (Stelaris) can show the diagnostics list; it already shows the diagnostics of `COMPILATION_FAILED` in the same shape.
