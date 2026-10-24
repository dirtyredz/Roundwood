---
id: dk-7abe21de
type: task
created: 2026-10-21
status: done
since: 2026-10-24
area: structure
priority: P2
rank: z
parent:
fixes: []
blocked_by: []
relates: []
---
# One Direction quarter-turn helper in geometry/

Baseline review 2026-10-21.

The turn-a-Direction-N-quarter-turns loop is in ModelRetexturing.java:117, CamouflageModels.java:98 (without floorMod, so negative turns differ) and data/client/window/WindowPaneModelWriter.java:175.
Direction: one turn(Direction, int) beside VoxelShapeFactory.quarterTurns, used by all three.

## Completed 2026-10-24

VoxelShapeFactory.turn now owns signed quarter turns; all geometry/client callers and direction tests use it. The cited datagen copy was already absent.

Verified as a batch: ./gradlew --offline test build compileGameTestJava runData; 331 JUnit tests passed. node tools/generate-textures.mjs and datagen leave generated resources and textures byte-identical. git diff --check, shared-file sync -Check and dk check pass. Runtime GameTests remain with the orchestrator.
