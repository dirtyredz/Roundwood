# Roundwood

Forge 1.20.1 mod: round 2x2 wooden doors, round windows and round frames (wooden or radial brick),
hobbit-hole style, in every vanilla wood and four painted finishes. Install on **both** the server and every client. Generic: not tied
to any modpack.

## Supported versions

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x (built and tested on 47.4.10) |
| Dependencies | none |

## Content

**Round door** (one item places a 2 wide x 2 high assembly). One round leaf on one hinge, like a
hobbit door: vertical boards running the full height of the door, brass straps on the hinge side
and a prominent brass knob standing out from the middle of each face. There is no built-in ring
around the leaf any more (0.4.0): add a **round trim** if you want one. It swings 90 degrees away
from you with a short animation; the open leaf lies along the hinge-side wall and leaves 29/16 of
clear width. Looking at any part outlines the whole door as it is: the leaf (open or closed) plus
any filled camouflage corners. The knobs and straps are decoration and do not collide.

- Placement: the cell you target becomes the **lower-left** part (seen from where you stand); the
  door extends one block to your right and one up. All four cells must be empty or replaceable,
  dry (no water), free of mobs, inside the world, and both columns need a solid floor. If anything
  is in the way nothing is placed and the item is kept.
- Hinge: chosen like a vanilla door, judged at the door's two outer edges. Next to another round
  door of the same finish it hinges on the far side (a pair opens from the middle); otherwise the
  side with more solid blocks beside it; otherwise the half of the targeted block you aimed at
  (aim at its left half for a left hinge, right half for a right hinge).
- Use: clicking any part (including the open leaf's part against the hinge wall, from either side),
  a villager, or any mod calling `setOpen` moves the whole door. Redstone on any of the four cells
  opens it; removing the last signal closes it. Between power changes you can still open and close
  it by hand.
- The open leaf's outer half sticks out one block in front of the hinge column. It is only drawn:
  it does not collide, cannot be clicked, and can overlap whatever is there.
- Camouflage: the four square corners around the round leaf can take one block's look. Right-click
  a corner holding a full, ordinary block (stone, planks, bricks...; not glass, slabs, chests or
  light sources) to fill all four; right-click with another block to swap (you get the old one
  back); sneak-right-click a filled corner with both hands empty to clear it. Survival uses one
  block, returned when you clear, swap or break the door; creative uses none. An offhand block works
  when the main hand is empty. The corners are only decoration: they do not collide, block light or
  change redstone, and the opening is as wide as before.
- Breaking any part removes the whole door and drops **one** door (none in creative), plus the
  stored camouflage block. Losing the
  floor under either column breaks it the same way. Pistons cannot move it.
- It is a normal wooden door to the game: villagers and pathfinding use it, it is in
  `minecraft:doors` / `minecraft:wooden_doors`, it burns as fuel, an axe mines it fastest.
- Upgrading from 0.1.0: break placed 0.1.0 doors first and place them again after the update.

**Round window** (single block). A thin square wooden pane, wood in all four corners, with round
tinted glass in the middle. It faces the player who places it and connects like a glass pane:
windows, panes, bars and walls in line simply meet it; a pane, bars, a wall or a crossing window
against its front or back face gets a glass arm. A solid block in front of or behind it does not
(0.4.0). It can be waterlogged and bucketed, and drops itself without Silk Touch.

**Round trim** (one item places a 2 x 2 frame). A flat ring, the width of the door's edge, open in
the middle. It never collides, blocks light, holds redstone or stops mobs; only the ring itself can
be clicked, so the door (or whatever) behind it stays usable through the opening.

- Fit it to a door: **sneak-right-click the front of a closed round door** with the trim. It goes in
  the four cells just in front of the door, lined up with it, one pixel proud of the leaf, covering
  the leaf's edge (camouflaged corners stay visible). The back face, the sides and an open door are
  refused and nothing is used up. A plain right-click still opens the door.
- Freestanding: anywhere else the cell you target becomes the lower-left quarter, like a door; it
  frames any 2 x 2 opening. Both lower cells need a solid floor; all four cells must be empty or
  replaceable and dry.
- Breaking any quarter removes the frame and drops one trim; the door it was fitted to stays (and
  breaking the door leaves the frame). Brick trims need a pickaxe, like bricks. Pistons cannot move it.
- 19 materials: the eleven woods (in their stripped log) and the four paints (in the painted
  boards), plus brick, stone brick, deepslate brick and mud brick with the bricks laid radially
  round the circle, a brick centred at the crown.

**Finishes** (49 items): doors and windows in oak, spruce, birch, jungle, acacia, dark oak,
mangrove, cherry, bamboo, crimson, warped, plus green, yellow, red and blue painted (on oak); trims
in those 15 plus the four bricks. Door leaves use Roundwood's own board artwork in each wood's
colours (one image across the whole leaf, so the boards never restart); windows use the planks.
Upgrading from 0.3.x: doors keep working and simply get the new look. Windows that already show an
arm towards a solid block keep it until a neighbour changes; re-place them to clear it.

## Showcase command

`/roundwood showcase` (permission level 2: ops, or single-player with cheats) builds a labelled test
layout on the ground ahead of you, centred on the way you face (62 x 8 x 72 blocks, starting
three blocks ahead). It first clears that whole box to air and lays a stone floor, so use it on
open ground; it refuses if the box would leave the build limits, the world border or loaded chunks,
and never touches blocks outside it. The layout: every door finish closed and open on both
hinges (labelled with signs), doors with stone-brick, plank, grass and wool corner camouflage, every
window finish in one connected row, a junction with a vanilla glass pane, a stacked pair on
different axes, a waterlogged window, a window with stone right behind it (no arm), a lever beside a
door, every trim fitted round a blue door, a camouflaged oak door and an open red door (57
combinations, each labelled), and the four brick trims standing free so both faces can be seen.

## Recipes

| Result | Recipe |
|---|---|
| Round door (natural wood) | 8 matching planks around 1 gold nugget |
| Round window (natural wood) | 4 matching planks around 1 glass pane (plus shape) |
| Painted door / window | oak or another painted door/window + the dye (shapeless) |
| Round trim (wood or brick) | 4 matching planks, or 4 brick blocks, in the four corners of the grid |
| Painted trim | oak or another painted trim + the dye (shapeless) |

Everything is ordinary shaped/shapeless crafting, so EMI and JEI show it with no integration.
All items are in the **Roundwood** creative tab.

## Build, datagen, test

```
./gradlew runData             # regenerate src/generated/resources (checked in)
./gradlew test build          # unit tests + reobfuscated release jar
./gradlew runGameTestServer   # headless Forge server GameTests
git diff --check
```

The release jar is `build/libs/roundwood-1.20.1-<version>.jar`. `node tools/generate-textures.mjs`
and `node tools/generate-test-structures.mjs` rebuild the original textures (door boards, radial
bricks, window glass and the five 0.1.0 textures) and the GameTest templates (both deterministic). `-PpackModsDir=<mods folder>` adds Double Doors, Collective and
Dramatic Doors from a pack instance to the GameTest runtime for compatibility runs.

## Release

Not released yet; 0.4.0 is a local candidate. The flow follows DankRecipeTransfer: an immutable
GitHub prerelease asset, then a direct-URL `mods/roundwood.pw.toml` (with SHA-256,
`side = "both"`) in the pack repo, rehashed with
that repo's tools. Both steps need the owner's go-ahead.

See [STRUCTURE.md](STRUCTURE.md) for the code map and [docs/](docs/) for architecture, decisions
and gotchas.

## License

MIT. Textures under `src/main/resources` are original; vanilla textures are referenced, never copied.
