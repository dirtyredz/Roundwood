# Architecture - Roundwood

## Door assembly

An assembly is four ordinary `DoorBlock` states of one block, 2 wide x 2 high x 1 deep, showing one
round leaf. Each part has the vanilla `facing`, `half`, `hinge`, `open`, `powered` plus `column`
(left/right). **Identity** is `column` + `half`; **`hinge`** is the physical side the single leaf
turns on and is the same in all four parts.

**Anchor resolution** (`DoorLayout.of`): from any part, go down if `half=upper`, then one block
towards the counter-clockwise side of `facing` if `column=right`. That cell is the anchor
(lower-left). A part's partners are only the cells this rule names, and only if they hold the same
block with the same facing, hinge, column and half, so two assemblies side by side never merge.

**Hinge choice** (`DoorHingePlacement`): vanilla `DoorBlock.getHinge`, measured at the two cells
just outside the footprint (left of the anchor, right of the right column) and the cells above them:
a same-block door outside one edge puts the hinge on the other; otherwise the side with more full
blocks gets it; on a tie, the half of the clicked cell the player aimed at. The clicked cell is
always the lower-left part.

**Invariants**

- All present parts share `hinge`, `open` and `powered`.
- Every write Roundwood makes goes through `DoorPlacement`, `DoorStateController` or `DoorRemoval`,
  inside an `AssemblyMutationBatch` naming the four cells.
- Nothing reads an unloaded cell. State changes and redstone do nothing while any cell is unloaded
  (deferred, not treated as air); removal only clears loaded partners.

## Mutation publication

| Operation | Write flags | Why |
|---|---|---|
| Place | `UPDATE_ALL`, all four inside one batch | Forge's snapshot capture defers notifications until all four exist, and rolls all four back if the multi-place event is cancelled |
| Open / power | clients + immediate + known-shape | every part is written explicitly; no shape cascade and no neighbour updates (as vanilla door toggles) |
| Teardown | clients + known-shape + suppress-drops, then neighbour and shape updates per cleared cell | partners vanish silently before anyone else is told |

The batch suppresses Roundwood's own `onRemove`, `onPlace` reconciliation and neighbour reactions
for the marked cells. Nesting is capped at 4 and logged if exceeded.

**External writes.** `onPlace` sees every in-place state change on the server, whatever the flags
(even `UPDATE_CLIENTS` alone). If something else wrote a new `open` into one part without changing
its membership (Double Doors, commands), `DoorStateController.reconcileExternalOpen` writes the same
value to the partners, silently: the writer owns the sound. Our own batches and snapshot rollback
are ignored.

**Integrity.** A part appearing from anything but an in-place change (structure templates,
commands, our own placement) schedules a block tick (`DoorIntegrity`, 1 tick later, after the
writer finished). If all four cells are loaded and the assembly is incomplete, the part is broken
with its own loot and `DoorRemoval` (via `AssemblyTeardown`) clears its matching partners: at most one item. Missing parts
are never created; an unloaded cell re-schedules the check (every 20 ticks) without being read.
Vertically, `updateShape` keeps a part only while the cell above/below is its own column's other
half. Mirroring cycles `hinge` (vanilla) and `column`; rotation turns `facing` only.

## Loot ownership

Every part's loot table is `dropSelf` with the explosion condition. Exactly one item drops because
of ordering, not counting: the removal that started it (player break, explosion, support loss via
`updateShape`, integrity cleanup) evaluates its own loot; `onRemove` of that part clears the
partners with drops suppressed before vanilla notifies neighbours, so no partner is ever evaluated.
Creative breaks, replacement, fire and `doTileDrops=false` therefore drop nothing. Pistons are
refused (`PushReaction.BLOCK`). A cancelled placement being rolled back (`restoringBlockSnapshots`)
is not treated as a removal.

## Opening and redstone

- Click: the server toggles; the client returns success without predicting, and the server plays
  one sound for everyone.
