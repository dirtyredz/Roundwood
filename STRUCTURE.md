# STRUCTURE — Roundwood

<!-- Last full review: 2026-10-21 -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

```
src/main/java/.../roundwood/
  appearance/{material,item,interaction}   the look: value, donors, stacks, gestures
  migration/{block,conversion}             frozen 0.4.0 aliases, queue, per-family conversions
  registry/  geometry/  showcase/{section}
  block/
    attachment/{storage,host,interaction,item}   face trims shared by every host, anchor entity
    assembly/{placement}                         multi-cell contract and services
    door/ archway/ trim/ window/                 the families (each with its own subfolders)
  client/{door,trim,window,archway,render,item,material,camouflage,compat/{emi,jei}}
  data/{client,server}
src/test/...      mirrors main (pure JUnit)
src/gameTest/...  mirrors main (Forge GameTests; door/{lifecycle,placement}, window/{design,placement})
tools/{textures/masonry}   deterministic texture and structure generators
```

Per-family `appearance/` and `interaction/` folders (door, archway, window, trim), and the one-file `block/window/connection/` and `showcase/section/archway/`, are deliberate: every family keeps the same sibling set so a reader finds the same thing in the same place, and each is expected to grow.
- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/appearance/` — the appearance value, family acceptance, defaults contract, versioned NBT, pure restyle rules, bounded samples
- `src/main/java/io/github/dirtyredz/roundwood/appearance/material/` — server-safe donor recognition: tagged planks, the fixed four masonry materials
- `src/main/java/io/github/dirtyredz/roundwood/appearance/item/` — appearance on stacks, the shared family block item (tooltip, alias rules), loot decoration
- `src/main/java/io/github/dirtyredz/roundwood/appearance/interaction/` — gesture recognition, validated server mutation and payment, the narrow sneak-use event bridge
- `src/main/java/io/github/dirtyredz/roundwood/migration/` — frozen 0.4.0 mappings, alias registration, chunk-load scheduling and the migration queue, the 0.4.0 alias policy; the conversions sit in `conversion/`
- `src/main/java/io/github/dirtyredz/roundwood/migration/block/` — the deprecated alias block adapters (historical default, convert before integrity)
- `src/main/java/io/github/dirtyredz/roundwood/migration/conversion/` — the per-family conversions (door, frame, window), their shared cell rewrite (`LegacyCells`), and the 0.7.0 fitted-frame to door-trim conversion (`DoorTrimConversion`)
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration: the 31 canonical blocks and 32 advertised items, block entities, creative tab, mod tags, the `trim_face` loot condition type
- `src/main/java/io/github/dirtyredz/roundwood/block/` — the block families and what they share (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/` — the archways (0.12.0): the Minecraft adapters (the block, its anchor entity, the item)
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/assembly/` — the four-cell archway: part identity, its layout (the shared `Assembly` contract), the typed placement plan, placement, typed anchor lookup (integrity and removal are `block/assembly`'s)
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/geometry/` — the two designs' profiles (ordinary, keyhole), the frame boxes, the cached per-cell shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/appearance/` — the archway's look through its anchor, and its right-click router
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/trim/` — the archway as a trim host: payload format, casing geometry, hit targeting, the host adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/` — the face-trim attachment shared by every host: face (and the shared depth rule and proud constant), slot, the two-face value, the read-only source contract, the shared anchor-entity persistence and synchronisation (`AppearanceAnchorBlockEntity`: look, declared stores, save/load/update, packets; the door, archway, window and frame entities extend it), the `roundwood:trim_face` loot condition
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/storage/` — the versioned two-face codec and the per-slot store (unknown-payload custody)
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/host/` — the narrow writable host view (live, callback-backed) and the one fit/remove/install/restyle transaction
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/interaction/` — the held-item classifier every trim host's router reads (acting hand, attachment, shears, donor)
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/item/` — the attachment items: the slot-naming recognition contract, the Universal Trim item and the Arch Sill item
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/` — what the multi-cell families (doors, archways, the compatibility frame) share: the `Assembly` contract, integrity, removal, typed anchor lookup, the mutation batch, partner teardown, deferred harvest, the whole-assembly restyling target
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/placement/` — the read-only placement plan skeleton, the installation transaction (with rollback) and player placement
- `src/main/java/io/github/dirtyredz/roundwood/block/door/` — the door family (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/adapter/` — the Minecraft adapters: the shared door adapter (`AssemblyDoorBlock`), the round and arched door blocks, the item that places either design and the anchor block entity
- `src/main/java/io/github/dirtyredz/roundwood/block/door/assembly/` — the two- or four-part door: layout (with its design; the shared `Assembly` contract), hinge choice, the typed placement plan, placement, state, typed anchor block-entity lookup (integrity and removal are `block/assembly`'s)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/appearance/` — reading, targeting and dropping the door's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/door/interaction/` — the door's right-click router: restyle, then camouflage, then toggle
- `src/main/java/io/github/dirtyredz/roundwood/block/door/leaf/` — the single leaf: the design (`DoorDesign`), geometry and hardware, collision shapes, swing timeline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/selection/` — target and visible whole-door shapes (with the installed trim rings), the swinging leaf outline, hit re-attribution, corner-or-leaf hit region, the placement preview outline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/camouflage/` — corner geometry, accepted materials, stored data, interaction, refunds
- `src/main/java/io/github/dirtyredz/roundwood/block/door/trim/` — the door as a trim host: payload format, per-design casing geometry and placement of the two faces, targeting, the host adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/` — the compatibility frame from older saves: the block, its anchor block entity (Minecraft adapters only; the `round_trim` item is the Universal Trim)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/appearance/` — reading, targeting and dropping the frame's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/assembly/` — the four-quarter frame: part identity, layout (the shared `Assembly` contract), player-less placement (integrity and removal are `block/assembly`'s)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/geometry/` — the ring (plain Java) and its cached per-quarter selection shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/window/` — the window family (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/window/pane/` — the pane blocks (round window; oriented arches, corners and edges), their entity (frame look, glass, trim and sill stores), items and model data, restyle and drops
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/` — pure window geometry shared by every design: pose, depth slabs, face identity (front/back slot to geometric side), pane panels and centred-pane arms, cached shapes and culling footprints (the selection composes the pane with the in-cell trim bands and sills; the pane geometry imports no trim code), the visible-surface generator
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/face/` — one window face: profile (round; upper and lower arches; bottom and top corners; four single edges), arch, corner and edge glazing, cross-beam pattern and geometry, the design that names a block
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/trim/` — the attachments per face and slot: perimeter bands, the arch sill, their visible surfaces, the whole-face UVs and the masonry layout per profile
- `src/main/java/io/github/dirtyredz/roundwood/block/window/interaction/` — the window's right-click router: the hit-region classifier (trim band, sill, frame, glass), the hit target (Minecraft hit to face and region), the pure routing table, the adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/window/trim/` — the window as an attachment host: the trim payload and 0.7 adapter, the sill payload, the slot-aware host adapter and `trimmed` reconcile
- `src/main/java/io/github/dirtyredz/roundwood/block/window/glass/` — glass colour: the versioned payload, window item stacks, the dye action
- `src/main/java/io/github/dirtyredz/roundwood/block/window/placement/` — targeted placement: the face-zone classifier and its polygons (one authority), the use-context adapter, round position and arch pose
- `src/main/java/io/github/dirtyredz/roundwood/block/window/connection/` — the connection rule and junction decisions between panes, walls, bars and windows
- `src/main/java/io/github/dirtyredz/roundwood/showcase/` — the `/roundwood showcase` command, bounded builder, coordinate frame, canvas (samples, appearance placement), the listed `ShowcaseWindow` and section registry
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/` — the showcase sections outside the window and door families (paints, brick arches)
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/archway/` — the archway section (0.12.0)
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/door/` — the door sections (round doors, camouflage, redstone, trim combinations; since 0.11.0 the arched doors)
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/window/` — the window sections (windows, positions, arches, corners, cross beams (`WindowMuntinsSection`), trims; since 0.10.0 circles, sills, edges)
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, the shared ring profile, the round-headed arched profile (`ArchedProfile`), a pixel mask's visible slab (`PixelSlab`), tile-normalised UVs (`FaceUv.tiled`), tiled and whole-assembly face UVs (rectangular mapping too), voxel-shape conversion (shared)
- `src/main/java/io/github/dirtyredz/roundwood/client/` — client-only mod-bus wiring (`RoundwoodClient`)
- `src/main/java/io/github/dirtyredz/roundwood/client/archway/` — the archway renderer, its baked frame, casing and item models, the placement preview (0.12.0)
- `src/main/java/io/github/dirtyredz/roundwood/client/door/` — the door leaf renderer, the door trim renderer, the per-design casing models, the placement preview, appearance leaf models, leaf model names, targeting events, highlight, particles
- `src/main/java/io/github/dirtyredz/roundwood/client/trim/` — the frame renderer, appearance quarter models (also used by the door trim), source model names
- `src/main/java/io/github/dirtyredz/roundwood/client/window/` — the appearance-aware window blockstate/item model (frame and glass chunk model), the restyled item-model cache, the placement preview and overlay
- `src/main/java/io/github/dirtyredz/roundwood/client/window/trim/` — the trim and sill block-entity renderer, its cached world models and the attachment look per slot and profile
- `src/main/java/io/github/dirtyredz/roundwood/client/render/` — line drawing shared by the highlights and placement previews; baked pixel-mask slab models in a trim look (`PixelSlabModels`, 0.12.0: the arched door casing and the archways)
- `src/main/java/io/github/dirtyredz/roundwood/client/item/` — NBT-aware item model overrides, `FamilyItemModels` (the table of which wrapper each item kind gets), the attachment icons (Universal Trim, Arch Sill) per look
- `src/main/java/io/github/dirtyredz/roundwood/client/material/` — donor sprites and colours, built-in wood and paint artwork bindings, masonry faces (thin mortar), the shared trim look resolver, quad retexturing, bounded model caches
- `src/main/java/io/github/dirtyredz/roundwood/client/camouflage/` — static corner renderer, cached corner models
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/emi/` — optional EMI adapter: alias removal and the appearance comparison key
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/jei/` — optional JEI adapter: subtypes, hidden aliases
- `src/main/java/io/github/dirtyredz/roundwood/data/` — datagen provider registration
- `src/main/java/io/github/dirtyredz/roundwood/data/client/` — blockstate/model providers per family, alias compatibility models, item-model providers, display transforms, language
- `src/main/java/io/github/dirtyredz/roundwood/data/client/archway/` — the archway blockstates (empty world models) and item models (0.12.0)
- `src/main/java/io/github/dirtyredz/roundwood/data/client/door/` — the door blockstates, leaf models and item models per design
- `src/main/java/io/github/dirtyredz/roundwood/data/client/window/` — the window models (pane-only visible-surface models per design, pose and combination), their blockstates and item models
- `src/main/java/io/github/dirtyredz/roundwood/data/client/trim/` — the compatibility frame's blockstate and models, the attachment item models (square frame, sill bar)
- `src/main/java/io/github/dirtyredz/roundwood/data/server/` — the recipe coordinator, loot (including the attachment loot pools) and tag providers
- `src/main/java/io/github/dirtyredz/roundwood/data/server/recipe/` — recipe emission per family (door, windows, archways, attachments)
- `src/main/resources/` — mods.toml, pack.mcmeta, the generated original textures (checked in)
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored and excluded)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests (`testsupport/` bootstrap shim, `appearance/`, `migration/`, `client/material/`, `geometry/`, `block/assembly/`, `block/attachment/`, `block/attachment/storage/`, `block/attachment/host/`, `block/archway/geometry/`, `block/archway/trim/`, `block/door/leaf/`, `block/door/selection/`, `block/door/camouflage/`, `block/door/trim/`, `block/trim/geometry/`, `block/window/geometry/`, `block/window/geometry/face/`, `block/window/geometry/trim/`, `block/window/interaction/`, `block/window/glass/`, `block/window/placement/`, `block/window/trim/`, `client/window/`, `data/`)
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests (`fixture/`, `fixture/assembly/`, `appearance/`, `block/assembly/`, `block/assembly/placement/`, `block/attachment/` (anchor persistence), `block/attachment/interaction/`, `migration/`, `block/archway/`, `block/door/`, `block/door/lifecycle/`, `block/door/placement/`, `block/door/appearance/`, `block/door/selection/`, `block/door/camouflage/`, `block/door/trim/`, `block/trim/`, `block/window/`, `block/window/design/`, `block/window/placement/`, `block/window/trim/`, `showcase/`, `showcase/section/` (one check class per section family beside the sections: `archway/`, `door/`, `window/`, the root for paints and brick arches), `data/`); never in the release jar
- `src/gameTest/resources/` — GameTest structure templates (empty ones, the 0.4.0 fixture and the 0.7.0 fitted-frame fixture), a test-only plank tag and a test-only `forge:shears` tag (a damageable non-shears item)
- `tools/` — entry points of the deterministic generators for the textures and GameTest structures (Node, not packaged)
- `tools/textures/` — texture artwork modules: PNG encoding, noise, the 0.1.0 textures, palettes, door boards, paint panels, radial bricks, window glass (and its clearer 0.7.0 variant), stained glass, the round window face and trim markers (`window-trims.mjs`)
- `tools/textures/masonry/` — the contour-following thin-mortar masonry faces built on the shared thin-mortar primitives in `mortar.mjs` (joint coverage, the one parameterised polar brick position, brick and mortar colours; 0.12.0 grouping): the arched window face, the square-frame Universal Trim face, the 0.10.0 window shape faces, the 0.11.0 arched door casing, the 0.12.0 archway faces
- `docs/` — architecture, decisions, gotchas

