# STRUCTURE — Roundwood

<!-- Last full review: 2026-10-21 -->

Code map. Behaviour: [README.md](README.md). How it works: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

- `src/test/.../showcase/FeedbackShowcaseLayoutTest` guards the fixed arena bounds and new sample spacing. The pieces row retains thirteen framed profiles; the frameless pane is demonstrated in the assembled window, keeping every door on the existing floor. `-PshowcaseTargets` selects the complete showcase GameTest package and its environment guard for local reproduction.

- `block/door/hardware/` owns typed spear/knob meshes, the two-slot value, strict versioned storage, checked stack custody and donor recognition. `block/door/interaction/DoorHardwareTarget` owns the shared endpoint proposal; `DoorHardwareInteraction` revalidates and pays. Selection acquires both ordinary and projected hardware surfaces.
- `client/door/DoorHardwareModels` consumes baked slot markers before retexturing; `DoorHardwarePreview` renders the shared proposal. Door model keys include both slot finishes; item overrides and viewer keys use the complete stack. Hardware unit tests mirror geometry, store, targeting and rendering; `src/gameTest/.../block/door/hardware/` checks real rays and custody.


- `WindowDesign.all()` enumerates runtime profile/pattern combinations. `ModBlocks.WindowProducts.all()` owns products: fourteen plain panes and exactly thirteen historical crossed IDs. The appended BLANK profile uses the existing oriented pane, glass and beam paths; its focused tests live in the window geometry and design test packages.

- `block/door/frame/` owns the fitted-top compatibility table, immutable attachment, strict versioned store, closed-plane compatibility and fitted volumes/surfaces. The same fit proposal drives ordinary clicks, playerless showcase construction and preview.
- `block/assembly/placement/AssemblyWriteTransaction` owns full-NBT rollback for installation and fitting, including unchanged-state cells. `block/arch/fitting/ArchDoorFitting` is the narrow bootstrap-bound port; arch adapters never import the door fitting implementation. `client/door/DoorArchPlacementPreview` draws the shared proposal and refusals.
- `block/door/selection/DoorRaycast` owns nearest world/open-leaf acquisition, shared by client picking and projected server validation. Its bounded scan reads loaded cells only and deduplicates assemblies.
- `client/door/DoorArchRenderer` draws fitted tops as static cell models, using the arch donor/masonry baker. `DoorArchInteraction` owns frame hit routing and independent trim hosts; `DoorArchLoot` appends component refunds during ordinary loot evaluation.
- `showcase/section/arch/ArchCompositionsSection` includes all twelve fitted samples; `FittedShowcaseGameTests` is the bounded section gate.

- `block/arch/geometry/ArchPieceThickness` and `block/arch/assembly/ArchPieceDepth` own full/thin slab dimensions and thin poses.
  `block/arch/adapter/ThinArchPieceBlock` adds only the thin depth property; assembly membership includes thickness and depth.
- `block/assembly/placement/FaceDepthBands` is the shared aimed-face band authority for doors and thin arches.
  `ArchPieceTargeting.regions` clips shoulder wedges against those bands for the placement overlay.
- `data/server/recipe/ArchThinningRecipe` owns metadata-preserving full-to-thin crafting;
  `registry/ModRecipeSerializers` registers its serializer. Focused crafting and thin casing/drop checks
  live in `src/gameTest/.../block/arch/ArchThinningGameTests`.

- `block/door/adapter/DoubleArchedDoorBlock` owns the size-parameterized nine/twelve-cell saved schema;
  `block/door/assembly/DoorDepth` owns saved depth offsets and shared face bands.
- `src/test/java/io/github/dirtyredz/roundwood/block/door/assembly/` guards depth targeting.
- `block/door/trim/FramedDoorFacesTest` checks production casing surfaces against panels, hardware, adjacent arch models and actual fitted full/thin frames.
  `client/door/DoorLeafModels` owns legal model-coordinate mapping for both paired-door heights.
  Leaf/selection tests guard both hinges, three depths, matching arch masks and projected rays.
- `src/gameTest/java/io/github/dirtyredz/roundwood/block/door/placement/DoorTargetsGameTests`
  guards real item proposals/writes, old-state defaults and nine/twelve-cell lifecycle.
  `-PdoorTargets` selects these, arch placement and environment checks for the bounded headless gate.


- `migration/conversion/ArchwayConversion` replaces historical assemblies atomically; `migration/item/ArchwayStacks`
  plans lossless stack splits; `migration/ArchwayStackMigration` owns loaded-holder hooks and overflow.
- `src/gameTest/java/io/github/dirtyredz/roundwood/migration/ArchwayMigrationGameTests.java` guards the historical
  0.13.3 fixture, queued conversion, counts, looks, trims, refusal and idempotence.
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/arch/` owns pieces/compositions;
  `src/gameTest/java/io/github/dirtyredz/roundwood/showcase/section/arch/` checks them in every facing.

- `src/main/java/io/github/dirtyredz/roundwood/data/client/arch/` emits modular empty world models and full-shape item geometry.

- `src/main/java/io/github/dirtyredz/roundwood/block/arch/trim/` owns modular casing geometry, targeting and adapters over shared attachment storage/fitting.
- `src/test/java/io/github/dirtyredz/roundwood/block/arch/trim/` guards whole-mask casing depth and coverage.

- `src/main/java/io/github/dirtyredz/roundwood/client/arch/` owns modular arch preview and rendering.
- `src/test/java/io/github/dirtyredz/roundwood/client/arch/` guards previews and baked models.

- `tools/structures/archways-v0133.mjs` pins historical archway IDs, parts and known
  appearance/trim payloads; `archways_v0133.nbt` is its generated GameTest fixture.
- `src/test/java/io/github/dirtyredz/roundwood/block/arch/geometry/` holds independent
  historical acceptance masks and modular profile/composition tests.
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/geometry/` owns modular
  product dimensions, spring-offset semicircle and full-height shoulder profiles, cell-clipped body/surface geometry
  and shared legal icon translation. Profile, rotated-shape and composition tests guard owner feedback geometry.
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/adapter/` owns the modular
  block/item callbacks and distinct `arch_piece` anchor entity.
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/assembly/` owns variable
  cell identity, layouts, targeting and placement proposals over shared assembly services.
