# Architecture - Roundwood

## Door assembly

An assembly is four ordinary `DoorBlock` states of one block, 2 wide x 2 high x 1 deep. Each column
is a vanilla lower/upper pair; the left column has `hinge=left`, the right `hinge=right`, all share
`facing`. No block entity, no extra properties, no networking.

**Anchor resolution** (`DoorLayout.of`): from any part, go down if `half=upper`, then one block
towards the counter-clockwise side of `facing` if `hinge=right`. That cell is the anchor
(lower-left). The right column is the anchor's clockwise neighbour. A part's partners are only the
cells this rule names, and only if they hold the same block with the same facing and the expected
hinge/half, so two assemblies placed side by side never merge.

**Invariants**

- All present parts share `open` and `powered`.
- Every write to a part goes through `DoorPlacement`, `DoorStateController` or `DoorRemoval`,
  inside a `DoorMutationBatch` naming the four cells.
- Nothing reads an unloaded cell. State changes and redstone do nothing while any cell is unloaded
  (deferred, not treated as air); removal only clears loaded partners.

## Mutation publication

| Operation | Write flags | Why |
|---|---|---|
| Place | `UPDATE_ALL`, all four inside one batch | Forge's snapshot capture defers notifications until all four exist, and rolls all four back if the multi-place event is cancelled |
| Open / power | clients + immediate + known-shape | every part is written explicitly; no shape cascade and no neighbour updates (as vanilla door toggles) |
| Teardown | clients + known-shape + suppress-drops, then neighbour and shape updates per cleared cell | partners vanish silently before anyone else is told |

The batch suppresses Roundwood's own `onRemove` and neighbour reactions for the marked cells.
Nesting is capped at 4 and logged if exceeded.

## Loot ownership

Every part's loot table is `dropSelf` with the explosion condition. Exactly one item drops because
of ordering, not counting: the removal that started it (player break, explosion, support loss via
`updateShape`) evaluates its own loot; `onRemove` of that part clears the partners with drops
suppressed before vanilla notifies neighbours, so no partner is ever evaluated. Creative breaks,
replacement, fire and `doTileDrops=false` therefore drop nothing. Pistons are refused
(`PushReaction.BLOCK`). A cancelled placement being rolled back (`restoringBlockSnapshots`) is not
treated as a removal.

## Opening and redstone

- Click: the server toggles; the client returns success without predicting, and the server plays
  one sound for everyone.
- `setOpen` (villagers, MineColonies, other mods): explicit and idempotent.
- Redstone: powered = any of the four cells has a neighbour signal. Only a change of that value
  opens or closes; in between, players keep manual control.

## Geometry

`geometry/PixelCircle` rasterizes circles by pixel centre and merges runs into rectangles; `Box` is
an immutable pixel cuboid with quarter-turn rotation; `VoxelShapeFactory` is the only conversion to
Minecraft shapes. `DoorGeometry` builds the whole closed door once (2-pixel trim ring, recessed
panel, brass knobs and straps) in a 32x32 plane, clips it per cell and swings open leaves about
their outer front edge. Models (through `GeometryModelWriter`) and collision come from the same
boxes: models use every piece, collision uses the full 3/16 disc slab. Everything is authored
facing north and turned by blockstate `y` rotation / `Box.rotateY`.

The window is authored for `axis=x`: rim ring and glass disc centred in depth, a corner fill for
in-plane connections and a pane arm for perpendicular ones; `axis=z` turns everything 90 degrees.
`WindowGeometry.shape` and the multipart blockstate use the same side mapping.

## Data

Everything except the five textures is generated: 8 door templates plus per-finish texture
bindings, blockstates that ignore `powered`/`waterlogged`, item models from the same geometry,
recipes and their advancements, loot, tags and English names.
