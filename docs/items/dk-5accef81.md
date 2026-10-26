---
id: dk-5accef81
type: task
created: 2026-10-25
status: wip
since: 2026-10-25
area: arch
priority: P1
rank: zzzzt
parent:
fixes: []
blocked_by: []
relates: []
---
# Implement modular arch overlay, trims and rendering (batches 6–9)

Implement the approved arch-piece plan batches 6-9 in the arch-pieces worktree: proposal-based Hammer placement overlay; independent body/front/back appearances and ArchPieceTrim v1 storage/fitting/loot; anchor BER body/casing models with tiled donor UVs; masonry face partitions and legally bounded styled item models. Batches 11-12 remain cut under dk-ade471a7. Part 3 owns retired-content aggregation, recipes/tags/names, showcase and final release documentation.

Implemented and verified so far:
- db40683 and 1b534e1: shared placement proposals, conservative interactive-target guard, every reserved cell, selected shoulder zones and all-facing tests.
- 026e589: shared attachment transactions and persistence, face/reveal routing, unsupported-slot hints, exact-item body loot and three new GameTests.
- 96d050e: body/casing BER models, per-cell lighting, donor tiling, renderer registration, empty world models and geometry/UV tests.
- Batch 9 implementation passed its broad gate and is ready for the closing ANU run. It partitions front/back faces into disjoint donor/mortar polygons, shares that bake with items, shifts the three-wide icon by -16 pixels in X, and supplies dimension-aware transforms. Quarter-pixel joint widths are exact; circular joint ends are tessellated with less than 0.0001 pixel radial error. No historical PNG or retired archway rendering changes.

Observed verification:
- Initial ANU job Roundwood-arch-pieces-20261025-200708-f4c, commit 95c532caf4862cc57f56a140735a2a0c3a1a8302: PASS, 357 unit tests, all 360 GameTests. This settles part 1's two former failures and closes dk-8569b03a.
- ./gradlew.bat --offline --no-daemon runData test --tests '*ArchPiece*' --tests '*ArchMasonry*' compileGameTestJava: passed for attachment and ordinary rendering work.
- ./gradlew.bat --offline --no-daemon runData test build verifyReleaseJar: passed, 367 unit tests, release jar verified (437 main classes).
- Repeat datagen retained all 444 earlier generated files byte-for-byte; eight new empty-world model/blockstate resources were added for batch 8.
- Final focused ./gradlew.bat --offline --no-daemon test --tests '*ArchPiece*' --tests '*ArchMasonry*' --tests '*AppearanceModelCacheTest' compileGameTestJava: passed.
- node tools/generate-textures.mjs: passed, 113 textures, no tracked texture changes. Shared-file sync check, git diff --check and dk check passed.

Intermediate failures, corrected:
- runData compilation: "error: incompatible types: List<CAP#1> cannot be converted to Iterable<Block>". Fixed the loot-provider stream's result type.
- Focused test: "java.lang.NoClassDefFoundError: Could not initialize class net.minecraft.world.item.Items". SimpleBakedModel construction requires Forge's transformed launch; retained direct baked-quad tests and existing shared cache-clear guards instead of constructing the live renderer in plain JUnit.
- Focused test: "org.opentest4j.AssertionFailedError: gui ==> expected: <true> but was: <false>". Forge omits identity transforms; the icon guard now applies vanilla's identity default.

Verification gates for adjacent batches were grouped during Java contention. One accidentally overlapping retry was stopped; subsequent Gradle commands wait for no java.exe, polling every 30 seconds with a 20-minute cap. No local GameTest server or client launch. The closing ANU run remains pending.
Final broad gate: ./gradlew.bat --offline --no-daemon runData test build verifyReleaseJar passed with 370 tests, zero failures, and a verified release jar containing 441 main classes. All 456 generated files are byte-identical after repeat runData. A focused rerun of ArchPieceModelsTest is pending for an added mortar-colour assertion after the broad gate had compiled tests.
