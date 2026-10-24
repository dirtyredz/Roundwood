---
id: dk-1af2b67e
type: task
created: 2026-10-21
status: done
since: 2026-10-24
area: tools
priority: P1
rank: zt
parent:
fixes: []
blocked_by: []
relates: []
---
# Texture tool modules: split by responsibility, not release; dedupe glass and polar maths

Baseline review 2026-10-21 (componentization + abstraction).

- tools/textures/window-trims.mjs:117 also holds clearWindowGlass and trimMarker and re-exports GLASS_SIZE; glass work is spread over three files.
- window-trims.mjs:31 polar()/brickColour() generalise radial-bricks.mjs:16,32 polar()/radialFace.
- The glass-streak mask is copied in window-glass.mjs:13 and stained-glass.mjs:14.
GeneratedTexturesTest pins output bytes, so dedup is safe to verify.
Direction: move clearWindowGlass to window-glass.mjs, trimMarker to its own module, import GLASS_SIZE from its home; reimplement legacy polar/radialFace on the general functions; one shared streak-mask helper; the SHA tests confirm.

0.12.0 started the split: tools/textures/masonry/ holds the contour masonry faces (square-trims, window-shape-trims, door-casings moved with byte-identical output; archways new), leaving tools/textures/ at 10 modules plus masonry/.

Done 2026-10-22 (round-1 fixes, bytes unchanged: generate-textures rewrites no PNG, GeneratedTexturesTest green):
- New masonry/mortar.mjs owns jointCoverage, nearest, brickColour, withMortar, radialMortar and ONE parameterised polar(x, y, cx, cy, count, span); it replaces window-trims polar, door-casings polarAbout and archways voussoir (a partial span keeps the unwrapped index the half-circle voussoirs used).
- New masonry/arch-window.mjs holds ARCH_FACE_SIZE, courseMortar and archWindowFace, so masonry/ no longer imports the parent window-trims.mjs. window-trims.mjs keeps only the round face and trimMarker.
- clearWindowGlass and CLEAR_ALPHA_DROP moved into window-glass.mjs; the GLASS_SIZE re-export is gone.

Still open: trimMarker in its own module (tools/textures/ is already at the 10-entry cap, so it needs a grouping folder first); reimplementing radial-bricks.mjs polar/radialFace on mortar.mjs polar (its byte pins are the oldest); the shared glass-streak mask (window-glass.mjs / stained-glass.mjs).

## Completed 2026-10-24

Glass tools are grouped with one streak mask; trimMarker has its own module, and radial masonry lives under masonry/. The earlier partial refactor is retained.

Verified as a batch: ./gradlew --offline test build compileGameTestJava runData; 331 JUnit tests passed. node tools/generate-textures.mjs and datagen leave generated resources and textures byte-identical. git diff --check, shared-file sync -Check and dk check pass. Runtime GameTests remain with the orchestrator.
