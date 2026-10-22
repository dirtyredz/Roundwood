# STRUCTURE — Roundwood

<!-- Last full review: 2026-10-21 -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/appearance/` — the appearance value, family acceptance, defaults contract, versioned NBT, pure restyle rules, bounded samples
- `src/main/java/io/github/dirtyredz/roundwood/appearance/material/` — server-safe donor recognition: tagged planks, the fixed four masonry materials
- `src/main/java/io/github/dirtyredz/roundwood/appearance/item/` — appearance on stacks, the shared family block item (tooltip, alias rules), loot decoration
- `src/main/java/io/github/dirtyredz/roundwood/appearance/interaction/` — gesture recognition, validated server mutation and payment, the narrow sneak-use event bridge
- `src/main/java/io/github/dirtyredz/roundwood/migration/` — frozen 0.4.0 mappings, alias registration, chunk-load scheduling and the migration queue, the 0.4.0 alias policy, per-family conversions
- `src/main/java/io/github/dirtyredz/roundwood/migration/block/` — the deprecated alias block adapters (historical default, convert before integrity)
- `src/main/java/io/github/dirtyredz/roundwood/migration/trim/` — the 0.7.0 fitted-frame to door-trim conversion
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration: the twelve canonical blocks and thirteen items, block entities, creative tab, mod tags, the `trim_face` loot condition type
- `src/main/java/io/github/dirtyredz/roundwood/block/` — the three block families and what they share (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/` — the face-trim attachment shared by windows and doors: face, the two-face value, its versioned codec, the read-only source contract
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/loot/` — the `roundwood:trim_face` loot condition
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/` — services shared by the multi-cell families: the mutation batch, partner teardown, deferred harvest
- `src/main/java/io/github/dirtyredz/roundwood/block/door/` — the door block, its item and its anchor block entity (Minecraft adapters only)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/assembly/` — the four-part door: layout, hinge choice, placement, state, removal, integrity, anchor block-entity lookup
- `src/main/java/io/github/dirtyredz/roundwood/block/door/appearance/` — reading, targeting and dropping the door's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/door/interaction/` — the door's right-click router: restyle, then camouflage, then toggle
- `src/main/java/io/github/dirtyredz/roundwood/block/door/leaf/` — the single leaf: geometry and hardware, collision shapes, swing timeline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/selection/` — target and visible whole-door shapes (with the installed trim rings), the swinging leaf outline, hit re-attribution, corner-or-leaf hit region
- `src/main/java/io/github/dirtyredz/roundwood/block/door/camouflage/` — corner geometry, accepted materials, stored data, interaction, refunds
- `src/main/java/io/github/dirtyredz/roundwood/block/door/trim/` — the Door Trim: item, versioned payload, ring geometry and placement of the two faces, targeting, fit/remove/restyle actions
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/` — the compatibility frame from older saves: the block, its anchor block entity (Minecraft adapters only; the `round_trim` item is the Door Trim)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/appearance/` — reading, targeting and dropping the frame's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/assembly/` — the four-quarter frame: part identity, layout, placement (fitted / freestanding), removal, integrity
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/geometry/` — the ring (plain Java) and its cached per-quarter selection shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/window/` — the window family: the pane blocks (round window; oriented arches and corners), their entity, items and model data, restyle and drops
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/` — pure window geometry shared by every design: pose, depth slabs, face identity (front/back slot to geometric side), pane panels and centred-pane arms, cached shapes and culling footprints (selection includes the in-cell trim bands), the visible-surface generator
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/face/` — one window face: profile (round, arch left and right, corner bottom left and right), arch and corner glazing, cross-beam pattern and geometry, the design that names a block
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/trim/` — the trim bands per face and their visible surfaces, and the whole-face UVs
- `src/main/java/io/github/dirtyredz/roundwood/block/window/interaction/` — the window's right-click router: the hit-region classifier, the hit target (Minecraft hit to face slot and region), the pure routing table, the adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/window/trim/` — the window trim item, its versioned payload and 0.7 adapter, fit/remove/restyle actions, reconcile
- `src/main/java/io/github/dirtyredz/roundwood/block/window/glass/` — glass colour: the versioned payload, window item stacks, the dye action
- `src/main/java/io/github/dirtyredz/roundwood/block/window/placement/` — targeted placement: the face-zone classifier and its polygons (one authority), the use-context adapter, round position and arch pose
- `src/main/java/io/github/dirtyredz/roundwood/block/window/connection/` — the connection rule and junction decisions between panes, walls, bars and windows
- `src/main/java/io/github/dirtyredz/roundwood/showcase/` — the `/roundwood showcase` command, bounded builder, coordinate frame, canvas (samples, appearance placement), the listed `ShowcaseWindow` and section registry
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/` — one class per showcase section (doors, camouflage, windows, window positions, window arches, window corners, window cross beams (`WindowMuntinsSection`), window trims, redstone, paints, trim combinations, brick arches)
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, the shared ring profile, tile-normalised UVs (`FaceUv.tiled`), tiled and whole-assembly face UVs, voxel-shape conversion (shared)
- `src/main/java/io/github/dirtyredz/roundwood/client/` — client-only mod-bus wiring (`RoundwoodClient`)
- `src/main/java/io/github/dirtyredz/roundwood/client/door/` — the door leaf renderer, the door trim renderer, appearance leaf models, leaf model names, targeting events, highlight, particles
- `src/main/java/io/github/dirtyredz/roundwood/client/trim/` — the frame renderer, appearance quarter models (also used by the door trim), source model names
- `src/main/java/io/github/dirtyredz/roundwood/client/window/` — the appearance-aware window blockstate/item model (frame and glass chunk model), the trim block-entity renderer and its cached trim models, the restyled item-model cache, the placement preview and overlay
- `src/main/java/io/github/dirtyredz/roundwood/client/render/` — line drawing shared by the door highlight and the window placement overlay
- `src/main/java/io/github/dirtyredz/roundwood/client/item/` — NBT-aware item model overrides
- `src/main/java/io/github/dirtyredz/roundwood/client/material/` — donor sprites and colours, built-in wood and paint artwork bindings, masonry faces (thin mortar), quad retexturing, bounded model caches
- `src/main/java/io/github/dirtyredz/roundwood/client/camouflage/` — static corner renderer, cached corner models
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/emi/` — optional EMI adapter: cycling preview entries, serializer
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/jei/` — optional JEI adapter: subtypes, hidden aliases
- `src/main/java/io/github/dirtyredz/roundwood/data/` — datagen provider registration
- `src/main/java/io/github/dirtyredz/roundwood/data/client/` — blockstate/model providers per family, alias compatibility models, item-model providers, display transforms, language
- `src/main/java/io/github/dirtyredz/roundwood/data/client/window/` — the window models (pane-only visible-surface models per design, pose and combination), their blockstates and item models
- `src/main/java/io/github/dirtyredz/roundwood/data/server/` — recipe, loot (including the attachment loot pools) and tag providers
- `src/main/resources/` — mods.toml, pack.mcmeta, the generated original textures (checked in)
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored and excluded)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests (`testsupport/` bootstrap shim, `appearance/`, `migration/`, `client/material/`, `geometry/`, `block/attachment/`, `block/door/leaf/`, `block/door/selection/`, `block/door/camouflage/`, `block/door/trim/`, `block/trim/geometry/`, `block/window/geometry/`, `block/window/interaction/`, `block/window/glass/`, `block/window/placement/`, `block/window/trim/`, `client/window/`, `data/`)
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests (`fixture/`, `appearance/`, `migration/`, `block/door/`, `block/door/appearance/`, `block/door/selection/`, `block/door/camouflage/`, `block/door/trim/`, `block/trim/`, `block/window/`, `showcase/`, `data/`); never in the release jar
- `src/gameTest/resources/` — GameTest structure templates (empty ones, the 0.4.0 fixture and the 0.7.0 fitted-frame fixture) and a test-only plank tag
- `tools/` — entry points of the deterministic generators for the textures and GameTest structures (Node, not packaged)
- `tools/textures/` — texture artwork modules: PNG encoding, noise, the 0.1.0 textures, palettes, door boards, paint panels, radial bricks, window glass, thin-mortar window and door trim faces
- `docs/` — architecture, decisions, gotchas

## Components

| Component | Files | Responsibility |
|---|---|---|
| Appearance | `appearance/Appearance`, `AppearanceFamily`, `AppearanceDefaults`, `AppearanceNbt`, `AppearanceChanges`, `AppearanceSamples` | Immutable look (wood/masonry + optional dye), which family takes what, a block's/item's default, the one NBT form, restyle rules, bounded examples |
| Materials | `appearance/material/PlankMaterials`, `MasonryMaterials`, `DonorStacks` (package-private) | Plank donors by tag (+ `roundwood:appearance_planks`), the four masonry blocks and their artwork keys; the shared rule that a stack carrying placed contents or a non-default state is never a donor |
| Items | `appearance/item/AppearanceStacks`, `AppearanceBlockItem`, `AppearanceDrops` | Normalised stack NBT, the family item (tooltip, alias name, no block-map claim), loot decoration |
| Gestures | `appearance/interaction/AppearanceGestures`, `AppearanceActions`, `AppearanceUseEvents` | Recognise sneak+planks/dye/masonry; one checked mutation and payment; let sneak gestures reach the block |
| Registry | `registry/*` (incl. `ModLootConditions`) | `round_door`, ten windows (`round_window`, `round_window_arch_left/right`, `round_window_bottom_left/right`, each also `_cross`), `round_trim`: twelve canonical blocks, thirteen items (the thirteenth is `window_trim`, an item only; the `round_trim` item is the Door Trim, its block the compatibility frame); 61 block and 62 item registrations with the 49 aliases; entity types valid for canonical + aliases (the window entity serves the ten windows and 15 alias windows); tab with thirteen items; family tags (`roundwood:round_windows` lists the ten windows; `round_trims` holds the Door Trim item, and window trim as an item); the `roundwood:trim_face` loot condition type |
| Attachments | `block/attachment/AttachmentFace`, `FaceTrims`, `FaceTrimsNbt`, `TrimAttachmentSource`, `loot/TrimFaceCondition` | The face-trim value shared by windows and doors: FRONT/BACK slots, two optional looks, the one versioned codec, the read-only source contract, the per-face loot condition |
| Migration | `migration/LegacyAppearances`, `LegacyContent`, `ChunkMigrations`, `ChunkMigrationQueue`, `LegacyChunkMigration`, `LegacyCells`, `Legacy{Door,Trim,Window}Conversion`, `migration/trim/DoorTrimConversion`, `migration/block/*` | Frozen 49-id table, alias registration, chunk-load discovery and one deduplicated queue (64 entries per level per tick), the 0.4.0 alias policy, silent guarded rewrites; the 0.7.0 fitted frame becomes the door's FRONT trim |
| Shared assembly | `block/assembly/AssemblyMutationBatch`, `AssemblyTeardown`, `AssemblyHarvest` | Marks cells being rewritten; silent partner teardown; deferred removal so harvest loot sees the anchor |
| Door adapters | `block/door/RoundDoorBlock`, `RoundDoorItem`, `RoundDoorBlockEntity` | Delegating `DoorBlock` / item / anchor entity (swing, camouflage, appearance, update packet) |
| Door appearance / router | `block/door/appearance/DoorAppearance`, `block/door/interaction/DoorInteraction`, `block/door/selection/DoorHitRegion` | Anchor look for picks/drops/targets; restyle-camouflage-toggle precedence; one corner-or-leaf decision |
| Door layout, placement, state, removal | `block/door/assembly/*` | As in 0.4.0; placement installs the stack's appearance in the same transaction |
| Leaf, selection, camouflage | `block/door/leaf/*`, `selection/*`, `camouflage/*` | Leaf geometry and swing timeline; resting and swinging outlines (`DoorSwingOutline`, 0.6.1); camouflage unchanged |
| Trim (compatibility frame) | `block/trim/RoundTrimBlock`, `RoundTrimBlockEntity`, `appearance/TrimAppearance`, `assembly/*`, `geometry/*` | Static ring quarters from older saves with an anchor entity holding the look; no item places them any more |
| Door trim | `block/door/trim/*` (`DoorTrimItem`, `DoorTrimNbt`, `DoorTrimGeometry`, `DoorTrimTarget`, actions), `block/door/assembly/DoorBlockEntities`, `block/door/interaction/DoorInteraction` | The Door Trim item and its per-face rings stored on the door anchor; targeting and routing ahead of restyle, camouflage and toggle |
| Window | `block/window/*` (`WindowPaneBlock`, `RoundWindowBlock`, `OrientedWindowBlock`, `RoundWindowBlockEntity`, `RoundWindowItem`, `WindowAppearanceData`), `placement/*` (`WindowOrientation`, `WindowPosition`, `WindowTargetZones`, `WindowPlacement`), `geometry/*` (`WindowPose`, `WindowFaceIdentity`, `WindowDepth`, `WindowPaneGeometry`, `WindowShapes`, `WindowSurfaceGeometry`), `geometry/face/*` (`WindowProfile`, `WindowArchGeometry`, `WindowCornerGeometry`, `WindowPattern`, `WindowMuntinGeometry`, `WindowDesign`), `geometry/trim/*` (`WindowTrimGeometry`, `WindowTrimSurfaces`, `WindowTrimUv`), `interaction/*` (`WindowHitRegions`, `WindowHitTarget`, `WindowUseRules`, `WindowInteraction`), `trim/*` (`WindowTrimItem`, `WindowTrimNbt`, `WindowTrimActions`, `WindowTrimTarget`), `glass/*` (`WindowGlassNbt`, `WindowGlassStacks`, `WindowGlassActions`), `connection/WindowConnections` | Pane (round, arch or corner; plain or cross beams; posed by orientation/depth/position, `face_reversed` for face identity) with a per-window frame look and glass colour handed to its chunk model as model data; the logical pieces painted into visible surfaces; the trim attachments (item, versioned payload, bands per face); the shared hit classification and the pure routing table behind the click router; zone classifier and placement; junction and connection rules |
| Client materials | `client/material/DonorSprites`, `DonorColors`, `BuiltinWoodStyles`, `PaintStyles`, `MasonryStyles`, `GlassStyles`, `ModelRetexturing`, `AppearanceModelCache` | Representative donor sprites, sampled tints, wood, paint and glass (clear and stained) artwork bindings, masonry faces (thin-mortar door ring, window faces), immutable quad transforms (also by position UVs), bounded caches cleared together on reload |
| Door rendering | `client/door/RoundDoorRenderer`, `DoorTrimRenderer`, `DoorAppearanceModels`, `DoorLeafModels`, `DoorParticles`, `DoorTargetingEvents`, `DoorHighlightRenderer` | BER drawing the appearance leaf (panel retextured/tinted, brass untouched); `DoorTrimRenderer` draws the face rings before the swing transforms from the frame's turned quarter models |
| Trim / window / item rendering | `client/trim/*`, `client/window/*`, `client/render/OutlineLines`, `client/item/AppearanceItemOverrides` | Anchor BER tessellating turned quarter models; the window chunk model (frame look and glass colour, pane only); `WindowTrimRenderer` drawing trims from cached `WindowTrimWorldModels`; item cache keyed by model, wood and glass (`WindowItemModels`), trim item model per stack; placement preview and overlay (`WindowPlacementPreview`, `WindowPlacementOverlay`); per-stack item models |
| Viewers | `client/compat/emi/*`, `client/compat/jei/*` | Optional, annotation-discovered, never loaded without the viewer |
| Showcase | `showcase/*`, `showcase/section/*` | Op-only command; sections place canonical blocks through production paths with explicit appearances; `ShowcaseWindow` lists each window (design, pose, frame coordinates, looks) so building and the GameTest check read the same list |
| Datagen | `data/**`, `data/client/window/*` | Oak source models + empty world models, window models as visible surfaces (`WindowModelProvider`, `OrientedWindowModelProvider`, `WindowPaneModelWriter`, `GeometryModelWriter.addSurfaces`; 110 pane-only window block models, each state one complete model, no trimmed variants), item providers (`WindowItemModelProvider`, `WindowTrimItemModelProvider`), display transforms (`ItemModelTransforms`), alias models (`LegacyModelProvider`), alias compatibility resources, thirteen recipes, loot (`AttachmentLootPools`: one pool per face for windows and doors), tags, names |
| Texture tools | `tools/generate-textures.mjs`, `tools/textures/*` | Orchestrator plus artwork modules (palettes append only, so old bytes never change); `stained-glass.mjs` writes the sixteen `window_glass_<dye>` textures, `window-trims.mjs` the thin-mortar trim faces and the clearer glass (`clearWindowGlass`) |

## Dependency direction

`showcase` -> `block/*`, `registry`, `appearance`. `registry` -> `block/*`, `migration`. `migration`
-> `block/*`, `registry`, `appearance`. `block/*` -> `appearance`, `geometry`. `appearance` imports
no block, registry or client code. `client` -> `block/*`, `appearance`, `registry`, `migration`
(model wrapping covers the aliases). `client/compat/*` -> everything above plus the viewer APIs;
nothing imports them. `data` -> everything above. `block/door`, `block/window` and `block/trim`
never import each other (`TrimPlacement` no longer reads doors; `DoorTrimItem` takes a plain `Block`,
so `block/door` does not import `block/trim`). `block/door` and `block/window` both import
`block/attachment`. `client/door/DoorTrimRenderer` imports `block/trim/assembly/TrimPart` and
`client/trim/TrimAppearanceModels`; `migration/trim` reads `block/door` and `block/trim`. Inside
`block/door`: `appearance` and `interaction` read `assembly`, `selection` and the
adapters; `assembly/DoorPlacement` writes the appearance straight to the anchor entity (no import of
`door/appearance`, keeping that pair acyclic). Inside `block/window`: `connection` reads
`WindowPaneBlock.pose` of neighbours and the block calls the connection rule (a deliberate two-way
edge); `geometry` and `placement` import no block adapter (`WindowShapes` is the only geometry
class that touches Minecraft shapes). `geometry/face` and `geometry/trim` build on `geometry` and
are read by `interaction`, `trim`, `glass` and the client; `interaction` calls `trim` and `glass` (the
item no longer imports `interaction`). `client/window` imports `block/door/selection/DoorHitResolver`
for the placement preview (a refused projected-door hit shows nothing). The door imports `client`
only for
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
- 0.7.0 split `block/window/geometry/` so it stays under ten entries: what varies per design
  (profile, arch and corner glazing, cross beams, `WindowDesign`) moved to `geometry/face/`, the
  trim band and its UVs to `geometry/trim/`, and `geometry/` keeps what every design shares. The
  0.5.0 baseline class `WindowGeometry` is gone; `WindowGeometryTest` pins the plain round design
  against the general geometry instead.
- `WindowInteraction` was extracted from `WindowPaneBlock.use` (0.7.0): the click now routes trim
  item, trim band and pane gestures, and the block keeps only the Minecraft adapter. The shared
  `appearance/interaction/AppearanceUseTarget` lets the sneak-use bridge judge a gesture by the part
  under the hit, and `appearance/item/AppearanceTooltip` (extracted from `AppearanceBlockItem`) is
  shared by the trim item and EMI.
- 0.8.0 added `block/attachment/` because `AttachmentFace`, `FaceTrims` and `FaceTrimsNbt` are
  shared by two hosts (windows and doors); neither host owns the codec. `RingGeometry` was extracted
  to `geometry/` from `block/trim/geometry/TrimGeometry` for the same reason (frame and door trim).
- `WindowHitRegions` lives in `block/window/interaction/`, not `geometry/face` as planned: the
  classifier needs the trim band, `geometry/trim` already imports `geometry/face`, and putting it in
  `face` would make a package cycle.
- Window trim fitting moved from the item into the window router, so `WindowTrimItem` does not import
  `interaction/`; the bridge asks the block's router (`claimsUse`), not the item.
- Debt: `block/window/` now has 12 entries, over the 10-entry cap (it was already 11 in 0.7.0). Split
  later, for example by grouping `trim/` and `glass/` under a `looks/` parent (dk-846bea79).
- `RoundWindowArchBlock` became `OrientedWindowBlock` (arches and corners); property names and
  values are unchanged. `MasonryStyles` was extracted from `PaintStyles`, which keeps paint only.

## Structural debt (baseline review 2026-10-21)

The first full review found no correctness problems. The debt is tracked in Docket, one line each;
`migration/` is also over the directory breadth cap (dk-aef47f84).

- dk-08d20a47 face-trim transaction duplicated (door and window trim actions)
- dk-f9d8d245 trims and unknown-payload holder duplicated in both block entities
- dk-992020e6 door/frame assembly mirror
- dk-846bea79 `block/window/` has 12 entries
- dk-73e57897 look resolver duplicated across the client trim, door and window models
- dk-3d456d06 `generate-test-structures.mjs` mixes NBT writer, builder and fixtures
- dk-1af2b67e texture modules split by release, not responsibility
- dk-aef47f84 `migration/` breadth, single-file folders
- dk-c28677ae window geometry tests do not mirror `geometry/face` and `geometry/trim`
- dk-0dc49165 `showcase/section` at cap; verifiers far from their sections
- dk-7fd38157 client tidy-ups
- dk-7abe21de Direction quarter-turn helper copied three times
- dk-75a60754 dependency direction (block -> client import, registry <-> migration cycle, Door Trim on frame concepts)
- dk-4cf186bd `LegacyAlias` contract
- dk-1d129342 test duplication
- dk-d402bd77 shared appearance-family wiring (P3, only if a fourth family appears)
