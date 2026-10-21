# Gotchas - Roundwood

- **Clearing camouflage needs both hands empty.** Forge never calls block use for a sneaking
  player holding anything, so sneak + empty hands on a filled corner is the only clear gesture;
  sneaking with a block item places that block as vanilla would.
- **Empty corners are only targetable with a block item in hand.** The outline adds them while the
  looking player holds a block item (either hand), so placing blocks "through" an empty corner
  hits the door instead; filled corners are always targetable.
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
  reconciliation makes any such single-part write move the whole assembly, silently. Two
  assemblies of the **same finish** placed touching are linked by it for levers/buttons/plates and
  villagers (its intended behaviour); different finishes are not. No adapter, so no `compat/`.
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
