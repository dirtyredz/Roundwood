# STRUCTURE — Roundwood

<!-- Last full review: never -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/variant/` — finish descriptors and the one catalogue of finishes
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration: blocks, block entity, items, creative tab, mod tags
- `src/main/java/io/github/dirtyredz/roundwood/door/` — the door block, its item and its anchor block entity (Minecraft adapters only)
- `src/main/java/io/github/dirtyredz/roundwood/door/assembly/` — the four-part assembly: layout, hinge choice, placement, state, removal, integrity
- `src/main/java/io/github/dirtyredz/roundwood/door/leaf/` — the single leaf: geometry, collision shapes, swing timeline
- `src/main/java/io/github/dirtyredz/roundwood/door/selection/` — whole-assembly outline geometry and shapes, hit re-attribution
- `src/main/java/io/github/dirtyredz/roundwood/door/camouflage/` — corner geometry, accepted materials, stored data, interaction, refunds
- `src/main/java/io/github/dirtyredz/roundwood/window/` — the window block (a pane), its geometry and shapes
- `src/main/java/io/github/dirtyredz/roundwood/showcase/` — the `/roundwood showcase` command, bounded builder, coordinate frame, canvas and section registry
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/` — one class per showcase section (doors, camouflage, windows, redstone)
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, face UVs, voxel-shape conversion (shared)
- `src/main/java/io/github/dirtyredz/roundwood/client/` — client-only mod-bus wiring (`RoundwoodClient`)
- `src/main/java/io/github/dirtyredz/roundwood/client/door/` — the door leaf renderer, leaf model names, targeting events, break/hit particles
- `src/main/java/io/github/dirtyredz/roundwood/client/camouflage/` — static corner renderer, donor face materials, cached corner models
- `src/main/java/io/github/dirtyredz/roundwood/data/` — datagen provider registration
- `src/main/java/io/github/dirtyredz/roundwood/data/client/` — blockstate/model/item-model/language providers
- `src/main/java/io/github/dirtyredz/roundwood/data/server/` — recipe, loot and tag providers
- `src/main/resources/` — mods.toml, pack.mcmeta, the five original textures
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored and excluded)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests (`geometry/`, `door/leaf/`, `door/selection/`, `door/camouflage/`, `window/`, `showcase/`, `data/`)
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests (`fixture/`, `door/`, `door/selection/`, `door/camouflage/`, `window/`, `showcase/`, `data/`); never in the release jar
- `src/gameTest/resources/` — empty GameTest structure templates
- `tools/` — deterministic generators for the textures and GameTest structures (Node, not packaged)
- `docs/` — architecture, decisions, gotchas

## Components

