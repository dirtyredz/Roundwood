---
id: dk-8e9ddbb7
type: task
created: 2026-10-27
status: done
since: 2026-10-27
area: rendering
priority: P1
rank: zzzzzz
parent:
fixes: []
blocked_by: []
relates: []
---
# Implement showcase static rendering FPS fix

Implement section 4 of the showcase FPS fix spec (2026-10-26 report2.md), preserving 0.17.0 geometry.

Slice 1 remains implemented by da04e1f: fitted surfaces reuse the standalone merged builder. The captured 135-case fitted corpus produces 8,622 quads, below the 9,000 budget. Preparation commit c43fe8c is preserved.

Slices 2-4 are implemented on fps-chunk-bake. Immutable assembly snapshots publish before model-data refresh and old/new footprint invalidation. Forge model wrappers cache quads by state, side, render type and snapshot. Fitted/standalone arches, door casings/camouflage, window attachments and compatibility frames/archways use chunk geometry. Stable donor tint bindings preserve original indices. A narrow client breaking-data adapter resolves satellites. Surviving animated leaves cache layout, member/hinge iteration, materials and conservative sweep bounds; warmed lookup measurements are 0 B/op. Missing or invalid anchors retire old snapshots and recover when membership is restored.

Payload/load/update/removal invalidation is event-driven. Only supported assembly anchors use bounded client-tick member-state comparisons because Forge lacks a public client block-update event covering all satellite packets; ordinary windows are excluded.

Owner approved vanilla owning-cell/host-state lighting on 2026-10-27, including documented proud/front-casing and camouflage shading differences. The second-attempt correction explicitly excludes a transformed lighting-equivalence harness. Plain JUnit checks pre-lighting geometry, tint, shade/AO flags, publication, membership and emitter guards. Camouflage retains existing shade/AO flags; flat-light fallback is not selected without client evidence.

Owner approved the proud casing, front door casing and camouflaged round-door corner in game on 2026-10-28. The legacyStaticRendering config, all seven static renderers, their registrations and shared BER cell helper are removed. The captured census retains 12,774 static quads and guards 215 migrated pieces with zero static BER submissions (135 fitted + 72 standalone + 8 other). Leaves retain BER ownership for every pose; slice 5 remains excluded.

Local verification: 437 JUnit tests pass; build and release-jar guard pass; 142 focused rendering/door GameTests pass. The invalid-anchor regression was observed failing before its fix. Two combined local invocations were externally stopped after unit/build success, so the outstanding focused GameTests were rerun separately and passed. Earlier slice gates passed 429/104 and 431/68 unit/GameTest counts. An existing local Embeddium 0.3.31+mc1.20.1 jar passed headless class loading, and its bytecode uses the Forge model APIs implemented here. Headless raw quad-bake timing covers all eight captured sections, excluding lighting and GPU upload.

Removal follow-up: screenshot approval is complete. Local removal verification passes the unit/build gate and 73 focused rendering GameTests. Final committed-ref ANU and installation remain orchestrator checks; no client run was performed.