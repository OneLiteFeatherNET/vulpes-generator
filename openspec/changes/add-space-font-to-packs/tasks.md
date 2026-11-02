# Tasks

Test notes: unit tests only, `@TempDir` for pack output, no network, no sleeps, no system clock.

## 0. Prerequisite

- [ ] 0.1 Confirm that the pack-build change is merged or applied in the same pull request, and link it

## 1. Bundled font

- [ ] 1.1 Add `vulpes-base` as a test dependency
- [ ] 1.2 Write the failing test that the bundled `space/font/default.json` has exactly the advances of `space:default` in `vulpes-base`'s `font/font-widths.json`, naming the first differing codepoint
- [ ] 1.3 Add the bundled resource with the single `space` provider and its 28 advances

## 2. Pack build

- [ ] 2.1 Write the failing test that a pack built for a project without fonts contains `assets/space/font/default.json`
- [ ] 2.2 Write the failing test that a pack built for a project with fonts contains both the project's font files and the space font
- [ ] 2.3 Write the failing test that two builds of the same release write byte-identical space font files
- [ ] 2.4 Write the space font in the pack build with sorted keys

## 3. Reserved namespace

- [ ] 3.1 Write the failing test that `POST /releases` with a font or asset in namespace `space` answers `422`, names the key and publishes nothing
- [ ] 3.2 Reject the `space` namespace before anything is built or published

## 4. Pull request

- [ ] 4.1 Open the code pull request `feat(pack): ship the space font in every generated pack`
