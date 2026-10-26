# Architecture - Roundwood

## Thin standalone arch pieces

Each modular product has a separate `_thin` registration. Existing IDs remain full-depth and
retain their old state schema. Thin pieces add `depth`: FRONT is authored z=13..16,
MIDDLE=6.5..9.5 and BACK=0..3. Product, thickness, side and depth participate in assembly
membership and geometry caches. Tops remain non-colliding; shoulders collide through their slab.

Shoulder wedges resolve facing/side first, then `FaceDepthBands` resolves depth in that facing.
The overlay clips those same wedge and band regions. Placement and preview consume the same
proposal. Body and casing model layers are explicit, so thin slabs cannot be mistaken for casings.
Item caches include thickness; item geometry uses the centred thin slab and the existing legal
icon translation. The showcase stacks all three thin depths beside the full products.

Thin casings retain their one-pixel depth on either slab face. Selection expands only into the
in-cell portion of installed casings; collision stays at the body slab. The `arch_thinning`
serializer wraps a one-ingredient shapeless recipe and copies supported stack metadata to one
matching thin item. Unknown top-level payloads, future appearance data and opaque capabilities
refuse conversion. There is no reverse recipe.

## Modular arch geometry (0.14.0)

`block/arch/geometry` is separate from the frozen migration-source `block/archway` family.
`ArchPieceDesign` names three tops and one mirrored shoulder. `ArchTopProfile`
subtracts radius-8/16/24 semicircles above springs 8/0/0; rows below the spring are empty.
`KeyholeShoulderProfile` keeps the historical transition thinned by two pixels (widths 4,3,3,2,2,1,1,0) and extends its four-pixel base to y=0.
`ArchPieceGeometry` supplies
full-depth boxes and per-cell visible surfaces through the shared raster/slab
helpers. Internal cell faces are omitted using the whole mask. All six cells of
the three-wide top are owned even though its lower centre has no drawn pixels.
Tops have empty collision; shoulder collision equals its full-height body.

`ArchMasonryJoints` is the pure, continuous-coordinate layout for model
baking: 5/9/13 equal-angle sectors, a radius-r..r+2 band, quarter-pixel joints,
no bearing joints and shoulder levels 0/4/8/12 (the 16 level is a zero-width row), clipped to actual solid pixels.
It supplies no material policy and no overlay faces. `ArchMasonryModels` partitions
donor and mortar surfaces for bodies, casings and items from this same layout.
Convex clipping subtracts each joint from the donor faces, then triangulates both
regions without coplanar overlays. Quarter-pixel widths are exact; each circular
joint end uses four chords with radial error below 0.0001 pixel. Wood and paint
bypass this partition. Retained archway models and historical PNGs are unchanged.

`ArchPieceLayout.Assembly` implements the shared assembly contract, anchored at
the lower-left cell. Membership includes exact block/product, facing, cell and
side. `ArchPieceBlock` delegates lifecycle callbacks to `AssemblyIntegrity`,
`AssemblyRemoval` and `AssemblyHarvest`; no external floor is required. Only the
anchor carries the distinct `arch_piece` entity, extending shared appearance
persistence. `ArchPieceAppearance` uses shared anchor lookup and appearance targets.
The four registrations have separate `archPieces()` enumerations and replace the old
archways in advertised lists. `archways()` remains the old two designs, registered
through retired adapters and aggregated by `LegacyContent`. Their entity ID and state schema
remain readable. `ArchwayConversion` replaces the four cells atomically: a top above, plus two
shoulders for keyholes or air for ordinary arches. The new crown is wider than the historical one;
keyhole lower geometry is preserved. Both casings transfer to the top. Scheduled
ticks convert before integrity cleanup; unknown trims defer. Item placement and editing are refused. Command-created cell states outside a product's dimensions are
inert and removed without looking up or tearing down a neighbouring assembly.

`ArchPieceTargeting` classifies four triangular face wedges (diagonal ties choose U).
Floor/ceiling wedges select their world edge; wall U wedges select the horizontal edge,
and lower/upper V wedges select touching/far edges. The result is an existing facing/side
pair, so saved shoulder states are unchanged. The overlay draws the same wedge polygons. `ArchPiecePlacement.of` uses the context's landing cell as lower-left anchor,
then the shared preflight checks every cell for world bounds, border, loaded state,
fluid, replacement and player permission. Entity checks use the product's collision,
without a floor predicate. `at` applies the same playerless checks. Item use recomputes
the proposal and passes it to `AssemblyPlacement`; showcase-style placement uses
the existing `AssemblyInstallation`. Neither shared installer signature changes.
The modular overlay consumes this proposal and shares hand precedence. It outlines every owned cell,
the complete silhouette, the landing face and the four shoulder wedges. Without secondary use,
it suppresses success on blocks overriding the use callback: those may consume the click first.
Callback detection uses its parameter types, so mapped runtime names do not affect the guard.

