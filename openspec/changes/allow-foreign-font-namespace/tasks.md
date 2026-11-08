# Tasks

Test notes: pure unit tests of `ProjectSnapshotMapper` with in-memory input; release refusals with the existing in-process fakes (no network, no sleeps, injected `Clock`, `@TempDir`). Every behaviour starts with a failing test (red, green, refactor).

## 1. Font key from the mapper

- [ ] 1.1 Write the failing test that mapper `olf:rank_tags` in project `cygnus` gives `font` `olf:rank_tags`
- [ ] 1.2 Write the test that mapper `olf.rank_tags` gives `font` `cygnus:olf.rank_tags` (dot rule not applied)
- [ ] 1.3 Keep the test that mapper `glitch` gives `font` `cygnus:glitch`
- [ ] 1.4 Write the failing test that mappers `olf:`, `:x`, `a:b:c` and `OLF:rank_tags` are rejected with `UNSUPPORTED_CONTENT` and the message names the font key
- [ ] 1.5 Use a mapper with a colon unchanged as `font` in `ProjectSnapshotMapper.fonts()` after checking it against the resource location pattern, and update its Javadoc

## 2. Space reservation stays in place

- [ ] 2.1 Write the test that a snapshot with font `space:default` is refused as `GENERATION_REFUSED` and the message names the font key

## 3. Pull request

- [ ] 3.1 Run `heavy ./gradlew build --max-workers=2`
- [ ] 3.2 Open the code pull request `feat(release): let a font mapper name a font in another namespace`
