---
id: dk-2abfb2b9
type: bug
created: 2026-10-22
status: done
since: 2026-10-24
area: tests
priority: P2
rank: zzzy
parent:
fixes: []
blocked_by: []
relates: [dk-79af4223]
---
# Intermittent assembly/migration GameTests under load

Seen once on ANU (showcase-slimming branch, 0c051d7 second run), passed on rerun at the same SHA: partialarchwaysareremovednevercompleted, alonehalfisremovednotcompleted, aliasesplacedlaterconvertontheirowntick, incompletecleanuprefundsatmostonce. Likely tick-timing under server load like dk-79af4223. Investigate or isolate into batches; do not quarantine silently.

## Resolution (2026-10-24)

Root cause: the GameTest world was never flat. ForgeGradle's GameTest server generated normal terrain, so plots sat in caves with falling blocks, fluids and cave mobs. Fixed in `26b249c` (shared `gradle/gametest-world.gradle`: flat, no structures, no spawning; guard `environment/TestWorldGameTests`). A separate villager-passage fixture flake was fixed in `0a77f98` (dk-dae6be18).

Evidence: full `test,runGameTestServer` on ANU, 347/347 passing on three consecutive runs at `9e87c1f` (2026-10-24 12:36, 12:38 and 12:40), plus 347/347 at `26b249c` (two runs) and at `86dabc2`. The only failure seen after the flat-world fix was the villager fixture, now fixed.

## Notes

### 2026-10-24T16:05:53.268Z · open · agent

Suspected cause: default normal-terrain GameTest worlds expose plots to cave gravel, fluids and mobs, as confirmed for Herbal Witch. The flat-world setup and environment guard are ported on flat-gametest-world; keep this item open pending clean ANU runs.