- `setOpen` (villagers, MineColonies, other mods): explicit and idempotent.
- Redstone: powered = any of the four cells has a neighbour signal. Only a change of that value
  opens or closes; in between, players keep manual control.

## The leaf: geometry, collision, rendering

`block/door/leaf/DoorGeometry` describes one closed 32-pixel disc in a 32x32 plane, authored facing
north: one board panel over the whole disc at depth z 13..16 (no trim ring since 0.4.0), brass
straps on the hinge side half a pixel proud of each face (z 12.5..13 and 16..16.5) and a 2 x 2
knob in the middle of each face standing two pixels proud (z 11..13 and 16..18). The hardware is
decorative: collision and selection use the disc slab only. It turns as a rigid body about the vertical axis through the middle
of its hinge-side thickness (x 1.5 or 30.5, z 14.5), +90 / -90 degrees, so the open leaf lies along
the hinge wall (3/16 thick) and its outer half projects one block in the `facing` direction.

- **Collision** (`leaf/DoorShapes`, `getCollisionShape`): the disc slab turned first, then clipped
  to each of the four cells and turned for `facing`; cached (64 variants). Open: the free column is
  empty, the hinge column keeps the inner half (29/16 clear width). Support and visual shapes
  inherit it. The projection never collides.
- **Selection** (`selection/DoorSelectionShapes`): two whole-assembly shapes, the same in world
  space from every part (no quarter seams), both following the authoritative `open`, not the 8-tick
  animation. The **target** shape (`getShape`, what rays hit) is the leaf at rest (closed disc, or
  the open leaf including its projection) plus the four static corners when filled or while the
  looking player (`EntityCollisionContext`) holds a block item in either hand, so empty corners can
  take camouflage. The **visible** shape adds only filled corners. Vanilla highlights `getShape`,
  which outlined an open door as the closed frame whenever a block was held (0.3.x), so
  `client/door/DoorHighlightRenderer` cancels `RenderHighlightEvent.Block` for round doors and draws
  the visible shape's edges with vanilla's line style (nothing for an incomplete assembly).
- **Hit re-attribution** (`selection/DoorHitResolver`, client `DoorTargetingEvents` on
  `InputEvent.InteractionKeyMappingTriggered`): a ray is credited to the first part it passes,
  which may not contain the hit point (looking through an empty corner). Before use, attack,
  held mining and pick-block the client moves the hit to the loaded part of the same assembly that
  contains the point (nudged into the surface), so the server's one-block distance check passes.
  A point in no part (the projection, a neighbouring assembly) cancels the input. Held mining keeps
  the part it started on (any part removes the whole door).
