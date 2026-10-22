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
  angle), plus the filled corners and the installed trim rings (`DoorSelectionShapes.visibleStatic`),
  so the outline never leads the leaf (0.6.1).
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

Ten blocks share one pane base, `WindowPaneBlock` (a vanilla `IronBarsBlock`): `RoundWindowBlock`
and the single-cell `OrientedWindowBlock` pieces (arch left and right, corner bottom left and
right), each also in a `_cross` version. One block entity type, `roundwood:round_window`, holds the
look of all ten (and of 15 alias windows). Every state has the four horizontal connections,
`waterlogged`, `trimmed` and `face_reversed`.

**State.** Round window: `axis` (the 0.5.0 value) plus `position` = center/north/east/south/west/
up/down (default center). Arches and corners: `orientation` (12: north/east/south/west upright with
top up; `up_<dir>` / `down_<dir>` = front facing up or down with that horizontal top) and `depth`
(center/negative/positive along the positive world axis of the pane's normal). Both reduce to a
`WindowPose` (front, top, depth; right = top x front) in `block/window/geometry/`. What a block
draws is a `WindowDesign` (`geometry/face/`): a `WindowProfile` (ROUND, ARCH_LEFT, ARCH_RIGHT,
BOTTOM_LEFT, BOTTOM_RIGHT, each with its assembly offset) says which pixels are glazed, and a
`WindowPattern` (PLAIN or CROSS) says whether beams cross the opening. The pattern is fixed per
registration, not a blockstate. `trimmed` (boolean, every window and alias; missing in an old save
reads false) is a derived summary: either face has a trim (see Window trims). `face_reversed`
(boolean, every window and alias, default false) is the face identity bit (see Face identity).

**Pane geometry.** The slab is 2 px thick at 7..9 (centre), 0..2 (negative) or 14..16 (positive); the
glass sits 0.5 px inside each side. The round window is a 16 x 16 pane with a 12 px glazed circle.
The arches are two independent pieces that together form the upper half of a two-block window:
combined face u 0..32, v 0..16, circle centre (16, 0), outer 32 px, glazed opening 28 px, pixels
taken by their centre. Each piece is a square pane whose glass is a quarter circle in its bottom
inner corner (square wooden corners, no mullion, nothing below the diameter); left and right are
judged from the front and the back shows the mirrored silhouette. `WindowArchGeometry` is the arch
circle. The corners (`WindowCornerGeometry`) glaze u 2..16, v 2..16 (bottom left) or u 0..14, v 2..16
(bottom right): a two-pixel outer jamb and sill meeting square, no top or inner border. Arches over
corners make a 32 x 32 window whose lower opening is x 2..30, y 2..16 under the radius-14
semicircle, with one continuous frame and no divider. Corners are independent pieces (no partner
lookup or teardown) with the arches' placement, overlay, restyling and water.

**Cross beams.** A crossed piece adds a one-pixel cross through its own middle (u and v 7.5..8.5),
clipped to its own opening (`WindowMuntinGeometry`). Each crossed piece has its own cross, so four
crossed pieces in a 2 x 2 show four crosses (a grid), never one shared cross. Beams are wood (the
pane's wood or paint) at full slab depth; the glass is cut away under them and a centred pane's
arms respect them. Beams are never trimmed.

**Exposed-surface generation.** `WindowPaneGeometry` gives the logical pieces (wood, glass, beams,
arms); `WindowShapes` the cached voxel shapes and culling footprints (the Minecraft adapter; keyed
by design, pose, connections and, for selection, the trimmed sides; collision and selection distinct). The models do not
draw those pieces. `WindowSurfaceGeometry` paints them into a half-pixel voxel grid (later pieces
replace earlier ones) and emits only visible faces: glass never against anything (no translucent
sheet can sit inside glass), opaque material (wood, beams) only against air or glass so
reveals stay, merged into rectangles, with default position UVs so the glass texture runs on
continuously. Before 0.7.0 every face of every glass box was drawn, which produced horizontal seam
lines where translucent sheets overlapped.

**Targeted placement** (`block/window/placement/`). `WindowTargetZones` is the one authority: a
centre square [1/4,3/4]^2 (boundary included) and four edge trapezoids on the clicked face; outside
the centre the larger distance from the face middle picks the edge, an exact diagonal tie the u
edge. Face coordinates: up/down faces u = east, v = south; north/south u = east, v = up; east/west
u = south, v = up. An edge zone names a world direction (the top and bottom zones of a wall face
mean up and down). `WindowPlacement` adapts the ordinary use context: the destination cell is
vanilla's (a replaceable clicked block is replaced), the zone is read on the originally clicked
cell's face, centre = upright and centred facing the player (round window: the 0.5.0 plane across
the line of sight), edge = flush against that side (six sides). Arches and corners: centre = front towards the
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
geometry covers everything this one draws there (half-pixel footprints, `WindowShapes.hides`), not
because the blocks or connections match; so a 2 x 2 has no inner divider. Non-window neighbours never
hide window faces. The glazing and arms use `window_glass_clear` (the `window_glass` colours with
alpha 24..36; the diagonal accent texels keep alpha 176) in a translucent model, or the dyed
`window_glass_<dye>` when the window is tinted (see Glass colour); collision and outline are the
panel plus active junction parts (the outline also includes the in-cell part of installed trim
bands, never collision).

**Models** (`data/client/window/`). `GeometryModelWriter.addSurfaces` writes each surface as one
element: a half-pixel-thick backing box with one face (the shared record is `geometry/Surface`).
Models are authored per canonical pose (upright facing south, or front up/down with top north) and
every other pose is a blockstate y rotation of one. Each blockstate state applies exactly one
complete model: the blockstates are multipart (`getMultipartBuilder`, `WindowPaneModelWriter.addParts`),
but with mutually exclusive parts, one per pose and arm combination, each selected by the pose
properties plus the connection properties it depends on, so exactly one part (one complete model)
matches any state. A centred upright pose has one part per combination of its two across-plane
connections (`_arms_north`, `_arms_south`, `_arms_both`), and every other pose has one. The models are pane-only (frame and glass): trims are not in them
(0.7.0 also wrote a `_trimmed` copy of each). Names are
`<design>[_back|_front|_up_<floor|level|ceiling>|_down_<...>][_arms_<...>]`.
`OrientedWindowModelProvider` and `WindowModelProvider` write them (110 window block models, 132
block models in all); `GeometryModelWriter` drops an element whose faces are all buried.

**Face identity.** A window has two faces, FRONT and BACK (`block/attachment/AttachmentFace`, saved
keys "Front" and "Back"), and a trim belongs to one of them. The blockstate bit `face_reversed`
(default false) says which geometric side the FRONT slot is on: FRONT faces `WindowPose.front`, or
the opposite side when the bit is set (`geometry/WindowFaceIdentity`, the pure mapping). The bit
changes no geometry and selects no model. Rotations and mirrors move the attachment front like any
direction and recompute the bit (`WindowPaneBlock.keepFaces`): a centred round window turned 180
degrees keeps its pose but the bit toggles, so each trim stays on the face it was on; oriented
rotations keep the bit; a mirror toggles it (the pose mirror reverses the geometric front to keep
handedness). Never set it by hand.

**Window trims.** Since 0.9.0 the Universal Trim (see Face-trim attachments; family TRIM, so it
takes wood, paint or the four masonry materials). One item trims one face, the
clicked one, so a window takes up to two trims, each with its own look. A plain click fits it (no
sneaking); a face that already has a trim is a free no-op and nothing is used. The item has no use
of its own: the window router fits it (see Hit classification and routing). Fitting, removal and
restyling go through the shared `TrimFitting` with a `WindowTrimActions` host whose writes keep the
`trimmed` summary in step.

- **State and payload.** The shared value is `block/attachment/FaceTrims` (two optional looks;
  whether a face has a trim never depends on its look) with one codec, `FaceTrimsNbt`:
  `{Version, Front: {RoundwoodAppearance?}, Back: {...}}`, where an empty face compound is a present
  oak trim and an absent key is no trim. A window stores it under `WindowTrim: {Version: 2, Front,
  Back}` (`trim/WindowTrimNbt`, which also owns the 0.7 adapter); the pane's top-level
  `RoundwoodAppearance` is untouched. `trimmed` stays a derived "either face present" summary.
  Loading never writes world state: `RoundWindowBlockEntity.onLoad` schedules a block tick
  (`WindowPaneBlock.tick`) that corrects an out-of-step summary through `WindowTrimActions.reconcile`.
  Save and update tag always write version 2, and the update tag always carries it so removals reach
  clients. Read rules: no payload with `trimmed=true` is oak on both faces (0.7.0 stored nothing for
  oak); version 1 (one shared look) with `trimmed=true` is both faces in that look; version 1 or no
  payload on an untrimmed state is no trims; version 2 is authoritative, including an explicit
  empty (a stale `trimmed` never resurrects a removed trim); an unknown version reads as no trims,
  the raw payload is kept and saved back, and trim edits are refused (fail closed).
- **Bands and depth** (`geometry/trim/WindowTrimGeometry`, independent of `face`; visible surfaces
  from `WindowTrimSurfaces`). Round window: a ring 16 outer / 12 inner; arches: their quarter of the
  32/28 ring; corners: the outer jamb and sill. Every band stands one pixel proud of its face of the
  slab: a centred pane (7..9) gets 6..7 and 9..10; a flush pane at 0..2 gets -1..0 and 2..3; one at
  14..16 gets 13..14 and 16..17. The wall-side band of a flush, floor or ceiling pane is therefore
  one pixel beyond the cell. It is render-only (the block-entity renderer): never collision,
  support, connection or a neighbour block. Surfaces are the outward face plus the perimeter
  reveals, once each, with no mounting face and no internal faces. 0.7.0 inlaid the flush band at
  0..1 (or 15..16), replacing the pane's own outer pixels instead of standing proud, so a trim on a
  flush, floor or ceiling window read as no trim at all. That was the root cause of the owner
  report "trim missing on the other window blocks" (it showed only when the pane was on an edge); it
  was not a targeting failure. `WindowTrimGeometryTest.flushTrimProjectsBeyondTheCellOnTheWallSide`
  guards it.
- **Selection.** Outline and rays include only the in-cell part of the bands of trimmed faces
  (`WindowShapes.selection(..., trimSides)`, read from the entity); a flush wall-side band is
  targeted through the slab face under it. Collision never includes trims.
- **Hit classification and routing.** `interaction/WindowHitRegions` is pure and classifies a point
  as TRIM, FRAME, GLASS, CONNECTOR_GLASS or NONE: the beams (7.5..8.5 half pixels) are FRAME, never
  glass or trim; the trim band wins only on the clicked face and only if it has a trim; the exact
  outer edge 16 belongs to the last cell; outside is NONE. `interaction/WindowHitTarget` turns a
  Minecraft hit into a face slot plus a region: the side comes from the hit position along the
  normal against the slab middle after nudging into the surface; an exact tie goes to the viewer's
  side, else FRONT; outside the slab an installed band on that side is TRIM, a centred pane's arms
  are CONNECTOR_GLASS, anything else NONE. `interaction/WindowUseRules` is the pure routing table;
  `WindowInteraction` is the adapter (extracted from `WindowPaneBlock.use`). Main hand first, the
  offhand only with an empty main hand. No action needs sneaking and sneaking does the same:
  - Any Universal Trim id (`TrimAttachmentItem`, perimeter slot) on any face target: fits the clicked face.
  - Planks: on a trim band restyle that face's trim (removing its paint); on frame, beam or glass
    restyle the frame and beams, the glass kept.
  - Dye: on a trim band paint that face's trim (masonry refuses, free); on frame or beam paint the
    frame; on glass or connector glass tint the whole window's glass.
  - Masonry: on a trim band restyle that face; elsewhere refused with the action-bar hint "Masonry
    restyles a trim; aim at the trim", nothing used and no block placed.
  - Shears: on a trim band remove that face only (survival: a styled `round_trim` back or dropped
    and one wear; creative: nothing back, no wear); elsewhere no change.
  - Bucket or anything unrelated: the ordinary item use.
  Payment is one item per actual change in survival, none in creative, repeats free.
- **Sneak bridge.** `appearance/interaction/AppearanceUseTarget` asks the target block instead of
  guessing a family at the hit: `claimsUse(state, level, pos, player, hand, hit)`. Windows and doors
  answer with their router's own decision (`WindowInteraction.claims`, `DoorInteraction.claims`), so
  the bridge and the routing share one targeting. `AppearanceGestures.donor` recognises donors
  without the sneak requirement.
- **Loot.** Each window loot table (canonical and 15 aliases) is the window item pool plus one pool
  per face (`roundwood:trim_face` with `"face": "front"|"back"`, plus survives_explosion, and
  `copy_nbt` from `WindowTrim.Front.RoundwoodAppearance` or `WindowTrim.Back.RoundwoodAppearance` to
  `RoundwoodAppearance`), built by `data/server/AttachmentLootPools`. The condition
  (`block/attachment/loot/TrimFaceCondition`, registered by `registry/ModLootConditions`) reads the
  loot context's block entity only (a `TrimAttachmentSource`), never the block state. Breaking
  yields the pane and one trim per trimmed face; the pane drops with its frame look and glass;
  trims are never in pick-block.
- **Glass colour.** `WindowGlass: {Version: 1, Color: "<dye>"}` on the window entity and on window
  item stacks (`glass/WindowGlassNbt`, `WindowGlassStacks`, `WindowGlassActions`). A missing key is
  clear and is never written, so clear stacks stack with crafted ones. One colour covers the whole
  window including the connector arms; dyeing the glass replaces the tint and there is no way back
  to clear except a new window. Drops and pick-block keep frame look and glass; placement installs
  both in the placement transaction. Sixteen generated textures `window_glass_<dye>` (vanilla
  stained-glass colours, the clear glass's diagonal-mark mask and alpha 176, drawn darker; body alpha
  108..120; palette `STAINED_GLASS` appended to `tools/textures/palettes.mjs`, artwork in
  `tools/textures/stained-glass.mjs`); the clear glass bytes are unchanged.
- **Rendering split.** The chunk model is pane-only. `WindowAppearanceData` carries the frame look
  and glass colour; `client/window/WindowAppearanceModel` retextures oak planks to the frame look
  and the clear glass to the tinted glass, and its cache key adds the glass sprite. The item model
  cache key is (source, wood, glass) (`WindowItemModels`). Trims are drawn by
  `client/window/WindowTrimRenderer`, a block-entity renderer on the window entity: solid layer,
  cached models from `WindowTrimWorldModels`, general quads with no cull side, world light, render
  bounds the cell inflated by one pixel, `shouldRenderOffScreen`. That is how a wall-side band
  can stand outside the cell. Looks: vanilla woods with their stripped log tiled, other woods with
  the plank sprite tiled, paint with the door boards, masonry with a thin-mortar face
  (`window_trim_round_<m>`, 32 x 32, 12 bricks, over the round window; `window_trim_arch_<m>`,
  64 x 64, over the whole 2 x 2 arched window), the edge in the granular `trim_edge_<m>`. Paint and
  masonry spread over the whole round or 2 x 2 face through `WindowTrimUv`. Edge UVs fold into the
  sprite through `geometry/FaceUv.tiled`, which handles -1 and 17; every projected UV must go
  through it. The marker textures `window_trim_face` and `_edge` are still generated but no model
  uses them. The trim item model `item/window_trim` is resolved per stack.
- **Viewers.** JEI subtypes and the EMI comparison key are the appearance key plus the glass colour
  (`WindowGlassStacks.key`); the index still lists thirteen default entries.

**Client and save.** `WindowAppearanceModel` wraps every window state alike. The restyled item
model cache (`client/window/WindowItemModels`) is keyed by source item model, wood and glass, since the
window items would otherwise share one item's geometry. Windows saved by 0.5.0 have no `position`
and load centred; windows saved by 0.6.x have no `trimmed` and load untrimmed, both with their
axis, connections, water and look unchanged; aliases (`LegacyWindowBlock`) inherit the properties,
and `LegacyAppearances` and the 0.4.0 fixture are unchanged.

## Face-trim attachments (0.9.0)

One attachment item and one transaction for every host; the sill (0.10.0) and the archways (0.12.0)
plug in as further slots and hosts, not copies.

- **Slot** (`block/attachment/TrimSlot`): `PERIMETER` (the Universal Trim, item `round_trim`) and
  `SILL` (planned, item `arch_sill_trim`, no host stores it yet). A slot names the canonical item
  a removed or dropped attachment becomes, and is the loot condition's `slot`.
- **Storage** (`FaceTrimStore`): one host's FRONT/BACK pair in one slot plus custody of an unknown
  payload. The host supplies a `Format` (key, version, whether an empty pair is written: windows
  yes, doors no) and, at load, a `Legacy` reader (the window's 0.7.0 adapter; doors have none).
  The current version decodes through `FaceTrimsNbt`; anything not understood reads as no trims,
  is kept and saved back byte-for-byte, and `set` refuses until a build that understands it loads
  it. Update packets always carry the payload, explicit empty while an unknown one is held. One
  store per slot, so slots never see each other's payload. Both block entities delegate to it.
- **Transaction** (`TrimFitting`, on a `TrimHost`): fit (empty face takes the stack's look, one item
  unless creative; occupied is a free no-op), remove (shears: the slot's item in that look back or
  dropped, one wear), install (no player), restyle and a per-face `AppearanceActions.Target`. A host
  adapter (`DoorTrimActions`, `WindowTrimActions`) does only its own lookup and checks (complete,
  loaded, permission over every cell) and its store hook.
- **Items** (`block/attachment/item`): `TrimAttachmentItem` names a slot; hosts recognise trims
  through `TrimAttachmentItem.slotOf`, never a class or id. `UniversalTrimItem` is registered as the
  advertised `round_trim` (it keeps its block association with the compatibility frame, never places
  one, and claims the frame's item mapping) and, hidden, as every older id: the 19 0.4.0 trim aliases
  (historical looks) and the 0.8.0 `window_trim` (`migration/LegacyContent`). Its own use does
  nothing: the host router fits, so a click is never fitted twice.
- **Loot.** `roundwood:trim_face` takes an optional `slot` (omitted = perimeter, which is how it is
  written, so 0.8.0 tables are unchanged). `AttachmentLootPools` builds a slot's two face pools;
  windows and doors drop `round_trim`.
- **Icon** (`client/item/UniversalTrimModels`): every TRIM-family item (all ids) wraps the generated
  square frame (`item/round_trim`, 16 px, 2 px band, 2 px deep, whole-face UVs over 16) and
  retextures it through the shared resolver `client/material/TrimLooks` (masonry: the square face
  `universal_trim_<m>`, straight courses). `TrimLooks` also serves the frame/door rings and the
  window trims, which differ only in the masonry face.

## Door trim

The Universal Trim attaches to a door face as a static casing, using the shared `block/attachment`
services (see Face-trim attachments). Until 0.9.0 the `round_trim` item was the "Door Trim"
(`block/door/trim/DoorTrimItem`, now deleted).

- **Use.** On a closed door it trims the clicked face: any hit inside the ring's outer edge counts,
  and the face is chosen by depth against the leaf's middle z 14.5. On an open door only an aimed
  static ring target works. An occupied face is a free no-op; anything else is refused without
  toggling, camouflaging or placing. Main hand wins, the offhand only with an empty main hand.
- **Geometry** (`block/door/trim/DoorTrimGeometry`, door authored frame, facing north, front at
  z=16). The 32/28 ring is the shared `geometry/RingGeometry`, extracted from
  `block/trim/geometry/TrimGeometry`. FRONT at z 16..17, where a 0.7.0 fitted frame stood
  (render-only, one pixel into the cells in front); BACK at 12..13. The casing is static: it never
  swings and has no collision, pathfinding, support or redstone effect.
- **Storage.** `DoorTrim: {Version: 1, Front, Back}` on the door anchor entity (`DoorTrimNbt`, the
  `FaceTrimsNbt` codec; an unknown version is kept and fails closed). The update tag always carries
  it. `block/door/assembly/DoorBlockEntities` (extracted from `DoorAppearance`) finds the anchor entity.
- **Targeting.** `DoorSelectionShapes` adds in-cell proxies to the target shape: installed rings
  (the FRONT proxy at z 15..16 inside the door cells, the BACK ring itself), and both while the Door
  Trim is held, so an open door can be fitted. The highlight's visible shape draws installed rings
  where they are (the front ring at 16..17) and never an absent ring; while swinging the static
  outline is the corners plus the rings (`visibleStatic`). `DoorHitResolver` is unchanged: there are
  no hits in neighbouring cells.
- **Routing** (`DoorInteraction`, ahead of restyle, camouflage and toggle). Holding a trim
  attachment (any id) the router fits the face `DoorTrimTarget.fit` picks, or refuses; it never
  toggles, camouflages or places (an empty main hand with a trim in the offhand passes to the
  offhand). `claims` answers yes for a fit target, so sneaking fits the same way. On a ring (`DoorTrimTarget.ring`): shears remove that
  face (a free no-op if untrimmed); planks, masonry and dye restyle an installed ring by the trim
  family rules (masonry is not paintable). Otherwise the existing leaf restyle (sneak + planks,
  dye), camouflage and toggle apply. `DoorInteraction.claims` feeds the sneak bridge.
- **Rendering.** `client/door/DoorTrimRenderer`, called by `RoundDoorRenderer` before the facing and
  swing transforms, reuses the frame's turned quarter models (`client/trim/TrimAppearanceModels`,
  same looks). FRONT is drawn at the cells in front, BACK in the door's cells shifted to 12..13;
  rear surfaces are kept. The BACK ring overlaps the hinge-side brass straps where they cross it
  (cosmetic).
- **Loot.** Door tables (canonical and aliases) gain the two face pools, dropping `roundwood:round_trim`
  with `DoorTrim.Front` or `Back` `.RoundwoodAppearance`. `RoundDoorBlock.getDrops` supplies the
  anchor's entity as the loot BLOCK_ENTITY from whichever quarter broke, so one door and one trim per
  face drop once.

## Frame (compatibility)

Before 0.8.0 the `round_trim` item placed an independent 2 x 2 assembly of `RoundTrimBlock` quarters
(`facing` + `part`) through the ordinary `ItemStack.useOn` path. No item places one any more, but the
block stays registered for frames in older saves. Geometry (`block/trim/geometry/TrimGeometry`, now
built on the shared `RingGeometry`): a ring, outer diameter 32, inner 28, one pixel deep at the back
of its cells (z 0..1, authored facing north), cut into cell-local quarters.

- **Fitted frames** (0.7.0) stood in the cells in front of a closed door, the ring one pixel proud of
  the leaf. Migration turns them into the door's FRONT trim (see Migration). `TrimPlacement` no
  longer reads doors; it keeps the checks below for `placeAt`.
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
  64 x 64 radial brick face (24 wedge bricks, a brick at the crown) spread across the ring. Since
  0.7.0 that face is `trim_radial_thin_<m>` (joint half width 0.425 texels, anti-aliased) via
  `client/material/MasonryStyles`; the old `trim_radial_<m>` stay generated and pinned, unused.
  Freestanding frames break normally, drop the Universal Trim in their look and keep their
  integrity policy; they restyle through the sneak gestures.

## Appearance (0.5.0)

One canonical block and item per door and trim; the window family has ten blocks (since 0.7.0) and
the window trim is a trim-family item. The look is data.

- **Value** (`appearance/Appearance`): kind (wood/masonry), the donor block's id and an optional dye.
  Saved as `RoundwoodAppearance: {Version, Kind, Material, Paint}` (`AppearanceNbt`) in item tags and
  block entities alike. Missing means the holder's default (`AppearanceDefaults`: oak, or an alias's
  historical finish), so only non-default looks are stored and default stacks stack. Reading never
  fails; an unregistered but well-formed id is kept (drawn as oak, restored when its mod returns).
- **Storage**: door anchor (which also holds the door trims under `DoorTrim`), frame anchor
  (non-ticking `RoundTrimBlockEntity`) and every window (`RoundWindowBlockEntity`, which also holds
  the window trims under `WindowTrim` and the glass colour under `WindowGlass`). Loads and update packets replace the whole look (a paint removal
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
- **Gestures** (`appearance/interaction`): planks (tag `minecraft:planks` or
  `roundwood:appearance_planks`), dye, one of four masonry blocks (trims). Windows and door trims
  need no sneaking; the door leaf and freestanding frames still want sneak + planks or masonry.
  Forge never calls block use for a sneaking player holding something, so `AppearanceUseEvents` sets
  `useBlock=ALLOW`, `useItem=DENY` on `RightClickBlock` for exactly the gestures the target block
  claims (cancelled events and existing denials are left alone). The bridge asks the block's router
  through `AppearanceUseTarget.claimsUse` (windows and doors answer with their own routing
  decision), so targeting is identical; the block's router then acts once. `AppearanceActions` checks
  build permission over every loaded cell and completeness, applies `AppearanceChanges`, and charges
  one item for a change (none in creative, for an unchanged result or for a refusal). Doors route in
  `DoorInteraction`: restyle on the leaf (refused with a hint on a corner), then the offhand pass,
  then camouflage, then the toggle; `DoorHitRegion` decides corner or leaf once.
- **Rendering** (client): the oak source models are retextured, never rebuilt per frame.
  `DoorAppearanceModels` swaps only the leaf's `door_panel_oak` quads (vanilla woods and paint: their
  own panel art; other woods: `door_panel_neutral` with tint index 0, the BER passing the donor's
  sampled colour so only the panel is tinted). `RoundTrimRenderer` (anchor BER) tessellates each
  quarter, retextured by face/edge and turned for the facing (positions, normals, winding, cull
  sides), at its own cell with world light; `DoorTrimRenderer` does the same for door trims. Window
  frame and glass stay chunk-baked: `WindowAppearanceModel` wraps every window blockstate model and
  swaps the `oak_planks` quads (frame look) and the clear glass quads (tint) by the model data its
  entity hands it; window trims are drawn by `WindowTrimRenderer` (see Window trims). `AppearanceItemOverrides` resolves item stacks the same way. `DonorSprites` (shared with
  camouflage) and `DonorColors` cache 512 donors; model caches hold 2048 entries keyed by look and
  geometry variant (the window item cache by source item model, wood and glass); all of them clear together on every model bake.
- **Viewers**: `client/compat/emi` replaces each canonical index entry with a stack whose identity,
  NBT and comparison are the default's and whose icon (unbatched) cycles through `AppearanceSamples`;
  it serialises as the plain stack. Styled stacks compare by appearance key. `client/compat/jei` gives
  subtypes by key and hides the aliases. Both are discovered by annotation, so nothing loads without
  the viewer.

## Migration (from 0.4.0 and 0.7.0)

The 49 0.4.0 ids stay registered as hidden aliases (`migration/LegacyContent`; blocks extend the
canonical classes with their frozen look, items place the canonical block). `ChunkEvent.Load` only
reads the chunk's sections (skipping palettes without an alias) and queues one cell per object;
`migration/ChunkMigrations` and `ChunkMigrationQueue` own the scheduling (since 0.8.0): discovery
reads only the chunk's own sections, one deduplicated queue runs at most 64 entries per level at the
end of the level tick, and work for chunks that unloaded again is dropped. `Roundwood` registers
`ChunkMigrations`' listeners. `LegacyChunkMigration` keeps only the 0.4.0 alias policy (`isLegacy`,
`convert`, `key`, `convertOnTick`). Aliases placed later convert through their own scheduled tick,
tried before the integrity check. Order for a pair: the 0.4.0 aliases canonicalize first, then the
fitted-frame conversion is tried. A conversion (`Legacy{Door,Trim,Window}Conversion`, `LegacyCells`) reads
the whole object (each cell the alias or the canonical block in exactly its slot), takes the anchor's
look and camouflage (refund included), rewrites the alias cells silently inside an
`AssemblyMutationBatch` (`updateShape` now stands down for marked cells) keeping every shared state
property, restores look and camouflage on the new anchor, then notifies neighbours. Nothing drops,
refunds or sounds. An unloaded cell defers; a loaded incomplete object is cleared by integrity with a
canonical drop in its old look.

