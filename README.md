# Roundwood

Forge 1.20.1 mod: a round 2x2 wooden door, a round window and a round 2x2 trim, hobbit-hole style.
Each is one item you restyle in the world: any planks (vanilla or modded) set the wood, any of the
16 dyes paints it, and trims can also be faced with radial brick. Install on **both** the server
and every client. Generic: not tied to any modpack.

## Supported versions

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x (built and tested on 47.4.10) |
| Dependencies | none (EMI and JEI optional, client side) |

## Content

Three items: **Round Door**, **Round Window**, **Round Trim**. They are crafted (and found in the
creative tab) in oak; their look is changed once placed and travels with them when picked or broken.

**Round door** (one item places a 2 wide x 2 high assembly). One round leaf on one hinge, like a
hobbit door: vertical boards running the full height of the door, brass straps on the hinge side
and a prominent brass knob standing out from the middle of each face. It swings 90 degrees away
from you with a short animation; the open leaf lies along the hinge-side wall and leaves 29/16 of
clear width. Looking at any part outlines the whole door as it is: the leaf (open or closed) plus
any filled camouflage corners. The knobs and straps are decoration and do not collide.

- Placement: the cell you target becomes the **lower-left** part (seen from where you stand); the
  door extends one block to your right and one up. All four cells must be empty or replaceable,
  dry, free of mobs, inside the world, and both columns need a solid floor. If anything is in the
  way nothing is placed and the item is kept.
- Hinge: chosen like a vanilla door, judged at the door's two outer edges. Next to another round
  door it hinges on the far side (a pair opens from the middle); otherwise the side with more solid
  blocks beside it; otherwise the half of the targeted block you aimed at.
- Use: clicking any part, a villager, or any mod calling `setOpen` moves the whole door. Redstone on
  any of the four cells opens it; removing the last signal closes it.
- The open leaf's outer half sticks out one block in front of the hinge column. It is only drawn:
  it does not collide, cannot be clicked, and can overlap whatever is there.
- Camouflage: the four square corners around the round leaf can take one block's look. Right-click
  a corner holding a full, ordinary block (stone, planks, bricks...) to fill all four; another block
  swaps it (you get the old one back); sneak-right-click a filled corner with both hands empty to
  clear it. Survival uses one block, returned when you clear, swap or break the door.
- Breaking any part removes the whole door and drops **one** door in its look (none in creative),
  plus the stored camouflage block. Pistons cannot move it. To the game it is a normal wooden door
  (villagers, pathfinding, `minecraft:doors`, fuel, axe).

**Round window** (single block). A thin square wooden pane with round tinted glass in the middle,
facing the player who places it, connecting like a glass pane (an arm only towards a real pane
junction across its face, never towards a solid block). Waterloggable; drops itself.

**Round trim** (one item places a 2 x 2 frame). A flat ring, open in the middle, purely decorative.
Fit it to a door by **sneak-right-clicking the front of a closed round door** with it (it goes in
the four cells in front, one pixel proud of the leaf); anywhere else it stands free, the target cell
being its lower-left quarter. Breaking any quarter drops one trim; door and trim never remove each
other.

## Restyling

| Aim at | Holding | Result | Cost |
|---|---|---|---|
| Door leaf | **Sneak** + planks | That wood; any paint removed | 1 plank |
| Door leaf | Dye (sneaking or not) | Painted, the wood kept underneath | 1 dye |
| Door corner | Block, not sneaking (planks included) | Camouflage, as above | 1 block, refunded later |
| Door corner | Sneak + planks or dye | Refused with a hint to aim at the leaf | nothing |
| Window | **Sneak** + planks / dye | The wooden face only; glass and arms never change | 1 plank / dye |
| Trim | **Sneak** + planks | That wood (also replaces masonry) | 1 plank |
| Trim | **Sneak** + bricks, stone bricks, deepslate bricks or mud bricks | Radial brickwork, paint cleared | 1 block |
| Wooden trim | Dye | Painted | 1 dye |
| Brick trim | Dye | Refused (masonry is not painted) | nothing |

