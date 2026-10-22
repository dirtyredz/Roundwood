---
id: dk-ff89f8e5
type: task
created: 2026-10-22
status: todo
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

The acting-hand rule (offhand acts only when main hand is empty), TrimAttachmentItem.slotOf, the shears test (ShearsItem || Tags.Items.SHEARS) and the TRIM-donor test are written three times: ArchwayInteraction.java:81, WindowInteraction.java:89-115, DoorInteraction.java:117-147. The sneak-use bridge (claims) trusts all three to agree.
Direction: a HeldTrimTool value in block/attachment classifying the stack as ATTACHMENT(slot) / SHEARS / DONOR(family) / OTHER with the acting-hand rule; each host maps it to its own Action (WindowUseRules.Item is already that shape).
