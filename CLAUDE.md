# CLAUDE.md - working on Roundwood

Standalone mod repo inside the `minecraft-mods` workspace (see `../../CLAUDE.md`). Orientation:
[README.md](README.md), [STRUCTURE.md](STRUCTURE.md), [docs/](docs/).

- **Commit identity:** `dirtyredz <dirtyredz@live.com>`.
- **Headless only.** Never run `runClient`. Verification is `node tools/generate-textures.mjs`, `runData`, `test build`,
  `runGameTestServer`, `git diff --check`, and `..\..\tools\sync-mod-files.ps1 -Check` from the
  workspace. The owner checks visuals and client behaviour in game.
- **Generated resources are source.** Change a provider, run `./gradlew runData`, commit the
  regenerated `src/generated/resources/` with the code. Never hand-edit generated JSON.
- **Separate authorities, no catalogue.** Content is three blocks; looks are data. Which items
  restyle: `appearance/material` (tagged planks, the fixed four masonry). What a look draws:
  `client/material` (`BuiltinWoodStyles`, `PaintStyles`) plus the generated art. Examples:
  `AppearanceSamples`. History: `migration/LegacyAppearances` is frozen, never edit it. New artwork
  needs a palette entry appended (never reordered: seeds follow position) in `tools/textures/` and
  `node tools/generate-textures.mjs`; the `data/` tests pin the exact texture manifest and the old
  bytes. Run `node tools/generate-test-structures.mjs` when a template changes.
- **Layout:** every source and resource directory is declared in STRUCTURE.md `## Layout`. GameTests
  stay in `src/gameTest/`, unit tests in `src/test/`, generators in `tools/`.
- **Version** lives only in `gradle.properties` (`mod_version`).
- Shared files (wrapper, `.gitattributes`, `.gitignore`) come from `../../tools/mod-template/`;
  never edit them here.
- **Showcase rule:** every new block or feature adds or updates its section in `showcase/`
  (`ShowcaseSections` is the registry; `/roundwood showcase` builds them all), plus the matching
  check in `ShowcaseGameTests`.