- Nothing is refunded when you restyle; using the same planks or dye again does nothing and costs
  nothing; re-planking a painted object with its own wood strips the paint (one plank). Creative
  costs nothing. A plain click (or a plain click with planks) still opens the door; sneak + a trim
  item on a door still fits a trim; buckets still work on windows.
- The offhand works only when the main hand is empty.
- **Planks** are any item tagged `minecraft:planks`, or added by a datapack to
  `roundwood:appearance_planks`. Modded woods show their own plank texture on windows and trims and
  Roundwood's full-height boards tinted to that wood's colour on doors. A wood whose mod is later
  removed shows as oak and comes back when the mod returns.
- The look is cosmetic: every door, window and trim behaves like the oak one (an axe mines all of
  them, nether woods do not change fire behaviour, brick trims do not need a pickaxe).

## Showcase command

`/roundwood showcase` (permission level 2: ops, or single-player with cheats) builds a labelled test
layout on the ground ahead of you, centred on the way you face (78 x 8 x 82 blocks, starting three
blocks ahead). It first clears that whole box to air and lays a stone floor, so use it on open
ground; it refuses if the box would leave the build limits, the world border or loaded chunks, and
never touches blocks outside it. The layout: every vanilla wood (and up to eight installed modded
planks) as doors closed and open on both hinges and as one connected window row; camouflaged doors;
window junction, stacked, waterlogged and stone-behind demos; a lever-driven door; every paint on a
door, a trim and a window; every wood and masonry trim fitted round a blue, a camouflaged oak and an
open red door; and the four brick trims standing free.

## Recipes

| Result | Recipe |
|---|---|
| Round door | 8 planks (any, mixed allowed) around 1 gold nugget |
| Round window | 4 planks around 1 glass pane (plus shape) |
| Round trim | 4 planks in the four corners of the grid |

Crafting always gives the oak object; restyle it in the world. With **EMI** each item is one index
entry whose icon cycles through example looks (the tooltip says it is a preview; taking or
bookmarking it gives the plain oak item); styled stacks in your inventory are told apart by their
look. With **JEI** styled stacks are separate subtypes and the three plain items are listed. Neither
viewer is required.

## Upgrading from 0.4.0

The 49 old door, window and trim ids still load. Placed doors, windows and trims (fitted or
freestanding) turn into the new blocks shortly after their chunk loads, keeping their wood or paint,
facing, hinge, open state, water and camouflage (with its refund); nothing drops. Old items in chests
keep their names and looks and place the new blocks. Mirrored structure copies keep the 0.4.0
limitation (camouflage is lost when mirroring).

## Build, datagen, test

```
node tools/generate-textures.mjs        # original textures (deterministic)
node tools/generate-test-structures.mjs # GameTest templates incl. the 0.4.0 fixture
./gradlew runData                       # regenerate src/generated/resources (checked in)
./gradlew test build                    # unit tests + reobfuscated release jar
./gradlew runGameTestServer             # headless Forge server GameTests (fresh world each run)
git diff --check
```

`-PwithViewers=emi|jei|jei,emi` puts the optional viewers on the dev runtime; `-PpackModsDir=<mods
folder>` adds Double Doors, Collective and Dramatic Doors from a pack instance for compatibility runs.
The release jar is `build/libs/roundwood-1.20.1-<version>.jar`.

## Release

Not released yet; 0.5.0 is a local candidate. The flow follows DankRecipeTransfer: an immutable
GitHub prerelease asset, then a direct-URL `mods/roundwood.pw.toml` (with SHA-256, `side = "both"`)
in the pack repo. Both steps need the owner's go-ahead.

See [STRUCTURE.md](STRUCTURE.md) for the code map and [docs/](docs/) for architecture, decisions
and gotchas.

## License

MIT. Textures under `src/main/resources` are original; vanilla textures are referenced, never copied.