## Components

| Component | Files | Responsibility |
|---|---|---|
| Appearance | `appearance/Appearance`, `AppearanceFamily`, `AppearanceDefaults`, `AppearanceNbt`, `AppearanceChanges`, `AppearanceSamples` | Immutable look (wood/masonry + optional dye), which family takes what, a block's/item's default, the one NBT form, restyle rules, bounded examples |
| Materials | `appearance/material/PlankMaterials`, `MasonryMaterials`, `DonorStacks` (package-private) | Plank donors by tag (+ `roundwood:appearance_planks`), the four masonry blocks and their artwork keys; the shared rule that a stack carrying placed contents or a non-default state is never a donor |
| Items | `appearance/item/AppearanceStacks`, `AppearanceBlockItem`, `AppearanceTooltip`, `AppearanceDrops` | Normalised stack NBT, the family item (tooltip, alias name, no block-map claim), loot decoration |
| Gestures | `appearance/interaction/AppearanceGestures`, `AppearanceActions`, `AppearanceUseEvents`, `AppearanceUseTarget` | Recognise sneak+planks/dye/masonry; one checked mutation and payment; let sneak gestures reach the block |
| Registry | `registry/*` (incl. `ModLootConditions`) | `round_door`, `arched_door`, 26 windows (`round_window`, `round_window_arch_left/right`, `round_window_bottom_left/right`, `round_window_arch_bottom_left/right`, `round_window_top_left/right`, `round_window_edge_top/left/bottom/right`, each also `_cross`), `archway`, `keyhole_archway` (0.12.0), `round_trim`: 31 canonical blocks; 32 advertised items (plus the item-only `arch_sill_trim`; the `round_trim` item is the Universal Trim, its block the compatibility frame); 80 block and 82 item registrations with the 49 aliases and the hidden `window_trim` (registered by `migration/LegacyContent`); entity types valid for canonical + aliases (the window entity serves the 26 windows and 15 alias windows; the `archway` entity both archways); tab with 32 items; family tags (`roundwood:round_doors` lists both doors; `roundwood:round_windows` lists the 26 windows; `roundwood:archways` both archways, never a door tag; `round_trims` holds the Universal Trim and the hidden window trim); the `roundwood:trim_face` loot condition type |
| Archway (0.12.0) | `block/archway/*` (`ArchwayBlock`, `ArchwayBlockEntity`, `ArchwayItem`), `assembly/*` (`ArchwayPart`, `ArchwayLayout`, `ArchwayPlacementPlan`, `ArchwayPlacement`, `ArchwayBlockEntities`), `geometry/*` (`ArchwayDesign`, `KeyholeGeometry`, `ArchwayGeometry`, `ArchwayShapes`), `appearance/*` (`ArchwayAppearance`, `ArchwayInteraction`), `trim/*` (`ArchwayTrimNbt`, `ArchwayTrimGeometry`, `ArchwayTrimTarget`, `ArchwayTrimActions`) | Static 2 x 2 open frame in two designs, a trim-family look on one anchor entity, one read-only placement plan for the server and the preview, the shared assembly lifecycle, placement and anchor lookup behind typed facades, per-cell frame shapes (collision = target = outline), and the face casings through the shared attachment services |
| Attachments | `block/attachment/AttachmentFace`, `TrimSlot`, `FaceTrims`, `TrimAttachmentSource`, `storage/FaceTrimsNbt`, `storage/FaceTrimStore`, `host/TrimHost`, `host/TrimFitting`, `AppearanceAnchorBlockEntity`, `interaction/HeldTrimTool`, `TrimFaceCondition`, `item/TrimAttachmentItem`, `item/UniversalTrimItem`, `item/ArchSillTrimItem` | The face-trim attachment shared by every host (0.9.0): FRONT/BACK faces, the attachment slot (perimeter; the arch sill since 0.10.0) and its canonical item, two optional looks, the one versioned codec, one store per slot with fail-closed custody of unknown payloads, the one fit/remove/install/restyle transaction on a narrow writable host, the read-only source contract, the per-face (and per-slot) loot condition, item recognition, the Universal Trim item (every trim id), the Arch Sill item (no block) |
| Migration | `migration/LegacyAppearances`, `LegacyContent`, `ChunkMigrations`, `ChunkMigrationQueue`, `LegacyChunkMigration`, `migration/conversion/*` (`LegacyCells`, `Legacy{Door,Trim,Window}Conversion`, `DoorTrimConversion`), `migration/block/*` | Frozen 49-id table, alias and hidden `window_trim` registration, chunk-load discovery and one deduplicated queue (64 entries per level per tick), the 0.4.0 alias policy, silent guarded rewrites; the 0.7.0 fitted frame becomes the door's FRONT trim |
| Shared assembly | `block/assembly/Assembly`, `AssemblyIntegrity`, `AssemblyRemoval`, `AssemblyAnchors`, `AssemblyMutationBatch`, `AssemblyTeardown`, `AssemblyHarvest`, `AssemblyAppearanceTarget`, `placement/AssemblyPlacementPlan`, `AssemblyInstallation`, `AssemblyPlacement` | The narrow contract the door, archway and frame layouts implement (ordered parts, family membership, shared loaded-safe traversal and authored frame); one integrity check and one removal (with a pre-teardown hook); typed anchor lookup and loot fallback; one restyling target; one placement skeleton, installation transaction (all or nothing) and player placement; marks cells being rewritten; silent partner teardown; deferred removal so harvest loot sees the anchor |
| Door adapters | `block/door/adapter/*` (`AssemblyDoorBlock`, `RoundDoorBlock`, `ArchedDoorBlock`, `RoundDoorItem`, `RoundDoorBlockEntity`) | One shared delegating `DoorBlock` adapter (`AssemblyDoorBlock`) with two thin blocks (the round door adds only `column`; the arched door has none), the item placing either design and the one anchor entity (swing, camouflage, appearance, update packet; type id `round_door` serves both) |
| Door appearance / router | `block/door/appearance/DoorAppearance`, `block/door/interaction/DoorInteraction`, `block/door/selection/DoorHitRegion` | Anchor look for picks/drops/targets; restyle-camouflage-toggle precedence; one corner-or-leaf decision |
| Door layout, placement, state, removal | `block/door/assembly/*` (`DoorLayout`, `DoorPlacementPlan`, `DoorPlacement`, `DoorHingePlacement`, `DoorBlockEntities`) | Two or four parts by design (`DoorLayout.Assembly` carries it); `DoorPlacementPlan` is the one read-only placement decision (server placement, showcase and the client preview read it); `DoorPlacement` hands the plan to the shared installation and player placement; removal and integrity are the shared services (the camouflage release is the door's pre-teardown hook) |
| Leaf, selection, camouflage | `block/door/leaf/*` (`DoorDesign`, `DoorGeometry`, `DoorShapes`), `selection/*` (incl. `DoorPlacementOutline`), `camouflage/*` | `DoorDesign` (ROUND, ARCHED) names the outline, columns, width and hardware; leaf geometry and swing timeline per design; resting and swinging outlines (`DoorSwingOutline`, 0.6.1); the pure placement preview outline; camouflage corners per design |
| Trim (compatibility frame) | `block/trim/RoundTrimBlock`, `RoundTrimBlockEntity`, `appearance/TrimAppearance`, `assembly/*`, `geometry/*` | Static ring quarters from older saves with an anchor entity holding the look; no item places them any more |
| Door trim | `block/door/trim/*` (`DoorTrimNbt`, `DoorTrimGeometry` (per design), `DoorTrimTarget`, `DoorTrimActions`), `block/door/assembly/DoorBlockEntities`, `block/door/interaction/DoorInteraction` | The door as a trim host: payload format, per-face rings stored on the door anchor, the host adapter over `TrimFitting`; targeting and fitting in the router ahead of restyle, camouflage and toggle |
| Window | `block/window/pane/*` (`WindowPaneBlock`, `RoundWindowBlock`, `OrientedWindowBlock`, `RoundWindowBlockEntity`, `RoundWindowItem`, `WindowAppearanceData`), `placement/*` (`WindowOrientation`, `WindowPosition`, `WindowTargetZones`, `WindowPlacement`), `geometry/*` (`WindowPose`, `WindowFaceIdentity`, `WindowDepth`, `WindowPaneGeometry`, `WindowShapes`, `WindowSurfaceGeometry`), `geometry/face/*` (`WindowProfile`, `WindowArchGeometry`, `WindowCornerGeometry`, `WindowEdgeGeometry`, `WindowPattern`, `WindowMuntinGeometry`, `WindowDesign`), `geometry/trim/*` (`WindowTrimGeometry`, `WindowSillGeometry`, `WindowTrimSurfaces`, `WindowTrimUv`, `WindowTrimLayout`), `interaction/*` (`WindowHitRegions`, `WindowHitTarget`, `WindowUseRules`, `WindowInteraction`), `trim/*` (`WindowTrimNbt`, `WindowSillNbt`, `WindowTrimActions`, `WindowAttachmentState`), `glass/*` (`WindowGlassNbt`, `WindowGlassStacks`, `WindowGlassActions`), `connection/WindowConnections` | Pane (round, arch, corner or edge; plain or cross beams; posed by orientation/depth/position, `face_reversed` for face identity) with a per-window frame look and glass colour handed to its chunk model as model data; the logical pieces painted into visible surfaces; the attachments (trim and sill payloads, slot-aware host adapter, bands and sills per face); the shared hit classification and the pure routing table behind the click router; zone classifier and placement; junction and connection rules |
| Client materials | `client/material/DonorSprites`, `DonorColors`, `BuiltinWoodStyles`, `PaintStyles`, `MasonryStyles`, `GlassStyles`, `ModelRetexturing`, `AppearanceModelCache` | Representative donor sprites, sampled tints, wood, paint and glass (clear and stained) artwork bindings, masonry faces (thin-mortar door ring, window faces, the square icon face), `TrimLooks` (one trim look resolver for rings, window trims and the icon), immutable quad transforms (also by position UVs), bounded caches cleared together on reload |
| Door rendering | `client/door/RoundDoorRenderer`, `DoorTrimRenderer`, `DoorTrimModels`, `DoorAppearanceModels`, `DoorLeafModels`, `DoorPlacementPreview`, `DoorParticles`, `DoorTargetingEvents`, `DoorHighlightRenderer` | BER drawing the appearance leaf (panel retextured/tinted, brass untouched; caches keyed by design); `DoorTrimRenderer` draws the face casings before the swing transforms: the round ring from the frame's turned quarter models, the arched casing from `DoorTrimModels` (built from the design's outline); `DoorPlacementPreview` draws the plan for either door |
| Trim / window / item rendering | `client/trim/*`, `client/window/*`, `client/window/trim/*`, `client/render/OutlineLines`, `client/item/AppearanceItemOverrides`, `client/item/TrimItemModels` | Anchor BER tessellating turned quarter models; the window chunk model (frame look and glass colour, pane only); `WindowTrimRenderer` drawing trims and sills from cached `WindowTrimWorldModels`; item cache keyed by model, wood and glass (`WindowItemModels`), the attachment icons per stack (every trim id, the Arch Sill); placement preview and overlay (`WindowPlacementPreview`, `WindowPlacementOverlay`); per-stack item models |
| Archway rendering (0.12.0) | `client/archway/ArchwayRenderer`, `ArchwayModels`, `ArchwayPlacementPreview`, `client/render/PixelSlabModels` | The anchor BER drawing each cell's frame and casing pieces with world light; frame, casing and styled item models baked from the design's mask in a trim look (cache keyed by design, layer, cell, facing, look); the preview drawing the plan's frame outline and cells. `PixelSlabModels` is shared with `client/door/DoorTrimModels` |
| Viewers | `client/compat/emi/*`, `client/compat/jei/*` | Optional, annotation-discovered, never loaded without the viewer |
| Showcase | `showcase/*`, `showcase/section/*`, `showcase/section/archway/*`, `showcase/section/door/*`, `showcase/section/window/*` | Op-only command; `ShowcaseSamples` (0.11.0) is the one sample set and sign label shared by canvas and GameTest checks; sections place canonical blocks through production paths with explicit appearances; `ShowcaseWindow` lists each window (design, pose, frame coordinates, looks) so building and the GameTest check read the same list |
| Datagen | `data/**`, `data/client/door/*` (`DoorModelProvider`, `DoorItemModelProvider`: one leaf template per design and hinge), `data/client/window/*`, `data/client/trim/*` | Oak source models + empty world models, window models as visible surfaces (`WindowModelProvider`, `OrientedWindowModelProvider`, `WindowPaneModelWriter`, `GeometryModelWriter.addSurfaces`; 302 pane-only window block models, each state one complete model, no trimmed variants), item providers (`WindowItemModelProvider`, `trim/AttachmentItemModelProvider`), the frame (`trim/TrimModelProvider`), display transforms (`ItemModelTransforms`), alias models (`LegacyModelProvider`), alias compatibility resources, the archways' empty world models and geometry item models (`archway/ArchwayModelProvider`, `ArchwayItemModelProvider`, 0.12.0), 32 recipes (`recipe/DoorRecipes`, `WindowRecipes`, `ArchwayRecipes`, `TrimRecipes` under the `RoundwoodRecipeProvider` coordinator), loot (`AttachmentLootPools`: one pool per face and slot for windows, doors and archways), tags, names |
| Texture tools | `tools/generate-textures.mjs`, `tools/textures/*` | Orchestrator plus artwork modules (palettes append only, so old bytes never change); `stained-glass.mjs` writes the sixteen `window_glass_<dye>` textures, `window-trims.mjs` the thin-mortar trim faces and the clearer glass (`clearWindowGlass`), `masonry/square-trims.mjs` the Universal Trim's square masonry face, `masonry/window-shape-trims.mjs` the 0.10.0 top-corner, single-edge and sill faces, `masonry/door-casings.mjs` the 0.11.0 arched door casing faces, `masonry/archways.mjs` the 0.12.0 archway faces (radial voussoirs, jamb courses, keyhole corbels), `masonry/mortar.mjs` the shared thin-mortar primitives (joint coverage, the one parameterised polar brick position, brick and mortar colours), `masonry/arch-window.mjs` the arched window face and its course mortar |