The anchor declares `ArchPieceTrim` v1 with independent front/back TRIM appearances.
The host adapter uses shared fitting, payment, shears refunds and permission checks.
Face donors restyle an installed casing; reveal donors restyle the body. Sill and beams
refuse with the existing hints. Loot evaluates against the anchor from any owned cell
and decorates only the exact body item, keeping both casing looks independent.
Casings cover the whole mask at z=-1..0 or 16..17 and never add collision.

`client/arch` draws body and casings from the anchor through `AssemblyCellRenderer`,
with each owned cell's light and AO. Cached models key product, side, cell, facing,
depth and look; resource rebakes clear them through `AppearanceModelCache`.
Every face uses the donor sprite (or painted boards), with UV projection after rotation.
Empty world models ensure that only the BER draws the body.
Styled items reuse the body baker and retain its donor UVs while translating cells
into the complete icon. `ArchPieceItemGeometry` shifts the three-wide icon by -16
pixels in X, shared by runtime and datagen, fitting the legal [-16,32] interval.
The dimension-aware transform overload fits each product into the inventory slot;
the historical two-argument overload keeps all existing transforms unchanged.

## Archway stack migration

`ArchwayStacks` plans complete container contents, preserving metadata on replacement tops and
body looks on every output. Shoulders fill permitted storage slots; overflow is accepted before
slots change. `ArchwayStackMigration` discovers block containers on chunk load, draining at most
64 per level tick. Player and ender inventories scan every 20 ticks; container entities and dropped
items queue on join and convert only after the join succeeds. Overflow drops at the holder; a
refused spawn discards partial overflow and preserves the original. No recursive nested-stack scan.
Unopened loot-table containers are left untouched so migration never rolls loot without the opening player.
The old crossed-window and trim paths are unchanged (those stacks still convert only when used).

## Aimed depths and double doors (0.15.0)

`assembly/DoorDepth` owns the three saved depths and aimed-face bands. FRONT defaults to
the historical slab 13..16; MIDDLE shifts by -6.5 pixels and BACK by -13. The round block
extends its inherited default state; no existing identity value changes. Arched 1x2 doors
retain their old schema. Depth participates in assembly membership, shape caches and placement.
`Assembly.toAuthored` removes the shift before hit classification; collision shifts before
cell clipping, selection shifts before facing rotation, and the renderer moves leaf,
camouflage and casings together. Back-flush casing targets stay inside the cell.

`DoubleArchedDoorBlock` takes a DoorDesign size parameter and has cell_x 0..2 / cell_y 0..3 alongside vanilla door properties and depth.
`DoorLayout.Part` retains the four historical enum entries first, then the five 3x3 cells and three additional 3x4 cells. Use columnIndex()/row() for geometry and Part.of(design) for traversal; the legacy
column/half fields alone cannot identify a nine/twelve-cell part. Vanilla half remains a compatibility
summary, while vertical survival uses the real row. Shared assembly services own installation,
integrity, teardown, appearance and synchronized open/redstone state.

The paired profiles have one or two square lower rows plus the complement of TOP_2X3, translated up 16 or 32
pixels. Each half is clipped at x=24, with its own outer pivot and full two-sided hardware.
Rendering and swing outlines turn both halves; collision and target geometry use the same
clipped envelopes. Leaf templates shift x/y by -16 to fit the legal model range; the 3x4 template first scales Y by 3/4;
the renderer and item transforms undo the Y compression, and the renderer undoes that shift after swinging, and the item combines both halves. Board
UVs span one square 48px/64px image extent; casings use tiled donor/paint sprites.

Door casing models all consume DoorTrimGeometry.surfaces through PixelSlabModels.
Surface.separated moves each render face 1/256 pixel along its normal, including hidden
contact faces and jamb/reveal edges. Modular arch casings use the same separation; body,
collision and targeting geometry remain exact. This avoids coplanar frame/panel/hardware
and frame/arch faces while retaining rear faces visible when a door opens.

## Door assembly

There are four door designs (`leaf/DoorDesign`: ROUND, ARCHED, DOUBLE_ARCHED, TALL_DOUBLE_ARCHED). This
section describes the round door's four-part assembly; the arched door is the two-part case.

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
- Every write Roundwood makes goes through `DoorPlacement` (the shared installation), `DoorStateController`
  or the shared removal (`AssemblyRemoval`), inside an `AssemblyMutationBatch` naming the cells.
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
commands, our own placement) schedules a block tick (`block/assembly/AssemblyIntegrity`, shared with archways and frames, 1 tick later, after the
writer finished). If all four cells are loaded and the assembly is incomplete, the part is broken
with its own loot and `AssemblyRemoval` (via `AssemblyTeardown`) clears its matching partners: at most one item. Missing parts
are never created; an unloaded cell re-schedules the check (every 20 ticks) without being read.
Vertically, `updateShape` keeps a part only while the cell above/below is its own column's other
half. Mirroring cycles `hinge` (vanilla) and `column`; rotation turns `facing` only.

