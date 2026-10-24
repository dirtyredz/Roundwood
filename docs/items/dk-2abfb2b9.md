---
id: dk-2abfb2b9
type: bug
created: 2026-10-22
status: todo
since: 2026-10-22
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

## Notes

### 2026-10-24T16:05:53.268Z · open · agent

Suspected cause: default normal-terrain GameTest worlds expose plots to cave gravel, fluids and mobs, as confirmed for Herbal Witch. The flat-world setup and environment guard are ported on flat-gametest-world; keep this item open pending clean ANU runs.