## Dependency direction

`showcase` -> `block/*`, `registry`, `appearance`. `registry` -> `block/*`, `migration`. `migration`
-> `block/*`, `registry`, `appearance`. `block/*` -> `appearance`, `geometry`. `appearance` imports
no block, registry or client code. `client` -> `block/*`, `appearance`, `registry`, `migration`
(model wrapping covers the aliases). `client/compat/*` -> everything above plus the viewer APIs;
nothing imports them. `data` -> everything above. `block/door`, `block/window`, `block/trim` and
`block/archway` never import each other (the archway reads only `block/attachment` and
`block/assembly`; `client/archway` reads no door or window client code, only `client/render` and
`client/material`). `TrimPlacement` no longer reads doors; `UniversalTrimItem` takes a plain `Block`,
so `block/attachment` does not import `block/trim`. `block/door` and `block/window` both import
`block/attachment` (and its subpackages); `block/attachment` imports no host, and
`block/attachment/item` imports only `block/attachment` and `appearance`. Inside `block/attachment`, the root `AppearanceAnchorBlockEntity` reads `storage`, `host` reads the root anchor (the anchored view) and `storage`, and `interaction` reads `item` and `appearance`; the root imports no other subfolder. `block/assembly` reads `block/attachment/AppearanceAnchorBlockEntity` (the installation and the restyling target set the look on an anchor) and nothing of any host family. `client/door/DoorTrimRenderer` imports `block/trim/assembly/TrimPart` and
`client/trim/TrimAppearanceModels`; `migration/conversion` reads `block/door` and `block/trim`. Inside
`block/door`: `appearance` and `interaction` read `assembly`, `selection` and the
adapters; `assembly` does not import `selection` (the preview outline lives in `selection` and reads the plan's assembly); `assembly/DoorPlacement` writes the appearance straight to the anchor entity (no import of
`door/appearance`, keeping that pair acyclic). Inside `block/window`: `connection` reads
`WindowPaneBlock.pose` of neighbours and the block calls the connection rule (a deliberate two-way
edge); `geometry` and `placement` import no block adapter (`WindowShapes` is the only geometry
class that touches Minecraft shapes). `geometry/face` and `geometry/trim` build on `geometry` and
are read by `interaction`, `trim`, `glass` and the client (since 0.10.0 only `WindowShapes` in
`geometry` imports `geometry/trim`, to compose the selection; the pane geometry does not); `interaction` calls `trim` and `glass` (the
item no longer imports `interaction`). `client/window` imports `block/door/selection/DoorHitResolver`
for the placement preview (a refused projected-door hit shows nothing); `client/door` imports `block/door/selection` for the door placement preview. The door imports `client`
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
- Doors, archways and the frame share the `block/assembly` contract and services (lifecycle, anchor lookup, restyling target, placement skeleton and installation); each keeps its typed layout, part identity, state construction, bounds and placement policy. The frame adopted it rather than being frozen (dk-992020e6).
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
- Window trim fitting moved from the item into the window router (0.8.0), and door trim fitting into
  the door router (0.9.0): no trim item imports a host; the bridge asks the block's router
  (`claimsUse`), not the item.