## Shared assembly services

Doors (both designs), archways and the compatibility frame share one lifecycle; windows share only
the anchor persistence and the attachment services.

- **Contract** (`block/assembly/Assembly<P>`): anchor, facing, the ordered parts (lower-left,
  upper-left, lower-right, upper-right; the arched door only the first two), `pos` and the family's
  exact `isPart`. `positions`, `loaded`, `presentParts`, `complete`, `mayEditAll` and `toAuthored`
  are shared and ask `isLoaded` before every read. `DoorLayout.Assembly`, `ArchwayLayout.Assembly`
  and `TrimLayout.Assembly` implement it and keep their own `stateFor` and bounds; door membership
  includes design and hinge, never `open` or `powered`.
- **Lifecycle.** `AssemblyIntegrity` schedules from `onPlace` (not for an in-place change or a
  snapshot restore) and checks one tick later: retry every 20 ticks while a cell is unloaded, break
  an incomplete part with its loot. `AssemblyRemoval` stands down for the same block, a snapshot
  restore or our own batch, runs the family's pre-teardown hook (the door's camouflage release,
  resolved from the removed part's old state) and clears the loaded partners through
  `AssemblyTeardown`. The blocks call both directly.
- **Anchors** (`AssemblyAnchors`): from any part, the loaded anchor cell holding the same family class
  whose reconstructed layout equals the part's. That is anchor identity, which lets canonical and alias
  cells share an anchor, unlike exact membership. Loot falls back to the captured entity of the right
  type. Each family appearance adapter exposes one typed `AssemblyAnchors.Lookup<A,E>` constant;
  no per-family forwarding class remains.
  `AssemblyAppearanceTarget` is the whole-assembly restyling target: loaded, complete,
  typed anchor, permission over every cell, announced at the clicked cell.
- **Placement** (`placement/`). `AssemblyPlacementPlan` is the read-only skeleton: every cell in
  the world and dry first; then replaceable in the click's context with `mayInteract` and
  `mayUseItemAt` at every cell, or plainly replaceable without a player; then the family's per-part
  policy. `DoorPlacement` and `ArchwayPlacement` build the typed `AssemblyPlacementPlan<P,A>` directly (hinge, power, design,
  obstruction against their own collision); the frame uses the skeleton with survival only and no
  entity check. `AssemblyInstallation` writes the plan (`UPDATE_ALL`, in part order, inside one
  batch), then the look on the anchor; success means all of it happened. A refused write or batch, a
  missing anchor entity or an exception puts every cell back, block state and block-entity data, with
  drops and teardown suppressed (`restoringBlockSnapshots` on and capture off, both restored in a
  `finally`), drops only the block snapshots it added, and fails. Entry during a snapshot restore is
  refused. `AssemblyPlacement` adds the player's effects only on success: criterion, sound,
  `BLOCK_PLACE`, one item (none in creative), `CONSUME`. Forge's cancellation of the multi-place
  event is unchanged.
Assembly placement and restyling depend only on `appearance/AppearanceAnchor` (`appearance` and
`setAppearance`), not attachment persistence. Family placement policy and entry points share one class;
there is no copied state map or reconstructed plan.

- **Anchor persistence** (`block/attachment/AppearanceAnchorBlockEntity`). The door, archway,
  window and frame entities extend it and declare their appearance family and per-slot store formats
  in the constructor (the frame declares none, so it is never a trim host). It owns the look,
  normalised against the block's default; the guarded trim setters (a refused write neither dirties
  nor syncs); the save, load and update templates (disk keeps an unknown payload, updates always carry
  every store, explicit empty while one is unknown); and the packets (no tag means the default look
  and every store explicitly empty). Family hooks: door camouflage (saved with its refund, sent without
  it, explicit empty when cleared), window glass, the window's 0.7.0 perimeter `legacy` reader and its
  client model refresh (`afterClientUpdate`). Constructors and entity types are unchanged.

## Loot ownership

`TrimFaceCondition` owns its type and serializer; `ModLootConditions` registers that same type
under `roundwood:trim_face`. The condition has no dependency on the registry adapter.

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
  Since 0.13.4, a point on the resting open leaf outside those cells belongs to the nearest loaded
  part of its own assembly, without moving the point. Projected use travels through
  `adapter/DoorUseChannel`: `interaction/ProjectedDoorUse` validates reach, complete assembly,
  permissions, world border and real outline/collision rays, then invokes the normal server
  interaction router, main hand before offhand. This avoids vanilla's per-cell packet limit without
  faking a hinge-side hit or changing the highlight. Stale, obstructed or unrelated hits fail.
  Mining and picking the projection remain refused; held mining inside the cells keeps the part
  it started on (any part removes the whole door). Collision and the visual swing remain unchanged.
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

