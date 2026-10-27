---
id: dk-8e9ddbb7
type: task
created: 2026-10-27
status: todo
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

Slice 1 is implemented by da04e1f: fitted surfaces reuse the standalone merged builder, leaving logical volumes and normal separation intact. A captured 135-case fitted corpus guards the 9,000-quad budget.

Slices 2-4 remain unfinished. The experimental chunk migration was removed before committing. In particular, front door trims currently use the adjacent cell's light/AO context while translating their geometry to the door; ordinary owner-cell chunk wrappers change that context. Camouflage also needs its donor-state tint context. A follow-up must prove these paths, immutable snapshot publication, section invalidation and zero static BER submissions before removing their renderers. The existing static BER paths remain active.

Slice 5 is deferred: leaves retain BER ownership for every pose. No bounded chunk/BER handoff has been proven under delayed uploads or reversals.

Owner approved vanilla owning-cell/host-state lighting on 2026-10-27, accepting documented proud/front-casing and camouflage shading differences while preserving geometry and donor tint. Static BER renderers must remain available behind a legacy client toggle until owner screenshot approval; chunk mode must submit zero static BER geometry and legacy mode must suppress corresponding chunk geometry. Acceptance still outstanding: zero static BER submissions in chunk mode, geometry preservation and owner-cell lighting validation, invalidation across sections, cached surviving hot paths, and owner in-game FPS verification.

Chunk-bake preparation extracts DoorArchModels and its typed cache key without changing BER ownership. The strict headless lighting prerequisite failed after SharedConstants.tryDetectVersion(): NoSuchMethodException: net.minecraftforge.network.NetworkEvent.<init>() during Bootstrap.bootStrap(), because the ordinary JUnit runner lacks Forge event-class transformation. A transformed headless lighting harness remains required before switching ownership. Slices 2-4, the legacy/chunk toggle, section rebuild measurement and rendering compatibility validation remain unfinished. Existing BER rendering remains active.