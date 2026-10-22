---
id: dk-aef47f84
type: task
created: 2026-10-21
status: done
since: 2026-10-22
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

Done 2026-10-22 (round-2 fixes, package moves only): migration/ is now at seven entries. LegacyDoorConversion, LegacyTrimConversion, LegacyWindowConversion, LegacyCells and DoorTrimConversion moved under migration/conversion/ and migration/trim/ is gone (the conversion classes and LegacyCells.Result became public for the cross-package calls). Single-file folders: block/attachment/loot and block/attachment/entity folded into block/attachment/ (still ten entries) and block/assembly/appearance folded into block/assembly/. The per-family appearance/ and interaction/ pairs (door, archway, window, trim), block/window/connection and showcase/section/archway are declared deliberate in STRUCTURE.md. client/item and client/render are no longer single-file folders.