| Component | Files | Responsibility |
|---|---|---|
| Catalogue | `variant/Finish`, `FinishCatalog` | The 15 finishes; everything else iterates this list |
| Registry | `registry/ModBlocks`, `ModBlockEntities`, `ModItems`, `ModCreativeTab`, `ModTags` | One door + one window per finish; the anchor entity type; tab; family tag keys |
| Door adapters | `door/RoundDoorBlock`, `RoundDoorItem`, `RoundDoorBlockEntity` | `DoorBlock` / `BlockItem` / `BlockEntity` subclasses that only delegate (entity: swing state, render bounds, camouflage NBT + update packet) |
| Layout | `door/assembly/DoorLayout`, `DoorColumn` | Anchor and part positions from facing/column/half; shared hinge; membership checks |
| Hinge choice | `door/assembly/DoorHingePlacement` | Vanilla hinge rules judged at the assembly's two outer edges |
| Placement | `door/assembly/DoorPlacement` | Four-cell preflight, then one write per cell |
| State | `door/assembly/DoorStateController` | Click, `setOpen`, redstone, external `open` writes: whole-assembly, idempotent, one sound |
| Removal / integrity | `door/assembly/DoorRemoval`, `DoorIntegrity` | Teardown from `onRemove`; deferred cleanup of incomplete assemblies |
| Batch | `door/assembly/DoorMutationBatch` | Marks cells Roundwood is rewriting; bounded nesting |
| Leaf | `door/leaf/DoorGeometry`, `DoorShapes`, `DoorSwing` | One 32-pixel leaf, its quarter turn, clipped per-cell **collision**, eased client swing |
| Selection | `door/selection/DoorOutlineGeometry`, `DoorSelectionShapes`, `DoorHitResolver` | Whole-door outline from every part (plus corners when targetable); hits credited to the containing part, projection hits refused |
| Camouflage | `door/camouflage/CamouflageGeometry`, `CamouflageMaterial`, `CamouflageData`, `CamouflageInteraction`, `CamouflageDrops` | Static corner geometry and hit classification; donor rules; NBT record; apply/replace/clear; exactly-once refunds and creative discard scope |
| Door rendering | `client/RoundwoodClient`, `client/door/RoundDoorRenderer`, `DoorLeafModels`, `DoorParticles` | BER drawing the baked leaf model from the anchor; leaf model registration; per-cell break/hit particles |
| Door targeting | `client/door/DoorTargetingEvents` | Re-attributes or cancels the crosshair hit before use/attack/pick (Forge bus) |
| Camouflage rendering | `client/camouflage/CamouflageRenderer`, `CamouflageMaterials`, `CamouflageModels` | Per-cell corner models tessellated with world light/AO/tint; caches dropped on model reload |
| Window | `window/RoundWindowBlock`, `WindowGeometry`, `WindowShapes` | Thin pane (`IronBarsBlock`): square wooden face, round glazing, glass arms across the plane; waterloggable |
| Showcase | `showcase/ShowcaseCommand`, `ShowcaseBuilder`, `ShowcaseFrame`, `ShowcaseCanvas`, `ShowcaseSection(s)`, `section/*` | Op-only command; bounded clear + floor + registered sections, built through `DoorPlacement.placeAt` and real block states; a new feature adds a section |
| Geometry | `geometry/PixelCircle`, `Box`, `Piece`, `FaceUv`, `VoxelShapeFactory` | Rasterize, describe, rotate, block-like face UVs, convert |
| Datagen | `data/**` | Models from the same geometry (tile-split, explicit UVs); items; recipes, loot, tags, names |

## Dependency direction

`showcase` -> `door`, `window`, `registry`, `variant` (nothing imports it but the bootstrap). `registry` -> `door`, `window` -> `geometry`; `client` -> `door`, `registry`; `data` -> everything
above. `door` and `window` never import each other, and `door` does not import `registry` (the
block receives its entity type as a supplier). Inside `door`: the adapters call `assembly`,
`leaf`, `selection` and `camouflage`; `selection` reads `leaf` and `camouflage/CamouflageGeometry`;
`camouflage` reads `assembly`, `leaf` and the anchor entity; `assembly/DoorRemoval` makes one call
into `camouflage/CamouflageDrops` (release before teardown). One deliberate exception to
"door never imports client": `RoundDoorBlock.initializeClient` hands Forge `client/door/DoorParticles`
(only ever invoked on the client). Shapes are made by `geometry/VoxelShapeFactory` via
`leaf/DoorShapes`, `selection/DoorSelectionShapes` and `window/WindowShapes`; `DoorGeometry`,
`DoorOutlineGeometry`, `CamouflageGeometry`, `DoorSwing` and `WindowGeometry` are plain Java, so the
unit tests need no bootstrap.

## Structural notes

- Door services are static classes with one job each, called from `RoundDoorBlock`; the block class
  holds no logic, so callbacks stay readable and the services are individually testable.
- `door/` was split into `assembly/` and `leaf/` in 0.2.0 when it passed the 12-file fan-out cap;
  0.3.0 added `selection/` (outline vs collision) and `camouflage/` rather than growing `leaf/` or
  the adapters.
- `client/door/DoorLeafModels` is pure naming shared by the client and the model generator.
- `RoundwoodLootProvider` is a factory, not a provider subclass: vanilla's `LootTableProvider`
  already does the work.
- No compat package: the pack's door mods needed no adapter (see docs/GOTCHAS.md).
