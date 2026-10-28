# STRUCTURE — Roundwood

<!-- Last full review: 2026-10-21 -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

Every code home is declared below as a full path (the placement hook reads these bullets); what each
component does and which tests guard it is in [Components](#components). `…/` in the tree stands for
`src/main/java/io/github/dirtyredz/roundwood/`.

```
.github/{workflows,actions/setup-mod-build}   .ci/{mod.json,scripts}   gradle/   docs/{releases,items}
tools/{textures/{glass,masonry},structures,test-world/datapack}
src/main/java/…/
  appearance/{material,item,interaction}
  block/
    assembly/{placement,render}                 shared multi-cell contract, services, static-render port
    attachment/{storage,host,interaction,item}  face trims shared by every host, anchor entity
    arch/{adapter,appearance,assembly,geometry,trim}
    door/{adapter,appearance,assembly,camouflage,frame/fitting,hardware,interaction,leaf,selection,trim}
    archway/{appearance,assembly,geometry,trim}  trim/{appearance,assembly,geometry}
    window/{pane,geometry/{face,trim},glass,interaction,placement,trim}
  client/
    render/world   arch/archway   door/{arch,hardware,leaf,preview}   material/tint
    trim   window/trim   item   mixin   compat/{emi,jei}
  data/{client/{arch,archway,door,trim,window},server/recipe}
  geometry/   migration/{block,conversion,item,retired}   registry/   showcase/section/{arch,door,window}
src/generated/resources/   src/main/resources/
src/test/java/…      mirrors main (pure JUnit) + testsupport/
src/gameTest/java/…  mirrors main (Forge GameTests) + fixture/, environment/
```

Per-family `appearance/` and `interaction/` folders (door, archway, window, trim) are deliberate:
every family keeps the same sibling set so a reader finds the same thing in the same place.

- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/appearance/` — the appearance value, family acceptance, defaults contract, versioned NBT, the appearance-only anchor contract, pure restyle rules, bounded samples
- `src/main/java/io/github/dirtyredz/roundwood/appearance/material/` — server-safe donor recognition: tagged planks, the fixed four masonry materials, block finishes
- `src/main/java/io/github/dirtyredz/roundwood/appearance/item/` — appearance on stacks, the shared family block item (tooltip, alias rules), loot decoration
- `src/main/java/io/github/dirtyredz/roundwood/appearance/interaction/` — gesture recognition, validated server mutation and payment, the narrow sneak-use event bridge
- `src/main/java/io/github/dirtyredz/roundwood/block/` — the block families and what they share (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/` — what the multi-cell families share: the `Assembly` contract, integrity, removal, typed anchor lookup, the mutation batch, partner teardown, deferred harvest, the whole-assembly restyling target
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/placement/` — typed read-only placement plans, the installation transaction, full-NBT write rollback, captured multi-block placement, player placement, shared face depth bands
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/render/` — immutable static-render snapshots, publication ordering and the family ports (`StaticRenderBlock`, `AssemblyRenderExtras`); server-safe
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/` — the face-trim attachment shared by every host: face, slot, two-face value, read-only source contract, the shared anchor entity (`AppearanceAnchorBlockEntity`), the `roundwood:trim_face` loot condition
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/storage/` — the versioned two-face codec and the per-slot store (unknown-payload custody)
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/host/` — the narrow writable host view and the one fit/remove/install/restyle transaction
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/interaction/` — the held-item classifier every trim router reads
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/item/` — the attachment items: recognition contract, Universal Trim, Arch Sill and Cross Beams
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/` — the modular arch pieces (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/adapter/` — block/item callbacks, the thin-piece block, the `arch_piece` anchor entity and `ArchDoorFitting` (the bootstrap-bound port to door fitting)
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/appearance/` — typed anchor lookup, whole-piece appearance targets, face-versus-reveal gesture routing
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/assembly/` — variable cell identity, thin depth, layouts, targeting and placement proposals over shared assembly services
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/geometry/` — product dimensions and thickness, semicircle and shoulder profiles, cell-clipped body/surface geometry, masonry joints, shapes, legal icon translation
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/trim/` — modular casing geometry, payload, targeting and the host adapter over shared attachment services
- `src/main/java/io/github/dirtyredz/roundwood/block/door/` — the door family (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/adapter/` — Minecraft adapters: `AssemblyDoorBlock` and the thin design blocks, the item, the anchor entity, its static-render extras, `DoorUseChannel`
- `src/main/java/io/github/dirtyredz/roundwood/block/door/appearance/` — reading, targeting and dropping the door's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/door/assembly/` — the door layout (design-sized parts), hinge choice, depth, placement policy and typed plans
- `src/main/java/io/github/dirtyredz/roundwood/block/door/camouflage/` — corner geometry and coverage, accepted materials, stored data, interaction, refunds
- `src/main/java/io/github/dirtyredz/roundwood/block/door/frame/` — fitted arch tops: compatibility table, attachment value, versioned store, clearance, fitted volumes/surfaces, hit routing, loot
- `src/main/java/io/github/dirtyredz/roundwood/block/door/frame/fitting/` — fitting a door and an arch top together: click entry, the two proposal directions and their shared validated proposal
- `src/main/java/io/github/dirtyredz/roundwood/block/door/hardware/` — typed spear/knob hardware: value, slots, geometry, versioned store, checked stacks, donors
- `src/main/java/io/github/dirtyredz/roundwood/block/door/interaction/` — the right-click router, the hardware proposal and interaction, projected-leaf use
- `src/main/java/io/github/dirtyredz/roundwood/block/door/leaf/` — the four door designs, panel geometry, collision shapes, the shared swing timeline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/selection/` — target and outline shapes, nearest-door raycast, hit re-attribution, corner-or-leaf hit region, the placement outline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/trim/` — the door as a trim host: payload, per-design casing geometry, targeting, host adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/` — frozen archway migration sources (0.12.0): the block, its anchor entity, the item
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/appearance/` — the archway's look through its anchor and its right-click router
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/assembly/` — the four-cell archway: part identity, layout, placement policy and plans
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/geometry/` — the two designs' profiles, frame boxes and cached per-cell shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/trim/` — the archway as a trim host: payload, casing geometry, targeting, host adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/` — the compatibility frame from older saves: the block and its anchor entity (the `round_trim` item is the Universal Trim)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/appearance/` — the frame's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/assembly/` — the four-quarter frame: part identity, layout, player-less placement
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/geometry/` — the ring's cached per-quarter selection shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/window/` — the window family (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/window/pane/` — pane blocks, their entity (frame look, glass, trim, sill and beam stores), items, model data, restyle and drops, the retired-cross port
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/` — pure geometry every design shares: pose, depth, face identity, pane panels, shapes, culling footprints, visible surfaces
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/face/` — one face: profiles, arch/corner/edge glazing, cross-beam pattern and geometry, the runtime design value
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/trim/` — perimeter bands, the arch sill, their surfaces, UVs and masonry layout per profile
- `src/main/java/io/github/dirtyredz/roundwood/block/window/glass/` — glass colour payload, stacks, the dye action
- `src/main/java/io/github/dirtyredz/roundwood/block/window/interaction/` — hit regions, hit target, the pure routing table, the router, the beam fit plan
- `src/main/java/io/github/dirtyredz/roundwood/block/window/placement/` — face-zone classifier, use-context adapter, round position and arch pose
- `src/main/java/io/github/dirtyredz/roundwood/block/window/trim/` — the window as an attachment host: trim, sill and beam payloads, slot-aware adapter, reconcile
- `src/main/java/io/github/dirtyredz/roundwood/client/` — client-only mod-bus wiring (`RoundwoodClient`), the composition root that binds each family's static model source
- `src/main/java/io/github/dirtyredz/roundwood/client/render/` — family-neutral bakers and drawing: surface quad baker, general quad models, pixel-slab models, render separation offsets, outline lines, placement highlight input
- `src/main/java/io/github/dirtyredz/roundwood/client/render/world/` — the static chunk model: `StaticWorldModel`, the `StaticModelSource` port and its bindings, `CellLayers`, worker-safe model data, the snapshot client lifecycle, quad translation, the breaking-data marker
- `src/main/java/io/github/dirtyredz/roundwood/client/arch/` — arch-piece meshes (conforming topology, masonry), cell models, the arch-piece static source and placement preview
- `src/main/java/io/github/dirtyredz/roundwood/client/arch/archway/` — compatibility archway models, its static source and placement preview
- `src/main/java/io/github/dirtyredz/roundwood/client/door/` — door appearance models, particles, the door static source, casing models and camouflage corner models
- `src/main/java/io/github/dirtyredz/roundwood/client/door/arch/` — fitted-arch cell models and the fitting preview
- `src/main/java/io/github/dirtyredz/roundwood/client/door/hardware/` — hardware models and the hardware preview with its render-only cache
- `src/main/java/io/github/dirtyredz/roundwood/client/door/leaf/` — the animated leaf renderer (the only assembly BER), leaf models and names, per-anchor leaf state, donor surface panels
- `src/main/java/io/github/dirtyredz/roundwood/client/door/preview/` — door placement preview, targeting events, highlight, the current-target memo
- `src/main/java/io/github/dirtyredz/roundwood/client/trim/` — the compatibility frame's static source and its ring quarter models and names (shared with datagen)
- `src/main/java/io/github/dirtyredz/roundwood/client/window/` — appearance-aware window block/item model (frame, glass, beams, culling), beam and placement previews, overlay
- `src/main/java/io/github/dirtyredz/roundwood/client/window/trim/` — cached chunk models and looks per slot and profile for trims and sills
- `src/main/java/io/github/dirtyredz/roundwood/client/item/` — NBT-aware item overrides, typed family item-model binding, attachment icons
- `src/main/java/io/github/dirtyredz/roundwood/client/material/` — donor sprites, built-in wood, paint, glass and masonry artwork, look resolvers, bounded model caches
- `src/main/java/io/github/dirtyredz/roundwood/client/material/tint/` — donor colours, stable tint bindings and block-colour routing, quad retexturing and raw vertex maths
- `src/main/java/io/github/dirtyredz/roundwood/client/mixin/` — the client-only breaking-overload model-data mixin
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/emi/` — optional EMI adapter: alias removal, appearance comparison key
- `src/main/java/io/github/dirtyredz/roundwood/client/compat/jei/` — optional JEI adapter: subtypes, hidden aliases
- `src/main/java/io/github/dirtyredz/roundwood/data/` — datagen provider registration
- `src/main/java/io/github/dirtyredz/roundwood/data/client/` — shared model writers, display transforms, alias models, language
- `src/main/java/io/github/dirtyredz/roundwood/data/client/arch/` — modular arch empty world models and full-shape item geometry
- `src/main/java/io/github/dirtyredz/roundwood/data/client/archway/` — archway blockstates (empty world models) and item models
- `src/main/java/io/github/dirtyredz/roundwood/data/client/door/` — door blockstates, leaf models and item models per design
- `src/main/java/io/github/dirtyredz/roundwood/data/client/trim/` — the frame's blockstate and models, attachment item models
- `src/main/java/io/github/dirtyredz/roundwood/data/client/window/` — window visible-surface models, blockstates and item models
- `src/main/java/io/github/dirtyredz/roundwood/data/server/` — recipe coordinator, loot (with attachment pools) and tag providers
- `src/main/java/io/github/dirtyredz/roundwood/data/server/recipe/` — recipes per family, including metadata-preserving arch thinning
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, the ring profile, arched profile, pixel-slab masks, tiled UVs, voxel-shape conversion
- `src/main/java/io/github/dirtyredz/roundwood/migration/` — frozen 0.4.0 mappings, alias registration, chunk-load scheduling and the shared queue
- `src/main/java/io/github/dirtyredz/roundwood/migration/block/` — deprecated alias and retired block adapters
- `src/main/java/io/github/dirtyredz/roundwood/migration/conversion/` — per-family conversions and their shared cell rewrites
- `src/main/java/io/github/dirtyredz/roundwood/migration/item/` — retired stack adapters, archway stack plans and checked crossed-window placement
- `src/main/java/io/github/dirtyredz/roundwood/migration/retired/` — retired-content compatibility: frozen archway identities, archway stack migration hooks, the crossed-window identities and handler
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration of blocks, items, block entities, creative tab, tags, loot condition and recipe serializers
- `src/main/java/io/github/dirtyredz/roundwood/showcase/` — the `/roundwood showcase` command, builder, frame, canvas, spacing, window list and section registry
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/` — the pieces row and materials wall
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/arch/` — arch pieces and compositions (all fitted samples)
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/door/` — door attachment, fitted and hardware examples
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/window/` — window configurations and attachment examples
- `src/main/resources/` — mods.toml, pack.mcmeta, client mixin config, the generated original textures (checked in)
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests mirroring main's packages, plus `testsupport/` (bootstrap shim, baked-face checks, optional Embeddium class-load) and the root `DependencyDirectionTest`
- `src/test/resources/` — FPS census and fitted-corpus fixtures
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests mirroring main's packages, plus `fixture/` (production-path builders, `fixture/assembly/` family adapters) and `environment/` (startup rule, flat-world guard, navigation repeats); never in the release jar
- `src/gameTest/resources/` — GameTest structure templates and test-only tags
- `.github/` — synced workflows and the `actions/setup-mod-build/` Java/Gradle composite
- `.ci/` — per-mod `mod.json` plus synced Node `scripts/` for verification, assets, GameTest completion and publication
- `gradle/` — wrapper, shared test-world staging and the final-jar guard (emits `build/ci/release.json`)
- `tools/` — deterministic generators for textures, the logo and GameTest structures (not packaged)
- `tools/textures/` — PNG encoding, noise, palettes, historical artwork, boards, markers, shared `mortar.mjs`
- `tools/textures/masonry/` — radial, arch, square, casing, sill and archway masonry faces
- `tools/textures/glass/` — historical, clear and stained glazing with one shared streak mask
- `tools/structures/` — NBT writer, structure builder and independent historical fixtures
- `tools/test-world/datapack/` — canonical flat-world override, only used in GameTest saves
- `docs/` — architecture, decisions, features, gotchas, roadmap, `releases/` bodies and Docket `items/`

## Components

| Component | Files | Responsibility |
|---|---|---|
| **CI/release verification** | `.github/`, `.ci/`, `gradle/`, `docs/releases/` | Shared hosted verify, GameTest completion, jar isolation, manifest, assets and draft readback |
| Appearance | `appearance/Appearance`, `AppearanceFamily`, `AppearanceDefaults`, `AppearanceNbt`, `AppearanceChanges`, `AppearanceSamples` | Immutable look (wood/masonry + optional dye), which family takes what, a block's/item's default, the one NBT form, restyle rules, bounded examples |
| Materials | `appearance/material/PlankMaterials`, `MasonryMaterials`, `SurfaceMaterials`, `DonorStacks` (package-private) | Plank donors by tag (+ `roundwood:appearance_planks`), the four masonry blocks, the exact block-finish allowlist and wax normalization; a stack carrying placed contents or a non-default state is never a donor |
| Items | `appearance/item/AppearanceStacks`, `AppearanceBlockItem`, `AppearanceTooltip`, `AppearanceDrops` | Normalised stack NBT, the family item (tooltip, alias name, no block-map claim), loot decoration |
| Gestures | `appearance/interaction/AppearanceGestures`, `AppearanceActions`, `AppearanceUseEvents`, `AppearanceUseTarget` | Recognise sneak+planks/dye/masonry; one checked mutation and payment; let sneak gestures reach the block |
| Registry | `registry/*`, `migration/LegacyContent`, `migration/retired/CrossWindowCompatibility` | 27 canonical/compatibility blocks, 29 advertised items; 13 retired crossed blocks/items, two retired archways, 49 frozen aliases and hidden `window_trim`; 91 registered blocks, 94 registered items. Window entity valid for 14 plain + 13 retired + 15 alias panes. Gameplay tags retain all 27 window IDs. `ModRecipeSerializers` registers the arch-thinning serializer |
| Shared assembly | `block/assembly/Assembly`, `AssemblyIntegrity`, `AssemblyRemoval`, `AssemblyAnchors`, `AssemblyMutationBatch`, `AssemblyTeardown`, `AssemblyHarvest`, `AssemblyAppearanceTarget`, `placement/AssemblyPlacementPlan`, `AssemblyInstallation`, `AssemblyPlacement`, `AssemblyWriteTransaction`, `AssemblyCapturedPlacement`, `FaceDepthBands` | The narrow contract door, arch-piece, archway and frame layouts implement; one integrity check and one removal; typed anchor lookup and loot fallback; one restyling target; one placement skeleton and installation transaction; full-NBT write rollback including unchanged-state cells; Forge snapshot capture with the multi-place event for fitting; the shared aimed-face depth bands for doors and thin arches |
| Static-render snapshot | `block/assembly/render/AssemblyRenderSnapshot`, `AssemblyRenderPublication`, `AssemblyRenderUpdates`, `StaticRenderBlock`, `AssemblyRenderExtras`, `block/door/adapter/DoorRenderExtras`, `client/render/world/AssemblyModelData`, `AssemblyRenderLifecycle` | Immutable anchor snapshots published before the anchor refresh and footprint dirtying; host blocks supply their render assembly through `StaticRenderBlock`, anchors add family values through `renderExtras()` (the door: fitted arch and camouflage donor); workers validate snapshots against their render region only; bounded client-tick membership polling; `src/test/.../client/render/world/AssemblyModelDataTest` and `src/gameTest/.../block/assembly/StaticRenderLifecycleGameTests` guard it |
| Static chunk model | `client/render/world/StaticWorldModel`, `StaticModelSource`, `StaticModelSources`, `CellLayers`, `StaticQuadTransform`, `StaticGeometryModel`, `client/mixin/BreakingModelDataMixin`, family sources `client/door/DoorStaticModel`, `client/arch/ArchPieceWorldModel`, `client/arch/archway/ArchwayWorldModel`, `client/trim/TrimWorldModel` | Forge model wrapper caching complete quad lists by state, side, layer and snapshot; each family's source (bound to its block class in `RoundwoodClient`) supplies cell models, side culling (the frame's ring quarters keep their cull lists) and particle sprite; `CellLayers` composes body plus per-face casings in face order; door depth moves into casing/camouflage vertices; the mixin resolves satellite data for Forge's breaking overload (client-only `roundwood.mixins.json`); no BER fallback. `StaticModelPortTest`, `StaticWorldGeometryTest`, `StaticBreakingModelTest`, `client/arch/ChunkSectionRebuildTest` (replays `src/test/resources/fps/static-census.json`) and `DependencyDirectionTest` (shared static rendering names no family) |
| Attachments | `block/attachment/AttachmentFace`, `TrimSlot`, `FaceTrims`, `TrimAttachmentSource`, `storage/FaceTrimsNbt`, `storage/FaceTrimStore`, `host/TrimHost`, `host/TrimFitting`, `AppearanceAnchorBlockEntity`, `interaction/HeldTrimTool`, `TrimFaceCondition`, `item/*`, `block/window/trim/WindowBeamNbt`, `block/window/interaction/WindowBeamFitPlan` | The face-trim attachment shared by every host: FRONT/BACK faces, the slot (perimeter, Arch Sill, Cross Beams) and its item, two optional looks, one versioned codec, one store per slot with fail-closed custody, one fit/remove/install/restyle transaction on a narrow host, per-face loot condition, the anchor entity's persistence, sync and static-render hooks |
| Arch pieces | `block/arch/adapter/*`, `appearance/*`, `assembly/*` (`ArchPieceCell`, `ArchPieceDepth`, `ArchPieceLayout`, `ArchPieceTargeting`, `ArchPiecePlacement`), `geometry/*` (`ArchPieceDesign`, `ArchPieceThickness`, `ArchTopProfile`, `KeyholeShoulderProfile`, `ArchPieceGeometry`), `trim/*`, `data/server/recipe/ArchThinningRecipe` | Eight full/thin modular pieces on one anchor entity; full/thin slab dimensions and thin poses (`ThinArchPieceBlock` adds only depth; membership includes thickness and depth); targeting clips shoulder wedges against the shared face bands; casings through shared attachment services; metadata-preserving full-to-thin crafting. Tests: `src/test/.../block/arch/{assembly,geometry,trim}/`, `src/gameTest/.../block/arch/` (placement, lifecycle, thinning, historical replacement, walking; `fixture/ArchPieceFixtures`, `fixture/assembly/ArchPieceFamily`) |
| Arch rendering | `client/arch/ArchMeshModels`, `ArchMeshTopology`, `ArchMasonryModels`, `ArchPieceModels`, `ArchPieceWorldModel`, `ArchPiecePlacementPreview` | Complete layers baked before owning-cell quads are returned; shared edge vertices inserted after mortar clipping; cached cell models in one donor on every surface; the preview. `src/test/.../client/arch/` (`ArchWatertightTest` checks baked edge pairing across cells, thicknesses, depths, facings and materials; `FittedCorpusTest` enforces the 9,000-quad fitted budget over `src/test/resources/fps/fitted-corpus.json`) |
| Archway (0.12.0, migration source) | `block/archway/*`, `assembly/*`, `geometry/*`, `appearance/*`, `trim/*`, `client/arch/archway/*`, `client/render/PixelSlabModels` | Frozen 2 x 2 opening in two designs, kept to read old saves until chunk-load conversion; frame and casing chunk models from the design mask in a tiled donor look; the preview draws the plan (retired items are excluded) |
| Migration | `migration/LegacyAppearances`, `LegacyContent`, `ChunkMigrations`, `ChunkMigrationQueue`, `LegacyChunkMigration`, `conversion/*` (`LegacyCells`, `Legacy{Door,Trim,Window}Conversion`, `DoorTrimConversion`, `WindowCrossConversion`, `WindowCellConversion`, `ArchwayConversion`), `block/*`, `item/*` (`ArchwayStacks`, `RetiredCrossWindowItem`, `CrossWindowPlacement`), `retired/*` (`ArchwayCompatibility`, `ArchwayStackMigration`, `CrossWindowCompatibility`) | Frozen 49-id table, alias and hidden `window_trim` registration, chunk-load discovery and one deduplicated queue (64 entries per level per tick), silent guarded rewrites; the 0.7.0 fitted frame becomes the door's FRONT trim; 13 crossed-window identities migrate through checked, lossless cell conversion (`CrossWindowCompatibility` implements the window-owned `RetiredCrossWindows` port); 0.13.3 archways convert atomically into modular pieces and their stacks split losslessly in loaded holders. `tools/structures/archways-v0133.mjs` pins the historical fixture; `src/gameTest/.../migration/` guards conversions |
| Door adapters | `block/door/adapter/*` (`AssemblyDoorBlock`, `RoundDoorBlock`, `ArchedDoorBlock`, `DoubleArchedDoorBlock`, `RoundDoorItem`, `RoundDoorBlockEntity`, `DoorRenderExtras`, `DoorUseChannel`) | One delegating `DoorBlock` adapter with thin design blocks (the double-arched block owns the size-parameterized nine/twelve-cell schema), the item, the one anchor entity (swing, camouflage, hardware, arch custody, appearance, packets) |
| Door appearance / router | `block/door/appearance/DoorAppearance`, `block/door/interaction/DoorInteraction`, `ProjectedDoorUse`, `block/door/selection/DoorHitRegion` | Anchor look for picks/drops/targets; restyle-camouflage-toggle precedence; validated projected-leaf use; one corner-or-leaf decision |
| Door layout, placement, state | `block/door/assembly/*` (`DoorLayout`, `DoorPlacement`, `DoorHingePlacement`, `DoorDepth`) | Parts by design; one read-only placement decision for server, showcase and preview; saved depth offsets on the shared face bands; removal and integrity are the shared services. `src/test/.../block/door/assembly/` guards depth targeting; `src/gameTest/.../block/door/placement/DoorTargetsGameTests` guards item proposals/writes, old-state defaults and nine/twelve-cell lifecycle (`-PdoorTargets`) |
| Leaf, selection, camouflage | `block/door/leaf/*`, `selection/*` (`DoorRaycast`, `DoorTargetShapes`, `DoorSwingOutline`, `DoorPlacementOutline`), `camouflage/*` (`CamouflageCoverage`) | Four designs, leaf geometry and swing timeline; nearest world/open-leaf acquisition shared by client picking and projected server validation (loaded cells only); bounded cached hardware/Boolean selection geometry; the effective ALL/LOWER/NONE camouflage mask used by volumes, hits, outlines and model caches |
| Door hardware | `block/door/hardware/*`, `block/door/interaction/DoorHardwareTarget`, `DoorHardwareInteraction`, `client/door/hardware/*` | Typed spear/knob meshes, the two-slot value, strict versioned storage, checked stack custody and donor recognition; one endpoint proposal shared by preview and payment; models consume baked slot markers before retexturing; the preview cache keys on target, hands, payload, camera and tick revision. Tests mirror geometry, store, targeting and rendering; `src/gameTest/.../block/door/hardware/` checks real rays and custody |
| Door frame (fitted arches) | `block/door/frame/*` (`DoorArchFitPlan`, `DoorArchAttachment`, `DoorArchStore`, `DoorArchClearance`, `DoorArchGeometry`, `DoorArchInteraction`, `DoorArchLoot`), `frame/fitting/*` (`DoorArchFitting`, `DoorArchProposal`, `DoorIntoArchProposal`, `ArchOntoDoorProposal`), `client/door/arch/*` | Compatibility table, immutable custody, strict store, continuous clearance, fitted volumes and render surfaces (reusing the standalone arch builder); frame hit routing and independent trim hosts; component refunds in ordinary loot. Fitting: the entry resolves the clicked source, each click order builds its own proposal, `DoorArchProposal` validates cells, permissions and obstruction; ordinary clicks, the playerless showcase (`fitAt`) and the preview share it. `src/gameTest/.../block/door/frame/` and `frame/fitting/` guard lifecycle, persistence and fitting |
| Trim (compatibility frame) | `block/trim/RoundTrimBlock`, `RoundTrimBlockEntity`, `appearance/TrimAppearance`, `assembly/*`, `geometry/*`, `client/trim/*` | Static ring quarters from older saves with an anchor entity holding the look; no item places them; chunk model keeps the original directional cull lists |
| Door trim | `block/door/trim/*`, `client/door/DoorTrimModels` | The door as a trim host: payload, per-face casings on the anchor, the host adapter over `TrimFitting`; casings baked from the design outline. `FramedDoorFacesTest` checks casing surfaces against panels, hardware and fitted frames |
| Window | `block/window/pane/*`, `placement/*`, `geometry/*`, `geometry/face/*`, `geometry/trim/*`, `interaction/*`, `trim/*`, `glass/*` | Pane (round, arch, corner, edge, blank; plain or cross beams) with frame look and glass colour handed to its chunk model as model data; `WindowDesign.all()` enumerates runtime combinations, `ModBlocks.WindowProducts.all()` the fourteen plain panes and thirteen historical crossed IDs; attachments per face; shared hit classification and routing table; zone classifier and placement |
| Client materials | `client/material/DonorSprites`, `BuiltinWoodStyles`, `PaintStyles`, `MasonryStyles`, `GlassStyles`, `AppearanceLooks`, `TrimLooks`, `AppearanceModelCache`, `tint/DonorColors`, `tint/TintBindings`, `tint/StaticBlockColors`, `tint/ModelRetexturing`, `tint/QuadVertices` | Donor sprites, artwork bindings, masonry faces, look precedence and the trim look resolver, bounded caches cleared together on reload; under `tint/`: sampled colours, session-stable tint indices, donor tint routing through BlockColors at the owning position, quad retexturing and pure vertex maths |
| Door rendering | `client/door/leaf/RoundDoorRenderer`, `DoorLeafRenderState`, `DoorLeafModels`, `DoorSurfaceModels`, `client/door/DoorAppearanceModels`, `DoorStaticModel`, `CamouflageModels`, `DoorParticles`, `client/door/preview/*` | The BER draws only the animated leaf (panel and hardware finishes; caches keyed by design and finishes, per-anchor leaf state by payload/reload generation); the chunk model draws camouflage, casings and fitted arches; legal model-coordinate mapping for paired-door heights; donor panels tiled before compression; placement preview, highlight and current-target memo |
| Window / item rendering | `client/window/*`, `client/window/trim/*`, `client/render/OutlineLines`, `client/item/*` | Window frames, glass and beams in the window's model with attachments in a separate solid pass (`WindowStaticModels`); item caches keyed by source model and looks; placement preview and overlay; per-stack item models and attachment icons |
| Viewers | `client/compat/emi/*`, `client/compat/jei/*` | EMI/JEI: each item shown once; optional, annotation-discovered, never loaded without the viewer |
| Showcase | `showcase/*`, `showcase/section/*`, `section/arch/*`, `section/door/*`, `section/window/*` | Op-only command; one of each piece, the materials wall (fourteen donors), window configurations, every fitted arch, hardware examples, one example per attachment, every sample spaced by `ShowcaseSpacing`; sections place through production paths. `FeedbackShowcaseLayoutTest` guards arena bounds and spacing; `-PshowcaseTargets` selects the showcase GameTests |
| Datagen | `data/**` | Oak source models + empty world models, window models as visible surfaces (pinned by `GeneratedResourceManifestTest`), item providers, the frame, display transforms, alias models, arch and archway models, recipes per family, loot (one pool per face and slot), tags, names |
| Texture tools | `tools/generate-textures.mjs`, `tools/textures/*` | Orchestrator plus artwork modules (palettes append only, so old bytes never change); glass and masonry groups share `mortar.mjs` primitives |

## Dependency direction

`showcase` -> `block/*`, `registry`, `appearance`. `registry` -> `block/*`, `migration`. `migration`
-> `block/*`, `registry`, `appearance`. `block/*` -> `appearance`, `geometry`. `appearance` imports
no block, registry or client code. `client` -> `block/*`, `appearance`, `registry`, `migration`
(model wrapping covers the aliases). `client/compat/*` -> everything above plus the viewer APIs;
nothing imports them. `data` -> everything above.

Shared versus families: `block/assembly` (including `render/` and `placement/`) and `block/attachment`
import no host family; families reach them through ports. The static snapshot asks a block's
`StaticRenderBlock.renderAssembly` and an anchor's `renderExtras()`; `client/render/world` asks a
`StaticModelSource`, and only `RoundwoodClient` (the client composition root) names family classes to
bind them. `client/render` imports no family package; `client/material` reads only
`block/window/geometry/trim/WindowTrimLayout` for masonry faces. `DependencyDirectionTest`
enforces the static-rendering half. `block/arch/adapter/ArchDoorFitting` is the bootstrap-bound port
for door fitting (bound in `Roundwood`), so arch adapters never import `block/door`.

Between families: `block/door`, `block/window`, `block/trim` and `block/archway` never import each
other. `block/door/frame` reads `block/arch` (a fitted top is an arch product); arch reads no door
code. `client/door` imports `client/arch` for the shared arch cell baker and `block/door/selection`
for previews; `client/window` imports `block/door/selection/DoorHitResolver` and
`migration/item/CrossWindowPlacement` for its previews. The door imports `client` only for
`DoorParticles` (client-only call). `migration/conversion` reads `block/door`, `block/trim` and
`block/arch`.

Inside `block/door`: `appearance` and `interaction` read `assembly`, `selection` and the adapters;
`assembly` does not import `selection`; `assembly/DoorPlacement` writes the appearance straight to the
anchor entity. Inside `block/window`: `geometry` and `placement` import no block adapter
(`WindowShapes` is the only geometry class that touches Minecraft shapes); `geometry/face` and
`geometry/trim` build on `geometry`; `interaction` calls `trim` and `glass`. Inside `block/attachment`,
the root anchor reads `storage`, `host` reads the root and `storage`, `interaction` reads `item` and
`appearance`, and `item` imports only `block/attachment` and `appearance`. Geometry, `CamouflageGeometry`,
`DoorSwing`, the appearance rules and `QuadVertices` are plain enough for unit tests without a bootstrap.

The registry/migration cycle is an intended compatibility exception: registry constructs the retired
block and item adapters; migration looks up canonical blocks/items and entity types for lossless
conversion and previews. Registry imports from migration are limited to those adapters by
`DependencyDirectionTest`. Window code imports no migration code: `pane/RetiredCrossWindows` owns the
narrow conversion/drop port; `migration/retired/CrossWindowCompatibility` registers its implementation
in `Roundwood` bootstrap.

## Structural notes

- 0.17.0 review fixes: static rendering reaches families through ports (`StaticRenderBlock`,
  `AssemblyRenderExtras`, `StaticModelSource`; DECISIONS.md 2026-10-28) and `CellLayers` names the
  shared cell composition. `DoorArchFitting` was split into `frame/fitting/` (entry, two proposal
  directions, shared validated proposal) with Forge capture in `AssemblyCapturedPlacement`. Breadth
  was relieved downward: `client/` 12 -> 10 (camouflage into `door/`, archway under `arch/`, the
  one-file `trim/` now also holds the ring models), `client/door/` 17 loose -> 5 files plus
  `arch/`, `hardware/`, `leaf/`, `preview/`, `client/render/` 12 -> 7 (static model under `world/`),
  `client/material/` 13 -> 9 (`tint/`), `migration/` 11 -> 9 (`retired/`), and the one-file
  `block/arch/fitting/` folded into `block/arch/adapter/`.

- 0.5.0 replaced the `variant/` catalogue: material acceptance (`appearance/material`), artwork
  (`client/material`), samples (`AppearanceSamples`) and frozen history (`migration/LegacyAppearances`)
  are separate authorities. The vanilla wood list appears in each on purpose (artwork, samples and
  the frozen table must not share a mutable source).
- The appearance system nests `material/`, `item/` and `interaction/` under `appearance/` so the
  root package stays at ten entries (the plan had them at the root).
- Doors, archways and the frame share the `block/assembly` contract and services (lifecycle, anchor lookup, restyling target, placement skeleton and installation); each keeps its typed layout, part identity, state construction, bounds and placement policy. The frame adopted it rather than being frozen (dk-992020e6).
- `client/door/leaf/DoorLeafModels` and `client/trim/RingModelNames` are pure naming shared by the client
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
  to `geometry/` from `geometry/RingGeometry` for the same reason (frame and door trim).
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
  gameTest `block/window/` folder holds 5 classes plus `design/`, `placement/` and `trim/` (the trim and sill host tests).
  `UniversalTrimModels` became `client/item/TrimItemModels` and `UniversalTrimItemModelProvider`
  `AttachmentItemModelProvider` (both serve the Universal Trim and the Arch Sill).
- 0.11.0 extracted the shared door adapter into `block/door/adapter/` (`AssemblyDoorBlock`; the round and
  arched doors are thin subclasses; `block/door/` is now grouping only) and made the design explicit
  (`DoorDesign`, `DoorLayout.Assembly.design`) with one read-only `DoorPlacement`. The door
  showcase sections moved to `showcase/section/door/` with a sibling gameTest check
  (`ArchedDoorsChecks`). `ArchedProfile` and `PixelSlab` sit flat in `geometry/` (10 entries) instead of
  the plan's single-file `geometry/aperture/`. `tools/textures/` is now 13 modules, over the 12-file
  fan-out (covered by dk-1af2b67e). Tradeoff: two casing render paths until the round ring also moves
  to `DoorTrimModels` (the arched casing is built from geometry, the round ring keeps the frame
  quarter models so its verified look is untouched).
- 0.12.0 added `block/archway/` with the plan's `assembly/`, `geometry/`, `appearance/` and `trim/`
  packages (the adapters at its root, 7 entries) and `client/archway/` (models, preview; since 0.17.0 `client/arch/archway/`; the
  plan's `ArchwayModelNames` was not needed: the item and world models are baked from geometry, and
  the only shared name is the design id). The arched door casing's model baking moved to
  `client/render/PixelSlabModels` now that the archway frame and casings are a second caller. No
  `client/render/AssemblyPlacementOverlay`: the door preview draws swing edges and the archway preview
  voxel-shape outlines, so only `OutlineLines` is shared. Showcase placement helpers stayed on
  `ShowcaseCanvas` (one `archway` method) rather than the plan's `showcase/placement/` package, as in
  0.11.0. `tools/textures/masonry/` takes the contour masonry modules (three moved, bytes
  unchanged; `mortar.mjs` and `arch-window.mjs` joined it in the round-1 fixes), bringing `tools/textures/` back under its fan-out cap (part of dk-1af2b67e). The archway
  lifecycle (`ArchwayIntegrity`, `ArchwayRemoval`, `ArchwayAppearance.ANCHORS`, `ArchwayAppearance.target`)
  was a third copy of the door/frame pattern, as the plan chose; recorded on dk-992020e6 and since
  merged into `block/assembly` (below).
  `AppearanceDrops.decorate(drops, item, look)` exists because an archway is a trim-family object whose
  loot also holds Universal Trims: decorating by family would paint the archway's look onto its trims.
- The assembly refactor (0.12.0, dk-992020e6, dk-ff89f8e5) added the `Assembly` contract and its services to `block/assembly/` (seven files plus `appearance/` and `placement/`: nine entries) and split `block/attachment/` by responsibility into `storage/`, `host/`, `entity/` and `interaction/` beside `item/` and `loot/` (four root files: ten entries). The round-2 review then folded the one-file `entity/` and `loot/` back into the root (six root files plus `storage/`, `host/`, `interaction/`, `item/`: still ten entries) and the one-file `block/assembly/appearance/` into `block/assembly/`; `migration/` grouped its five conversion classes under `conversion/` (`trim/` is gone: seven entries then; nine since 0.17.0, when the archway and crossed-window compatibility moved to `retired/`). The six per-family integrity/removal copies are gone; `DoorAppearance.ANCHORS`, `ArchwayAppearance.ANCHORS`, `DoorPlacement` and `ArchwayPlacement` stay as typed plans and lookup constants. Window summary bookkeeping moved out of `WindowTrimActions` into `WindowAttachmentState`.
- 0.13.1 (owner feedback): `block/window/connection/WindowConnections` was deleted with the glass arm it judged (a window never connects: `WindowPaneBlock.withoutConnections`); `ArchwayShapes` has a second, smaller collision shape; the showcase gained `ShowcaseSpacing` (the one gap rule the sections add up, so no section carries its own pitch) and its section constants now chain from one another along a row (door sections read the window row's constants). `MaterialsSection` is a wall, not archways. The test-side `SpacingChecks` scans the built world rather than the sums.
- `RoundWindowArchBlock` became `OrientedWindowBlock` (arches and corners); property names and
  values are unchanged. `MasonryStyles` was extracted from `PaintStyles`, which keeps paint only.

The structure generator delegates binary encoding to `tools/structures/nbt.mjs`, ordered palettes and cell placement to `structure-builder.mjs`, and each historical build to its own fixture module. The 0.4.0 and 0.7.0 bytes are unchanged; `window-cross-v0120.mjs` records all thirteen retired window IDs independently of Java registration.

## Structural debt (baseline review 2026-10-21)

The first full review found no correctness problems. The debt is tracked in Docket, one line each;
`migration/` was also over the directory breadth cap; round 2 grouped it (dk-aef47f84, done).

- dk-992020e6 done: one `Assembly` contract and shared lifecycle, anchors, placement and anchor persistence (the frame adopted it)
- dk-ff89f8e5 done: `HeldTrimTool` classifies the acting hand for the three trim routers
- dk-73e57897 done: `AppearanceLooks` owns paint / built-in artwork / donor / fallback precedence; callers retain their panel, plank, masonry and tint policies.
- dk-3d456d06 done: `tools/structures/` owns the NBT writer, shared builder and independent historical fixtures; the entry script only writes their output
- dk-1af2b67e done: responsibility groups `glass/` and `masonry/`, one streak mask, separate `trim-marker.mjs`; generated texture bytes unchanged.
- dk-aef47f84 done: `migration/` grouped under `conversion/` (nine entries since 0.17.0 added `retired/`), no-growth single-file folders folded, the per-family `appearance/` and `interaction/` pairs declared deliberate
- dk-7279982a done: GameTest `block/door/` and `block/window/` grouped, tree diagram in Layout, one-file folders declared
- dk-0dc49165 done in 0.11.0 (door sections grouped, family checks beside the sections, `ShowcaseSamples`)
- dk-7fd38157 done: `QuadVertices` owns raw vertex maths; door tint belongs to DoorAppearanceModels; camouflage and window items use AppearanceModelCache directly; GeneralQuadModels shares unculled world model assembly.
- dk-7abe21de done: `VoxelShapeFactory.turn` owns signed Direction quarter turns for model rotation and camouflage; the old datagen copy was already removed.
- dk-75a60754 done: block-entity valid sets come from the shared deferred block register; RingGeometry.Quarter is the neutral owner; RingModels and RingModelNames now live with the frame in `client/trim/` (their only users besides datagen). Forge initializeClient is the deliberate particle boundary (DECISIONS.md).
- dk-4cf186bd done: `migration/block/LegacyAlias` supplies canonical block, historical appearance, conversion and queue identity; family conversions retain their distinct custody rules.
- dk-1d129342 done: `DoorFixtures.placeOakDoor` owns the repeated player/place/assert sequence; callers keep floor setup and player identity. The generated-resource test split was already complete.
- dk-d402bd77 done: family adapters remain explicit; three appearance families and different pick/drop ownership do not justify a wiring abstraction (DECISIONS.md).
- dk-87f3ad8f done: TrimFaceCondition owns TYPE; registration points to the attachment, with no reverse dependency.

- dk-2ce8b225 done: `tools/textures/mortar.mjs` owns polar and brick shading; positions and shading options are named records, shared by historical and thin radial art.

- dk-d1d87975 done: `appearance/AppearanceAnchor`, typed `AssemblyPlacementPlan<P,A>`, per-family `AssemblyAnchors.Lookup` constants and typed item binding replace forwarding facades; window wrapping lives with its model.

- dk-ea090069 done: `PlacementHighlights` owns held-hand precedence and the live-client event guard. Its former BER tessellation helper was removed with the static renderers.

`block/door/camouflage/CamouflageCoverage` owns the effective ALL/LOWER/NONE mask used by corner volumes, hits, outlines and model caches. Fitted round doors retain only y<16; other fitted designs have no visible corners.

`block/door/hardware/DoorHardwareGeometry` owns half-pixel spear straps and fixed-height knobs. `tools/structures/doors-v0160.mjs` records independent historical door states and possessions; DoorFeedbackUpgradeGameTests loads them without replacement.

`client/render/SurfaceSeparation` owns the three render offsets. DoorTrimModels, DoorArchModels and ArchPieceModels expose their render surfaces; geometry classes return authored surfaces. `client/door/arch/DoorArchBakedFacesTest` and `testsupport/BakedCoplanarity` inspect generated leaf and production frame vertices across the supported pose matrix.

`appearance/material/SurfaceMaterials` owns the exact block-finish allowlist and wax normalization. `client/door/leaf/DoorSurfaceModels` tiles donor panels before tall-model compression. SurfaceMaterialsTest, DoorSurfaceModelsTest and SurfaceFinishGameTests cover kind compatibility, UVs, cache distinction and payment.

`showcase/section/door/DoorHardwareSection` adds four bounded component examples beside the fitted row. `HardwareShowcaseChecks` is shared by the focused hardware showcase GameTest and full showcase suite. WindowEdgesSection owns the 3x3 frameless-centre example; MaterialsSection owns the fourteen distinct donor blocks.

- Fitted render surfaces in `block/door/frame/DoorArchGeometry` reuse the standalone
  `ArchPieceGeometry` merged builder. Logical selection volumes stay in the door module.
  `src/test/resources/fps/fitted-corpus.json` pins the diagnostic census's 135 fitted inputs;
  `client/arch/FittedCorpusTest` enforces the production-baked 9,000-quad budget.
