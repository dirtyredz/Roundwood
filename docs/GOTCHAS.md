# Gotchas - Roundwood

- **A window packet without a tag clears its trims.** The shared anchor reads a tagless packet as
  every store explicitly empty. Read as "missing" instead, a window whose state says `trimmed=true`
  would get oak trims on both faces from its 0.7.0 legacy reading. `AnchorPersistenceGameTests` guards
  it.
- **Door camouflage differs on disk and in updates.** Saves keep the refund, updates never carry it,
  and an update carries an explicit empty compound once it is cleared. `takeCamouflage` only marks
  the entity dirty. Loading never touches `DoorSwing`.
- **Assembly targets announce at the clicked cell.** `AssemblyAppearanceTarget` and every `TrimHost`
  use the cell that was used as the origin, never the anchor. Sounds and game events come from there.
- **Anchor identity is not part membership.** `AssemblyAnchors` accepts any block of the family class
  whose reconstructed layout matches, so canonical and 0.4.0 alias cells share an anchor. Integrity
  and teardown use the exact `isPart` (same block). `DoorSelectionShapes` keeps its own looser anchor
  read. Don't swap one for another.
- **An installation rollback owns only its own snapshots.** Inside Forge's capture (a player
  placement), `AssemblyInstallation` drops just the block snapshots it added and leaves capture on.
  Clearing the outer list or switching capture off would break the enclosing transaction.
  `AssemblyPlacementGameTests` guards it.

- **Archway looks use donor blocks, not trim artwork (0.13.3).** A stripped-log sprite can look
  smeared on a thin soffit strip even with correct UVs. Bodies, casings and items tile the same
  donor sprite on all six faces; project UVs after facing rotation so vertical plank rows stay
  horizontal. `PixelSlabModelsTest` checks baked vertex orientation and spans, accounting for
  FaceBakery's small anti-bleed inset.
- **Archway upper corners are filled but do not collide (0.13.2).** Keep the square frame mask
  shared by body, casing, item and preview; keep collision lower-jamb-only to preserve 0.13.1
  headroom. Do not add separate overlapping corner panels.
- **An archway's loot holds two trim-family items.** The archway and its Universal Trims are both
  TRIM family, so `AppearanceDrops.decorate(drops, family, look)` would stamp the archway's look on
  the trims; the archway decorates by item. `ArchwayTrimGameTests` guards it.
- **Restyling an archway with a casing: aim at the passage side.** On a face with a casing, planks,
  masonry and dye restyle the casing (there is no other way to reach it); the archway itself takes
  them on its other surfaces. A face without a casing restyles the archway.
- **Archway models are baked on the client, cached by design, layer, cell, facing and look.** The
  generated `archway` / `keyhole_archway` block models are empty (particle only); the item JSON is the
  oak icon whose transforms the styled icons copy. A geometry change needs no `runData` for the world
  look, but does for the oak item model.

- **Interactive targets take the click before placement.** Aiming a window item at a door, chest or
  lever uses that block first; the overlay still shows the zone, so it is not a promise.
- **No window connects** (0.13.1; flush and floor/ceiling windows since 0.6.1). The geometry is the
  slab and nothing else, so all four connection properties stay false (`WindowPaneBlock` clears them on
  placement and every neighbour update). A vanilla pane beside one still connects (vanilla links to any
  pane-family block) and draws its own arm to the cell edge, short of the window. Never reintroduce
  geometry that depends on a neighbour: `WindowPaneGeometry` takes no connection set on purpose.
- **0.5.0 windows load centred.** `position` defaults to center when absent, so saved windows keep
  their axis, connections, water and look. Aliases inherit the property.
- **Planks and dye on a window change the frame; dye on glass tints.** Planks and frame paint leave
  the glass alone; dye aimed at the glass tints the whole window's glass. Aim
  decides (trim band, frame or beam, glass), and no gesture needs sneaking.
- **Every window blockstate applies one complete model** (pane-only visible surfaces, one model
  per pose; there are no `_arms_*` or `_trimmed` models, trims are drawn by a block-entity
  renderer). After any pose or geometry change rerun
  `runData`; `GeneratedWindowPaneModelsTest` compares every state's model with the block geometry
  and fails on a stale file.
