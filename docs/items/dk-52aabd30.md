---
id: dk-52aabd30
type: task
created: 2026-10-28
status: done
since: 2026-10-28
area: structure
priority: P2
rank: zzzzz
parent:
fixes: []
blocked_by: []
relates: []
---
# Collapse ArchPieceModels quads/look overload cascade

Push review 2026-10-28 (abstraction P2): ArchPieceModels has three quads(...) arities and look(appearance, textures) only for tests; ArchwayModels.look same shape; DoorArchModels.quads calls through. Collapse to one explicit-parameter quads, tests call it directly; move the renderSurfaces separation policy next to SurfaceSeparation/ArchMeshModels.
