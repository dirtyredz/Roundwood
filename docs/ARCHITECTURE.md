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
  the visible shape's edges with vanilla's line style (nothing for an incomplete assembly). While
  the anchor's swing is moving it draws `selection/DoorSwingOutline` instead: the closed leaf's
  silhouette turned by `DoorGeometry.swingDegrees` of the swing's progress (the renderer's own
  angle), plus the filled corners alone (`DoorSelectionShapes.visibleCorners`), so the outline never
  leads the leaf (0.6.1).
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
  a non-ticking `RoundDoorBlockEntity`; `client/door/RoundDoorRenderer` draws the leaf in the door's
  appearance (from `block/round_door_leaf_<hinge>`, registered for baking by `RoundwoodClient`,
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

Three blocks share one pane base, `WindowPaneBlock` (a vanilla `IronBarsBlock`): `RoundWindowBlock`
and the two single-cell arch pieces `RoundWindowArchBlock` (left, right). One block entity type,
`roundwood:round_window`, holds the look of all three. Every state has the four horizontal
connections and `waterlogged`.

**State.** Round window: `axis` (the 0.5.0 value) plus `position` = center/north/east/south/west/
up/down (default center). Arch: `orientation` (12: north/east/south/west upright with top up;
`up_<dir>` / `down_<dir>` = front facing up or down with that horizontal top) and `depth`
(center/negative/positive along the positive world axis of the pane's normal). Both reduce to a
`WindowPose` (front, top, depth; right = top x front) in `block/window/geometry/`; a
`WindowProfile` (ROUND, ARCH_LEFT, ARCH_RIGHT) says which pixels are glazed. `WindowGeometry` keeps
the 0.5.0 centred round window as the regression baseline.

**Pane geometry.** The slab is 2 px thick at 7..9 (centre), 0..2 (negative) or 14..16 (positive); the
glass sits 0.5 px inside each side. The round window is a 16 x 16 pane with a 12 px glazed circle.
The arches are two independent pieces that together form the upper half of a two-block window:
combined face u 0..32, v 0..16, circle centre (16, 0), outer 32 px, glazed opening 28 px, pixels
taken by their centre. Each piece is a square pane whose glass is a quarter circle in its bottom
inner corner (square wooden corners, no mullion, nothing below the diameter); left and right are
judged from the front and the back shows the mirrored silhouette. `WindowArchGeometry` is the arch
circle, `WindowPaneGeometry` the panel, junctions, shapes, boundary footprints and the buried-face
test, `WindowShapes` the cached voxel shapes and culling footprints (the Minecraft adapter).

**Targeted placement** (`block/window/placement/`). `WindowTargetZones` is the one authority: a
centre square [1/4,3/4]^2 (boundary included) and four edge trapezoids on the clicked face; outside
the centre the larger distance from the face middle picks the edge, an exact diagonal tie the u
edge. Face coordinates: up/down faces u = east, v = south; north/south u = east, v = up; east/west
u = south, v = up. An edge zone names a world direction (the top and bottom zones of a wall face
mean up and down). `WindowPlacement` adapts the ordinary use context: the destination cell is
vanilla's (a replaceable clicked block is replaced), the zone is read on the originally clicked
cell's face, centre = upright and centred facing the player (round window: the 0.5.0 plane across
the line of sight), edge = flush against that side (six sides). Arches: centre = front towards the
player; edge = front towards the player's eye (tie or no player: towards the player if the plane
faces them, else into the cell); floor and ceiling pieces take the top the player sees as up. The
server decides from the use context, no packet carries the zone, and sneaking changes nothing.
Moving a window is break and replace. `client/window/WindowPlacementPreview` runs the same
placement path read-only; `WindowPlacementOverlay` (RenderHighlightEvent.Block, receiveCanceled,
never changes cancellation) draws the five-zone grid, the targeted zone in white and the pane
outline in light blue (hidden when local checks refuse), with `client/render/OutlineLines` shared
with the door highlight. Door hits go through `DoorHitResolver`.

**Junctions.** Connections stay the four horizontal vanilla ones. Only a centred upright pane
bridges (`WindowPaneGeometry.bridges`): a connected side across its plane gets the vanilla-pane
glass arm (x or z 7..9, full height) from the cell edge to the slab, reaching on to the recessed
glass where it ends on the opening; its in-plane sides need nothing. A positioned pane (flush,
floor, ceiling) never grows glass beyond its slab (0.6.1; 0.6.0's post, stem and fins filled the
corner where two flush windows met).

