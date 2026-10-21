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
  inside a `DoorMutationBatch` naming the four cells.
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
with its own loot and `DoorRemoval` clears its matching partners: at most one item. Missing parts
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

`door/leaf/DoorGeometry` describes one closed 32-pixel disc (2-pixel trim ring, recessed panel,
brass straps on the hinge side, one centred knob per face) in a 32x32 plane at depth
z 13..16, authored facing north. It turns as a rigid body about the vertical axis through the middle
of its hinge-side thickness (x 1.5 or 30.5, z 14.5), +90 / -90 degrees, so the open leaf lies along
the hinge wall (3/16 thick) and its outer half projects one block in the `facing` direction.

- **Collision** (`leaf/DoorShapes`, `getCollisionShape`): the disc slab turned first, then clipped
  to each of the four cells and turned for `facing`; cached (64 variants). Open: the free column is
  empty, the hinge column keeps the inner half (29/16 clear width). Support and visual shapes
  inherit it. The projection never collides.
- **Selection** (`selection/DoorSelectionShapes`, `getShape`): every part returns the whole
  assembly's outline at rest (closed disc, or the open leaf including its projection) relative to
  itself, so all four give the same world shape and the highlight has no quarter seams. It follows
  the authoritative `open`, not the 8-tick animation. The four static corners join it when filled,
  or while the looking player (`EntityCollisionContext`) holds a block item in either hand.
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
  incomplete assembly. `getRenderBoundingBox` covers the four cells plus the projection cells;
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
(reaching the recessed glass where the opening crosses it; buried faces omitted). Collision and
outline are the panel plus active arms (`WindowShapes`, 32 variants). Same-block faces above/below
are hidden only when the axes match.

## Data

Everything except the five textures is generated. `GeometryModelWriter` cuts every element at the
16-pixel texture-tile boundaries and writes vanilla's default UVs for its position inside its own
tile (Forge omits a UV equal to the default), so no face samples outside its sprite. Door: two leaf
templates (hinge left/right) plus per-finish bindings, one empty world model per finish and a
blockstate with a single `""` variant. Window: one core template, per-finish bindings and a shared glass arm model, in a multipart
blockstate (core by axis, arms by axis + connection).
Items (`RoundwoodItemModelProvider`): door items inherit the left-hinge leaf, window items the
window template, each with front GUI light and computed centred display transforms. Plus recipes
and their advancements, loot, tags and English names.
