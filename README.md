# Roundwood

Forge 1.20.1 mod: round 2x2 wooden doors and round windows, hobbit-hole style, in every vanilla
wood and four painted finishes. Install on **both** the server and every client. Generic: not tied
to any modpack.

## Supported versions

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x (built and tested on 47.4.10) |
| Dependencies | none |

## Content

**Round door** (one item places a 2 wide x 2 high assembly). One round leaf on one hinge, like a
hobbit door, with a brass knob in the middle of each face. It swings 90 degrees away from you
with a short animation; the open leaf lies along the hinge-side wall and leaves 29/16 of clear
width. Looking at any part outlines the whole door.

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
glass in the middle. It faces the player who places it and connects like a glass pane: windows,
panes, bars and walls in line simply meet it; one across the window's plane gets a glass arm. It can
be waterlogged and bucketed, and drops itself without Silk Touch.

**Finishes** (15 per family, 30 items): oak, spruce, birch, jungle, acacia, dark oak, mangrove,
cherry, bamboo, crimson, warped, plus green, yellow, red and blue painted (on oak). Natural doors
use the wood's planks with a stripped-log trim; painted doors use painted panels with oak trim.

## Showcase command

`/roundwood showcase` (permission level 2: ops, or single-player with cheats) builds a labelled test
layout on the ground ahead of you, centred on the way you face (about 46 x 8 x 36 blocks, starting
three blocks ahead). It first clears that whole box to air and lays a stone floor, so use it on
open ground; it refuses if the box would leave the build limits, the world border or loaded chunks,
and never touches blocks outside it. The layout: every door finish closed and open on both
hinges (labelled with signs), doors with stone-brick, plank, grass and wool corner camouflage, every
window finish in one connected row, a junction with a vanilla glass pane, a stacked pair on
different axes, a waterlogged window, and a lever beside a door.

## Recipes

| Result | Recipe |
|---|---|
| Round door (natural wood) | 8 matching planks around 1 gold nugget |
| Round window (natural wood) | 4 matching planks around 1 glass pane (plus shape) |
| Painted door / window | oak or another painted door/window + the dye (shapeless) |

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
and `node tools/generate-test-structures.mjs` rebuild the five original textures and the GameTest
templates (both deterministic). `-PpackModsDir=<mods folder>` adds Double Doors, Collective and
Dramatic Doors from a pack instance to the GameTest runtime for compatibility runs.

## Release

Not released yet; 0.3.1 is a local candidate. The flow follows DankRecipeTransfer: an immutable
GitHub prerelease asset, then a direct-URL `mods/roundwood.pw.toml` (with SHA-256,
`side = "both"`) in the pack repo, rehashed with
that repo's tools. Both steps need the owner's go-ahead.

See [STRUCTURE.md](STRUCTURE.md) for the code map and [docs/](docs/) for architecture, decisions
and gotchas.

## License

MIT. Textures under `src/main/resources` are original; vanilla textures are referenced, never copied.