**0.7.0 fitted frames.** `migration/trim/DoorTrimConversion` converts a fitted frame into the door's
FRONT trim only when the arrangement is exact: a complete canonical frame with the same facing as a
complete canonical door, the frame anchor equal to the door anchor offset by `facing.getOpposite()`,
all eight cells loaded and the door's FRONT absent. Open doors are eligible. In one
`AssemblyMutationBatch` over the eight cells it removes the four quarters silently (no teardown, no
loot, no neighbour cascade); only if all four went does it install FRONT (otherwise it restores the
captured quarters and look); then it sends neighbour updates for the frame cells. Conflicts keep the
frame untouched: an occupied FRONT (even in the same look), another facing or offset, an incomplete
frame (its own integrity policy), and aliases until canonical. It never force-loads and never runs
during snapshot restore. Discovery finds both frames and door anchors (a door is queued when the
cell in front may hold a frame: in another chunk, or a frame in this chunk), so whichever loads
last retries. A geometrically identical freestanding frame directly in front of a door cannot be
told from a fitted one and converts too. Other frames remain as legacy content. The 0.4.0 fixture's
fitted green trim now ends as the spruce door's FRONT trim.

**0.7.0 window trims** are not converted by a chunk pass: the window entity's `WindowTrimNbt`
adapter reads the old payload on load (see Window trims), so a 0.7.0 trim becomes the same look on
both faces and the tick reconciles `trimmed`.