- 0.9.0 extracted `FaceTrimStore`, `TrimFitting`, `TrimHost` and `TrimSlot` into `block/attachment/`
  (dk-08d20a47, dk-f9d8d245, done): the door and window entities and actions are thin adapters. The
  plan's `migration/item/LegacyTrimItems` was not created: the one hidden `window_trim` registration
  sits in `LegacyContent` beside the other compatibility items, so `migration/` does not grow
  another single-file folder (dk-aef47f84). `WindowTrimTarget` and `DoorTrimTarget.of` folded into
  `TrimFitting.target`. `data/client/trim/` holds the frame provider (moved from `data/client/`) and
  the icon provider; the 0.8.0 trim item providers are gone.
- 0.10.0 moved the six window adapters to `block/window/pane/` (dk-846bea79, done; `block/window/` is
  now grouping only, 7 entries), the trim rendering to `client/window/trim/`, the window geometry tests
  to `geometry/face/` and `geometry/trim/` (dk-c28677ae, done), and the pane/attachment selection
  composition out of `WindowPaneGeometry` into `WindowShapes` (dk-90159902, done). The window
  showcase sections moved to `showcase/section/window/` (part of dk-0dc49165). Recipes split by family
  under `data/server/recipe/`. The sill classes live with the window host (`block/window/trim/`,
  `geometry/trim/`) because `block/attachment/` is at its cap; only the item is in
  `block/attachment/item/`. There is no `WindowSillActions`: `WindowTrimActions` is slot-aware. The
  gameTest `block/window/` folder holds 11 classes plus `trim/` (the trim and sill host tests).
  `UniversalTrimModels` became `client/item/TrimItemModels` and `UniversalTrimItemModelProvider`
  `AttachmentItemModelProvider` (both serve the Universal Trim and the Arch Sill).
