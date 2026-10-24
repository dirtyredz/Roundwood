---
id: dk-87f3ad8f
type: task
created: 2026-10-21
status: done
since: 2026-10-24
area: structure
priority: P2
rank: zzn
parent:
fixes: []
blocked_by: []
relates: [dk-75a60754]
---
# TrimFaceCondition <-> ModLootConditions registry cycle

Codex sign-off, baseline review 2026-10-21.

src/main/java/io/github/dirtyredz/roundwood/block/attachment/loot/TrimFaceCondition.java:9 depends on registry/ModLootConditions, a second registry cycle that dk-75a60754 does not list.
Direction: the condition owns its type constant and the registry registers it, so the edge points registry -> attachment only.

Sign-off notes on the other items: dk-992020e6 and dk-846bea79 lowered to P2 (frozen frame; cohesive window adapters). dk-73e57897: door vs window masonry faces differ on purpose, so unify the resolver policy, not the artwork. dk-aef47f84: keep responsibility-named single-file packages; fix only migration breadth. dk-75a60754: DoorParticles access is confined to initializeClient; the source edge is still inverted.

## Completed 2026-10-24

TrimFaceCondition owns TYPE and ModLootConditions registers that instance; the attachment-to-registry import is gone.

Verified as a batch: ./gradlew --offline test build compileGameTestJava runData; 331 JUnit tests passed. node tools/generate-textures.mjs and datagen leave generated resources and textures byte-identical. git diff --check, shared-file sync -Check and dk check pass. Runtime GameTests remain with the orchestrator.
