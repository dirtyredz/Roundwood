# Decisions - Roundwood

Newest first.

## 2026-10-21 - Arch top on a floor or ceiling is the side the placer sees as up (0.6.0)

The plan had the arch's top always follow the look direction. On a ceiling that would show the left
piece as a right one to whoever placed it. Floor and ceiling pieces therefore take the top the player
sees as up (away from them on a floor, towards them on a ceiling), so a left piece looks left to its
placer. Tradeoff: the top depends on the placer's facing, not on the block alone.

## 2026-10-21 - Posed models per canonical pose, caps for buried faces, disjoint AND conditions (0.6.0)

Each profile's models are authored once per canonical pose (upright facing south, or front up/down
with top north) and every other pose is a blockstate y rotation of one; a core, a hub and per-side
arms and caps assemble in a multipart. A cap covers the hub faces an arm hides and is drawn only while
that side is open, so no glass face sits behind glass. Conditions are written as disjoint AND terms
because vanilla's multipart reader cannot mix OR with other keys. `GeometryModelWriter` drops an
element whose faces are all buried (Minecraft rejects elements without faces). Rejected: one model
per state (hundreds of near copies). Cost: 186 new block models and arch blockstates of about 160 KB
each, guarded by tests that evaluate every state.

## 2026-10-21 - Fixed central junctions, no vertical links (0.6.0)

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

## 2026-10-21 - Round trims: independent 2x2 frames, fitted or freestanding (0.4.0, supersedes the door's built-in trim ring)

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
