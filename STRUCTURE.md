# STRUCTURE — Roundwood

<!-- Last full review: never -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/variant/` — finish and frame-material descriptors and the one catalogue of both
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration: blocks, block entity, items, creative tab, mod tags
- `src/main/java/io/github/dirtyredz/roundwood/block/` — the three block families and what they share (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/` — services shared by the multi-cell families: the mutation batch, partner teardown
- `src/main/java/io/github/dirtyredz/roundwood/block/door/` — the door block, its item and its anchor block entity (Minecraft adapters only)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/assembly/` — the four-part door: layout, hinge choice, placement, state, removal, integrity
- `src/main/java/io/github/dirtyredz/roundwood/block/door/leaf/` — the single leaf: geometry and hardware, collision shapes, swing timeline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/selection/` — target and visible whole-door shapes, hit re-attribution
- `src/main/java/io/github/dirtyredz/roundwood/block/door/camouflage/` — corner geometry, accepted materials, stored data, interaction, refunds
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/` — the frame block and its whole-frame item (Minecraft adapters only)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/assembly/` — the four-quarter frame: part identity, layout, placement (fitted / freestanding), removal, integrity
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/geometry/` — the ring (plain Java) and its cached per-quarter selection shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/window/` — the window block (a pane), its geometry, shapes and across-plane connection rule
- `src/main/java/io/github/dirtyredz/roundwood/showcase/` — the `/roundwood showcase` command, bounded builder, coordinate frame, canvas and section registry
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/` — one class per showcase section (doors, camouflage, windows, redstone, trim combinations, brick arches)
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, tiled and whole-assembly face UVs, voxel-shape conversion (shared)
- `src/main/java/io/github/dirtyredz/roundwood/client/` — client-only mod-bus wiring (`RoundwoodClient`)
- `src/main/java/io/github/dirtyredz/roundwood/client/door/` — the door leaf renderer, leaf model names, targeting events, highlight, break/hit particles
- `src/main/java/io/github/dirtyredz/roundwood/client/camouflage/` — static corner renderer, donor face materials, cached corner models
- `src/main/java/io/github/dirtyredz/roundwood/data/` — datagen provider registration
- `src/main/java/io/github/dirtyredz/roundwood/data/client/` — blockstate/model providers per family, item-model providers per family, shared display transforms, language
- `src/main/java/io/github/dirtyredz/roundwood/data/server/` — recipe, loot and tag providers
- `src/main/resources/` — mods.toml, pack.mcmeta, the generated original textures (checked in)
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored and excluded)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests (`geometry/`, `block/door/leaf/`, `block/door/selection/`, `block/door/camouflage/`, `block/trim/geometry/`, `block/window/`, `data/`)
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests (`fixture/`, `block/door/`, `block/door/selection/`, `block/door/camouflage/`, `block/trim/`, `block/window/`, `showcase/`, `data/`); never in the release jar
- `src/gameTest/resources/` — empty GameTest structure templates
- `tools/` — entry points of the deterministic generators for the textures and GameTest structures (Node, not packaged)
- `tools/textures/` — texture artwork modules: PNG encoding, noise, the 0.1.0 textures, palettes, door boards, radial bricks, window glass
- `docs/` — architecture, decisions, gotchas

## Components

| Component | Files | Responsibility |
|---|---|---|
| Catalogue | `variant/Finish`, `TrimMaterial`, `FinishCatalog` | The 15 finishes and 19 frame materials (15 derived from the finishes + 4 masonry); everything else iterates these lists |
| Registry | `registry/ModBlocks`, `ModBlockEntities`, `ModItems`, `ModCreativeTab`, `ModTags` | One door + one window per finish, one frame per material; the anchor entity type; tab (doors, windows, frames); family tag keys |
| Shared assembly | `block/assembly/AssemblyMutationBatch`, `AssemblyTeardown` | Marks cells Roundwood is rewriting (bounded nesting); clears loaded partners silently then notifies, so one item drops |
| Door adapters | `block/door/RoundDoorBlock`, `RoundDoorItem`, `RoundDoorBlockEntity` | `DoorBlock` / `BlockItem` / `BlockEntity` subclasses that only delegate (entity: swing state, render bounds, camouflage NBT + update packet) |
| Door layout | `block/door/assembly/DoorLayout`, `DoorColumn` | Anchor and part positions from facing/column/half; shared hinge; membership checks; render sweep bounds |
| Hinge choice | `block/door/assembly/DoorHingePlacement` | Vanilla hinge rules judged at the assembly's two outer edges |
| Door placement / state | `block/door/assembly/DoorPlacement`, `DoorStateController` | Four-cell preflight, then one write per cell; click, `setOpen`, redstone, external `open` writes |
| Door removal / integrity | `block/door/assembly/DoorRemoval`, `DoorIntegrity` | Camouflage release then shared teardown from `onRemove`; deferred cleanup of incomplete assemblies |
| Leaf | `block/door/leaf/DoorGeometry`, `DoorShapes`, `DoorSwing` | One 32-pixel board panel with proud brass hardware, its quarter turn, clipped per-cell **collision**, eased client swing |
| Selection | `block/door/selection/DoorOutlineGeometry`, `DoorSelectionShapes`, `DoorHitResolver` | Whole-door **target** shape (`getShape`, corners when targetable) and **visible** shape (filled corners only); hits credited to the containing part, projection hits refused |
| Camouflage | `block/door/camouflage/*` | Static corner geometry and hit classification; donor rules; NBT record; apply/replace/clear; exactly-once refunds |
| Frame adapters | `block/trim/RoundTrimBlock`, `RoundTrimItem` | Static decorative block (facing + part, no collision/occlusion/entity) and the whole-frame item; both only delegate |
| Frame assembly | `block/trim/assembly/TrimPart`, `TrimLayout`, `TrimPlacement`, `TrimRemoval`, `TrimIntegrity` | Quarter identity, membership, fitted/freestanding placement (+ `placeAt`), teardown via the shared service, deferred integrity |
| Frame geometry | `block/trim/geometry/TrimGeometry`, `TrimShapes` | The 32/28 ring one pixel deep, cell-local quarters; per-quarter selection shapes |
| Door rendering | `client/RoundwoodClient`, `client/door/RoundDoorRenderer`, `DoorLeafModels`, `DoorParticles` | BER drawing the baked leaf model from the anchor; leaf model registration; per-cell break/hit particles |
| Door targeting | `client/door/DoorTargetingEvents`, `DoorHighlightRenderer` | Re-attributes or cancels the crosshair hit before use/attack/pick; replaces the vanilla highlight with the visible shape (Forge bus) |
| Camouflage rendering | `client/camouflage/*` | Per-cell corner models tessellated with world light/AO/tint; caches dropped on model reload |
| Window | `block/window/RoundWindowBlock`, `WindowGeometry`, `WindowShapes`, `WindowConnections` | Thin pane (`IronBarsBlock`): square wooden face, round translucent glazing, glass arms across the plane only for genuine pane junctions; waterloggable |
| Showcase | `showcase/*`, `showcase/section/*` | Op-only command; bounded clear + floor + registered sections, built through `DoorPlacement.placeAt`, `TrimPlacement.placeAt` and real block states |
| Geometry | `geometry/PixelCircle`, `Box`, `Piece`, `FaceUv`, `AssemblyFaceUv`, `VoxelShapeFactory` | Rasterize, describe, rotate, tiled and whole-assembly face UVs, convert |
| Datagen | `data/**` | Models from the same geometry (`GeometryModelWriter`: tile-split, per-face style), per-family block and item model providers, `ItemModelTransforms`; recipes, loot, tags, names |
| Texture tools | `tools/generate-textures.mjs`, `tools/textures/*` | Orchestrator plus one module per concern; palettes keyed by catalogue ids |

## Dependency direction

`showcase` -> `block/*`, `registry`, `variant` (nothing imports it but the bootstrap). `registry` ->
`block/*` -> `geometry`; `client` -> `block/door`, `registry`; `data` -> everything above.
`block/door`, `block/window` and `block/trim` never import each other, with one deliberate
exception: `block/trim/assembly/TrimPlacement` reads a clicked door (`RoundDoorBlock`,
`DoorLayout`) to fit a frame to it. Both multi-cell families use `block/assembly`; it imports
neither. `door` does not import `registry` (the block receives its entity type as a supplier).
Inside `block/door`: the adapters call `assembly`, `leaf`, `selection` and `camouflage`;
`assembly/DoorLayout` reads `leaf/DoorGeometry` for the sweep margin; `selection` reads `leaf` and
`camouflage/CamouflageGeometry`; `camouflage` reads `assembly`, `leaf` and the anchor entity;
`assembly/DoorRemoval` makes one call into `camouflage/CamouflageDrops` (release before teardown).
One exception to "door never imports client": `RoundDoorBlock.initializeClient` hands Forge
`client/door/DoorParticles` (only ever invoked on the client). `DoorGeometry`,
`DoorOutlineGeometry`, `CamouflageGeometry`, `DoorSwing`, `WindowGeometry` and `TrimGeometry` are
plain Java, so the unit tests need no bootstrap.

## Structural notes

- Door and frame services are static classes with one job each, called from thin block classes,
  so callbacks stay readable and the services are individually testable.
- 0.4.0 grouped `door/`, `window/`, the new `trim/` and the shared `assembly/` under `block/`, so
  the root package stays readable (8 entries) instead of growing two more siblings.
- Door and frame keep separate layout and placement policies: their four-cell traversal is
  similar, but a shared framework would couple persistent door identity, redstone and block
  entities to a static decoration. Only the mutation batch and the teardown are shared.
- The 0.3.x combined item-model provider was split per family (`DoorItemModelProvider`,
  `WindowItemModelProvider`, `TrimItemModelProvider`) with the transform maths in
  `ItemModelTransforms`, before the third family arrived.
- `client/door/DoorLeafModels` is pure naming shared by the client and the model generator.
- `RoundwoodLootProvider` is a factory, not a provider subclass: vanilla's `LootTableProvider`
  already does the work.
- No compat package: the pack's door mods needed no adapter (see docs/GOTCHAS.md).
