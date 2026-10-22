---
id: dk-7abe21de
type: task
created: 2026-10-21
status: todo
since: 2026-10-21
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