## The arched door (0.11.0)

- **Membership is explicit.** `DoorLayout.Assembly(anchor, facing, hinge, design)` knows its design;
  `parts()` is two cells (arched) or four (round); `designOf(state)` reads it from the block (it throws
  for a non-door state) and `columnOf(state)` exists only for the round door. The arched door has no
  `column` property: lower and upper halves only, no fictitious right column.
- **One adapter.** `block/door/adapter/AssemblyDoorBlock` holds everything a door part does;
  `RoundDoorBlock` adds only `column` (the 0.4.0 aliases and the 0.7.0 frame migration recognise
  exactly this class) and `ArchedDoorBlock` fixes the design. Both anchor on `RoundDoorBlockEntity`
  (type id `round_door`), so appearance family DOOR, camouflage, trim storage and lifecycle are shared.
- **One placement plan.** `assembly/DoorPlacement` builds the typed, immutable
  `AssemblyPlacementPlan<DoorLayout.Part, DoorLayout.Assembly>` (`of(block, context)` for players,
  `at(level, block, assembly, open)` for the showcase and commands): cells, hinge, open state, or
  empty when refused. Its placement entry point installs that same plan. The client preview
  (`client/door/DoorPlacementPreview`, via `selection/DoorPlacementOutline`) draws the same plan for
  both doors, so preview and result cannot drift.
- **Per-design geometry.** `DoorGeometry`, `DoorShapes`, `CamouflageGeometry`, `DoorTrimGeometry` and
  the client model caches take the design and key on it. The arched leaf is 16 x 32 pixels from
  `geometry/ArchedProfile` (straight sides, semicircular crown of radius 8 at (8, 24)), pivot x 1.5 or
  14.5; open, it lies inside its own column (13/16 clear), collision clipped to the two cells, nothing
  projecting. Its panels sample the left half of the 32x32 board image through the rectangular
  `AssemblyFaceUv` mapping.
- **Camouflage and casing.** Camouflage is the two upper corners beside the crown (built with
  `geometry/PixelSlab`). The trim is a two-pixel jamb-and-crown casing along the arched outline
  (FRONT z 16..17, BACK z 12..13, no threshold), built from geometry by `client/door/DoorTrimModels`;
  the round ring keeps the frame quarter models. Masonry uses `door_casing_arched_<material>`.

## Archways (0.12.0)

- **A fourth block family, static.** `block/archway/ArchwayBlock` (one class, `design()` ORDINARY or
  KEYHOLE, ids `archway`, `keyhole_archway`) has `facing` and `part` (lower/upper, left/right) like the
  compatibility frame; the lower-left cell carries `ArchwayBlockEntity` (type `archway`) with the look
  (appearance family TRIM: wood, paint, the four masonry) and the `ArchwayTrim` payload. No swing,
  glass, camouflage or redstone. It imports `block/attachment` and `block/assembly` only.
