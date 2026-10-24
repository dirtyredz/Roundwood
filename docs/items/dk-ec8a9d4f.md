---
id: dk-ec8a9d4f
type: bug
created: 2026-10-24
status: done
since: 2026-10-24
area: archway
priority: P1
rank: zzzt
parent:
fixes: []
blocked_by: []
relates: []
---
# Fill archway corners without reducing walking clearance

Fill both archway square footprints outside their unchanged openings at full wall depth. Share geometry across body, casing, item, target and preview. Preserve 0.13.1 lower-jamb-only collision: upper fill stays non-colliding to avoid shrinking walking clearance. Add geometry guards, regenerate models, bump 0.13.2 and update docs. Verify offline datagen and test build; never run client or GameTest server.