- **Internal translucent faces cause seams.** Glass faces drawn inside or against other glass show
  as horizontal lines. Never add window geometry outside `WindowPaneGeometry` and
  `WindowSurfaceGeometry` (the models are emitted from those two), and rerun `runData` after any
  geometry change.
- **Window clicks use effective geometry.** Installed perimeter trim and sill win first, then
  BEAM, frame and glass. Beam donors and shears affect only the singleton attachment. Never route
  beams as FRAME or let a new slot fall through a sill/perimeter switch. The fit preview and server
  both use `WindowBeamFitPlan`; plain clicks and sneak clicks are identical.
- **Unknown beams retain custody.** `WindowCrossBeams` v1 accepts only Version and an optional
  Front record. Back, malformed appearances, non-compounds and future fields/versions refuse edits;
  disk saves retain the raw tag. Both physical sides normalize to Front, including loot addresses.
- **Window migration needs disk data.** `getUpdateTag()` deliberately hides unknown stores.
  Use `saveWithFullMetadata()` and `WindowCellConversion`, including for the older plain aliases.
  Check writes and destination entities before publishing; rollback owns only its own snapshots.
  Keep all 13 retired crossed IDs registered and never add invented IDs to frozen history.
- **Fitted models cannot read live neighbours on chunk workers.** `WindowPaneState` publishes
  immutable render snapshots on the level thread. Missing, unloaded and unknown neighbours retain
  faces. Refresh self and adjacent panes after attachment/state/load changes, including chunk edges.

- **Every window trim stands one pixel proud; the wall-side band of a flush, floor or ceiling pane
  is outside the cell.** It is render-only (`WindowTrimRenderer`): never collision, support or
  connection. Selection includes only the in-cell part, and a flush wall-side band is targeted
  through the slab face under it. Normal depth testing can hide it inside a solid neighbour, but it
  is never culled by one. (0.7.0 inlaid it, which read as no trim.)
- **A missing `trimmed` means untrimmed.** 0.6.x saves and old chunks load with no trim; never
  default it to true. The 0.7.0 payload is the exception: `trimmed=true` with no `WindowTrim`
  payload is the omitted-oak case and reads as an oak trim on both faces.
- **`face_reversed` parity.** A 180 degree turn of a centred round window keeps its pose but
  toggles the bit, so each trim stays on its face; mirrors toggle it too. It is derived by
  `WindowPaneBlock.keepFaces` and selects no model: never set it by hand.
- **Projected UVs must go through `FaceUv.tiled`.** Trim edges reach -1 and 17, outside the sprite;
  folding them in by hand samples neighbouring atlas sprites.
- **Loading never writes `trimmed`.** An out-of-step summary is corrected by a scheduled block tick
  (`RoundWindowBlockEntity.onLoad` to `WindowPaneBlock.tick` to `WindowTrimActions.reconcile`). An
  unknown `WindowTrim`/`DoorTrim` version reads as no trims, is kept and saved back, and trim edits
  are refused.
- **Pane and trims drop separately; pick-block gives the pane only.** One trim item drops per
  trimmed face. Count all items in drop tests, and use the per-face loot pools
  (`AttachmentLootPools`, condition `roundwood:trim_face`) when changing window or door loot. The
  pane keeps its frame look and glass in both drops and pick-block.
- **Window tests that sweep geometry are slow if assertion messages are built eagerly.** Check
  inside an `if` and call `fail(...)` there instead of passing a built string to `assertEquals`.
- **A junction face partly against wood is still drawn** (harmless, opposite-facing); a wholly
  buried one is not.
- **The strip where an arch's glazed edge reaches the cell edge keeps the 0.5 px glass recess.**
  Nothing fills it.
- **Sneak-use reaches Roundwood blocks only for what the block's router claims.** Forge never calls
  block use for a sneaking player holding anything; `AppearanceUseEvents` asks the block
  (`AppearanceUseTarget.claimsUse`, answered by `WindowInteraction.claims` and
  `DoorInteraction.claims`) and, for a claimed gesture, lets block use through and denies the item
  use, so a plank is never placed against a door. Windows and door trims treat sneaking like not
  sneaking; the door leaf and freestanding frames still need sneak + planks or masonry. Every other
  sneak-use is vanilla: sneak + a block item places it, and clearing camouflage still needs both
  hands empty.