- 0.11.0 extracted the shared door adapter into `block/door/adapter/` (`AssemblyDoorBlock`; the round and
  arched doors are thin subclasses; `block/door/` is now grouping only) and made the design explicit
  (`DoorDesign`, `DoorLayout.Assembly.design`) with one read-only `DoorPlacementPlan`. The door
  showcase sections moved to `showcase/section/door/` with a sibling gameTest check
  (`ArchedDoorsChecks`). `ArchedProfile` and `PixelSlab` sit flat in `geometry/` (10 entries) instead of
  the plan's single-file `geometry/aperture/`. `tools/textures/` is now 13 modules, over the 12-file
  fan-out (covered by dk-1af2b67e). Tradeoff: two casing render paths until the round ring also moves
  to `DoorTrimModels` (the arched casing is built from geometry, the round ring keeps the frame
  quarter models so its verified look is untouched).
- 0.12.0 added `block/archway/` with the plan's `assembly/`, `geometry/`, `appearance/` and `trim/`
  packages (the adapters at its root, 7 entries) and `client/archway/` (renderer, models, preview; the
  plan's `ArchwayModelNames` was not needed: the item and world models are baked from geometry, and
  the only shared name is the design id). The arched door casing's model baking moved to
  `client/render/PixelSlabModels` now that the archway frame and casings are a second caller. No
  `client/render/AssemblyPlacementOverlay`: the door preview draws swing edges and the archway preview
  voxel-shape outlines, so only `OutlineLines` is shared. Showcase placement helpers stayed on
  `ShowcaseCanvas` (one `archway` method) rather than the plan's `showcase/placement/` package, as in
  0.11.0. `tools/textures/masonry/` takes the contour masonry modules (three moved, bytes
  unchanged; `mortar.mjs` and `arch-window.mjs` joined it in the round-1 fixes), bringing `tools/textures/` back under its fan-out cap (part of dk-1af2b67e). The archway
  lifecycle (`ArchwayIntegrity`, `ArchwayRemoval`, `ArchwayBlockEntities`, `ArchwayAppearance.target`)
  was a third copy of the door/frame pattern, as the plan chose; recorded on dk-992020e6 and since
  merged into `block/assembly` (below).
  `AppearanceDrops.decorate(drops, item, look)` exists because an archway is a trim-family object whose
  loot also holds Universal Trims: decorating by family would paint the archway's look onto its trims.
