---
id: dk-aef47f84
type: task
created: 2026-10-21
status: todo
since: 2026-10-21
area: structure
priority: P2
rank: n
parent:
fixes: []
blocked_by: []
relates: []
---
# migration/ breadth (11 entries) and single-file folders

Baseline review 2026-10-21 (topology).

migration/ has 9 loose files + block/ + trim/ (trim/ holds only DoorTrimConversion). Single-file folders elsewhere: block/door/appearance, block/door/interaction, block/trim/appearance, block/attachment/loot, block/window/connection, client/item, client/render (client/compat/jei + emi are fine as a pair).
Direction: group conversions under migration/conversion/ (or lift DoorTrimConversion); fold no-growth single-file folders into their parents unless STRUCTURE.md names the symmetry as deliberate.