- **Particles** (`client/door/DoorParticles`): break and hit particles use the cell's collision
  piece, not the whole-door outline (which would scatter four doors' worth of particles).
- **Rendering**: every door state uses an empty world model (break particle only). The anchor carries
  a non-ticking `RoundDoorBlockEntity`; `client/door/RoundDoorRenderer` draws the finish's baked
  leaf model (`block/<finish>_round_door_leaf_<hinge>`, registered for baking by `RoundwoodClient`,
  looked up per frame so resource reloads are safe) through the normal baked-model path
  (`Sheets.solidBlockSheet`, packed light of the anchor, overlay). It draws nothing for an
  incomplete assembly. `getRenderBoundingBox` covers the four cells plus the projection cells, plus a 3-pixel margin
  for the proud hardware at any swing angle (`DoorGeometry.SWEEP_MARGIN`);
  `shouldRenderOffScreen` keeps it out of section culling.
- **Swing animation** (client only): `DoorSwing` holds a progress 0..1. `RoundDoorBlockEntity
  .setBlockState` (called by the chunk on every state change) retargets it when `open` flips, at
  the level's game time; the renderer samples it at game time + partial tick. 8 ticks for a full
  swing, sine ease-in-out; a reversal starts from the current progress over the proportional time.
  A new entity (placement, chunk load, relog) starts at rest. The server never animates: `open` and
  collision switch at once.

## Camouflage

One material covers all four static corners: the 32 x 32 square minus the disc, at the closed
leaf's depth (z 13..16), fixed in the closed door plane whatever the hinge or `open`
(`camouflage/CamouflageGeometry`). Decorative only: no collision, occlusion, light or redstone
change; the 0.2.0 passage is unchanged.

- **Data**: the anchor `RoundDoorBlockEntity` holds `CamouflageData(material, refund)`, saved as
  `Camouflage: {Material: <block state>, Refund: <item>}`. `getUpdateTag` carries the material only
  and always the key (empty when clear), and `onDataPacket` treats a missing tag as clear, so clears
  reach clients. Loading replaces the value and never touches `DoorSwing`. Reading fails closed
  (unknown/invalid material, or a refund that does not match it, reads as nothing).
- **Materials** (`CamouflageMaterial`): a `BlockItem` whose default state is a full, opaque,
  ordinary cube (full collision and occlusion, MODEL render shape, no fluid, no block entity, no
  light), not air or a Roundwood door, without `BlockEntityTag`/`BlockStateTag`. Appearance only.
- **Interaction** (`CamouflageInteraction`, ahead of the toggle in `use`): corner + block item in
  the main hand, not sneaking: apply/replace (an unacceptable block is refused: no toggle, nothing
  consumed or placed); main hand empty + block item in the offhand: main passes, offhand applies;
  filled corner + sneaking + both hands empty: clear (Forge skips block use for a sneaking player
  holding anything). Everything else toggles. Corner vs leaf is decided from the hit point in the
  assembly frame (`DoorLayout.Assembly.toAuthored`), the leaf in its current pose winning.
  Permission (`mayBuild`, `mayInteract`) and a complete loaded assembly are checked first.
  Survival consumes one item and stores it as the refund; creative consumes and stores nothing.
- **Exactly-once refunds** (`CamouflageDrops`): replace/clear give the old paid item to the player
  (overflow dropped). Teardown: `DoorRemoval.onRemove` first withdraws the payload from the anchor
  (found from the removed part's old state; never loaded on demand) and pops it at the removed
  part (`Block.popResource`, so `doTileDrops` applies) before partners go. Whoever withdraws it is
  the only one who can drop it, so partner removal and repeated callbacks find nothing. Creative
  breaks run inside a thread-local discard scope around `onDestroyedByPlayer` (try/finally).
  Explosions, replacement and fire return it (no explosion decay).
- **Rendering** (`client/camouflage`): the anchor's renderer draws the corners before the leaf's
  facing/swing transforms: per cell a cached `SimpleBakedModel` of the exposed corner faces, turned
  for `facing`, each face using the donor model's representative sprite/tint for that world side,
  UVs cropped like a full block at that spot; tessellated with `ModelBlockRenderer.tesselateBlock`
  at the cell's own position (world light, AO, biome tint) into `RenderType.solid()`, no side
  culling. Caches are bounded and dropped on `ModelEvent.BakingCompleted`.

## Window

`RoundWindowBlock extends IronBarsBlock` (vanilla pane): `north/east/south/west/waterlogged` plus
`axis`. Authored for `axis=x`: a square 16 x 16 wooden face at z 7..9, wood outside a centred
12-pixel circle, glass inside it at z 7.5..8.5. In-plane connections add nothing (the square edges
meet); connections across the plane add a 2-pixel glass arm from the block edge to the panel
(reaching the recessed glass where the opening crosses it; buried faces omitted). Across the
plane only genuine pane junctions connect (`WindowConnections`, applied after vanilla in
`getStateForPlacement` and `updateShape`): panes, bars, walls, or a window whose axis crosses this
one; vanilla's "any sturdy face" rule grew an arm towards a solid block in front (0.3.x). The
glazing and arms use Roundwood's `window_glass` (every texel visible, translucent) in a translucent
model: vanilla `glass.png` is transparent inside its border, which is all the round glazing sampled. Collision and
outline are the panel plus active arms (`WindowShapes`, 32 variants). Same-block faces above/below
are hidden only when the axes match.

## Frame (round trim)

An independent 2 x 2 assembly of `RoundTrimBlock` quarters (`facing` + `part`), placed whole by
`RoundTrimItem` through the ordinary `ItemStack.useOn` path (Forge's snapshot capture: one
multi-place event, all four rolled back if cancelled). Geometry (`block/trim/geometry/TrimGeometry`):
a ring, outer diameter 32, inner 28, one pixel deep at the back of its cells (z 0..1, authored
facing north), cut into cell-local quarters.

- **Fitted**: using the item on the front face of a complete, closed door (sneaking, since a plain
  click goes to the door) resolves the door's anchor from whichever part was clicked and places
  the frame at `doorAnchor.relative(facing.getOpposite())` with the door's facing: the cells in
  front, so the ring sits one pixel proud of the door's front face over the disc's edge. Other
  faces and open doors are refused (`FAIL`, nothing consumed). **Freestanding**: anywhere else the
  target cell is the lower-left quarter, facing the player's direction.
- **Checks** (`TrimPlacement`): loaded, inside build height and border, editable, replaceable, dry,
  both lower quarters on a sturdy floor. No entity check (nothing collides). Door cells are not
  replaceable, so a frame never overwrites a door. `placeAt` does the same without a player.
- **Behaviour**: empty collision and support shapes, pathfindable, no occlusion, not a redstone
  conductor, `PushReaction.BLOCK`, no block entity. The selection shape is only the quarter's own
  ring piece, so rays pass through the centre and the corners to the door behind.
- **Lifecycle**: the door's pattern, through the shared `block/assembly` services: `TrimRemoval`
  clears loaded partners with drops suppressed (one item from the initiating quarter),
  `TrimIntegrity` removes an incomplete frame a tick later (deferred while a cell is unloaded),
  losing the floor under a lower quarter breaks it. Door and frame never remove each other.
  Rotation turns `facing`; mirroring mirrors it and swaps the column of `part`.
- **Materials** (`variant/TrimMaterial`, declared in `FinishCatalog`): one wooden frame per finish
  (natural woods in their stripped log, tiled per block; painted ones in the painted door panel
  spread across the ring) and four masonry frames whose 64 x 64 radial brick face (24 wedge
  bricks, joints radiating from the centre, a brick at the crown) is spread across the ring.

## Data

Everything is generated: textures by `tools/generate-textures.mjs` (checked in under
`src/main/resources`), the rest by datagen. `GeometryModelWriter` cuts every element at the
16-pixel texture-tile boundaries and by default writes vanilla's default UVs for its position
inside its own tile (Forge omits a UV equal to the default), so no face samples outside its
sprite. A `FaceStyle` can hide buried faces, give some faces another texture variable (a frame's
sides) or explicit UVs: door panels and painted/brick frames use `AssemblyFaceUv`, which spreads one
image across the whole 32 x 32 front and back (front as seen from the front, back as seen from
behind), so boards and brick joints continue across elements and cells. Door: two leaf
templates (hinge left/right) plus per-finish bindings, one empty world model per finish and a
blockstate with a single `""` variant. Window: one core template, per-finish bindings and a shared glass arm model, in a multipart
blockstate (core by axis, arms by axis + connection).
Frame: four quarter templates in two families (tiled, whole-image), per-material bindings
(`#face`, `#edge`), a blockstate variant per facing x part turned by `y`.
Items (one provider per family, transforms in `ItemModelTransforms`): door items inherit the
left-hinge leaf, window items the window template, frame items carry the whole ring; each with
front GUI light and computed centred display transforms. Plus recipes
and their advancements, loot, tags and English names.