- **Restyling a door needs the leaf.** Sneak + planks or dye on a corner is refused with an
  action-bar hint (it would otherwise be ambiguous with camouflage); plain planks on a corner are
  camouflage.
- **The look is cosmetic.** Every door, window and trim has oak's properties: brick trims mine with
  an axe and need no pickaxe, nether-wood looks still burn as oak would.
- **Same-block linking is broader since 0.5.0.** All round doors are one block, so vanilla hinge
  pairing and Double Doors join doors of different looks. No appearance-aware patch.
- **Shared look resolution must retain resource-pack policy.** `AppearanceLooks` lets a built-in
  mapping decline authored artwork: windows and beams still read the donor plank sprite, while doors
  and perimeter trims use their panel/log bindings. Do not replace windows with hard-coded vanilla
  sprite names during a resolver refactor.
- **Unfamiliar woods on doors are a tint.** Doors in a non-vanilla wood show Roundwood's neutral
  full-height boards tinted with the donor plank's average colour, not the donor's pixels.
  Windows and trims show the donor's real plank texture (its north face; connected or layered
  textures are not reproduced).
- **Appearance caches are dropped on every model bake** (F3+T, resource packs): donor sprites,
  sampled colours, leaf/quarter/window/item models together. A donor whose texture changes is
  re-sampled then.
- **Trims are drawn by their anchor's renderer** since 0.5.0: like doors, no mining crack shows,
  break particles are oak, and an incomplete frame draws nothing.
- **Alias ids stay registered.** The 49 0.4.0 ids exist as hidden blocks and items so old chunks and
  inventories load; they are in no tab, recipe or viewer. Never delete them while worlds may hold
  0.4.0 content; old items in containers are not renamed (they place canonical blocks).
- **Structure templates in GameTests sit one block up and lose their scheduled ticks**: the framework
  clears block ticks in the area after placing, and places a template at the
  helper's origin plus (0, 1, 0), so template y 1 is `helper` y 2 (`legacy_v040` tests).
- **GameTest drop counts must be look-specific.** Every family is one item now, so tests that count
  drops use a look no other test drops (`DoorFixtures.dropped(helper, item, appearance)`).
- **Empty corners are only targetable with a block item in hand.** The target shape (`getShape`)
  adds them while the looking player holds a block item (either hand), so placing blocks "through"
  an empty corner hits the door instead; filled corners are always targetable. The highlight never
  shows empty corners (it draws the visible shape, `DoorHighlightRenderer`), so the aim can land on
  an outline-less corner. Code that needs "what is drawn" must use `DoorSelectionShapes.visible`,
  never `getShape`.
- **The highlight follows the swing, the target does not.** While a leaf turns, the outline is the
  leaf at its drawn angle (client `DoorSwing`), but rays and collision already use the final
  blockstate, so a click mid-swing lands on the final pose.
- **The open projection is outlined but refused.** Clicking, mining or picking the projected half
  of an open leaf does nothing (the client cancels it). A ray that crosses a door cell before the
  projection can also stop a click on a block that sits inside the projection.
- **Per-cell rendering has distinct seed and cull policies.** The compatibility frame culls;
  camouflage, archways and door casings do not. A projected door ring uses the door cell
  for its seed, the projected cell for lighting, and a separate depth shift. Preserve these inputs
  to `AssemblyCellRenderer`; the shared draw operation must not erase them.
- **Camouflage copies looks only.** Donor sounds, hardness, light, redstone, gravity, connected
  textures and overlays (grass side tint layer) do not transfer; one sprite per face.
- **Mirrored structure copies start without camouflage**: mirroring moves the old anchor's NBT onto a
  non-anchor cell, which has no entity. Rotations keep it. Structure copies duplicate a stored
  refund (structure tools are creative tools).