**Connection rule** (`block/window/connection/WindowConnections`). In-plane sides of a centred
upright window follow vanilla (as 0.5.0). A side across its plane connects only to a pane/bars, a
wall or a centred upright window crossing it (different normal); never a window face to face, a
positioned window or a solid block. A positioned window keeps no connection at all. The
connection package reads `WindowPaneBlock.pose` of neighbours and the block calls the rule: a
deliberate two-way edge inside `block/window`.

**Culling.** A window face on the cell boundary is skipped only when the neighbouring window's
geometry covers everything this one draws there (half-pixel footprints), not because the blocks or
connections match. Non-window neighbours never hide window faces. The glazing and arms use
Roundwood's `window_glass` (every texel visible, translucent) in a translucent model; collision
and outline are the panel plus active junction parts.

**Models** (`data/client/window/`). Posed models are authored once per canonical pose (upright
facing south, or front up/down with top north) and every other pose is a blockstate y rotation of
one. Per canonical pose: a core (panel, oak-bound), and for a centred upright pose one arm per
side across its plane. Names are `<profile>[_back|_front|_up_<floor|level|ceiling>|_down_<...>]`,
arms `_arm_<side>`. The round window's centred state reuses the 0.5.0 parts; flush uses
`round_window_back*` turned per side, ceiling `round_window_down_ceiling*`, floor
`round_window_up_floor*`. `GeometryModelWriter` drops an element whose faces are all buried.

**Client and save.** `WindowAppearanceModel` wraps round and arch states alike. The restyled item
model cache (`client/window/WindowItemModels`) is keyed by source item model and wood, since three
window items would otherwise share one item's geometry. Windows saved by 0.5.0 have no `position`
and load centred with their axis, connections, water and look; aliases (`LegacyWindowBlock`)
inherit the property, and `LegacyAppearances` and the 0.4.0 fixture are unchanged.

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
  conductor, `PushReaction.BLOCK`; only the anchor quarter has a block entity (the look). The selection shape is only the quarter's own
  ring piece, so rays pass through the centre and the corners to the door behind.
- **Lifecycle**: the door's pattern, through the shared `block/assembly` services: `TrimRemoval`
  clears loaded partners with drops suppressed (one item from the initiating quarter),
  `TrimIntegrity` removes an incomplete frame a tick later (deferred while a cell is unloaded),
  losing the floor under a lower quarter breaks it. Door and frame never remove each other.
  Rotation turns `facing`; mirroring mirrors it and swaps the column of `part`.
- **Looks** (see Appearance): vanilla woods in their stripped log, tiled per block; other woods in
  their plank sprite, tiled; paint in the painted board panel spread across the ring; masonry in a
  64 x 64 radial brick face (24 wedge bricks, a brick at the crown) spread across the ring.

## Appearance (0.5.0)

One canonical block and item per family; the look is data.

- **Value** (`appearance/Appearance`): kind (wood/masonry), the donor block's id and an optional dye.
  Saved as `RoundwoodAppearance: {Version, Kind, Material, Paint}` (`AppearanceNbt`) in item tags and
  block entities alike. Missing means the holder's default (`AppearanceDefaults`: oak, or an alias's
  historical finish), so only non-default looks are stored and default stacks stack. Reading never
  fails; an unregistered but well-formed id is kept (drawn as oak, restored when its mod returns).
- **Storage**: door anchor, trim anchor (non-ticking `RoundTrimBlockEntity`) and every window
  (`RoundWindowBlockEntity`). Loads and update packets replace the whole look (a paint removal
  reaches clients); loading never resets the door swing.
- **Items**: placement reads the stack (`AppearanceStacks`) and installs the look on the new anchor in
  the same placement transaction (windows via `updateCustomBlockEntityTag`), so a cancelled placement
  rolls it back with the cells. Picks (`getCloneItemStack`) and loot (`getDrops` -> `AppearanceDrops`)
  read the look through the anchor from any part.
- **Harvest lifetime** (`block/assembly/AssemblyHarvest`): Forge removes a block before its harvest
  callback, and removing one part tears down the anchor. For a harvesting survival break on the
  server, `onDestroyedByPlayer` only announces the break; `playerDestroy` evaluates loot with the
  assembly present and removes the part in a `finally` (normal teardown and refunds); a deferred part
  whose callback never came is removed at the end of the level tick. Explosions, support loss and
  integrity cleanup evaluate loot before removal anyway.