- **Profiles.** `geometry/ArchwayDesign` is a 32 x 32 pixel mask: square footprint (filled corners since 0.13.2)
  minus the opening, `ArchedProfile(2, 30, 16)` for the ordinary archway and, for the keyhole,
  `KeyholeGeometry` (the same crown above y 16; below it the edges move in linearly by 4 px over 8 px,
  then x 6..26 to the floor; pixel centres decide). `ArchwayGeometry` rasterises it once, extruded the
  full block (z 0..16). The target and outline are the drawn frame in each cell (`ArchwayShapes.shape`),
  so rays pass through the passage; since 0.13.1 the collision is smaller (`ArchwayShapes.collision`,
  `ArchwayGeometry.collisionCell`: the lower row's jambs only, nothing of the crown), so players and
  tall mobs walk through. The item model, the world models and the preview are built from the whole mask.
- **Placement.** `ArchwayPlacement.of(block, context)` (or `at(...)` for the showcase) is the one
  read-only decision: anchor = clicked or replaceable cell, facing = the placer's horizontal
  direction, right column = facing's clockwise side, four cells checked for bounds, border, loaded,
  permission, replaceable, dry, entity obstruction against the frame's own collision, and a sturdy
  floor under both lower cells. `ArchwayPlacement` writes exactly the plan through the
  shared `AssemblyPlacement` and `AssemblyInstallation` (one Forge multi-place event, rolled back on cancel) and charges once;
  `client/archway/ArchwayPlacementPreview` draws the same plan (frame outline from
  `ArchwayGeometry.placed`, the four-cell footprint).
- **Lifecycle.** Integrity is deferred one tick and retried while a cell is unloaded
  (`AssemblyIntegrity`); removing a cell clears the loaded partners silently (`AssemblyRemoval` over
  `AssemblyTeardown`); survival harvests defer removal (`AssemblyHarvest`) so loot reads the anchor;
  lower cells break when their floor goes. Loot: one archway in its look (`AppearanceDrops.decorate`
  by item, so the trims keep theirs) plus one `round_trim` per trimmed face via `AttachmentLootPools`
  keyed `ArchwayTrim`, evaluated with the anchor entity from whichever cell broke.
- **Face casings.** `ArchwayTrimNbt` (`ArchwayTrim` v1, no legacy, empty omitted) through
  `FaceTrimStore`; fitting, removal and restyling through `TrimFitting` with the anchor's live
  `TrimHost.anchored` view (`ArchwayTrimActions`). A casing covers the whole frame face (`ArchwayTrimGeometry.inCasing` = the
  frame mask), FRONT z 16..17, BACK -1..0, render-only. Targeting (`ArchwayTrimTarget`): a hit on the
  front or back plane addresses that face; any other surface is the archway; a held Universal Trim
  fits the nearer face by depth. The router (`ArchwayInteraction`, the held item classified by `HeldTrimTool`) is the window convention: plain
  click, sneaking identical through `claimsUse`, main hand first.
- **Rendering.** `ArchwayRenderer` (anchor BER, solid layer, per-cell world light) draws each cell's
  frame and installed casing pieces from `ArchwayModels`, which bakes them with the shared
  `client/render/PixelSlabModels` (the arched door casing uses it too). Since 0.13.3, archway bodies,
  casings and items use the donor block sprite on every face (paint: tiled window boards), with
  `FaceUv.tiled` applied after rotation: one sprite UV unit per model pixel, aligned with world
  block axes. They do not use stripped-log trim bindings or whole-face masonry artwork. The old
  masonry textures remain available to resource packs. Items are baked from the mask with the oak
  item model's transforms. The world model is empty, so no mining crack shows.

The round ring source geometry and quarter identity live in `geometry/RingGeometry`;
`client/render/RingModels` and `RingModelNames` serve both the frame and door attachment.
Their historical model identifiers and source depths remain unchanged.

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
  (overflow dropped). Teardown: the door's pre-teardown hook in `AssemblyRemoval.onRemove` first withdraws the payload from the anchor
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

Thirteen plain profiles share `WindowPaneBlock`: the round pane plus oriented upper/lower arches,
bottom/top corners and four edges. Thirteen retired crossed blocks retain their original state
encodings. One `roundwood:round_window` entity type serves these and the fifteen frozen window
aliases. It stores frame appearance, glass tint and three independent attachment stores.

**Pose and identity.** Round panes retain axis plus position (center or six flush sides); oriented
panes retain twelve orientations and three depths. Both reduce to `WindowPose(front, top, depth)`.
Every pane retains the four (always off, since 0.13.1) horizontal connection properties, waterlogged, trimmed and face_reversed. No beam property
is added. `WindowDesign(profile, pattern)` is a geometry/cache value: `WindowPaneState` derives CROSS
when beams are installed, or while a retired crossed block awaits conversion. Registrations and
advertised items use plain profiles. `WindowFaceIdentity` maps physical sides to stored face trims;
rotations/mirrors recompute face_reversed so trims stay on their original world faces. Whole-pane
beams always address Front and ignore that parity.

**Geometry.** Slabs are two pixels thick at 7..9, 0..2 or 14..16; glazing is inset half a pixel.
The round opening is diameter 12. Arch quarters form the 32/28 ring; lower arches reflect upper
ones. Corners have a two-pixel jamb and sill/head; edges have one two-pixel border. Every piece is
independent. `WindowMuntinGeometry` clips a one-pixel cross at u/v 7.5..8.5 to each opening, at full
slab depth. `WindowPaneGeometry` marks its pieces BEAM separately from FRAME and GLASS, removing
glazing beneath beams. `WindowSurfaceGeometry` emits only exposed surfaces
from the complete composition: opaque reveals remain against glass, glass never draws internally.
`WindowShapes` shares those volumes for collision and combines the in-cell trim/sill bands for
selection. Face attachments are decorative and never add collision.

**Connections and placement.** No pane connects (0.13.1): the geometry is the panel and its slab, so
no glass reaches a neighbour at a corner or junction; `WindowPaneBlock` writes the four connection
properties off on placement and on every neighbour update (the 0.5.0 centred arm, and before it the
0.6.0 posts, are gone). A vanilla pane beside a window still draws its own arm. Fitting beams never
changes these rules. `WindowTargetZones` and `WindowPlacement` remain the Hammer-style placement authority:
center square gives upright/centered, edge trapezoids give the flush pane, with the chosen face/top.
`WindowPlacementPreview` uses the same placement context; the overlay draws the target grid and
result outline. Hidden crossed items instead share `CrossWindowPlacement.Plan`, including the
saved trims/sills and clipped cross in their result outline. The server re-evaluates every proposal.

**Storage.** `RoundWindowBlockEntity` declares `WindowTrim` v2 (perimeter), `WindowSillTrim` v1
(sill) and `WindowCrossBeams` v1 through the shared anchor/store lifecycle. Perimeter and sill use
Front/Back; beams accept only Version and optional Front. Empty Front means oak. Unknown or malformed
beam tags remain unchanged on disk and refuse beam edits; other slots remain editable. Missing
beams mean absent. Updates explicitly clear all stores, and tagless packets clear everything.
The derived trimmed summary means any attachment in any slot; reconcile only while all stores are
understood. Disk always writes explicit-empty WindowTrim v2, even for a beam-only or sill-only pane,
so the 0.7 omitted-oak interpretation cannot invent perimeter trims. Legacy WindowTrim v1 or absent
payload with trimmed=true still materializes two oak/legacy-look trims before migration.

**Targeting and payment.** `WindowHitTarget` reads effective geometry. Installed perimeter trim
wins, then installed sill, BEAM, FRAME, GLASS/CONNECTOR_GLASS. Sills cover beam ends. Donors and
shears act on the selected attachment's slot; on frame/glass the prior rules remain (planks change
frame, dye tints glass or paints frame, masonry refuses the frame). `WindowUseRules` is the pure
table, `WindowInteraction` the router, `HeldTrimTool` the sole hand classifier. Main hand wins;
offhand requires an empty main hand. Sneaking changes nothing. `WindowBeamFitPlan` is shared by
server fitting and `WindowBeamPreview`: loaded editable pane, permitted player/hand, valid hit and
absent beams. Occupied/refused fits preview nothing and charge nothing. Survival fit uses one item;
shears refund one styled item and take one wear. Creative and unchanged restyles are free.