- `src/main/java/io/github/dirtyredz/roundwood/block/arch/appearance/` adapts typed
  anchor lookup, whole-piece appearance targets and face-versus-reveal gesture routing.
- `src/test/java/io/github/dirtyredz/roundwood/block/arch/assembly/` holds pure
  layout, rotation and targeting tests.
- `src/gameTest/java/io/github/dirtyredz/roundwood/block/arch/` verifies modular
  placement, lifecycle and historical replacement migration and modular walking. `fixture/ArchPieceFixtures`
  supplies production-path hits; `fixture/assembly/ArchPieceFamily` adapts variable
  layouts without adding fixed-index assumptions to historical assembly tests.

- `.github/` - synced workflows and `actions/setup-mod-build/` Java/Gradle composite (checkout bootstraps it).
- `.ci/` - per-mod `mod.json` plus synced Node `scripts/` for verification, assets, GameTest completion and publication.
- `gradle/` - wrapper, shared test-world definition/staging and final-jar guard with contamination/staging tests; emits `build/ci/release.json`.
- `tools/test-world/datapack/` - canonical flat-world override, only used in GameTest saves; its path mirrors the root sync source.
- `docs/releases/` - required versioned release bodies.

```
.github/{workflows,actions/setup-mod-build}
.ci/{mod.json,scripts}
gradle/
docs/releases/
tools/test-world/datapack/
src/main/java/.../roundwood/
  appearance/{material,item,interaction}   the look: value, donors, stacks, gestures
  migration/{block,conversion,item}             frozen 0.4.0 aliases, queue, per-family conversions
  registry/  geometry/  showcase/{section}
  block/
    attachment/{storage,host,interaction,item}   face trims shared by every host, anchor entity
    assembly/{placement}                         multi-cell contract and services
    door/ arch/ archway/ trim/ window/                 the families (each with its own subfolders)
  client/{door,trim,window,arch,archway,render,item,material,camouflage,compat/{emi,jei}}
  data/{client,server}
src/test/...      mirrors main (pure JUnit)
src/gameTest/...  mirrors main (Forge GameTests; door/{lifecycle,placement}, window/{design,placement})
tools/{textures/{glass,masonry},structures}   deterministic texture and structure generators
```

