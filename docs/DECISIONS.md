# Decisions - Roundwood

Newest first.

## 2026-10-20 - Paint recipes list the other paintable items instead of using a tag

The painting ingredient is oak plus the three other painted finishes (as vanilla wool dyeing does),
so no recipe turns an item into itself. Rejected: `paintable_doors` / `paintable_windows` item tags
as the ingredient, because they would include the target finish and make "red door + red dye =
red door" a valid, dye-wasting recipe.

## 2026-10-20 - Window orientation is a horizontal axis, not a facing

A round window looks the same from both sides, so `axis` (x/z) replaces a four-way facing and
halves the state count. Rejected: `facing`, which doubles identical models.

## 2026-10-20 - Window extends IronBarsBlock

1.20.1 has no `GlassPaneBlock`; vanilla glass panes are `IronBarsBlock`. Extending it keeps
reciprocal pane connections, waterlogging, buckets and fluid ticks inherited.

## 2026-10-20 - Split-leaf 2x2 footprint

Two half-round leaves on outer hinges, each a vanilla door column, meeting at a centre seam. Each
open leaf stays inside its own two cells and leaves 26/16 clear width. Rejected: one rigid round
leaf, which would swing out of its cells and need a custom occupancy model.

## 2026-10-20 - Stay in the vanilla door and pane hierarchies

`RoundDoorBlock extends DoorBlock` with the wooden-door tags, so vanilla villager AI
(`InteractWithDoor` checks the tag and the class), the walk node evaluator, MineColonies
(`instanceof DoorBlock` + `setOpen`) and fuel rules work unchanged. Rejected: an independent AI or
pathing integration.

## 2026-10-20 - No block entity, no extra state

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
