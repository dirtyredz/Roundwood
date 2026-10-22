---
id: dk-4cf186bd
type: task
created: 2026-10-21
status: todo
since: 2026-10-21
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