**Appearance and rendering.** Frame and tint are independent of beams, perimeter and sills. Beams
use TRIM-family wood, paint and four masonry materials. `TrimLooks.beams` retains window plank/paint
art and uses existing granular masonry edge art. `WindowAppearanceModel` retains the plain generated
model/retexture/culling path. Fitted and pending-crossed panes use `WindowBeamModels` to bake the
complete exposed-surface composition through `SurfaceQuadBaker`. They emit general quads only;
`WindowBeamCulling` tests actual neighbour boundary coverage, preserving opaque beam ends against
glazing. Plain boundary occupancy is a subset of fitted occupancy, so unchanged state-only plain
culling stays conservative. `WindowPaneState` publishes immutable neighbour geometry snapshots on
the level thread; chunk workers never read block entities. Missing, unknown and unloaded neighbours
retain faces. Load/unload, attachment updates and state changes refresh self and adjacent model data,
including across chunk boundaries. Bounded caches include geometry and all material inputs and clear
on resource reload. Hidden crossed item icons compose their beams and cache by source/frame/glass/beam.
`WindowTrimRenderer` explicitly draws PERIMETER and SILL only; neither draws whole-pane beams twice.

**Face attachments.** Perimeter bands follow each profile's outer border; only upper arches accept
sills. Each stands one pixel proud, even outside a flush pane's cell, and is drawn by the block-entity
renderer without collision. Selection includes only the in-cell portion; the wall-side band is
targeted through the slab beneath it. `WindowTrimGeometry`/`WindowTrimLayout` explicitly reject
CROSS_BEAMS. Faces retain their own independent looks and shears refunds.

**Loot, glass and viewers.** Canonical/alias panes drop their plain profile with frame look and tint,
one perimeter item per stored face, two sill pools on upper arches, and exactly one Front-addressed
beam pool. Pick-block includes only pane/frame/tint. Pending retired blocks instead return one
hidden stack with full contents, never separate refunds. `WindowGlass` v1 lives on entities and
pane stacks; absent means clear, tint covers glazing and connectors only. Viewers use frame plus
glass subtype keys; the twenty advertised defaults include thirteen profiles and Cross Beams, while
all retired crossed items and frozen aliases stay hidden. Generated world meshes contain only the
112 plain pane models; compatibility blockstates reference them and runtime composition supplies beams.

## Face-trim attachments (0.9.0)

Three attachment slots use the same store and transaction across every supported host.