- The assembly refactor (0.12.0, dk-992020e6, dk-ff89f8e5) added the `Assembly` contract and its services to `block/assembly/` (seven files plus `appearance/` and `placement/`: nine entries) and split `block/attachment/` by responsibility into `storage/`, `host/`, `entity/` and `interaction/` beside `item/` and `loot/` (four root files: ten entries). The round-2 review then folded the one-file `entity/` and `loot/` back into the root (six root files plus `storage/`, `host/`, `interaction/`, `item/`: still ten entries) and the one-file `block/assembly/appearance/` into `block/assembly/`; `migration/` grouped its five conversion classes under `conversion/` (`trim/` is gone: seven entries). The six per-family integrity/removal copies are gone; `DoorBlockEntities`, `ArchwayBlockEntities`, `DoorPlacementPlan` and `ArchwayPlacementPlan` stay as typed facades. Window summary bookkeeping moved out of `WindowTrimActions` into `WindowAttachmentState`.
- `RoundWindowArchBlock` became `OrientedWindowBlock` (arches and corners); property names and
  values are unchanged. `MasonryStyles` was extracted from `PaintStyles`, which keeps paint only.

## Structural debt (baseline review 2026-10-21)

The first full review found no correctness problems. The debt is tracked in Docket, one line each;
`migration/` was also over the directory breadth cap; round 2 grouped it (dk-aef47f84, done).