- **The open leaf passes through the hinge-side corners' edge.** At its hinge the open leaf crosses
  a sliver of the static corner region; when camouflaged the two overlap visually there.
- **A trim fits a door by a plain click on either face, and an open door only by aiming at the
  ring.** Holding any trim id, the door never toggles, camouflages or places anything; a face that
  already has a trim is a free no-op. On a closed door any hit inside the ring's outer edge fits the
  face chosen by depth against the leaf's middle (z 14.5). A held trim adds both rings to the
  target shape so an open door can be aimed at. The old 2 x 2 frame block is only a compatibility
  block now; nothing places it.
- **Trims and door hardware are decorative.** Nothing collides with the rings or the knobs and
  straps; mobs and players walk through the ring's area and the knob pokes two pixels into the
  cell in front of the door. The front ring is drawn one pixel into the cells in front of the door
  and the back ring overlaps the hinge-side brass straps where they cross it (cosmetic). Door
  trims are static and never swing with the leaf.
- **Fitted-frame migration is conservative.** An occupied FRONT keeps the old frame (even in the
  same look); a frame converts only when exactly matched to a complete door (same facing, anchor
  offset by the opposite facing, all eight cells loaded). A freestanding frame placed exactly where
  a fitted one would be converts too, since the two cannot be told apart. Other frames stay as
  legacy content and drop a Universal Trim in their look. A door holding an unknown `DoorTrim`
  version is not editable, so its fitted frame stays. The conversion, like the 0.4.0 aliases, is
  restricted to `RoundDoorBlock`/`ModBlocks.roundDoor()` (0.11.0): a legacy frame in front of an
  arched door is never converted (`ArchedDoorMigrationGameTests`).
- **Every door cache must key on the design** (0.11.0). Item models, leaf models, collision,
  selection, outline, camouflage and casing caches keyed by look alone would draw one door's shape
  for the other. `DoorLayout.designOf(state)` throws for a non-door state: ask it only of Roundwood
  door parts.
- **Door art stays square** (0.11.0). A non-square texture without `.mcmeta` is read as square
  animation frames, so the arched door samples the left part of the square 32x32 board image (and
  its casing art lives in the left half of a 64x64 image) through `AssemblyFaceUv`'s rectangular
  mapping; never author a 16x32 texture.
- **Old trim stacks stay distinct until used** (0.9.0). `window_trim`, `round_trim` and the 19
  0.4.0 trim aliases are separate item ids, so old stacks do not merge with new Universal Trims in
  an inventory. They are never rewritten (no inventory scan, no id remap); every id fits windows and
  doors, and anything fitted comes back as `round_trim` (shears, loot). Recognise a trim through
  `TrimAttachmentItem.slotOf`, never by class or id, or an old id stops working.
- **Fitting lives in the host router, never the item** (0.9.0). `UniversalTrimItem.useOn` returns
  FAIL; doors and windows fit in their router and claim the sneak gesture there. A fit added to the
  item's own use would run a second time after a router that passes.
- **Trim item icons resolve in Java.** Every TRIM-family item wraps its slot's icon (`item/round_trim`,
  the square frame; `item/arch_sill_trim`, the bar) and `TrimItemModels` retextures it per stack;
  changing only the JSON does not change a styled stack's icon, and a new attachment id must be a
  TRIM-family `AppearanceDefaults` item to get it.
- **One `FaceTrimStore` per slot, never two slots in one payload.** The arch sill (0.10.0,
  `WindowSillTrim` v1) has its own key and version, so an unknown sill payload never blocks the
  perimeter trims (and the other way round). Cross Beams has its own store too. The `trimmed` summary
  counts every slot, so a sill-only or beam-only
  window is `trimmed=true`: it must still write its explicit `WindowTrim` v2 empty (the window format
  always does), or the summary would read as the 0.7.0 omitted-oak case. Never make the perimeter
  format omit empty.
- **Only upper arches take a sill.** `WindowProfile.takesSill` gates the item (refused with a hint),
  the host (`WindowTrimActions` builds none), the geometry and the loot pools (only the two canonical upper-arch
  tables have sill pools). A lower arch's top diameter has no sill until the owner asks for one.
