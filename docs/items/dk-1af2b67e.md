---
id: dk-1af2b67e
type: task
created: 2026-10-21
status: todo
since: 2026-10-21
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
