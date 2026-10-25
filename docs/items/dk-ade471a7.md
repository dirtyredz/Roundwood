---
id: dk-ade471a7
type: task
created: 2026-10-25
status: todo
since: 2026-10-25
area: migration
priority: P3
rank: w
parent:
fixes: []
blocked_by: []
relates: []
---
# Retired archways: custody of unknown trim payloads on break/pick/re-place

Arch pieces plan 2026-10-25 (batches 11-12, cut by the orchestrator as an edge case). Retired roundwood:archway/keyhole_archway keep current behaviour. Hardening: breaking/picking an assembly with an uneditable trim payload yields a hidden stack carrying the full persisted payload (disk data, not getUpdateTag); retired-stack placement restores and validates it inside the assembly rollback boundary, or refuses unchanged. Needs a validated anchor-content overload in AssemblyInstallation. See C:/Users/dirty/projects/artifacts/minecraft-mods/jobs/2026-10-25-roundwood-arch-pieces/plan.md.