- **Window profiles are appended, never inserted.** `WindowDesign.all()` follows enum order and is
  the creative order and the registration order; GameTests pick designs by index
  (`WindowDesign.all().get(7)`). New profiles go at the end.
- **Lower arches draw the door ring's radial art.** Their masonry is `trim_radial_thin_<m>`, whose
  upper half equals the arched window face pixel for pixel (`WindowShapeArtworkTest`). Change either
  generator and a full masonry circle breaks at its diameter. The sill's art is one 4-texel course at
  texel rows 28..32 of the 2 x 2 image; moving the sill or its height needs new art.
- **GameTest window and door hits must come from real rays** (`fixture/WindowRays`,
  `fixture/DoorRays`), clipped against the real shapes. Constructed hit results skip the targeting
  that these gestures now depend on.
- **Door leaves no longer follow plank resource packs.** Since 0.4.0 the leaf uses Roundwood's own
  generated board art (one 32 x 32 image across the leaf); a resource pack must retexture
  `roundwood:block/door_panel_<wood|dye|neutral>`. Windows and wooden trims use the plank / stripped-log textures.
- **Windows from 0.3.x may keep an arm towards a solid block** in front or behind until a
  neighbour update re-judges it; re-place such a window (or change a neighbour) after upgrading.
- **Windows are translucent since 0.4.0** (visible tinted glass). Translucent sorting with water or
  stained glass behind it, and shader packs, are owner checks.
- **The GameTest world persists between runs** (`run/gametest/world`). Item entities and villagers
  saved by an earlier run load into later tests' areas a few ticks after they start, after
  `clearDrops`, and break drop counts (seen in 0.4.0 as "dropped 2" on integrity tests).
  `runGameTestServer` now deletes that world first (build.gradle); do not remove that.
- **GameTest drop counts reach past a 9-wide template** (`DoorFixtures.dropped` covers 17 x 6 x 9),
  and the showcase test clears its 86 x 86 area silently when done, so later tests on that ground
  are not hit by doors and frames losing their floor.
- **Upgrading a trial world from 0.2.0**: doors keep working (no camouflage means none). Re-place
  existing windows so their pane connections are computed (old states load disconnected).
- **Camouflage models are cached per donor/facing/cell** and dropped on every resource reload
  (F3+T); a donor whose model changes on reload is rebuilt then.
- **Placed 0.1.0 doors do not survive the 0.2.0 upgrade.** In 0.1.0 `hinge` meant the column; 0.2.0
  adds `column` (defaulting to left on load) and treats `hinge` as the physical hinge, so old
  right columns resolve to the wrong anchor and the integrity check breaks them. Break 0.1.0 doors
  before upgrading and re-place them; items and windows are unaffected.
- **Double Doors (pack, 7.2) links doors of the same block.** It mixes into `DoorBlock.use` (we
  never call `super.use`, so player clicks are untouched), listens to buttons, levers and pressure
  plates (`NeighborNotifyEvent`) and to villager door use, then writes `open` with
  `setBlock(..., 10)` on every same-block door in a 3x3x3 hop chain (up to 10 blocks). Our `onPlace`
  reconciliation makes any such single-part write move the whole assembly, silently. Since 0.5.0 every
  round door is the same block, so any two placed touching are linked by it for levers/buttons/plates
  and villagers, whatever their looks. No adapter, so no `compat/`.
- **Dramatic Doors (pack, 2.3.7_1) makes every `DoorBlock` waterloggable** through a mixin, so in
  the pack our door gains a `waterlogged` property. Placement still refuses wet cells, the door
  blockstate is one `""` variant so any injected property resolves, and teardown restores any fluid
  in a cleared cell. Never use `registerDefaultState(stateDefinition.any())` in `RoundDoorBlock`;
  extend `defaultBlockState()`.
