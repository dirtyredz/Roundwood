# Decisions - Roundwood

Newest first.

## 2026-10-22 - One shared door adapter with an explicit design (0.11.0)

The arched door is a second design of the existing door family, not a second family:
`AssemblyDoorBlock` holds everything a door part does, `DoorDesign` (ROUND, ARCHED) names the
outline, columns, width and hardware, and `DoorLayout.Assembly` carries the design. The arched door
reuses the round door's anchor entity type (id `round_door`), appearance family DOOR, camouflage, trim
storage and lifecycle; `RoundDoorBlock` adds only `column`. Rejected: a separate block entity type and
id (more registrations, nothing gained), and keeping `column` on the arched door (a fictitious right
column for a one-wide door).

## 2026-10-22 - One placement plan for the server and the preview (0.11.0)

`DoorPlacementPlan` is the single read-only placement decision, for both doors: players use
`of(block, context)`, the showcase and commands `at(level, block, assembly, open)`, and the client
preview draws the same plan, so what is outlined is what is placed (nothing when the plan is empty).
`DoorPlacement` only writes a plan. Rejected: re-deriving placement on the client (it would drift from
the server's checks).

## 2026-10-22 - Rectangular UV over the existing board art (0.11.0)

The 32x32 board panels are vertical boards with no round-specific feature, so the arched leaf
samples their left half at the same one-texel-per-pixel scale (`AssemblyFaceUv.of(box, face, width,
height, image)`); wood, paint and the neutral fallback come for free and no panel art is new.
Rejected: stretching the square panel (boards would change scale), and a 16x32 texture (Minecraft
reads a non-square texture without metadata as square frames).

## 2026-10-22 - The arched casing is built from geometry; the round ring is not moved (0.11.0)

The arched door's casing is built from the design's outline (`client/door/DoorTrimModels`,
`geometry/PixelSlab`), while the round ring keeps the legacy frame quarter models so its verified look
cannot change. Tradeoff: two casing render paths until the round ring is moved too; named so the owner
can decide, since moving it risks a visible change to a look already trialled.

## 2026-10-22 - Arched profile and pixel slab sit flat in geometry (0.11.0)

`ArchedProfile` (the round-headed opening, shared by the arched leaf and casing and meant for the
0.12.0 archways) and `PixelSlab` (a pixel mask's visible surfaces inside one cell, extracted from the
camouflage corners) live directly in `geometry/` (now 10 entries) instead of the plan's single-file
`geometry/aperture/` folder, which would have been a folder for two files with no sibling group.

## 2026-10-22 - Explicit reflected and single-edge window profiles (0.10.0)

Lower arches, top corners and four single-edge pieces are new `WindowProfile`s (sixteen blocks with
the crossed variants), not a flip blockstate on the existing pieces. Each is a fixed item, like the
left/right arches (0.6.0) and the crossed pieces (0.7.0); poses keep meaning placement only, the
saved names of every existing block are unchanged, and the oriented placement, overlay and preview
needed no new code. The circle is a circular glazed opening in four square panes, matching the
arches' construction, so breaking one quarter leaves the others. Profiles are appended to the enum.
Rejected: a `flipped` property (it would double every oriented blockstate, mix two shapes under one
item and confuse the one-item-one-shape rule). Edge pieces take whole-face artwork over their own 16
pixels: an assembly extent only makes sense for the 2 x 2 window's pieces.

## 2026-10-22 - The arch sill is a separate attachment slot (0.10.0)

The sill is its own item (`arch_sill_trim`) and slot (`TrimSlot.SILL`), with its own payload
(`WindowSillTrim` v1, a second `FaceTrimStore`), loot pools, restyle and removal, so a face can carry
a trim and a sill in different looks and an unknown sill version never blocks the trim. It lies in
the bottom two rows inside the opening, never under the perimeter band (no coplanar overlap), one
pixel proud, decorative over glass and beam ends. Only upper arches take one (the requested sill is
under an arch's diameter); a lower arch's top diameter is left as an explicit product choice. The
item is not a block item. Recipe: three planks on two sticks (three planks alone would be a slab).
`WindowTrimActions` became slot-aware rather than gaining a `WindowSillActions` copy, and sill
targeting is a region of the existing hit classifier, not a separate target class. Rejected: one
payload holding both slots (an unknown version of one would lock the other), a sill drawn into the
pane model (it would change pane geometry and tint).

## 2026-10-22 - Distinct recipes for the new window pieces (0.10.0)

Reflecting the old recipes would collide: an upturned upper arch is the bottom corner's shape and
an upturned bottom corner is the upper arch's. Each new piece has its own raw-material shape (lower
arches and top corners use five or six planks, edges three), left and right edges differ by the
pane's position because shaped recipes also match mirrored, and every shape is checked against
every other mirrored and shifted (`GeneratedRecipesTest`) and against the loaded server
(`ContentGameTests`). Recipe emission is split by family under `data/server/recipe/`.

## 2026-10-22 - One Universal Trim; old ids kept, not remapped (0.9.0)

The window trim and the door trim become one attachment, the **Universal Trim**, under the existing
`round_trim` id (its recipe, its 0.4.0 aliases and the compatibility frame's item mapping all keep
working), with a flat square-frame icon so it no longer looks like a door-only ring. `window_trim`
stays registered as a hidden compatibility item that behaves exactly like it (no recipe, tab or
viewer entry, its old name). Old stacks are not rewritten: every id fits windows and doors, and a
removed or dropped attachment comes back as `round_trim` in the same look, so ids converge through
use. Rejected: a missing-mapping remap or inventory scan (`window_trim` to `round_trim` is
many-to-one with different stack NBT rules and would touch arbitrary containers), a new id (would
orphan every recipe book, tag and 0.4.0 alias), and keeping two items (two recipes and two icons for
one behaviour). Tradeoff: old and new stacks do not stack together until used.

The square icon's masonry gets its own straight-course face (`universal_trim_<m>`) rather than the
radial ring faces, which would show radial joints on a square.

## 2026-10-22 - Shared trim storage and transaction in block/attachment (0.9.0)

The duplicated door/window code (dk-08d20a47, dk-f9d8d245) moved into `block/attachment`:
`FaceTrimStore` (the pair, the unknown-payload custody and the fail-closed save/update rules,
parameterised by a host `Format` and a load-time `Legacy` reader), `TrimFitting` (fit, remove,
install, restyle, restyle target, announce) on a narrow `TrimHost`, and `TrimSlot` naming the
attachment kind (`PERIMETER` now, `SILL` next) and its canonical item. Host adapters keep only
their lookup, permission and store hook (the window's `trimmed` summary). Fitting moved from the
door trim item into the door router, as the window's already was, so both hosts claim the sneak
gesture the same way and an item's own use can never fit twice. One store per slot, each with its
own payload key and version, so a later sill or archway casing adds a store rather than a branch
inside a codec. Rejected: a generic multi-slot payload (one unknown slot would block the others)
and a per-host transaction with a shared helper (the drift the review found). The trim look
resolver was shared at the same time (`client/material/TrimLooks`) so the icon did not add a third
copy (part of dk-73e57897; the pane and leaf resolvers are untouched).

## 2026-10-21 - Window gestures are routed by hit region, with no sneaking (0.8.0)

A shared classifier (`WindowHitRegions`, fed by `WindowHitTarget`) names what is under the cursor:
trim band, frame (the border and the cross beams), glass, connector glass or nothing. A pure table
(`WindowUseRules`) maps item plus region to one action: planks and dye restyle the frame or, on a
trim band, that face's trim; dye on glass tints; masonry works only on a trim band (elsewhere it is
refused with a hint, using nothing); shears remove one face's trim; the trim item fits the clicked
face. Sneaking does the same as not sneaking, and the sneak-use bridge asks the block's router
(`claimsUse`) instead of guessing a family at the hit, so the bridge and the routing cannot disagree.
Rejected: keeping sneak for the pane (the owner dislikes sneak-click mechanics, and 0.7.0 needed it
for planks but not for the band), and an item-specific use on the trim item (the router already knows
the target). The classifier lives in `block/window/interaction/`, not `geometry/face` as planned:
`geometry/trim` already imports `geometry/face`, and the classifier needs the trim band, so `face`
would have made a package cycle.

## 2026-10-21 - One trim item per face; 0.7.0 trims become two (0.8.0)

A trim item fits one face, so a fully trimmed window costs two trims and drops two. The 0.7.0 trims
(one look on both faces) migrate to both faces in that look, which is a one-time uplift of material:
the player gets back two trims where they once paid for one. Accepted, because splitting a 0.7.0
trim to one face would silently delete a visible trim, and a refund-by-conversion path would add a
second mutation route. Rejected: charging nothing for the second face (a free trim from nothing)
and keeping the shared-face trim as a mode.

## 2026-10-21 - Conservative fitted-frame migration (0.8.0)

A 0.7.0 fitted frame converts to the door's FRONT trim only when everything matches exactly: a
complete canonical frame and door, the same facing, the frame anchor exactly one block in front of
the door anchor, all eight cells loaded and the door's FRONT free. The conversion is one guarded
batch (the quarters go silently, FRONT is installed only if all four went, otherwise the frame is
restored). An occupied destination keeps the frame as legacy content, even when the look is the
same, and any other arrangement is left alone. A geometrically identical freestanding frame
directly in front of a door cannot be told from a fitted one and converts too (accepted). Rejected:
converting any nearby frame (could move a decoration the player placed on purpose), merging into an
occupied FRONT (silently loses one of two looks), and dropping the frame (loses the player's
material). Scheduling moved to `ChunkMigrations` and `ChunkMigrationQueue` so a frame and a door
that load in either order both retry.

## 2026-10-21 - Door trim is a static attachment; frames stay for compatibility (0.8.0; supersedes placing new frames in front of doors)

The `round_trim` item is now the Door Trim: used on a door it attaches a ring to the clicked face
(FRONT at the leaf's front face, one pixel into the cells in front; BACK just behind the leaf),
stored on the door anchor (`DoorTrim`) like the window trims, using the shared `block/attachment`
value. It is static: it never swings and has no collision, pathfinding, support or redstone effect.
The frame block stays registered so frames in older saves keep rendering, restyling and dropping,
but nothing places a new one. Rejected: keeping the independent 2 x 2 frame as the way to trim a door
(it needs four free cells, two floors and a sneak click, and cannot trim the back), deleting the frame
block (old saves would lose content) and a swinging trim (it would need collision and pathing
changes for a decoration). Cost: the ring's FRONT face is rendered a pixel outside the door's cells
by the door renderer, and the BACK ring overlaps the hinge-side brass straps (cosmetic).

## 2026-10-21 - Glass tint is its own payload, not frame paint (0.8.0)

Dyeing window glass sets a colour stored beside the look (`WindowGlass: {Version, Color}` on the
entity and on the item stack), and the chunk model swaps the clear glass quads for one of sixteen
generated `window_glass_<dye>` textures (the clear glass's mask, drawn darker). One colour covers the
whole window including connector arms; another dye replaces it, and only a new window is clear
again. Drops and pick-block keep frame look and glass. Rejected: tinting the whole model (the wood
would change) and a blockstate colour (16 times the models and states).
The cost is a viewer key that now includes the glass colour.

## 2026-10-21 - Flush trim projects one pixel beyond the cell, drawn by a block-entity renderer (0.8.0, owner report; supersedes the 0.7.0 flush inlay)

Owner trial of 0.7.0: trim "missing on the other window blocks", on closer look only when the pane
was on an edge (flush, floor, ceiling). The cause was the 0.7.0 inlay: a flush pane's wall-side band
replaced the pane's own outer pixels (0..1 or 15..16), so it stood at the surface instead of proud
and read as no trim. Every band now stands one pixel proud of its slab face (flush 0..2 gets -1..0
and 2..3; 14..16 gets 13..14 and 16..17), so the wall-side band lies one pixel outside the window's
block. The owner accepted that. It is render-only: `WindowTrimRenderer` draws it from cached models,
and collision, support, connection and neighbour blocks never see it. The chunk model is pane-only
now (window block models 220 to 110). Rejected: keeping the inlay (the reported bug), reserving the
neighbour cell (a trim would need a block there, and a cell holds one block) and thickening the
slab (changes collision and every pose). Tradeoff: normal depth testing can hide a wall-side band
inside a solid neighbour, but nothing culls it.

## 2026-10-21 - A trim per window face, with face identity (0.8.0; supersedes the 0.7.0 shared-face trim)

A window now has FRONT and BACK trims with their own looks, stored together (`WindowTrim`
version 2, `FaceTrims`). Which side FRONT is on is a blockstate bit, `face_reversed`, with the pure
mapping in `WindowFaceIdentity`; rotations and mirrors recompute it so a trim stays on the face it
was on (a centred round window turned 180 degrees keeps its pose and toggles the bit). `trimmed`
stays as a derived summary, corrected by a scheduled tick, never by a write on load. Unknown
payload versions fail closed. Rejected: inferring the face from the pose alone (a 180 degree turn
of a centred window would swap the trims), a second pair of window block states (more models) and
keeping one shared look (the owner wants independent faces). The codec and loot condition are shared
with doors in `block/attachment/`.

## 2026-10-21 - Window models are exposed surfaces, not stacked boxes (0.7.0, owner report)

Owner trial of 0.6.1: horizontal lines in the window glass (round window at heights 3, 4, 6, 10, 12,
13). The cause was that the glazing was rasterised into stacked glass boxes and every face of every
box was drawn, so translucent sheets sat inside the glass. `WindowSurfaceGeometry` now paints the
logical pieces into a half-pixel voxel grid (later pieces replace earlier ones) and emits only
visible faces: glass never against anything, opaque material only against air or glass, merged
rectangles with default position UVs so the texture runs on continuously. Each blockstate state
applies exactly one complete model (a centred upright pose has one per combination of its two
across-plane connections, every model also `_trimmed`), which replaces the multipart arms. Rejected:
hiding only wholly buried faces (the sheets inside the glass still overlap) and a striped-texture
theory (the texture is not striped; the faces were the cause). Cost: 220 window block models (110
since 0.8.0 moved the trims out of the models), and
the 0.5.0 baseline class `WindowGeometry`, the old round-window and arm models are gone.

## 2026-10-21 - Clearer glass is a new texture with the accents kept (0.7.0)

The glass was too opaque. `window_glass_clear` has the same RGB as `window_glass`, the diagonal
accent texels unchanged (RGBA 246,252,255,176) and every other texel at alpha 24..36 (was 92..104);
all window models use it. `window_glass` stays generated and byte-identical (its hash is pinned),
just unused, so the historical bytes survive. Rejected: lowering alpha in place (changes pinned
bytes and the accents) and a render-type trick (would affect other translucent blocks).

## 2026-10-21 - Cross beams are separate items, and every piece has its own cross (0.7.0)

Each of the five window pieces has a crossed version as its own block and item (`_cross`), with
its own recipe. Owner ruling: every crossed piece carries its OWN one-pixel cross through its middle,
clipped to its own opening, so four crossed pieces in a 2 x 2 show four crosses (a grid). Rejected:
one cross shared across the 2 x 2 (the plan's assumption; it would need partner lookups the pieces
deliberately do not have) and a click or sneak toggle for the pattern (hidden state, and the owner
dislikes sneak-click mechanics). Cost: five more registrations, items, recipes and tab entries; the
benefit is that the choice is explicit in the inventory, recipes and previews. The pattern is fixed
per registration, not a blockstate.

## 2026-10-21 - The window trim is an attachment item with a flag and a payload (0.7.0; face sharing, flush inlay and sneak routing superseded in 0.8.0)

Window trims take masonry like the door trim. `roundwood:window_trim` is an item that fits onto any of
the ten windows: blockstate `trimmed=true` plus the trim's look in the window block entity under a
versioned `WindowTrim` payload, leaving the pane's own look untouched. The band follows each piece's
shape, and beams are never trimmed. Rejected: a second block in the same cell (a cell holds one
block) and a separate placement system for trims (windows already route placement and clicks).
Tradeoff: a flush trim is inlaid at the wall plane (its outer face replaces the pane's outer
pixels) instead of projecting, so it never occupies a neighbouring cell; a centred trim projects one
pixel on each face. One look serves both faces; independent front and back looks and freestanding
window trims are out of scope. Clicks are routed by what is under the hit: the band takes shears
and restyling without sneaking, the rest of the pane keeps the sneak gestures.

## 2026-10-21 - The trim drops as its own item (0.7.0; one pool per face since 0.8.0)

Every window loot table has a second pool, conditioned on `trimmed=true`, dropping one window trim
with its look copied from `WindowTrim.RoundwoodAppearance`, under the same explosion rule as the
pane. Breaking yields separate pane and trim items; pick-block gives the pane only. Rejected: folding
the trim into the pane stack's NBT (a window item would then hold a hidden second look, and the trim
could not be recovered or reused on its own).

## 2026-10-21 - Thinner brick mortar as new textures; door trims switch to them (0.7.0)

The owner found the brick mortar too thick. The thin faces are new generated textures
(`trim_radial_thin_<m>`, joint half width 0.425 texels instead of 0.85, partial-coverage
anti-aliased joints, same tones; `window_trim_round_<m>` and `window_trim_arch_<m>` for windows), and
door and round trims now use the thin face (`client/material/MasonryStyles`, extracted from
`PaintStyles`). The old `trim_radial_<m>` stay generated and pinned, unused. Saved masonry looks need
no migration: the look is data and gets the thinner mortar automatically. Rejected: editing the old
textures in place (their bytes are pinned).

## 2026-10-21 - Positioned windows never grow glass bridges (0.6.1, owner ruling; supersedes "Fixed central junctions")

Owner trial of 0.6.0: two flush windows meeting in an L grew a glass post, stems and arms filling
the corner. Only a centred upright pane (the 0.5.0 window, round or arch) still bridges: an arm
across its plane towards a pane, bars, a wall or a centred window crossing it. A flush, floor or
ceiling pane keeps no connection and its model and shape are its slab, so flush windows meeting at
a corner or edge simply abut frame to frame. Rejected: per-neighbour corner pieces (the owner wants
no extra glass at all). Tradeoff: a vanilla pane beside a flush window still reaches the cell edge
(vanilla connects to any pane-family block) and ends there without meeting the flush slab. The
hub, cap, flush-arm and fin models are gone (161 block models fewer; arch blockstates are small
again).

## 2026-10-21 - Door highlight follows the swing (0.6.1, owner report)

The visible outline was read from the blockstate, whose `open` switches instantly while the leaf
turns for eight ticks, so the outline jumped to the final pose ahead of the leaf. While the anchor's
`DoorSwing` is moving, the highlight now draws the closed leaf's silhouette turned by the same
angle the renderer uses (`DoorGeometry.swingDegrees`) plus the filled corners; at rest it is the
0.4.0 visible shape. Rejected: delaying the blockstate (server authority, collision and redstone
would lag) and hiding the outline while moving (the owner asked that it follow). The ray target
still switches instantly (clicks hit the final pose).

## 2026-10-21 - Arch top on a floor or ceiling is the side the placer sees as up (0.6.0)

The plan had the arch's top always follow the look direction. On a ceiling that would show the left
piece as a right one to whoever placed it. Floor and ceiling pieces therefore take the top the player
sees as up (away from them on a floor, towards them on a ceiling), so a left piece looks left to its
placer. Tradeoff: the top depends on the placer's facing, not on the block alone.

## 2026-10-21 - Posed models per canonical pose, caps for buried faces, disjoint AND conditions (0.6.0; hubs and caps removed in 0.6.1)

Each profile's models are authored once per canonical pose (upright facing south, or front up/down
with top north) and every other pose is a blockstate y rotation of one; a core, a hub and per-side
arms and caps assemble in a multipart. A cap covers the hub faces an arm hides and is drawn only while
that side is open, so no glass face sits behind glass. Conditions are written as disjoint AND terms
because vanilla's multipart reader cannot mix OR with other keys. `GeometryModelWriter` drops an
element whose faces are all buried (Minecraft rejects elements without faces). Rejected: one model
per state (hundreds of near copies). Cost: 186 new block models and arch blockstates of about 160 KB
each, guarded by tests that evaluate every state.

## 2026-10-21 - Fixed central junctions, no vertical links (0.6.0, superseded by 0.6.1 for positioned windows)

Connections stay the four horizontal vanilla ones. Each connected side not already covered by the slab
gets a glass arm to a central post (plus a stem for a flush upright pane, vertical fins for floor and
ceiling panes). Rejected: up/down links and per-position junction rules (a state explosion).
Tradeoff (owner-visible): connected flush panes show glass reaching back to the cell centre, and
floor and ceiling panes grow vertical fins when connected.

## 2026-10-21 - Repositioning is break and replace (0.6.0)

A placed window gains no reposition click; to move it, break it and place it again, and the look
survives in the item. Rejected: a gesture that rewrites `position` or `orientation` in place (a second
mutation path to validate for survival, water and neighbours, and one more thing competing with
restyling clicks).

## 2026-10-21 - Explicit left and right arch items (0.6.0)

The arch is two items, `round_window_arch_left` and `round_window_arch_right`, each a single
independent block with no partner lookup. Rejected: one arch item whose handedness comes from a
gesture or from a neighbouring piece (hidden state, order-dependent placement, a partner to keep in
sync). Cost: two advertised entries and two recipes, deliberately not mirror images so neither
matches the other's grid (a test checks all five recipes against each other, mirrored).

## 2026-10-21 - Hammer-style face targeting for windows, not sneak-click (0.6.0)

Holding a window item shows a centre square and four edge trapezoids on the aimed face; the centre
places the pane upright and centred, an edge places it flush against that side of the destination
cell, so all six sides are reachable without a modifier key. Boundaries are deterministic: the centre
square includes its boundary, and an exact diagonal tie picks the u edge. The server decides from the
ordinary use context and no packet carries the zone, so client and server cannot disagree through a
stale message; the client overlay runs the same placement path read-only. Rejected: sneak-click to
cycle positions (sneak already means restyle in this mod), a custom packet. Limit: interactive blocks
can take the click first, so the overlay is not a promise.

## 2026-10-21 - One block per family; the look is data (0.5.0, supersedes "Limited catalogue" and the per-finish registrations)

Roundwood now registers `round_door`, `round_window` and `round_trim`; wood, paint and masonry are an
`Appearance` on the anchor block entity (every window has one) and on the item stack. Any tagged
planks (`minecraft:planks`, plus `roundwood:appearance_planks` for datapacks) restyle wood in the
world, any of the 16 dyes paints it. Rejected: registering a block per wood x paint (hundreds of
ids, cannot cover modded woods), runtime-generated blocks (registry freezes before tags load).
Tradeoff: every family behaves like its oak counterpart (sounds, hardness, fire, tools), so a
masonry trim no longer needs a pickaxe and nether woods no longer resist fire; the look is purely
cosmetic. Crafting gives oak only; a donor-copying recipe is deferred.

## 2026-10-21 - Hidden aliases plus deferred conversion, not a missing-mapping remap (0.5.0)

A bare many-to-one remap of the 49 0.4.0 ids to the three new ones cannot attach each occurrence's
old finish. Keeping the old ids registered (hidden: no tab, recipe or viewer entry) lets each saved
cell and stack keep its look until converted. Chunk load only queues; conversion runs on the level
tick, one guarded whole-object rewrite, never loading partners. Tradeoff: 49 permanent compatibility
registrations. Mirrored structure copies keep the 0.4.0 camouflage limitation.

## 2026-10-21 - Restyling gestures and payment (0.5.0)

Sneak + planks sets the wood and strips paint; dye paints (sneaking or not); sneak + one of four
masonry blocks faces a trim. One material per change, nothing refunded, unchanged results free,
creative free; re-planking with the same wood strips paint. A narrow `RightClickBlock` hook lets only
these sneak gestures reach the block (0.3/0.4 relied on Forge's sneak bypass to keep block use out of
sneak-placement). On a door, restyling needs the leaf; corners stay camouflage. Rejected: crafting
recipes for every look, a refund of the previous material (would make looks a free storage), paint
removal by water.

## 2026-10-21 - Unfamiliar woods on doors are tinted boards; masonry stays fixed (0.5.0)

Door leaves keep Roundwood's full-height vertical boards. The eleven vanilla woods and all paints
have their own generated board art (the 0.4.0 bytes unchanged); any other wood uses neutral grey
boards tinted with the donor plank's average colour, because copying a horizontal plank texture
would restart boards mid-door and runtime atlas generation is out of scope. Windows and trims show
the donor's real plank sprite. Masonry is exactly bricks, stone, deepslate and mud bricks with their
radial art; arbitrary modded masonry is not accepted rather than shown with a misleading
approximation.

## 2026-10-21 - Trims drawn by an anchor renderer; windows stay chunk-baked (0.5.0)

A trim's look lives on its anchor quarter, so the four quarters are tessellated by the anchor's
block-entity renderer at their own cells (world light, AO, culling) from cached, pre-turned models.
Rejected: a block entity per quarter (four copies of one look) and propagating the anchor's look to
chunk model data of cells without entities. Tradeoff: like doors, no mining crack on trims. Windows
are single blocks with their own entity, so they keep chunk baking through model data.

## 2026-10-21 - EMI previews keep a stable identity (0.5.0)

EMI shows one entry per item whose icon cycles through bounded samples while its key, NBT,
comparison, retrieval, recipes and saved form are the plain default stack; styled stacks compare by
appearance. Rejected: EMI stack providers (they report ingredients under screen coordinates) and one
index entry per look (floods the index, cannot list modded woods). JEI gets subtypes only.

## 2026-10-21 - Harvest defers removal until loot has read the anchor (0.5.0)

Forge removes a broken block before its harvest callback, and removing a part tears down the
anchor that holds the look. Harvesting survival breaks therefore keep the part until `playerDestroy`
has evaluated loot, then remove it normally; a missed callback is cleaned up at the end of the tick.
Loot tables stay plain; the block only decorates its family item. Rejected: unconditional drops from
`onRemove` (breaks explosion decay, `doTileDrops`, loot modifiers).

## 2026-10-21 - Round trims: independent 2x2 frames, fitted or freestanding (0.4.0, supersedes the door's built-in trim ring; new frames superseded by the Door Trim in 0.8.0)

The door's 2-pixel ring is gone; frames are their own block family so trim and door colours mix.
A frame is a 2 x 2 assembly of static quarters (`facing` + `part`, no block entity), one item
placing all four through the ordinary item-use path. It occupies the cells in front of the door
(a separate depth layer), its ring one pixel deep at the back of them, so a fitted frame sits one
pixel proud of the leaf over the disc's edge and the camouflage corners stay visible. Fitting is a
sneak-use on a closed door's front (standard sneak-placement against an interactive block; no new
interception in `RoundDoorBlock.use`); anywhere else it is freestanding, like a door, so it frames
any 2 x 2 opening. It is purely decorative: no collision, support, occlusion or redstone, and only
the ring quarter is targetable, so the aperture passes rays to the door. Door and frame keep
separate layout and placement policies; only the mutation batch and partner teardown are shared
(`block/assembly`). Rejected: making the frame part of the door (no mixing), four independently
auto-orienting quarter blocks (half-built frames, no one-item placement), a larger surround outside
the door footprint (would need cells beside the door), a door-style framework shared with the frame
(couples redstone, identity and block entities to a static decoration).

