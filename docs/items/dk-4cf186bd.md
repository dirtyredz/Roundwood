---
id: dk-4cf186bd
type: task
created: 2026-10-21
status: done
since: 2026-10-24
area: migration
priority: P2
rank: zt
parent:
fixes: []
blocked_by: []
relates: []
---
# LegacyAlias contract to replace three instanceof ladders (when next touched)

Baseline review 2026-10-21.

migration/LegacyChunkMigration.java:28,46,60: LegacyDoorBlock/LegacyTrimBlock/LegacyWindowBlock expose canonical()/defaultAppearance() with no shared contract; three ladders (isLegacy, convert, key) dispatch over them; each Legacy*Conversion repeats load -> check -> writes -> replace -> restore. The legacy code is frozen: only worth doing when next touched.

## Completed 2026-10-24

LegacyAlias replaces the three discovery, conversion and key ladders. Family-specific conversion custody remains explicit; DECISIONS.md records the paid-camouflage and full-payload differences.

Verified as a batch: ./gradlew --offline test build compileGameTestJava runData; 331 JUnit tests passed. node tools/generate-textures.mjs and datagen leave generated resources and textures byte-identical. git diff --check, shared-file sync -Check and dk check pass. Runtime GameTests remain with the orchestrator.
