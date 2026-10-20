# Gotchas - Roundwood

- **Double Doors (pack, 7.2) links doors of the same block.** It mixes into `DoorBlock.use` (we
  never call `super.use`, so player clicks are untouched), listens to buttons, levers and pressure
  plates (`NeighborNotifyEvent`) and to villager door use, then sets `open` with `setBlock` on every
  same-block door in a 3x3x3 hop chain (up to 10 blocks). Inside one assembly that is a no-op. Two
  assemblies of the **same finish** placed touching are linked by it for levers/buttons/plates and
  villagers (its intended double-door behaviour); different finishes are not. No adapter was
  needed, so there is no `compat/` package.
- **Dramatic Doors (pack, 2.3.7_1) makes every `DoorBlock` waterloggable** through a mixin, so in
  the pack our door gains a `waterlogged` property. Placement still refuses wet cells, blockstates
  omit `waterlogged` so models resolve, and teardown restores any fluid in a cleared cell. Never use
  `registerDefaultState(stateDefinition.any())` in `RoundDoorBlock`.
- **MineColonies (1.1.452) opens non-vanilla doors with `setOpen`** and treats any `DoorBlock` as
  passable for citizens. Our `setOpen` moves the whole assembly.
- **Vanilla villagers sometimes leave doors open.** `InteractWithDoor` forgets a door it has moved
  too far past. The walking GameTests assert opening and passage; the close path is tested by
  calling `InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough` directly.
- **`GameTestHelper.relativePos` is not the inverse of `absolutePos` in 1.20.1.** Use
  `DoorFixtures.rel`. `getBounds` is private and `killAllEntities` throws, so discard entities
  yourself.
- **Armor stands do not block placement**; use a mob in entity-obstruction tests.
- **Generated resources are tracked, the datagen cache is not.** `src/generated/resources/.cache/`
  is ignored and excluded from the jar. `runData` is deterministic: two runs give identical bytes.
- **Doors are always dry.** Placement refuses any fluid in the four cells.
- **Perpendicular pane neighbours form a junction** (a glass arm into the window plane), like two
  panes meeting. Restricting connections to the window's plane would need cooperation from the
  neighbouring vanilla panes.
- **Client checks belong to the owner.** Models, item display transforms, sounds, second-client
  sync and EMI display are verified in Evil Eye Local, never by agents.