## 2026-10-21 - Radial brickwork is painted, not modelled (0.4.0)

Brick frames show a generated 64 x 64 polar image (24 wedge bricks, mortar radiating from the
centre, a brick centred at the crown) spread once across the whole ring (`AssemblyFaceUv`), with a
granular edge texture on the sides. The ring is two pixels wide, so modelled voussoirs would be
dozens of rotated sub-pixel cuboids for no visible gain. Tradeoff: mortar relief is painted.

## 2026-10-21 - Door leaves: one board panel, prominent hardware (0.4.0)

The leaf is one panel over the whole disc with Roundwood's own 32 x 32 board art per finish (eight
vertical boards, full height) mapped once across the leaf, so boards never restart at a cell or tile
boundary. Brass straps stand half a pixel proud, the centred knobs two pixels proud of each face;
collision and selection stay the disc slab (the hardware is decorative), and the render bounds grow
by a 3-pixel margin to contain it at any swing angle. Tradeoff: door leaves no longer follow plank
resource packs (they can retexture Roundwood's own panels). Rejected: vanilla planks (16-pixel
horizontal courses that restart mid-door).

## 2026-10-21 - Target shape vs visible highlight (0.4.0)

`getShape` stays the target (empty corners join while a block item is held, so camouflage can be
applied); the client replaces vanilla's highlight for round doors with the visible shape (leaf at
rest plus filled corners). In 0.3.x the highlight was the target shape, so an open door with a
block in hand was outlined as the closed frame. Rejected: dropping empty corners from the target
(camouflage could no longer be applied to an open door's corners).

## 2026-10-21 - Window glazing and arms (0.4.0, owner report)

Windows show no glass in 0.3.x because vanilla `glass.png` is transparent inside its one-pixel
border and the round glazing samples only that interior; they now use Roundwood's own visible,
translucent `window_glass` in translucent models. Across the plane, a window connects only to
genuine pane junctions (panes, bars, walls, a crossing window), never to a sturdy block in front of
or behind it, and never to a parallel window face to face. In-plane connections keep vanilla rules.

## 2026-10-21 - Showcase command with a section registry (0.3.1)

`/roundwood showcase` (level 2) rebuilds one bounded test layout (46 x 8 x 36, one-block margin
and a floor) from a registry of `ShowcaseSection`s, so a new feature adds a class and a registry
line, and CLAUDE.md requires it. Doors go through `DoorPlacement.placeAt` (the item path's checks
and single write per cell, without a player); camouflage uses the anchor entity's own setter. The
clear and floor use client-only block updates, so no neighbour outside the box is notified; content
keeps one block inside the box so door and pane updates stay inside too. Cleared block entities
(chests) lose their contents: the command is for open ground. Rejected: structure templates
(cannot show new states without hand-edited NBT) and raw `setBlock` of door parts.

## 2026-10-21 - Pane windows again (0.3.0, supersedes "Windows are full wooden blocks")

Windows are `IronBarsBlock` panes again: a square 16 x 16 wooden face two pixels thick (wood in
every pixel outside the round opening, so the corners are wood), round glass recessed in it, and a
glass arm only for connections across the plane. In-plane neighbours need no geometry: the square
edges meet. Collision and outline are the thin panel plus arms. Tradeoff: perpendicular attachments
show glass junctions; that keeps reciprocal vanilla pane behaviour without glass in the corners.
Rejected: restoring 0.1.0's glass side fills (glass in the wooden corners).

## 2026-10-21 - Selection is the whole door, collision stays per cell (0.3.0)

`getShape` (outline) returns the whole assembly from every part; `getCollisionShape` returns the
0.2.0 clipped piece, unchanged, so passage width, pathing and support do not move. The outline
follows the resting `open` and includes the open leaf's projection, which stays non-colliding and
non-interactive: the client re-credits each hit to the part that contains it and cancels hits that
land in no part (the server only accepts hits within a block of their cell). Supersedes the shared
collision/outline of "Collision clipped to the four cells". Rejected: extra occupied cells.

## 2026-10-21 - One camouflage material for all four corners, decorative (0.3.0)

The four static corners (square minus disc, door-thin, in the closed plane) take one material per
assembly, stored on the anchor entity: survival pays one item, refunded exactly once (to the
player on replace/clear, as a drop on teardown, never with `doTileDrops=false`, discarded on a
creative break, not explosion-decayed); creative pays nothing. Sneak-use with both hands empty
clears (Forge bypasses block use for a sneaking player holding anything). Corners never collide
or occlude: solid corners would obstruct tall mobs and the column-centred door paths. Rendered by
the existing BER as cached per-cell models in the donor's representative face sprites, lit at
each cell; ordinary cubes look right, connected textures/overlays/dynamic models are not promised.
Rejected: per-corner materials, full-depth wall infill, chunk-baked `ModelData` replicated across
four cells from an anchor-only entity, Copycats+/Framed-style exact donor rendering.

## 2026-10-21 - Centred knob (0.3.0)

The brass knob moved from the free edge to the middle of each face (hobbit style); straps stay on
the hinge side.

## 2026-10-20 - Client-side swing animation (0.2.0, owner request)

The leaf swings over 8 ticks with a sine ease-in-out, drawn by the existing block-entity renderer
from the blockstate `open` change (`RoundDoorBlockEntity.setBlockState` retargets a pure
`DoorSwing` at the level's game time; the renderer samples game time + partial tick). A reversal
mid-swing turns back from the current angle over the proportional time; a newly created entity
(placement, chunk load) starts at rest. The server stays authoritative and instant: `open` and
collision switch at once, the animation is visual only. No networking, no server ticker.
Rejected: a ticking entity or synced animation state (cost on the server, nothing to gain); and
animating collision (needs timed occupancy and would fight vanilla door pathing).

## 2026-10-20 - One rigid round leaf, hinge separate from column (supersedes split leaves)

The door is one 32-pixel leaf on one hinge, like a hobbit door. A new `column` property carries
part identity; vanilla `hinge` is the shared physical hinge, chosen with vanilla's rules at the
assembly's outer edges. The leaf turns about the middle of its hinge-side thickness (x 1.5 / 30.5,
z 14.5), not the outer front edge the plan named: turning about the front edge would carry the 3/16
thickness outside the cells, leaving nothing to collide with or click. Supersedes "Split-leaf 2x2
footprint" below. Placed 0.1.0 doors are not migrated (their `hinge` meant column).

## 2026-10-20 - Collision clipped to the four cells; projection visual only (outline part superseded in 0.3.0)

The open leaf's outer half projects one block past the cells. Collision and outline are the turned
leaf clipped to the four cells (free column empty, 29/16 clear). The projection reserves no cells,
cannot be targeted and does not collide; it may overlap nearby blocks or entities. Rejected: extra
occupied cells or a multiblock occupancy model (changes placement, pathing and vanilla door
identity). Full swing occupancy is deferred.

## 2026-10-20 - Block entity renderer for the leaf (supersedes "No block entity")

The lower-left part carries a non-ticking, data-free block entity; its renderer draws the baked
leaf model. All door states use an empty world model. Chosen because the leaf extends beyond its
cells when open, where chunk-baked models get the owning cell's lighting and culling wrong.
Tradeoff: one entity per door and a per-frame draw (shared baked models, cached shapes, no
per-frame geometry), and the vanilla crack overlay does not show on the leaf. Supersedes "No block
entity, no extra state" below.

## 2026-10-20 - Windows are full wooden blocks (0.2.0; superseded in 0.3.0)

`RoundWindowBlock extends Block implements SimpleWaterloggedBlock`: a full-depth wooden housing
with wood in every corner and recessed round glazing; full collision and outline, sturdy faces,
no connection states. Waterlogging, buckets, fluid ticks and water restoration are kept. Tradeoff:
selection follows the cube, not the recessed glass. Supersedes "Window extends IronBarsBlock".

## 2026-10-20 - Item models are baked 3D models with explicit UVs

Inventory icons are ordinary baked models (whole closed leaf; whole window), front GUI light,
centred transforms computed in the generator. The model writer cuts every element at 16-pixel
tile boundaries and writes per-tile UVs: default UVs on a 32-pixel model sampled neighbouring
atlas sprites (the 0.1.0 scrambled icons). Rejected: a custom item renderer (extra client code and
a BER dependency for icons).

## 2026-10-20 - Paint recipes list the other paintable items instead of using a tag (superseded in 0.5.0: paint is applied in the world)

The painting ingredient is oak plus the three other painted finishes (as vanilla wool dyeing does),
so no recipe turns an item into itself. Rejected: `paintable_doors` / `paintable_windows` item tags
as the ingredient, because they would include the target finish and make "red door + red dye =
red door" a valid, dye-wasting recipe.

## 2026-10-20 - Window orientation is a horizontal axis, not a facing

A round window looks the same from both sides, so `axis` (x/z) replaces a four-way facing and
halves the state count. Rejected: `facing`, which doubles identical models.

## 2026-10-20 - Window extends IronBarsBlock (superseded in 0.2.0)

1.20.1 has no `GlassPaneBlock`; vanilla glass panes are `IronBarsBlock`. Extending it keeps
reciprocal pane connections, waterlogging, buckets and fluid ticks inherited.

## 2026-10-20 - Split-leaf 2x2 footprint (superseded in 0.2.0)

Two half-round leaves on outer hinges, each a vanilla door column, meeting at a centre seam. Each
open leaf stays inside its own two cells and leaves 26/16 clear width. Rejected: one rigid round
leaf, which would swing out of its cells and need a custom occupancy model.

## 2026-10-20 - Stay in the vanilla door and pane hierarchies

`RoundDoorBlock extends DoorBlock` with the wooden-door tags, so vanilla villager AI
(`InteractWithDoor` checks the tag and the class), the walk node evaluator, MineColonies
(`instanceof DoorBlock` + `setOpen`) and fuel rules work unchanged. Rejected: an independent AI or
pathing integration.

## 2026-10-20 - No block entity, no extra state (superseded in 0.2.0)

Facing, hinge and half already identify the anchor, so the assembly stores nothing beyond the
vanilla door properties.

## 2026-10-20 - Limited catalogue, no Biomes O' Plenty (superseded in 0.5.0: one block per family, any planks)

11 vanilla woods plus four oak-based paints, not a wood x paint product. BOP finishes and compat
recipes are deferred.

## 2026-10-20 - Original textures only, generated

Five 16x16 textures (four painted planks, one brass) come from `tools/generate-textures.mjs`;
everything else references vanilla textures by resource location and never copies them.

## 2026-10-20 - No RedzCore dependency

Nothing here is used by a second mod yet (workspace rule).
