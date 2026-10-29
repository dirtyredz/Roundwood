---
id: dk-b567c3da
type: task
created: 2026-10-28
status: done
since: 2026-10-28
area: structure
priority: P2
rank: zzzzw
parent:
fixes: []
blocked_by: []
relates: []
---
# Extract polygon clip/partition math out of ArchMasonryModels

Push review 2026-10-28 (componentization P2): ArchMasonryModels mixes pure 2D polygon clipping/partitioning (clip, distance, area, partition, Point, Patch) with mortar-joint layout and BakedQuad rewriting. Extract a pure PolygonClip/PolygonPartition class; keep joint layout + baking in ArchMasonryModels.