- **Slot** (`block/attachment/TrimSlot`): `PERIMETER` (the Universal Trim, item `round_trim`) and
  `SILL` (Arch Sill, `arch_sill_trim`, upper arch windows), plus `CROSS_BEAMS` (`cross_beams`,
  all windows). PERIMETER/SILL have FACE scope; CROSS_BEAMS has WHOLE_HOST scope and one Front
  storage address. Every fit/remove/install/restyle/target normalizes through `slot.address`. A slot names the canonical item
  a removed or dropped attachment becomes, and is the loot condition's `slot`.
- **Storage** (`storage/FaceTrimStore`): one host's FRONT/BACK pair in one slot plus custody of an unknown
  payload. The host supplies a `Format` (key, version, whether an empty pair is written: windows
  yes, doors no) and, at load, a `Legacy` reader (the window's 0.7.0 adapter; doors have none).
  The current version decodes through `FaceTrimsNbt`; anything not understood reads as no trims,
  is kept and saved back byte-for-byte, and `set` refuses until a build that understands it loads
  it. Update packets always carry the payload, explicit empty while an unknown one is held. One
  store per slot, so slots never see each other's payload. The anchor entities declare their stores
  through `block/attachment/AppearanceAnchorBlockEntity` (see Shared assembly services).
- **Transaction** (`host/TrimFitting`, on a `host/TrimHost`): fit (empty face takes the stack's look, one item
  unless creative; occupied is a free no-op), remove (shears: the slot's item in that look back or
  dropped, one wear), install (no player), restyle and a per-face `AppearanceActions.Target`. A host
  adapter (`DoorTrimActions`, `ArchwayTrimActions`, `WindowTrimActions`) does only its own lookup and
  checks (complete, loaded, permission over every cell) and hands over a live view:
  `TrimHost.anchored(anchor, slot, origin)`, or `TrimHost.of(...)` for the window's summary
  bookkeeping. The origin is the clicked cell.
- **Items** (`block/attachment/item`): `TrimAttachmentItem` names a slot; hosts recognise trims
  through `TrimAttachmentItem.slotOf`, never a class or id. The three routers classify the acting hand
  once through `interaction/HeldTrimTool`: an attachment with its slot, shears (class or `forge:shears`),
  a TRIM donor with its gesture, or other. Each router keeps its own claim policy. `UniversalTrimItem` is registered as the
  advertised `round_trim` (it keeps its block association with the compatibility frame, never places
  one, and claims the frame's item mapping) and, hidden, as every older id: the 19 0.4.0 trim aliases
  (historical looks) and the 0.8.0 `window_trim` (`migration/LegacyContent`). Its own use does
  nothing: the host router fits, so a click is never fitted twice.
- **Face rule.** `AttachmentFace.at(depth, middle)` (a tie is FRONT) and `AttachmentFace.PROUD` (one
  pixel) are the one authority for the door (middle 14.5) and archway (middle 8) targeting and every
  host's casing depth. Windows keep their pose-aware face identity.
- **Loot.** `roundwood:trim_face` takes an optional `slot` (omitted = perimeter, which is how it is
  written, so 0.8.0 tables are unchanged). `AttachmentLootPools` iterates the slot's stored addresses (two face pools or one singleton);
  windows and doors drop `round_trim`.
- **Icon** (`client/item/TrimItemModels`, was `UniversalTrimModels`): every TRIM-family item (all ids)
  wraps its slot's generated icon (`data/client/trim/AttachmentItemModelProvider`: the square frame
  `item/round_trim`, 16 px, 2 px band, 2 px deep; the sill bar `item/arch_sill_trim`, 16 x 4 px;
  whole-face UVs over 16) and retextures it through the shared resolver `client/material/TrimLooks` (masonry: the square face
  `universal_trim_<m>`, straight courses; the sill's `window_sill_<m>`). `StyledAttachmentItem` is the shared plain item for Cross Beams and `ArchSillTrimItem`,
  implementing `TrimAttachmentItem` and `AppearanceDefaults`; the client wraps
  any `AppearanceDefaults` item, not only block items. `TrimLooks` also serves the frame/door rings and the
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
  it. `DoorAppearance.ANCHORS`, a typed `AssemblyAnchors.Lookup`, finds the anchor entity.
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
  swing transforms, uses the shared turned quarter models (`client/render/RingModels`,
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
block stays registered for frames in older saves. Geometry (`geometry/RingGeometry`, shared with door rings): a ring, outer diameter 32, inner 28, one pixel deep at the back
of its cells (z 0..1, authored facing north), cut into cell-local quarters.

- **Fitted frames** (0.7.0) stood in the cells in front of a closed door, the ring one pixel proud of
  the leaf. Migration turns them into the door's FRONT trim (see Migration). `TrimPlacement` no
  longer reads doors; it keeps the checks below for `placeAt`.
- **Checks** (`TrimPlacement`, the shared player-less skeleton with the frame's policy): loaded, inside build height and border, editable, replaceable, dry,
  both lower quarters on a sturdy floor. No entity check (nothing collides). Door cells are not
  replaceable, so a frame never overwrites a door. `placeAt` does the same without a player.
- **Behaviour**: empty collision and support shapes, pathfindable, no occlusion, not a redstone
  conductor, `PushReaction.BLOCK`; only the anchor quarter has a block entity (the look). The selection shape is only the quarter's own
  ring piece, so rays pass through the centre and the corners to the door behind.
- **Lifecycle**: the door's pattern, through the shared `block/assembly` services: `AssemblyRemoval`
  clears loaded partners with drops suppressed (one item from the initiating quarter),
  `AssemblyIntegrity` removes an incomplete frame a tick later (deferred while a cell is unloaded),
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

There are 18 canonical blocks and 20 advertised items, including 13 plain windows and the three
TRIM-family attachment items. The 13 retired crossed products and 49 frozen aliases remain hidden.

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
  the same placement transaction (ordinary windows via `updateCustomBlockEntityTag`; hidden crossed items use `CrossWindowPlacement`), so a cancelled placement
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
- **Viewers**: each canonical item is listed once, as the default (oak) stack the creative tab gives
  it (no cycling previews since 0.12.0). `client/compat/emi` hides the aliases and compares styled
  stacks by appearance key; `client/compat/jei` gives subtypes by key and hides the aliases. Both are discovered by annotation, so nothing loads without
  the viewer.

## Migration (0.4.0 aliases, 0.7.0 trims and retired crossed windows)

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

**Retired crossed windows (0.13.0).** `CrossWindowCompatibility` retains all thirteen evidenced IDs;
`LegacyAppearances` stays frozen. Scheduled adapters and the bounded chunk queue discover them
without loading chunks. `WindowCrossConversion` and the older `LegacyWindowConversion` both use
`WindowCellConversion`: capture full disk data and state, materialize historical defaults, add an
absent beam in the frame look or keep an existing valid singleton, then install inside the guarded
mutation batch. Unknown beams refuse crossed conversion. Validate the write, destination entity and
restored contents before publication; failure restores source state/data and only removes the new
capture snapshots. Pose, water, connections, face identity, frame/glass, trims/sills and unknown
stores survive. No migration drops. Pending harvest carries full data in one hidden stack. The window-owned
`pane/RetiredCrossWindows` port delegates conversion and pending drops to the migration handler
registered by `Roundwood` bootstrap; window code never imports migration. Hidden
items retain inventory identity; their shared placement/preview plan installs all contents before
payment, with Forge cancellation and rollback preserved.

**0.7.0 fitted frames.** `migration/conversion/DoorTrimConversion` converts a fitted frame into the door's
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
variant. Window: pane-only visible-surface models per design and pose (trims are
not in them; see Window; no connection selects a model), in one-model-per-state blockstates; 112 plain window block models (141 block
models in all), 26 window item models. Textures:
`window_glass_clear`, `window_glass_<dye>` (sixteen), `trim_radial_thin_<m>`,
`window_trim_round_<m>`, `window_trim_arch_<m>`, `universal_trim_<m>` (0.9.0); `window_trim_face`
and `_edge` are still generated but unused. The Universal Trim, Arch Sill and Cross Beams item models are the attachment icons
(`data/client/trim/AttachmentItemModelProvider`), and `item/window_trim` parents the Universal Trim.
Archway: `data/client/archway/ArchwayModelProvider` writes the two world models and
`ArchwayItemModelProvider` the two inventory models, cut per cell from the frame mask by `PixelSlab`
through `GeometryModelWriter.addSurfaces` (the same surfaces `client/render/PixelSlabModels` bakes for
a styled stack).
Frame (`data/client/trim/TrimModelProvider`): quarter templates in two families (tiled,
whole-image), oak source quarters per family, one empty world model and a single `""` variant. `LegacyModelProvider` gives the 49 aliases and 13 retired crossed blocks blockstates
over the canonical models and item models parenting the canonical items. Items carry front GUI light
and computed centred transforms. Plus 20 recipes and their advancements, loot (aliases drop the
canonical item; windows and doors add one trim pool per face), tags and English names.

Fitted-top geometry uses a BACK door and a top anchored at y+1 (arched, round, double) or y+2 (tall double). Full tops retain their front mask; each mask pixel retreats behind conservative interval bounds of both hinges and all hardware, with room for a one-pixel rear casing. Exposed faces subtract neighboring depth intervals. Thin MIDDLE/FRONT slabs must pass the same clearance predicate; BACK is incompatible. The door anchor has an independent versioned `DoorArch` store; missing means absent, explicit empty updates clear clients, and unknown payloads remain opaque and refuse edits. Arched doors now default the existing depth property to FRONT.