Per-family `appearance/` and `interaction/` folders (door, archway, window, trim) are deliberate: every family keeps the same sibling set so a reader finds the same thing in the same place, and each is expected to grow.
- `src/main/java/io/github/dirtyredz/roundwood/` — mod bootstrap (`Roundwood.java`) only
- `src/main/java/io/github/dirtyredz/roundwood/appearance/` — the appearance value, family acceptance, defaults contract, versioned NBT, the appearance-only anchor contract, pure restyle rules, bounded samples
- `src/main/java/io/github/dirtyredz/roundwood/appearance/material/` — server-safe donor recognition: tagged planks, the fixed four masonry materials
- `src/main/java/io/github/dirtyredz/roundwood/appearance/item/` — appearance on stacks, the shared family block item (tooltip, alias rules), loot decoration
- `src/main/java/io/github/dirtyredz/roundwood/appearance/interaction/` — gesture recognition, validated server mutation and payment, the narrow sneak-use event bridge
- `src/main/java/io/github/dirtyredz/roundwood/migration/` — frozen 0.4.0 mappings, alias registration, chunk-load scheduling and the migration queue, the 0.4.0 alias policy; the conversions sit in `conversion/`
- `src/main/java/io/github/dirtyredz/roundwood/migration/block/` — the deprecated alias block adapters (historical default, convert before integrity)
- `src/main/java/io/github/dirtyredz/roundwood/migration/item/` — retired crossed-stack adapters and checked CrossWindowPlacement shared with the preview
- `src/main/java/io/github/dirtyredz/roundwood/migration/conversion/` — the per-family conversions (door, frame, window), their shared cell rewrites (`LegacyCells`; windows use checked, lossless `WindowCellConversion` and `WindowCrossConversion`), and the 0.7.0 fitted-frame to door-trim conversion (`DoorTrimConversion`)
- `src/main/java/io/github/dirtyredz/roundwood/registry/` — deferred registration: the 27 canonical/compatibility base blocks and 29 advertised items, block entities, creative tab, mod tags, the `trim_face` loot condition type
- `src/main/java/io/github/dirtyredz/roundwood/block/` — the block families and what they share (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/` — frozen archway migration sources (0.12.0): historical Minecraft adapters (the block, its anchor entity, the item)
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/assembly/` — the four-cell archway: part identity, its layout (the shared `Assembly` contract), placement policy and typed plans (integrity and removal are `block/assembly`'s)
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/geometry/` — the two designs' profiles (ordinary, keyhole), square-filled frame boxes, the cached per-cell shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/appearance/` — the archway's look through its anchor, and its right-click router
- `src/main/java/io/github/dirtyredz/roundwood/block/archway/trim/` — the archway as a trim host: payload format, casing geometry, hit targeting, the host adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/` — the face-trim attachment shared by every host: face (and the shared depth rule and proud constant), slot, the two-face value, the read-only source contract, the shared anchor-entity persistence and synchronisation (`AppearanceAnchorBlockEntity`: look, declared stores, save/load/update, packets; the door, archway, window and frame entities extend it), the `roundwood:trim_face` loot condition
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/storage/` — the versioned two-face codec and the per-slot store (unknown-payload custody)
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/host/` — the narrow writable host view (live, callback-backed) and the one fit/remove/install/restyle transaction
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/interaction/` — the held-item classifier every trim host's router reads (acting hand, attachment, shears, donor)
- `src/main/java/io/github/dirtyredz/roundwood/block/attachment/item/` — the attachment items: the slot-naming recognition contract, UniversalTrimItem and StyledAttachmentItem (Arch Sill and Cross Beams)
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/` — what the multi-cell families (doors, archways, the compatibility frame) share: the `Assembly` contract, integrity, removal, typed anchor lookup, the mutation batch, partner teardown, deferred harvest, the whole-assembly restyling target
- `src/main/java/io/github/dirtyredz/roundwood/block/assembly/placement/` — the typed read-only placement plan, the installation transaction (with rollback) and player placement
- `src/main/java/io/github/dirtyredz/roundwood/block/door/` — the door family (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/adapter/` — the Minecraft adapters: the shared door adapter (`AssemblyDoorBlock`), the round and arched door blocks, the item that places either design, the anchor block entity and `DoorUseChannel` (projected-leaf use transport)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/assembly/` — the two- or four-part door: layout (with its design; the shared `Assembly` contract), hinge choice, placement policy and typed plans, state (integrity and removal are `block/assembly`'s)
- `src/main/java/io/github/dirtyredz/roundwood/block/door/appearance/` — reading, targeting and dropping the door's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/door/interaction/` — the door's right-click router: restyle, then camouflage, then toggle; `ProjectedDoorUse` validates projected-leaf requests before the ordinary server interaction path
- `src/main/java/io/github/dirtyredz/roundwood/block/door/leaf/` — the four door designs, panel geometry, collision shapes and shared swing timeline; typed hardware is owned by `block/door/hardware/`
- `src/main/java/io/github/dirtyredz/roundwood/block/door/selection/` — target and visible whole-door shapes (with the installed trim rings), the swinging leaf outline, hit re-attribution, corner-or-leaf hit region, the placement preview outline
- `src/main/java/io/github/dirtyredz/roundwood/block/door/camouflage/` — corner geometry, accepted materials, stored data, interaction, refunds
- `src/main/java/io/github/dirtyredz/roundwood/block/door/trim/` — the door as a trim host: payload format, per-design casing geometry and placement of the two faces, targeting, the host adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/` — the compatibility frame from older saves: the block, its anchor block entity (Minecraft adapters only; the `round_trim` item is the Universal Trim)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/appearance/` — reading, targeting and dropping the frame's appearance through its anchor
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/assembly/` — the four-quarter frame: part identity, layout (the shared `Assembly` contract), player-less placement (integrity and removal are `block/assembly`'s)
- `src/main/java/io/github/dirtyredz/roundwood/block/trim/geometry/` — the ring (plain Java) and its cached per-quarter selection shapes
- `src/main/java/io/github/dirtyredz/roundwood/block/window/` — the window family (grouping only, no code)
- `src/main/java/io/github/dirtyredz/roundwood/block/window/pane/` — the pane blocks (round window; oriented arches, corners and edges), their entity (frame look, glass, trim, sill and beam stores), items and model data, restyle and drops
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/` — pure window geometry shared by every design: pose, depth slabs, face identity (front/back slot to geometric side), pane panels (no junction glass since 0.13.1), cached shapes and culling footprints (the selection composes the pane with the in-cell trim bands and sills; the pane geometry imports no trim code), the visible-surface generator
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/face/` — one window face: profile (round; upper and lower arches; bottom and top corners; four single edges), arch, corner and edge glazing, cross-beam pattern and geometry, the runtime design value, separate from the plain-profile catalogue
- `src/main/java/io/github/dirtyredz/roundwood/block/window/geometry/trim/` — the attachments per face and slot: perimeter bands, the arch sill, their visible surfaces, the whole-face UVs and the masonry layout per profile
- `src/main/java/io/github/dirtyredz/roundwood/block/window/interaction/` — the window's right-click router: the hit-region classifier (trim band, sill, beam, frame, glass), the hit target (Minecraft hit to face and region), the pure routing table, the adapter
- `src/main/java/io/github/dirtyredz/roundwood/block/window/trim/` — the window as an attachment host: the trim payload and 0.7 adapter, the sill and singleton beam payloads, the shared WindowBeamFitPlan preview/fit decision and slot-aware host adapter and `trimmed` reconcile
- `src/main/java/io/github/dirtyredz/roundwood/block/window/glass/` — glass colour: the versioned payload, window item stacks, the dye action
- `src/main/java/io/github/dirtyredz/roundwood/block/window/placement/` — targeted placement: the face-zone classifier and its polygons (one authority), the use-context adapter, round position and arch pose
- `src/main/java/io/github/dirtyredz/roundwood/showcase/` — the `/roundwood showcase` command, bounded builder, coordinate frame (88 x 6 x 38 content), canvas (samples, appearance placement, labels, wall signs, barrels), `ShowcaseSpacing` (the one gap and row rule, 0.13.1), the listed `ShowcaseWindow` and section registry
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/` — the showcase sections outside the window and door families: the one-of-each pieces row (`PiecesSection`) and the materials wall (`MaterialsSection`, 0.13.1: planks and masonry blocks with wall signs, a dye barrel)
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/door/` — the door-family attachment examples (one camouflaged round door, one lever-driven round door)
- `src/main/java/io/github/dirtyredz/roundwood/showcase/section/window/` — the window configurations in oak (neighbour demos, positions, arches, corners, circle, edges) and the Universal Trim, Arch Sill and Cross Beams examples (`WindowTrimsSection`, `WindowSillsSection`, `WindowMuntinsSection`)
- `src/main/java/io/github/dirtyredz/roundwood/geometry/` — pixel circles, boxes, the shared ring profile, the round-headed arched profile (`ArchedProfile`), a pixel mask's visible slab (`PixelSlab`), tile-normalised UVs (`FaceUv.tiled`), tiled and whole-assembly face UVs (rectangular mapping too), voxel-shape conversion (shared)
- `src/main/java/io/github/dirtyredz/roundwood/client/` — client-only mod-bus wiring (`RoundwoodClient`)
- `src/main/java/io/github/dirtyredz/roundwood/client/archway/` — frozen-source fallback rendering and historical geometry models; retired items are excluded from placement preview
- `src/main/java/io/github/dirtyredz/roundwood/client/door/` — the door leaf renderer, the door trim renderer, the per-design casing models, the placement preview, appearance leaf models, leaf model names, targeting events, highlight, particles
- `src/main/java/io/github/dirtyredz/roundwood/client/trim/` — compatibility frame renderer; shared ring models and names live in `client/render/`.
- `src/main/java/io/github/dirtyredz/roundwood/client/window/` — the appearance-aware window blockstate/item model (frame, glass and beams; WindowBeamModels composition and WindowBeamCulling snapshot coverage), the directly typed item-model cache, WindowBeamPreview and the placement preview/overlay
- `src/main/java/io/github/dirtyredz/roundwood/client/window/trim/` — the trim and sill block-entity renderer, its cached world models and the attachment look per slot and profile
- `src/main/java/io/github/dirtyredz/roundwood/client/render/` — SurfaceQuadBaker and GeneralQuadModels shared by fitted beams, face trims and camouflage; PlacementHighlights and AssemblyCellRenderer share preview and per-cell render mechanics; line drawing shared by the highlights and placement previews; baked pixel-mask slab models in a trim look (`PixelSlabModels`, 0.12.0: the arched door casing and the archways)
- `src/main/java/io/github/dirtyredz/roundwood/client/item/` — NBT-aware item model overrides, `FamilyItemModels` (typed binding of each item kind to its wrapper), the attachment icons (Universal Trim, Arch Sill) per look
- `src/main/java/io/github/dirtyredz/roundwood/client/material/` — donor sprites and colours, built-in wood and paint artwork bindings, masonry faces (thin mortar), the shared trim look resolver, quad retexturing and raw vertex maths, bounded model caches
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
- `src/main/java/io/github/dirtyredz/roundwood/data/server/recipe/` — recipe emission per family (door, windows, modular arch pieces, attachments)
- `src/main/resources/` — mods.toml, pack.mcmeta, the generated original textures (checked in)
- `src/generated/resources/` — datagen output, checked in and packaged (its `.cache/` is ignored and excluded)
- `src/test/java/io/github/dirtyredz/roundwood/` — pure JUnit tests (`testsupport/` bootstrap shim, `appearance/`, `migration/`, `client/material/`, `geometry/`, `block/assembly/`, `block/attachment/`, `block/attachment/storage/`, `block/attachment/host/`, `block/archway/geometry/`, `block/archway/trim/`, `block/door/leaf/`, `block/door/selection/`, `block/door/camouflage/`, `block/door/trim/`, `block/trim/geometry/`, `block/window/geometry/`, `block/window/geometry/face/`, `block/window/geometry/trim/`, `block/window/interaction/`, `block/window/glass/`, `block/window/placement/`, `block/window/trim/`, `client/window/`, `data/`)
- `src/gameTest/java/io/github/dirtyredz/roundwood/` — Forge GameTests (`fixture/`, `fixture/assembly/`, `appearance/`, `block/assembly/`, `block/assembly/placement/`, `block/attachment/` (anchor persistence), `block/attachment/interaction/`, `migration/`, `block/archway/`, `block/door/`, `block/door/lifecycle/`, `block/door/placement/`, `block/door/appearance/`, `block/door/selection/`, `block/door/camouflage/`, `block/door/trim/`, `block/trim/`, `block/window/`, `block/window/design/`, `block/window/placement/`, `block/window/trim/`, `showcase/`, `showcase/section/` (one check class per section family beside the sections: `door/`, `window/`, the root for the pieces and materials rows), `data/`); never in the release jar
- `src/gameTest/java/io/github/dirtyredz/roundwood/environment/` — test-only server startup spawning rule, flat-world environment guard and opt-in independent navigation repeats (`-PnavigationRepeats`)
- `src/gameTest/resources/` — GameTest structure templates (empty ones, the 0.4.0 fixture and the 0.7.0 fitted-frame fixture), a test-only plank tag and a test-only `forge:shears` tag (a damageable non-shears item)
- `tools/` — deterministic generators for textures, the mod logo and GameTest structures, plus the synced test-world datapack (not packaged)
- `tools/generate-logo.mjs` — 32px pixel-art master using the existing oak/stone palettes and PNG encoder; generates `src/main/resources/roundwood-logo.png` (256px, packaged for Forge). Run `node tools/generate-logo.mjs [artifact-directory]` to optionally export the identical launcher icon and a 256/64/32px preview; `node tools/generate-logo.mjs --check` checks reproducibility. `data/ModLogoTest` guards the resource binding, size, transparency and crisp scaling.
- `tools/textures/` — PNG encoding, noise, palettes, historical artwork, door and paint boards, trim markers, and shared `mortar.mjs` primitives.
- `tools/textures/masonry/` — historical and thin radial faces plus arch, square, casing, sill and archway masonry; every face imports the parent mortar primitives.
- `tools/textures/glass/` — historical, clear and stained glazing, with one shared diagonal streak mask.
- `docs/` — architecture, decisions, gotchas

## Components

| Component | Files | Responsibility |
|---|---|---|
| **CI/release verification** | `.github/`, `.ci/`, `gradle/`, `docs/releases/` | Shared hosted verify, GameTest completion, jar isolation, manifest, assets and draft readback |
| Appearance | `appearance/Appearance`, `AppearanceFamily`, `AppearanceDefaults`, `AppearanceNbt`, `AppearanceChanges`, `AppearanceSamples` | Immutable look (wood/masonry + optional dye), which family takes what, a block's/item's default, the one NBT form, restyle rules, bounded examples |
| Materials | `appearance/material/PlankMaterials`, `MasonryMaterials`, `DonorStacks` (package-private) | Plank donors by tag (+ `roundwood:appearance_planks`), the four masonry blocks and their artwork keys; the shared rule that a stack carrying placed contents or a non-default state is never a donor |
| Items | `appearance/item/AppearanceStacks`, `AppearanceBlockItem`, `AppearanceTooltip`, `AppearanceDrops` | Normalised stack NBT, the family item (tooltip, alias name, no block-map claim), loot decoration |
| Gestures | `appearance/interaction/AppearanceGestures`, `AppearanceActions`, `AppearanceUseEvents`, `AppearanceUseTarget` | Recognise sneak+planks/dye/masonry; one checked mutation and payment; let sneak gestures reach the block |
| Registry | `registry/*`, `migration/{LegacyContent,CrossWindowCompatibility}` | 27 canonical/compatibility blocks, 29 advertised items; 13 retired crossed blocks/items, two retired archways, 49 frozen aliases and hidden `window_trim`; 91 registered blocks, 94 registered items. Window entity valid for 14 plain + 13 retired + 15 alias panes. Gameplay tags retain all 27 window IDs. |
| Archway (0.12.0) | `block/archway/*` (`ArchwayBlock`, `ArchwayBlockEntity`, `ArchwayItem`), `assembly/*` (`ArchwayPart`, `ArchwayLayout`, `ArchwayPlacement`), `geometry/*` (`ArchwayDesign`, `KeyholeGeometry`, `ArchwayGeometry`, `ArchwayShapes`), `appearance/*` (`ArchwayAppearance`, `ArchwayInteraction`), `trim/*` (`ArchwayTrimNbt`, `ArchwayTrimGeometry`, `ArchwayTrimTarget`, `ArchwayTrimActions`) | Static 2 x 2 wall opening with filled corners in two designs, a trim-family look on one anchor entity, one read-only placement plan for the server and the preview, the shared assembly lifecycle, placement and anchor lookup behind typed plans and lookup constants, per-cell frame shapes (target = outline = the drawn frame; collision = the lower jambs only, 0.13.1), and the face casings through the shared attachment services |
| Attachments | `block/attachment/AttachmentFace`, `TrimSlot`, `FaceTrims`, `TrimAttachmentSource`, `storage/FaceTrimsNbt`, `storage/FaceTrimStore`, `host/TrimHost`, `host/TrimFitting`, `AppearanceAnchorBlockEntity`, `interaction/HeldTrimTool`, `TrimFaceCondition`, `item/TrimAttachmentItem`, `item/UniversalTrimItem`, `item/ArchSillTrimItem`, `item/StyledAttachmentItem`, `block/window/trim/WindowBeamNbt`, `block/window/interaction/WindowBeamFitPlan` | The face-trim attachment shared by every host (0.9.0): FRONT/BACK faces, the attachment slot (perimeter, Arch Sill, Cross Beams) and its canonical item, two optional looks, the one versioned codec, one store per slot with fail-closed custody of unknown payloads, the one fit/remove/install/restyle transaction on a narrow writable host, the read-only source contract, the per-face (and per-slot) loot condition, item recognition, the Universal Trim item (every trim id), styled Arch Sill and Cross Beams items (no blocks); Cross Beams uses one canonical FRONT address for the whole pane, an independent TRIM look, a singleton payload and the shared preview/fit plan |
| Migration | `migration/LegacyAppearances`, `LegacyContent`, `CrossWindowCompatibility`, `ChunkMigrations`, `ChunkMigrationQueue`, `LegacyChunkMigration`, `migration/conversion/*` (`LegacyCells`, `Legacy{Door,Trim,Window}Conversion`, `DoorTrimConversion`, `WindowCrossConversion`, `WindowCellConversion`), `migration/block/*`, `migration/item/*` (`RetiredCrossWindowItem`, `CrossWindowPlacement`) | Frozen 49-id table, alias and hidden `window_trim` registration, chunk-load discovery and one deduplicated queue (64 entries per level per tick), the 0.4.0 alias policy, silent guarded rewrites; the 0.7.0 fitted frame becomes the door's FRONT trim; 13 hidden crossed-window identities migrate through checked, lossless cell conversion, with unknown payloads retained and unsafe conversions deferred; CrossWindowCompatibility implements the window-owned `block/window/pane/RetiredCrossWindows` port for conversion and pending drops; retired stacks retain inventory identity and share a checked placement/preview plan |
| Shared assembly | `block/assembly/Assembly`, `AssemblyIntegrity`, `AssemblyRemoval`, `AssemblyAnchors`, `AssemblyMutationBatch`, `AssemblyTeardown`, `AssemblyHarvest`, `AssemblyAppearanceTarget`, `placement/AssemblyPlacementPlan`, `AssemblyInstallation`, `AssemblyPlacement` | The narrow contract the door, archway and frame layouts implement (ordered parts, family membership, shared loaded-safe traversal and authored frame); one integrity check and one removal (with a pre-teardown hook); typed anchor lookup and loot fallback; one restyling target; one placement skeleton, installation transaction (all or nothing) and player placement; marks cells being rewritten; silent partner teardown; deferred removal so harvest loot sees the anchor |
| Door adapters | `block/door/adapter/*` (`AssemblyDoorBlock`, `RoundDoorBlock`, `ArchedDoorBlock`, `RoundDoorItem`, `RoundDoorBlockEntity`) | One shared delegating `DoorBlock` adapter (`AssemblyDoorBlock`) with two thin blocks (the round door adds only `column`; the arched door has none), the item placing either design and the one anchor entity (swing, camouflage, appearance, update packet; type id `round_door` serves both) |
| Door appearance / router | `block/door/appearance/DoorAppearance`, `block/door/interaction/DoorInteraction`, `block/door/selection/DoorHitRegion` | Anchor look for picks/drops/targets; restyle-camouflage-toggle precedence; one corner-or-leaf decision |
| Door layout, placement, state, removal | `block/door/assembly/*` (`DoorLayout`, `DoorPlacement`, `DoorHingePlacement`, `DoorAppearance.ANCHORS`) | Two or four parts by design (`DoorLayout.Assembly` carries it); `DoorPlacement` is the one read-only placement decision (server placement, showcase and the client preview read it); `DoorPlacement` hands the plan to the shared installation and player placement; removal and integrity are the shared services (the camouflage release is the door's pre-teardown hook) |
| Leaf, selection, camouflage | `block/door/leaf/*` (`DoorDesign`, `DoorGeometry`, `DoorShapes`), `selection/*` (incl. `DoorPlacementOutline`), `camouflage/*` | `DoorDesign` (ROUND, ARCHED, DOUBLE_ARCHED, TALL_DOUBLE_ARCHED) names the outline, columns and width; leaf geometry and swing timeline per design; resting and swinging outlines (`DoorSwingOutline`, 0.6.1); the pure placement preview outline; camouflage corners per design |
| Trim (compatibility frame) | `block/trim/RoundTrimBlock`, `RoundTrimBlockEntity`, `appearance/TrimAppearance`, `assembly/*`, `geometry/*` | Static ring quarters from older saves with an anchor entity holding the look; no item places them any more |
| Door trim | `block/door/trim/*` (`DoorTrimNbt`, `DoorTrimGeometry` (per design), `DoorTrimTarget`, `DoorTrimActions`), `block/door/appearance/DoorAppearance.ANCHORS`, `block/door/interaction/DoorInteraction` | The door as a trim host: payload format, per-face rings stored on the door anchor, the host adapter over `TrimFitting`; targeting and fitting in the router ahead of restyle, camouflage and toggle |
| Window | `block/window/pane/*` (`WindowPaneBlock`, `RoundWindowBlock`, `OrientedWindowBlock`, `RoundWindowBlockEntity`, `RoundWindowItem`, `WindowAppearanceData`, `WindowPaneState`, `RetiredCrossWindows`), `placement/*` (`WindowOrientation`, `WindowPosition`, `WindowTargetZones`, `WindowPlacement`), `geometry/*` (`WindowPose`, `WindowFaceIdentity`, `WindowDepth`, `WindowPaneGeometry`, `WindowShapes`, `WindowSurfaceGeometry`), `geometry/face/*` (`WindowProfile`, `WindowArchGeometry`, `WindowCornerGeometry`, `WindowEdgeGeometry`, `WindowPattern`, `WindowMuntinGeometry`, `WindowDesign`), `geometry/trim/*` (`WindowTrimGeometry`, `WindowSillGeometry`, `WindowTrimSurfaces`, `WindowTrimUv`, `WindowTrimLayout`), `interaction/*` (`WindowHitRegions`, `WindowHitTarget`, `WindowUseRules`, `WindowInteraction`, `WindowBeamFitPlan`), `trim/*` (`WindowTrimNbt`, `WindowSillNbt`, `WindowBeamNbt`, `WindowTrimActions`, `WindowAttachmentState`), `glass/*` (`WindowGlassNbt`, `WindowGlassStacks`, `WindowGlassActions`) | Pane (round, arch, corner or edge; plain or cross beams; posed by orientation/depth/position, `face_reversed` for face identity) with a per-window frame look and glass colour handed to its chunk model as model data; the logical pieces painted into visible surfaces; the attachments (trim and sill payloads, slot-aware host adapter, bands and sills per face); the shared hit classification and the pure routing table behind the click router; zone classifier and placement; no connections or junction glass (0.13.1) |
| Client materials | `client/material/DonorSprites`, `DonorColors`, `BuiltinWoodStyles`, `PaintStyles`, `MasonryStyles`, `GlassStyles`, `AppearanceLooks`, `TrimLooks`, `QuadVertices`, `ModelRetexturing`, `AppearanceModelCache` | Representative donor sprites, sampled tints, wood, paint and glass (clear and stained) artwork bindings, masonry faces (thin-mortar door ring, window faces, the square icon face), `AppearanceLooks` donor/artwork precedence, `TrimLooks` (trim resolver and shared tiled look for archways and beams), immutable quad transforms (also by position UVs), bounded caches cleared together on reload |
| Door rendering | `client/door/RoundDoorRenderer`, `DoorTrimRenderer`, `DoorTrimModels`, `DoorAppearanceModels`, `DoorLeafModels`, `DoorPlacementPreview`, `DoorParticles`, `DoorTargetingEvents`, `DoorHighlightRenderer` | BER drawing the appearance leaf (panel and independent hardware finishes; caches keyed by design and all finishes); `DoorTrimRenderer` draws the face casings before the swing transforms: the round ring from the frame's turned quarter models, the arched casing from `DoorTrimModels` (built from the design's outline); `DoorPlacementPreview` draws the plan for all four designs |
| Trim / window / item rendering | `client/trim/*`, `client/window/*`, `client/window/trim/*`, `client/render/OutlineLines`, `client/item/AppearanceItemOverrides`, `client/item/TrimItemModels` | Anchor BER tessellating turned quarter models; the window chunk model (frame look and glass colour, pane only); `WindowTrimRenderer` drawing trims and sills from cached `WindowTrimWorldModels`; item cache keyed by source model, wood, glass and independent beams (`WindowAppearanceModel.ItemKey`), the attachment icons per stack (every trim id, the Arch Sill); placement preview and overlay (`WindowPlacementPreview`, `WindowPlacementOverlay`); per-stack item models |
| Archway rendering (0.12.0) | `client/archway/ArchwayRenderer`, `ArchwayModels`, `ArchwayPlacementPreview`, `client/render/PixelSlabModels` | The anchor BER drawing each cell's frame and casing pieces with world light; frame, casing and styled item models baked from the design's mask in a tiled donor look on every face (0.13.3: wood/masonry donor sprites or painted window boards; cache keyed by design, layer, cell, facing, look); the preview drawing the plan's frame outline and cells. `PixelSlabModels` is shared with `client/door/DoorTrimModels` |
| Viewers | `client/compat/emi/*`, `client/compat/jei/*` | EMI/JEI: each item shown once; optional, annotation-discovered, never loaded without the viewer |
| Showcase | `showcase/*`, `showcase/section/*`, `showcase/section/door/*`, `showcase/section/window/*` | Op-only command; the showcase is one of each piece, the materials wall (planks, masonry, a dye barrel), oak-only window configurations and one example per attachment, every sample four clear blocks from the next (`ShowcaseSpacing`); `ShowcaseSamples` (0.11.0) is the one sample set and sign label shared by canvas and GameTest checks; sections place canonical blocks through production paths with explicit appearances; `ShowcaseWindow` lists each window (design, pose, frame coordinates, looks) so building and the GameTest check read the same list |
| Datagen | `data/**`, `data/client/door/*` (`DoorModelProvider`, `DoorItemModelProvider`: one leaf template per design and hinge), `data/client/window/*`, `data/client/trim/*` | Oak source models + empty world models, window models as visible surfaces (`WindowModelProvider`, `OrientedWindowModelProvider`, `WindowPaneModelWriter`, `GeometryModelWriter.addSurfaces`; 112 plain window block models (141 total, pinned by `GeneratedResourceManifestTest`), each state one complete model, no trimmed variants), item providers (`WindowItemModelProvider`, `trim/AttachmentItemModelProvider`), the frame (`trim/TrimModelProvider`), display transforms (`ItemModelTransforms`), alias models (`LegacyModelProvider`), alias compatibility resources, the archways' empty world models and geometry item models (`archway/ArchwayModelProvider`, `ArchwayItemModelProvider`, 0.12.0), 22 recipes (`recipe/DoorRecipes`, `WindowRecipes`, `ArchPieceRecipes`, `TrimRecipes` under the `RoundwoodRecipeProvider` coordinator), loot (`AttachmentLootPools`: one pool per face and slot for windows, doors and archways), tags, names |
| Texture tools | `tools/generate-textures.mjs`, `tools/textures/*` | Orchestrator plus artwork modules (palettes append only, so old bytes never change); `glass/stained-glass.mjs` writes the sixteen `window_glass_<dye>` textures, `masonry/radial-trims.mjs` the thin-mortar radial faces, `glass/window-glass.mjs` the historical and clearer glass (`clearWindowGlass`), `trim-marker.mjs` the source markers, `masonry/square-trims.mjs` the Universal Trim's square masonry face, `masonry/window-shape-trims.mjs` the 0.10.0 top-corner, single-edge and sill faces, `masonry/door-casings.mjs` the 0.11.0 arched door casing faces, `masonry/archways.mjs` the 0.12.0 archway faces (radial voussoirs, jamb courses, keyhole corbels), `tools/textures/mortar.mjs` the shared thin-mortar primitives (joint coverage, the one parameterised polar brick position, brick and mortar colours), `masonry/arch-window.mjs` the arched window face and its course mortar |

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
`block/attachment/item` imports only `block/attachment` and `appearance`. Inside `block/attachment`, the root `AppearanceAnchorBlockEntity` reads `storage`, `host` reads the root anchor (the anchored view) and `storage`, and `interaction` reads `item` and `appearance`; the root imports no other subfolder. `block/assembly` writes looks through `appearance/AppearanceAnchor` and imports no host family. `client/door/DoorTrimRenderer` reads neutral `geometry/RingGeometry.Quarter` and
`client/render/RingModels`; `migration/conversion` reads `block/door` and `block/trim`. Inside
`block/door`: `appearance` and `interaction` read `assembly`, `selection` and the
adapters; `assembly` does not import `selection` (the preview outline lives in `selection` and reads the plan's assembly); `assembly/DoorPlacement` writes the appearance straight to the anchor entity (no import of
`door/appearance`, keeping that pair acyclic). Inside `block/window`: `geometry` and `placement` import no block adapter (`WindowShapes` is the only geometry
class that touches Minecraft shapes). `geometry/face` and `geometry/trim` build on `geometry` and
are read by `interaction`, `trim`, `glass` and the client (since 0.10.0 only `WindowShapes` in
`geometry` imports `geometry/trim`, to compose the selection; the pane geometry does not); `interaction` calls `trim` and `glass` (the
item no longer imports `interaction`). `client/window` imports `migration/item/CrossWindowPlacement` for the checked retired-stack preview and `block/door/selection/DoorHitResolver`
for the placement preview (using the same resolved owner and hit point as use); `client/door` imports `block/door/selection` for the door placement preview and `block/door/adapter/DoorUseChannel` for projected use. The door imports `client`
only for
`DoorParticles` (client-only call). Geometry, `CamouflageGeometry`, `DoorSwing`, the appearance
rules and `QuadVertices`' array arithmetic are plain enough for unit tests without a bootstrap.

The registry/migration cycle is an intended compatibility exception: registry constructs the two
retired block adapters and retired item; migration looks up canonical blocks/items and registered
entity types for lossless conversion and placement previews. Replacing these lookups with supplier
plumbing would broaden this compatibility refactor. Registry imports from migration are limited to
those three adapters by `DependencyDirectionTest`; conversions remain outside registry.
Window code imports no migration code: `pane/RetiredCrossWindows` owns the narrow conversion/drop
port and cross-pattern predicate; `CrossWindowCompatibility` registers its implementation in
`Roundwood` bootstrap. The handler preserves deferred retries, fail-closed clicks and full-data drops.

## Structural notes

- 0.5.0 replaced the `variant/` catalogue: material acceptance (`appearance/material`), artwork
  (`client/material`), samples (`AppearanceSamples`) and frozen history (`migration/LegacyAppearances`)
  are separate authorities. The vanilla wood list appears in each on purpose (artwork, samples and
  the frozen table must not share a mutable source).
- The appearance system nests `material/`, `item/` and `interaction/` under `appearance/` so the
  root package stays at ten entries (the plan had them at the root).
- Doors, archways and the frame share the `block/assembly` contract and services (lifecycle, anchor lookup, restyling target, placement skeleton and installation); each keeps its typed layout, part identity, state construction, bounds and placement policy. The frame adopted it rather than being frozen (dk-992020e6).
- `client/door/DoorLeafModels` and `client/render/RingModelNames` are pure naming shared by the client
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
  packages (the adapters at its root, 7 entries) and `client/archway/` (renderer, models, preview; the
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
- The assembly refactor (0.12.0, dk-992020e6, dk-ff89f8e5) added the `Assembly` contract and its services to `block/assembly/` (seven files plus `appearance/` and `placement/`: nine entries) and split `block/attachment/` by responsibility into `storage/`, `host/`, `entity/` and `interaction/` beside `item/` and `loot/` (four root files: ten entries). The round-2 review then folded the one-file `entity/` and `loot/` back into the root (six root files plus `storage/`, `host/`, `interaction/`, `item/`: still ten entries) and the one-file `block/assembly/appearance/` into `block/assembly/`; `migration/` grouped its five conversion classes under `conversion/` (`trim/` is gone: seven entries). The six per-family integrity/removal copies are gone; `DoorAppearance.ANCHORS`, `ArchwayAppearance.ANCHORS`, `DoorPlacement` and `ArchwayPlacement` stay as typed plans and lookup constants. Window summary bookkeeping moved out of `WindowTrimActions` into `WindowAttachmentState`.
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
- dk-aef47f84 done: `migration/` grouped under `conversion/` (seven entries), no-growth single-file folders folded, the per-family `appearance/` and `interaction/` pairs declared deliberate
- dk-7279982a done: GameTest `block/door/` and `block/window/` grouped, tree diagram in Layout, one-file folders declared
- dk-0dc49165 done in 0.11.0 (door sections grouped, family checks beside the sections, `ShowcaseSamples`)
- dk-7fd38157 done: `QuadVertices` owns raw vertex maths; door tint belongs to DoorAppearanceModels; camouflage and window items use AppearanceModelCache directly; GeneralQuadModels shares unculled world model assembly.
- dk-7abe21de done: `VoxelShapeFactory.turn` owns signed Direction quarter turns for model rotation and camouflage; the old datagen copy was already removed.
- dk-75a60754 done: block-entity valid sets come from the shared deferred block register; RingGeometry.Quarter, RingModels and RingModelNames are neutral owners. Forge initializeClient is the deliberate particle boundary (DECISIONS.md).
- dk-4cf186bd done: `migration/block/LegacyAlias` supplies canonical block, historical appearance, conversion and queue identity; family conversions retain their distinct custody rules.
- dk-1d129342 done: `DoorFixtures.placeOakDoor` owns the repeated player/place/assert sequence; callers keep floor setup and player identity. The generated-resource test split was already complete.
- dk-d402bd77 done: family adapters remain explicit; three appearance families and different pick/drop ownership do not justify a wiring abstraction (DECISIONS.md).
- dk-87f3ad8f done: TrimFaceCondition owns TYPE; registration points to the attachment, with no reverse dependency.

- dk-2ce8b225 done: `tools/textures/mortar.mjs` owns polar and brick shading; positions and shading options are named records, shared by historical and thin radial art.

- dk-d1d87975 done: `appearance/AppearanceAnchor`, typed `AssemblyPlacementPlan<P,A>`, per-family `AssemblyAnchors.Lookup` constants and typed item binding replace forwarding facades; window wrapping lives with its model.

- dk-ea090069 done: `PlacementHighlights` owns held-hand precedence and the live-client event guard; `AssemblyCellRenderer` owns per-cell tessellation and balanced poses.

`block/door/camouflage/CamouflageCoverage` owns the effective ALL/LOWER/NONE mask used by corner volumes, hits, outlines and model caches. Fitted round doors retain only y<16; other fitted designs have no visible corners.

`block/door/hardware/DoorHardwareGeometry` owns half-pixel spear straps and fixed-height knobs. `tools/structures/doors-v0160.mjs` records independent historical door states and possessions; DoorFeedbackUpgradeGameTests loads them without replacement.

`client/render/SurfaceSeparation` owns the three render offsets. DoorTrimModels, DoorArchRenderer and ArchPieceModels expose their render surfaces; geometry classes return authored surfaces. `client/door/DoorArchBakedFacesTest` and `testsupport/BakedCoplanarity` inspect generated leaf and production frame vertices across the supported pose matrix.

`appearance/material/SurfaceMaterials` owns the exact block-finish allowlist and wax normalization. `client/door/DoorSurfaceModels` tiles donor panels before tall-model compression. SurfaceMaterialsTest, DoorSurfaceModelsTest and SurfaceFinishGameTests cover kind compatibility, UVs, cache distinction and payment.

`showcase/section/door/DoorHardwareSection` adds four bounded component examples beside the fitted row. `HardwareShowcaseChecks` is shared by the focused hardware showcase GameTest and full showcase suite. WindowEdgesSection owns the 3x3 frameless-centre example; MaterialsSection owns the fourteen distinct donor blocks.

- Fitted render surfaces in `block/door/frame/DoorArchGeometry` reuse the standalone
  `ArchPieceGeometry` merged builder. Logical selection volumes stay in the door module.
  `src/test/resources/fps/fitted-corpus.json` pins the diagnostic census's 135 fitted inputs;
  `client/arch/FittedCorpusTest` enforces the production-baked 9,000-quad budget.
