# STRUCTURE — Roundwood

<!-- Last full review: never -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/variant/` — finish descriptors and the one catalogue of finishes
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration: blocks, items, creative tab, mod tags
- `src/main/java/io/github/dirtyredz/roundwood/door/` — the door block, its item, and the assembly services
- `src/main/java/io/github/dirtyredz/roundwood/window/` — the window block and its geometry
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, voxel-shape conversion (shared)
- `src/main/java/io/github/dirtyredz/roundwood/data/` — datagen provider registration
- `src/main/java/io/github/dirtyredz/roundwood/data/client/` — blockstate/model/language providers
- `src/main/java/io/github/dirtyredz/roundwood/data/server/` — recipe, loot and tag providers
- `src/main/resources/` — mods.toml, pack.mcmeta, the five original textures
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored and excluded)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests (geometry, generated-resource validation)
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests (`fixture/`, `door/`, `window/`, `data/`); never in the release jar
- `src/gameTest/resources/` — empty GameTest structure templates
- `tools/` — deterministic generators for the textures and GameTest structures (Node, not packaged)
- `docs/` — architecture, decisions, gotchas

## Components

| Component | Files | Responsibility |
|---|---|---|
| Catalogue | `variant/Finish`, `FinishCatalog` | The 15 finishes; everything else iterates this list |
| Registry | `registry/ModBlocks`, `ModItems`, `ModCreativeTab`, `ModTags` | One door + one window per finish; tab; family tag keys |
| Door block | `door/RoundDoorBlock`, `RoundDoorItem` | `DoorBlock` / `BlockItem` subclasses that only delegate |
| Layout | `door/DoorLayout` | Anchor and four part positions from facing/hinge/half; membership checks |
| Placement | `door/DoorPlacement` | Four-cell preflight, then one write per cell |
| State | `door/DoorStateController` | Click, `setOpen`, redstone: whole-assembly, idempotent, one sound |
| Removal | `door/DoorRemoval` | Teardown from `onRemove`; drop ownership by ordering |
| Batch | `door/DoorMutationBatch` | Marks cells Roundwood is rewriting; bounded nesting |
| Door geometry | `door/DoorGeometry` | The 32-pixel disc split into four cells, closed and open |
| Window | `window/RoundWindowBlock`, `WindowGeometry` | Pane-hierarchy block; rim, glass, connection pieces |
| Geometry | `geometry/PixelCircle`, `Box`, `Piece`, `VoxelShapeFactory` | Rasterize, describe, rotate, convert |
| Datagen | `data/**` | Models from the same geometry; recipes, loot, tags, names |

## Dependency direction

`registry` -> `door`, `window` -> `geometry`; `data` -> everything above. `door` and `window` never
import each other. Only `geometry/VoxelShapeFactory` turns geometry into Minecraft shapes; the rest
of `geometry` and `DoorGeometry`'s piece lists are plain Java, so the unit tests need no bootstrap.

## Structural notes

- Door services are static classes with one job each, called from `RoundDoorBlock`; the block class
  holds no logic, so callbacks stay readable and the services are individually testable.
- `RoundwoodLootProvider` is a factory, not a provider subclass: vanilla's `LootTableProvider`
  already does the work.
- No compat package: the pack's door mods needed no adapter (see docs/GOTCHAS.md).
