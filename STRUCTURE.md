# STRUCTURE — Roundwood

<!-- Last full review: never -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/appearance/` — the appearance value, family acceptance, defaults contract, versioned NBT, pure restyle rules, bounded samples
- `src/main/java/io/github/dirtyredz/roundwood/appearance/material/` — server-safe donor recognition: tagged planks, the fixed four masonry materials
- `src/main/java/io/github/dirtyredz/roundwood/appearance/item/` — appearance on stacks, the shared family block item (tooltip, alias rules), loot decoration
- `src/main/java/io/github/dirtyredz/roundwood/appearance/interaction/` — gesture recognition, validated server mutation and payment, the narrow sneak-use event bridge
- `src/main/java/io/github/dirtyredz/roundwood/migration/` — frozen 0.4.0 mappings, alias registration, deferred chunk migration, per-family conversions
- `src/main/java/io/github/dirtyredz/roundwood/migration/block/` — the deprecated alias block adapters (historical default, convert before integrity)
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration: the three canonical blocks/items, block entities, creative tab, mod tags
- `src/main/java/io/github/dirtyredz/roundwood/block/` — the three block families and what they share (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/` — services shared by the multi-cell families: the mutation batch, partner teardown, deferred harvest
- `src/main/java/io/github/dirtyredz/roundwood/block/door/` — the door block, its item and its anchor block entity (Minecraft adapters only)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/assembly/` — the four-part door: layout, hinge choice, placement, state, removal, integrity
- `src/main/java/io/github/dirtyredz/roundwood/block/door/appearance/` — reading, targeting and dropping the door's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/door/interaction/` — the door's right-click router: restyle, then camouflage, then toggle
- `src/main/java/io/github/dirtyredz/roundwood/block/door/leaf/` — the single leaf: geometry and hardware, collision shapes, swing timeline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/selection/` — target and visible whole-door shapes, hit re-attribution, corner-or-leaf hit region
- `src/main/java/io/github/dirtyredz/roundwood/block/door/camouflage/` — corner geometry, accepted materials, stored data, interaction, refunds
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/` — the frame block, its item and its anchor block entity (Minecraft adapters only)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/appearance/` — reading, targeting and dropping the frame's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/assembly/` — the four-quarter frame: part identity, layout, placement (fitted / freestanding), removal, integrity
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/geometry/` — the ring (plain Java) and its cached per-quarter selection shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/window/` — the window block (a pane), its entity, item and model data, geometry, shapes and connection rule
- `src/main/java/io/github/dirtyredz/roundwood/showcase/` — the `/roundwood showcase` command, bounded builder, coordinate frame, canvas (samples, appearance placement) and section registry
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/` — one class per showcase section (doors, camouflage, windows, redstone, paints, trim combinations, brick arches)
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, tiled and whole-assembly face UVs, voxel-shape conversion (shared)
- `src/main/java/io/github/dirtyredz/roundwood/client/` — client-only mod-bus wiring (`RoundwoodClient`)
- `src/main/java/io/github/dirtyredz/roundwood/client/door/` — the door leaf renderer, appearance leaf models, leaf model names, targeting events, highlight, particles
- `src/main/java/io/github/dirtyredz/roundwood/client/trim/` — the frame renderer, appearance quarter models, source model names
- `src/main/java/io/github/dirtyredz/roundwood/client/window/` — the appearance-aware window blockstate/item model
- `src/main/java/io/github/dirtyredz/roundwood/client/item/` — NBT-aware item model overrides
- `src/main/java/io/github/dirtyredz/roundwood/client/material/` — donor sprites and colours, built-in wood/paint/masonry artwork bindings, quad retexturing, bounded model caches
- `src/main/java/io/github/dirtyredz/roundwood/client/camouflage/` — static corner renderer, cached corner models
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/emi/` — optional EMI adapter: cycling preview entries, serializer
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/jei/` — optional JEI adapter: subtypes, hidden aliases
- `src/main/java/io/github/dirtyredz/roundwood/data/` — datagen provider registration
- `src/main/java/io/github/dirtyredz/roundwood/data/client/` — blockstate/model providers per family, alias compatibility models, item-model providers, display transforms, language
- `src/main/java/io/github/dirtyredz/roundwood/data/server/` — recipe, loot and tag providers
- `src/main/resources/` — mods.toml, pack.mcmeta, the generated original textures (checked in)
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored and excluded)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests (`testsupport/` bootstrap shim, `appearance/`, `migration/`, `client/material/`, `geometry/`, `block/door/leaf/`, `block/door/selection/`, `block/door/camouflage/`, `block/trim/geometry/`, `block/window/`, `data/`)
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests (`fixture/`, `appearance/`, `migration/`, `block/door/`, `block/door/appearance/`, `block/door/selection/`, `block/door/camouflage/`, `block/trim/`, `block/window/`, `showcase/`, `data/`); never in the release jar
- `src/gameTest/resources/` — GameTest structure templates (empty ones and the 0.4.0 fixture) and a test-only plank tag
- `tools/` — entry points of the deterministic generators for the textures and GameTest structures (Node, not packaged)
- `tools/textures/` — texture artwork modules: PNG encoding, noise, the 0.1.0 textures, palettes, door boards, paint panels, radial bricks, window glass
- `docs/` — architecture, decisions, gotchas

## Components

| Component | Files | Responsibility |
|---|---|---|
| Appearance | `appearance/Appearance`, `AppearanceFamily`, `AppearanceDefaults`, `AppearanceNbt`, `AppearanceChanges`, `AppearanceSamples` | Immutable look (wood/masonry + optional dye), which family takes what, a block's/item's default, the one NBT form, restyle rules, bounded examples |
| Materials | `appearance/material/PlankMaterials`, `MasonryMaterials` | Plank donors by tag (+ `roundwood:appearance_planks`), the four masonry blocks and their artwork keys |
| Items | `appearance/item/AppearanceStacks`, `AppearanceBlockItem`, `AppearanceDrops` | Normalised stack NBT, the family item (tooltip, alias name, no block-map claim), loot decoration |
| Gestures | `appearance/interaction/AppearanceGestures`, `AppearanceActions`, `AppearanceUseEvents` | Recognise sneak+planks/dye/masonry; one checked mutation and payment; let sneak gestures reach the block |
| Registry | `registry/*` | `round_door`, `round_window`, `round_trim` (oak behaviour); three entity types valid for canonical + aliases; tab with three items; family tags |
| Migration | `migration/LegacyAppearances`, `LegacyContent`, `LegacyChunkMigration`, `LegacyCells`, `Legacy{Door,Trim,Window}Conversion`, `migration/block/*` | Frozen 49-id table, alias registration, queue-on-load / convert-on-tick, silent guarded rewrites |
| Shared assembly | `block/assembly/AssemblyMutationBatch`, `AssemblyTeardown`, `AssemblyHarvest` | Marks cells being rewritten; silent partner teardown; deferred removal so harvest loot sees the anchor |
| Door adapters | `block/door/RoundDoorBlock`, `RoundDoorItem`, `RoundDoorBlockEntity` | Delegating `DoorBlock` / item / anchor entity (swing, camouflage, appearance, update packet) |
| Door appearance / router | `block/door/appearance/DoorAppearance`, `block/door/interaction/DoorInteraction`, `block/door/selection/DoorHitRegion` | Anchor look for picks/drops/targets; restyle-camouflage-toggle precedence; one corner-or-leaf decision |
| Door layout, placement, state, removal | `block/door/assembly/*` | As in 0.4.0; placement installs the stack's appearance in the same transaction |
| Leaf, selection, camouflage | `block/door/leaf/*`, `selection/*`, `camouflage/*` | Unchanged geometry and behaviour |
| Trim | `block/trim/RoundTrimBlock`, `RoundTrimItem`, `RoundTrimBlockEntity`, `appearance/TrimAppearance`, `assembly/*`, `geometry/*` | Static ring quarters with an anchor entity holding the look |
| Window | `block/window/RoundWindowBlock`, `RoundWindowBlockEntity`, `RoundWindowItem`, `WindowAppearanceData`, `WindowGeometry`, `WindowShapes`, `WindowConnections` | Pane with a per-window look handed to its chunk model as model data |
| Client materials | `client/material/DonorSprites`, `DonorColors`, `BuiltinWoodStyles`, `PaintStyles`, `ModelRetexturing`, `AppearanceModelCache` | Representative donor sprites, sampled tints, artwork bindings, immutable quad transforms, bounded caches cleared together on reload |
| Door rendering | `client/door/RoundDoorRenderer`, `DoorAppearanceModels`, `DoorLeafModels`, `DoorParticles`, `DoorTargetingEvents`, `DoorHighlightRenderer` | BER drawing the appearance leaf (panel retextured/tinted, brass untouched) |
| Trim / window / item rendering | `client/trim/*`, `client/window/WindowAppearanceModel`, `client/item/AppearanceItemOverrides` | Anchor BER tessellating turned quarter models; window model-data retexturing; per-stack item models |
| Viewers | `client/compat/emi/*`, `client/compat/jei/*` | Optional, annotation-discovered, never loaded without the viewer |
| Showcase | `showcase/*`, `showcase/section/*` | Op-only command; sections place canonical blocks through production paths with explicit appearances |
| Datagen | `data/**` | Oak source models + empty world models, alias compatibility resources, three recipes, loot, tags, names |
| Texture tools | `tools/generate-textures.mjs`, `tools/textures/*` | Orchestrator plus artwork modules (palettes append only, so old bytes never change) |

## Dependency direction

`showcase` -> `block/*`, `registry`, `appearance`. `registry` -> `block/*`, `migration`. `migration`
-> `block/*`, `registry`, `appearance`. `block/*` -> `appearance`, `geometry`. `appearance` imports
no block, registry or client code. `client` -> `block/*`, `appearance`, `registry`, `migration`
(model wrapping covers the aliases). `client/compat/*` -> everything above plus the viewer APIs;
nothing imports them. `data` -> everything above. `block/door`, `block/window` and `block/trim`
never import each other, except `block/trim/assembly/TrimPlacement` reading a clicked door to fit a
frame. Inside `block/door`: `appearance` and `interaction` read `assembly`, `selection` and the
adapters; `assembly/DoorPlacement` writes the appearance straight to the anchor entity (no import of
`door/appearance`, keeping that pair acyclic). The door imports `client` only for
`DoorParticles` (client-only call). Geometry, `CamouflageGeometry`, `DoorSwing`, the appearance
rules and `ModelRetexturing`'s array arithmetic are plain enough for unit tests without a bootstrap.

## Structural notes

- 0.5.0 replaced the `variant/` catalogue: material acceptance (`appearance/material`), artwork
  (`client/material`), samples (`AppearanceSamples`) and frozen history (`migration/LegacyAppearances`)
  are separate authorities. The vanilla wood list appears in each on purpose (artwork, samples and
  the frozen table must not share a mutable source).
- The appearance system nests `material/`, `item/` and `interaction/` under `appearance/` so the
  root package stays at nine entries (the plan had them at the root).
- Door and frame keep separate layout and placement policies; only the mutation batch, teardown and
  deferred harvest are shared.
- `client/door/DoorLeafModels` and `client/trim/TrimModelNames` are pure naming shared by the client
  and the model generator.
- `RoundwoodLootProvider` is a factory, not a provider subclass.
