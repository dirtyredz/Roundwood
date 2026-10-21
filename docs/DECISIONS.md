# Decisions - Roundwood

Newest first.

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

## 2026-10-20 - Collision clipped to the four cells; projection visual only

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

## 2026-10-20 - Windows are full wooden blocks (supersedes pane inheritance)

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

## 2026-10-20 - Paint recipes list the other paintable items instead of using a tag

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

## 2026-10-20 - Limited catalogue, no Biomes O' Plenty

11 vanilla woods plus four oak-based paints, not a wood x paint product. BOP finishes and compat
recipes are deferred.

## 2026-10-20 - Original textures only, generated

Five 16x16 textures (four painted planks, one brass) come from `tools/generate-textures.mjs`;
everything else references vanilla textures by resource location and never copies them.

## 2026-10-20 - No RedzCore dependency

Nothing here is used by a second mod yet (workspace rule).
