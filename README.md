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
hobbit door. It swings 90 degrees away from you with a short animation; the open leaf lies along
the hinge-side wall and leaves 29/16 of clear width.

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
- Breaking any part removes the whole door and drops **one** door (none in creative). Losing the
  floor under either column breaks it the same way. Pistons cannot move it.
- It is a normal wooden door to the game: villagers and pathfinding use it, it is in
  `minecraft:doors` / `minecraft:wooden_doors`, it burns as fuel, an axe mines it fastest.
- Upgrading from 0.1.0: break placed 0.1.0 doors first and place them again after the update.

**Round window** (single block). A full wooden block, wood in all four corners, with a round glazed
opening through it. It faces the player who places it, builds like any solid block (full collision;
panes and walls meet it), can be waterlogged and bucketed, and drops itself without Silk Touch.

**Finishes** (15 per family, 30 items): oak, spruce, birch, jungle, acacia, dark oak, mangrove,
cherry, bamboo, crimson, warped, plus green, yellow, red and blue painted (on oak). Natural doors
use the wood's planks with a stripped-log trim; painted doors use painted panels with oak trim.

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

Not released yet; 0.2.0 is a local candidate. The flow follows DankRecipeTransfer: an immutable
GitHub prerelease asset, then a direct-URL `mods/roundwood.pw.toml` (with SHA-256,
`side = "both"`) in the pack repo, rehashed with
that repo's tools. Both steps need the owner's go-ahead.

See [STRUCTURE.md](STRUCTURE.md) for the code map and [docs/](docs/) for architecture, decisions
and gotchas.

## License

MIT. Textures under `src/main/resources` are original; vanilla textures are referenced, never copied.