- dk-992020e6 done: one `Assembly` contract and shared lifecycle, anchors, placement and anchor persistence (the frame adopted it)
- dk-ff89f8e5 done: `HeldTrimTool` classifies the acting hand for the three trim routers
- dk-73e57897 look resolver duplicated across the door and window pane models (the trim part is shared since 0.9.0, `TrimLooks`)
- dk-3d456d06 `generate-test-structures.mjs` mixes NBT writer, builder and fixtures
- dk-1af2b67e texture modules split by release, not responsibility; partly done (`masonry/` grouping, `mortar.mjs` and `arch-window.mjs`, `clearWindowGlass` moved); left: `trimMarker` in its own module, `radial-bricks.mjs` on the shared polar, one glass-streak mask
- dk-aef47f84 done: `migration/` grouped under `conversion/` (seven entries), no-growth single-file folders folded, the per-family `appearance/` and `interaction/` pairs declared deliberate
- dk-7279982a done: GameTest `block/door/` and `block/window/` grouped, tree diagram in Layout, one-file folders declared
- dk-0dc49165 done in 0.11.0 (door sections grouped, family checks beside the sections, `ShowcaseSamples`)
- dk-7fd38157 client tidy-ups; partly done (`client/item/FamilyItemModels` replaced the `wrapModels` ladder); left: `QuadVertices`, the door item tint, the camouflage LRU, `WindowItemModels`
- dk-7abe21de Direction quarter-turn helper copied three times
- dk-75a60754 dependency direction (block -> client import, registry <-> migration cycle, trim item on frame concepts)
- dk-4cf186bd `LegacyAlias` contract
- dk-1d129342 test duplication
- dk-d402bd77 shared appearance-family wiring (P3, only if a fourth family appears)
- dk-87f3ad8f TrimFaceCondition <-> ModLootConditions registry cycle