## Data

Everything is generated: textures by `tools/generate-textures.mjs` (checked in under
`src/main/resources`), the rest by datagen. `GeometryModelWriter` cuts every element at the 16-pixel
texture-tile boundaries and by default writes vanilla's default UVs for its position inside its own
tile, so no face samples outside its sprite. A `FaceStyle` can hide buried faces, give some faces
another texture variable (a frame's sides) or explicit UVs: door panels and whole-ring frames use
`AssemblyFaceUv`, which spreads one image across the whole 32 x 32 front and back. Door: two leaf
templates, two oak leaves (`round_door_leaf_<hinge>`), one empty world model and a single `""`
variant. Window: pane-only visible-surface models per design, pose and arm combination (trims are
not in them; see Window), in one-model-per-state blockstates; 110 window block models (132 block
models in all), ten window item models. Textures:
`window_glass_clear`, `window_glass_<dye>` (sixteen), `trim_radial_thin_<m>`,
`window_trim_round_<m>`, `window_trim_arch_<m>`, `universal_trim_<m>` (0.9.0); `window_trim_face`
and `_edge` are still generated but unused. The Universal Trim item model is the square frame
(`data/client/trim/UniversalTrimItemModelProvider`), and `item/window_trim` parents it.
Frame (`data/client/trim/TrimModelProvider`): quarter templates in two families (tiled,
whole-image), oak source quarters per family, one empty world model and a single `""` variant. `LegacyModelProvider` gives the 49 aliases blockstates
over the canonical models and item models parenting the canonical items. Items carry front GUI light
and computed centred transforms. Plus twelve recipes and their advancements, loot (aliases drop the
canonical item; windows and doors add one trim pool per face), tags and English names.
