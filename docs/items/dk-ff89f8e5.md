---
id: dk-ff89f8e5
type: task
created: 2026-10-22
status: done
since: 2026-10-22
area: structure
priority: P1
rank: zzn
parent:
fixes: []
blocked_by: []
relates: [dk-992020e6]
---
# Shared held-item classifier for trim hosts (HeldTrimTool)

Change review 2026-10-22 (abstraction lens).

Problem: the acting-hand rule, TrimAttachmentItem.slotOf, the shears test (ShearsItem || Tags.Items.SHEARS) and the TRIM-donor test were written three times (ArchwayInteraction, WindowInteraction, DoorInteraction), and the sneak-use bridge (claims) trusted all three to agree.

Done (0.12.0 refactor): block/attachment/interaction/HeldTrimTool classifies the acting hand once, in priority attachment (with slot) > shears (class or forge:shears tag) > TRIM donor (keeping the AppearanceGestures gesture: kind WOOD/PAINT/MASONRY, hand, paying stack) > other; the offhand acts only while the main hand is empty. Classification only, no routing.

Callers: ArchwayInteraction.decide/use, DoorInteraction (attachment fit, ring remove/restyle, the empty-main-hand offhand hand-over), WindowInteraction.item (maps the donor kind to DYE/PLANKS/MASONRY), DoorSelectionShapes (HeldTrimTool.holdsAttachment for ring targeting; its separate looser anchor read is unchanged).

Claim policies kept per host: the door returns an unclaimed FAIL for an Arch Sill or an invalid open-door fit, while archways and windows claim and refuse with a hint; door leaf gestures keep their sneak requirement and DOOR acceptance; ring-versus-body targeting, installed-trim requirements and free shears no-ops unchanged. AppearanceUseEvents and WindowUseRules unchanged.

Verification: HeldTrimToolGameTests (20 tests, written before the extraction, with a test-only forge:shears tag on carrot_on_a_stick) pass before and after; ANU Roundwood-9375e5f-20261022-092627-8f4: 330 unit, 327 GameTests pass.