- **Gestures** (`appearance/interaction`): sneak + planks (tag `minecraft:planks` or
  `roundwood:appearance_planks`), dye, sneak + one of four masonry blocks (trims). Forge never calls
  block use for a sneaking player holding something, so `AppearanceUseEvents` sets `useBlock=ALLOW`,
  `useItem=DENY` on `RightClickBlock` for exactly those gestures on Roundwood blocks (cancelled events
  and existing denials are left alone); the block's router then acts once. `AppearanceActions` checks
  build permission over every loaded cell and completeness, applies `AppearanceChanges`, and charges
  one item for a change (none in creative, for an unchanged result or for a refusal). Doors route in
  `DoorInteraction`: restyle on the leaf (refused with a hint on a corner), then the offhand pass,
  then camouflage, then the toggle; `DoorHitRegion` decides corner or leaf once.
- **Rendering** (client): the oak source models are retextured, never rebuilt per frame.
  `DoorAppearanceModels` swaps only the leaf's `door_panel_oak` quads (vanilla woods and paint: their
  own panel art; other woods: `door_panel_neutral` with tint index 0, the BER passing the donor's
  sampled colour so only the panel is tinted). `RoundTrimRenderer` (anchor BER) tessellates each
  quarter, retextured by face/edge and turned for the facing (positions, normals, winding, cull
  sides), at its own cell with world light. Windows stay chunk-baked: `WindowAppearanceModel` wraps
  every window blockstate model and swaps the `oak_planks` quads by the model data its entity hands
  it. `AppearanceItemOverrides` resolves item stacks the same way. `DonorSprites` (shared with
  camouflage) and `DonorColors` cache 512 donors; model caches hold 2048 entries keyed by look and
  geometry variant (the window item cache by source item model and wood); all of them clear together on every model bake.
- **Viewers**: `client/compat/emi` replaces each canonical index entry with a stack whose identity,
  NBT and comparison are the default's and whose icon (unbatched) cycles through `AppearanceSamples`;
  it serialises as the plain stack. Styled stacks compare by appearance key. `client/compat/jei` gives
  subtypes by key and hides the aliases. Both are discovered by annotation, so nothing loads without
  the viewer.

## Migration from 0.4.0

The 49 0.4.0 ids stay registered as hidden aliases (`migration/LegacyContent`; blocks extend the
canonical classes with their frozen look, items place the canonical block). `ChunkEvent.Load` only
reads the chunk's sections (skipping palettes without an alias) and queues one cell per object;
`LegacyChunkMigration` converts at most 64 per level at the end of the level tick, dropping work for
chunks that unloaded again. Aliases placed later convert through their own scheduled tick, tried
before the integrity check. A conversion (`Legacy{Door,Trim,Window}Conversion`, `LegacyCells`) reads
the whole object (each cell the alias or the canonical block in exactly its slot), takes the anchor's
look and camouflage (refund included), rewrites the alias cells silently inside an
`AssemblyMutationBatch` (`updateShape` now stands down for marked cells) keeping every shared state
property, restores look and camouflage on the new anchor, then notifies neighbours. Nothing drops,
refunds or sounds. An unloaded cell defers; a loaded incomplete object is cleared by integrity with a
canonical drop in its old look.

## Data

Everything is generated: textures by `tools/generate-textures.mjs` (checked in under
`src/main/resources`), the rest by datagen. `GeometryModelWriter` cuts every element at the 16-pixel
texture-tile boundaries and by default writes vanilla's default UVs for its position inside its own
tile, so no face samples outside its sprite. A `FaceStyle` can hide buried faces, give some faces
another texture variable (a frame's sides) or explicit UVs: door panels and whole-ring frames use
`AssemblyFaceUv`, which spreads one image across the whole 32 x 32 front and back. Door: two leaf
templates, two oak leaves (`round_door_leaf_<hinge>`), one empty world model and a single `""`
variant. Window: posed models per canonical pose (core, plus arms for centred poses; see Window) in
multipart blockstates, 50 block models in all, three window item models.
Trim: quarter templates in two families (tiled, whole-image), oak source quarters per family, one
empty world model and a single `""` variant. `LegacyModelProvider` gives the 49 aliases blockstates
over the canonical models and item models parenting the canonical items. Items carry front GUI light
and computed centred transforms. Plus five recipes and their advancements, loot (aliases drop the
canonical item), tags and English names.