- **GameTest defaults generate normal terrain, putting plots in caves.** Falling gravel, flowing
  water/lava and cave mobs can disrupt unrelated tests; this caused Herbal Witch's flakes and is
  the suspected cause of Roundwood's assembly/migration flakes (dk-2abfb2b9, dk-79af4223), pending
  clean ANU runs. A passing rerun alone does not settle them. `gradle/gametest-world.gradle`
  writes superflat, structure-free server settings before launch; keep it byte-identical to Herbal
  Witch. The existing world deletion runs first so the new generator settings take effect.
  Test-only `environment/TestWorldGameTests` disables `doMobSpawning` at server startup and asserts
  flat generation, no structures and no natural spawning. Server spawn properties alone are not
  enough: the GameTest server retains always-on spawn flags. Tests explicitly spawn their own mobs.
- **MineColonies (1.1.452) opens non-vanilla doors with `setOpen`** and treats any `DoorBlock` as
  passable for citizens. Our `setOpen` moves the whole assembly. Not in the GameTest runtime.
- **Vanilla villagers sometimes leave doors open.** `InteractWithDoor` forgets a door it has moved
  too far past. The walking GameTests assert opening and passage; the close path is tested by
  calling `InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough` directly.
- **Villager passage tests need closed corridor ends and an initial destination.** An idle brain
  can choose its own walk target before an `onEachTick` callback supplies one. In an open-ended
  corridor it can leave the raised floor, get stuck outside, or go around the door; a z-only
  arrival check then even reports passage without an opening. `fixture/VillagerPassage` encloses
  the plot and sets `WALK_TARGET` at spawn, keeping normal brain/pathfinding/door behavior.
  Successful walkers are discarded. The flat-world reproduction failed 8/140 walks, including
  a frame-only walk, without any nested-mutation warnings; the bounded, directed fixture passed
  all 140 walks with the same 600-tick budget (`dk-dae6be18`).
- **Sprites only span UV 0..16.** Default UVs come from element coordinates, so any element beyond
  one block samples neighbouring atlas sprites. `GeometryModelWriter` splits at tile boundaries;
  never add elements to a model by another path. Forge's model builder omits a `uv` equal to the
  default, so generated JSON shows `uv` only on faces outside tile 0 (the tests account for this).
- **Textures are generated too.** `node tools/generate-textures.mjs` writes every PNG under
  `src/main/resources/assets/roundwood/textures/block/`; never hand-edit them. Every 0.4.0
  texture must stay byte-identical (`GeneratedTexturesTest` pins their SHA-256): door-board seeds
  follow palette order, so only append to `tools/textures/palettes.mjs`.
- **runData does not delete models nobody generates any more** without its local cache. When a
  model is renamed or removed, delete `src/generated/resources/assets/roundwood/models` and
  `blockstates` before `runData`; `GeneratedResourceManifestTest` lists the exact model manifest.
- **The leaf is drawn by a block-entity renderer.** The door's world model is empty, so the vanilla
  crack overlay does not appear while mining it, and a door whose four parts are not all present
  draws nothing (the integrity check removes it a tick later).
- **The open leaf's outer half is visual only.** It can overlap blocks or entities in front of the
  hinge column; it is not collidable or targetable by design.
- **`GameTestHelper.relativePos` is not the inverse of `absolutePos` in 1.20.1.** Use
  `DoorFixtures.rel`. `getBounds` is private and `killAllEntities` throws, so discard entities
  yourself.
- **Armor stands do not block placement**; use a mob in entity-obstruction tests.
- **Generated resources are tracked, the datagen cache is not.** `src/generated/resources/.cache/`
  is ignored and excluded from the jar. `runData` is deterministic: two runs give identical bytes.
- **Doors are always dry.** Placement refuses any fluid in the door's cells (two or four).
- **The arched door's icon is the closed left-hinge leaf at half a block's scale** (0.11.0), with the
  round door's display transforms; an owner check, not a defect.
- **Client checks belong to the owner.** Models, item display transforms, the swing animation,
  sounds, second-client sync and EMI display are verified in game, never by agents.
- **GameTest hits at an exact tie need binary-exact coordinates.** `WindowFixtures.hit` adds the cell-local point to the absolute block position, so a decimal such as 0.95 picks up float noise that depends on where the test runs; a test pinning an exact diagonal tie (`zoneBoundaries...`) failed once its class moved and the world positions shifted. Use values like 0.875.
