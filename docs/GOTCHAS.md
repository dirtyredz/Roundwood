# Gotchas - Roundwood

- **Interactive targets take the click before placement.** Aiming a window item at a door, chest or
  lever uses that block first; the overlay still shows the zone, so it is not a promise.
- **Flush and floor/ceiling windows never connect** (0.6.1). Their slab does not reach the middle
  of the cell edge where vanilla panes meet, and they grow no glass to get there, so all four
  connection properties stay false. A vanilla pane beside one still connects (vanilla links to any
  pane-family block) and ends at the cell edge, short of the flush slab. Only centred upright
  windows grow arms, across their plane; there are no up/down links.
- **0.5.0 windows load centred.** `position` defaults to center when absent, so saved windows keep
  their axis, connections, water and look. Aliases inherit the property.
- **Planks and dye on a window change the frame; dye on glass tints.** Planks and frame paint leave
  the glass alone; dye aimed at the glass or a connector arm tints the whole window's glass. Aim
  decides (trim band, frame or beam, glass), and no gesture needs sneaking.
- **Every window blockstate applies one complete model** (pane-only visible surfaces, with a model
  per centred arm combination; there are no `_trimmed` models, trims are drawn by a block-entity
  renderer). After any pose or geometry change rerun
  `runData`; `GeneratedWindowPaneModelsTest` compares every state's model with the block geometry
  and fails on a stale file.
- **Internal translucent faces cause seams.** Glass faces drawn inside or against other glass show
  as horizontal lines. Never add window geometry outside `WindowPaneGeometry` and
  `WindowSurfaceGeometry` (the models are emitted from those two), and rerun `runData` after any
  geometry change.
- **Window clicks are routed by hit region** (trim band, frame, glass), through
  `WindowHitRegions`/`WindowUseRules`. A trim band counts only on a face that has a trim; the cross
  beams are frame, never glass or trim. Masonry works only on a trim band: elsewhere it is refused
  with a hint and uses nothing.
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
- **Camouflage copies looks only.** Donor sounds, hardness, light, redstone, gravity, connected
  textures and overlays (grass side tint layer) do not transfer; one sprite per face.
- **Mirrored structure copies start without camouflage**: mirroring moves the old anchor's NBT onto a
  non-anchor cell, which has no entity. Rotations keep it. Structure copies duplicate a stored
  refund (structure tools are creative tools).
- **The open leaf passes through the hinge-side corners' edge.** At its hinge the open leaf crosses
  a sliver of the static corner region; when camouflaged the two overlap visually there.
- **The Door Trim fits by a plain click on either face, and an open door only by aiming at the
  ring.** Holding it, the door never toggles, camouflages or places anything; a face that already
  has a trim is a free no-op. On a closed door any hit inside the ring's outer edge fits the face
  chosen by depth against the leaf's middle (z 14.5). The held Door Trim adds both rings to the
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
  legacy content and drop a Door Trim in their look.
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
- **Villager walk GameTests are intermittent**, more so with the pack mods (Dramatic Doors replaces
  villager core brain tasks): one walk failed in 1 of 3 plain runs and in 1 of 2 pack runs on 0.2.0,
  and also failed on 0.1.0 with the pack mods. A rerun passes; do not read a single failure as a
  regression without a second run.
- **MineColonies (1.1.452) opens non-vanilla doors with `setOpen`** and treats any `DoorBlock` as
  passable for citizens. Our `setOpen` moves the whole assembly. Not in the GameTest runtime.
- **Vanilla villagers sometimes leave doors open.** `InteractWithDoor` forgets a door it has moved
  too far past. The walking GameTests assert opening and passage; the close path is tested by
  calling `InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough` directly.
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
  `blockstates` before `runData`; `GeneratedResourcesTest` lists the exact model manifest.
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
- **Doors are always dry.** Placement refuses any fluid in the four cells.
- **Client checks belong to the owner.** Models, item display transforms, the swing animation,
  sounds, second-client sync and EMI display are verified in game, never by agents.
