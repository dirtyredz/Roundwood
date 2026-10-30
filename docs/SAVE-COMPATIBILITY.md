# Save compatibility - Forge 1.20.1 worlds on NeoForge 1.21.1

Worlds saved by two Forge 1.20.1 releases must load on this line after Minecraft's own 1.20.1 -> 1.21.1
conversion, with Roundwood present:

| Source | Tag / commit | Jar SHA-256 | Why |
|---|---|---|---|
| 0.13.3 | `v0.13.3` `4f4776ec709107a7372aedea4609cac7700561d0` | `150874f1a48c88c8af0995cb33aa2970cdfb83dc828cd0b049a8b635f7f206e4` | installed in the Evil Eye pack |
| 0.17.0 | `v0.17.0` `4610a4b6daa468995bf825e56212a39d0e373d0f` | `116432453c3e96c080454c5a3e2852463b9bb672a0db5af4b4084020d8d3c99d` | latest release; the ported source line |

Both saves carry DataVersion 3465 (1.20.1). "Vanilla conversion" means Minecraft's data fixer with Roundwood
and its donor blocks present; never open such a world without Roundwood.

## Registrations (no renames)

Every historical name stays registered as itself, so the port needs no load-time ID mapping and no missing-mapping
handling. A missing `roundwood:` name in qualification is a compatibility failure, never a normalization.

- 0.13.3 -> 0.17.0 added: `double_arched_door`, `tall_double_arched_door`, `frameless_window`, `arch_top_1x1`,
  `arch_top_1x2`, `arch_top_2x3`, `keyhole_shoulder` and the four `_thin` products. Nothing was removed or renamed.
- Block entities: `round_door`, `round_window`, `round_trim`, `archway`, and since 0.14 `arch_piece`.
- Loot condition `roundwood:trim_face`; recipe serializer `roundwood:arch_thinning` (0.15).
- 49 frozen aliases: `{oak,spruce,birch,jungle,acacia,dark_oak,mangrove,cherry,bamboo,crimson,warped,green,yellow,red,blue}`
  x `{_round_door,_round_window,_round_trim}` plus `{brick,stone_brick,deepslate_brick,mud_brick}_round_trim`.
- 13 retired crossed windows `round_window_cross` and `round_window_<profile>_cross` (blocks and items), the retired
  `archway` / `keyhole_archway` (non-placeable migration sources) and the item-only `window_trim`.

The full id lists are in `src/gameTest/resources/compatibility/forge-1.20.1/<version>/manifest.json`, written by
`tools/compat/historical-manifest.mjs` from each release jar (blockstates and item models) and the tagged
registration classes.

## Blockstate contract

| Family | Members | Properties |
|---|---|---|
| Round door | `round_door` + 15 aliases | vanilla door (`facing`, `half`, `hinge`, `open`, `powered`), `column=left|right`; `depth=front|middle|back` since 0.16 |
| Arched door | `arched_door` | vanilla door; `depth` since 0.16 |
| Paired doors | `double_arched_door`, `tall_double_arched_door` | vanilla door, `cell_x=0..2`, `cell_y=0..3` (both designs), `depth` |
| Round windows | `round_window`, `round_window_cross`, 15 aliases | `north/east/south/west`, `waterlogged`, `trimmed`, `face_reversed`, `axis=x|z`, `position` (7) |
| Oriented windows | every other window id | the pane properties, `orientation` (12), `depth=center|negative|positive` |
| Compatibility frame | `round_trim` + 19 aliases | horizontal `facing`, `part` (4 quarters) |
| Archways | `archway`, `keyhole_archway` | horizontal `facing`, `part` (4) |
| Arch pieces | four full, four `_thin` | horizontal `facing`, `cell` (6), `side=left|right`; `_thin` adds `depth=front|middle|back` |

A 0.13.3 door state has no `depth`; it reads as `front` (aliases included). Connection properties stay saved but
behaviourally disabled.

## Payloads

Roundwood's block-entity payloads are unchanged NBT: `RoundwoodAppearance` v1 (`wood`, `masonry`, `block_surface`
since 0.16), `DoorTrim`/`ArchwayTrim`/`ArchPieceTrim` v1, `WindowTrim` v2 (with the 0.7.0 `trimmed` reading),
`WindowSillTrim` v1, `WindowCrossBeams` v1, `WindowGlass` v1, `DoorArch` v1, `DoorHardware` v1, `Camouflage`.

Item stacks: vanilla's 1.20.5 componentization moves names, lore, enchantments, repair cost, unbreakability,
adventure predicates and tooltip hiding into native components, `BlockEntityTag` into `block_entity_data` and
`BlockStateTag` into `block_state`, and leaves every other tag key in `minecraft:custom_data`. Roundwood reads its
keys (`RoundwoodAppearance`, `WindowGlass`, `DoorHardware`, `DoorArch`) from there unchanged (`StackPayloads`).

The one nested ItemStack Roundwood saves, `Camouflage.Refund`, is not visited by vanilla's upgrade: it keeps the
1.20.1 `{id, Count, tag}` shape. `CamouflageRefundCodec` runs the vanilla ItemStack fixer on that shape (from 3465)
and resaves the target encoding; a refund it cannot decode is locked custody (`CamouflageStore`), kept byte for byte,
refused by every edit and carried opaquely on the door's drop (the item then refuses placement).

## Semantic migrations (unchanged)

0.13.3 archways become a full `arch_top_1x2` at the upper row (keyholes add two `keyhole_shoulder` pieces), with
body and both casings transferred once; their stacks split in loaded holders (player inventory and ender chest,
containers, container entities, dropped items) with the whole component patch on the top piece; unopened loot
containers are skipped. Retired crossed windows convert to their plain profile plus a Cross Beams attachment on
chunk load and on item placement. Aliases canonicalize as before.

## Evidence (this line)

| Check | Where | Status |
|---|---|---|
| Every historical id resolves exactly; 49 aliases, 13 crossed; exact property domains; missing depth reads `front` | `migration/RegistryCompatibilityGameTests` | passing |
| 0.13.3 and 0.17.0 block entities: vanilla fixer, target load, locked unknown payloads, idempotent resave | `migration/SaveUpgradeGameTests` | passing |
| 0.13.3 and 0.17.0 stacks and holders (player, ender chest, chest, unopened loot chest, item entity, chest minecart) | `migration/ItemComponentUpgradeGameTests` | passing |
| Legacy camouflage refund: decoded once, refunded once, resaved, never synced; locked custody on the door drop | `block/door/camouflage/CamouflageRefundUpgradeGameTests`, `CamouflageStoreTest` | passing |
| World-level conversions from historical structures (0.4.0, 0.7.0, 0.12.0 crossed windows, 0.13.3 archways, 0.16.0 doors) loaded through vanilla's structure fixer | existing `migration/*` and door suites | passing |
| Whole-pack world conversion and restart | the Evil Eye pack session | not run here |

The SNBT fixture cases are written in each release's encoding from its serializers; they are not captured from a
live world save. The expectations in them are literal values, never produced by the converters under test.
