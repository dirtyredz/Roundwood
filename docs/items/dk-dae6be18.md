---
id: dk-dae6be18
type: bug
created: 2026-10-24
status: done
since: 2026-10-24
area: tests
priority: P1
rank: zzzw
parent:
fixes: []
blocked_by: []
relates: []
---
# Villager door passage GameTests fail intermittently in the flat world

## Problem

ANU job Roundwood-main-20261024-112111-921 failed right-hinge and fitted-frame villager passage tests on main 26b249c after two passing runs in the guarded flat world.

## Cause and fix

The test corridors had open ends. Normal idle AI could choose a random WALK_TARGET before the per-tick fixture supplied the crossing destination. Villagers could leave the raised floor and get stuck outside, or walk around the door and satisfy the z-only arrival check without opening it. The reproduction also failed a frame-only walk, so this is not a Roundwood door-opening defect.

Shared fixture VillagerPassage closes both ends and seeds WALK_TARGET immediately at spawn. The vanilla brain, navigation, collision and InteractWithDoor remain active; no retries or timeout increases were added. Successful walkers are discarded. Door and trim tests retain expanded target/path diagnostics. An opt-in navigationRepeats Gradle property runs independent required copies, with the usual world guard and related navigation checks.

## Evidence

- Read the ANU log through ssh anu. The supplied failures and No key layers in MapLike[{}] warning were present. The shared generator helper is outside this item and was not changed.
- Before: ./gradlew --offline runGameTestServer -PnavigationRepeats=20, original fixture behavior plus diagnostics, headless BelowNormal. 8/140 walks failed (5.7%); right hinge 2/20, fitted south 1/20, fitted north 2/20, north 1/20, camouflage 1/20, frame-only 1/20. Six related checks passed. Output: "8 required tests failed :(" and "non-zero exit value 8". Log: build/navigation-before.log.
- A right-hinge failure was at BlockPos{x=90, y=-60, z=156}, expected BlockPos{x=81, y=-59, z=147}, with WALK_TARGET and path both BlockPos{x=91, y=-59, z=157}, activities [core, idle]. Four failures said "the door never opened" after reaching the far side. No nested-mutation warning occurred in this focused reproduction; other tests explicitly exercise that guard.
- After: same command and 600-tick budget, 140/140 walks passed (20 each), plus all six related checks. Output: "All 146 required tests passed :)" and "BUILD SUCCESSFUL". Log: build/navigation-after.log.
- git diff --check passed. Workspace tools/sync-mod-files.ps1 -Check passed ("OK 4 mods in sync"); this worktree's six shared files also matched canonical SHA-256 hashes.
- Required fast gate ./gradlew --offline test build compileGameTestJava passed: BUILD SUCCESSFUL, 335 unit tests, zero failures/errors/skips, release jar built and the complete GameTest source set compiled. Log: build/navigation-fast-gate.log. All processes started for this item exited. Full GameTest suite is reserved for the orchestrator on ANU; no full local suite or client run.

Implementation commit: 563e618.