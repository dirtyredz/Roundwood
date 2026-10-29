package io.github.dirtyredz.roundwood.data;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.attachment.item.ArchSillTrimItem;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.migration.LegacyAppearances;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModCreativeTab;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.item.TrimAttachmentItem;
import io.github.dirtyredz.roundwood.block.attachment.item.UniversalTrimItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

/**
 * Loaded registrations and advertised content. Modular arches are registered separately until
 * their content batch; the historical advertised lists and resource guards still apply here.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentGameTests {
    private ContentGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void canonicalEntriesAndFortyNineHiddenAliasesAreRegistered(GameTestHelper helper) {
        long blocks = ForgeRegistries.BLOCKS.getKeys().stream().filter(k -> k.getNamespace().equals(Roundwood.MOD_ID)).count();
        long items = ForgeRegistries.ITEMS.getKeys().stream().filter(k -> k.getNamespace().equals(Roundwood.MOD_ID)).count();
        check(blocks == 91, "expected 20 + 8 modular (4 full, 4 thin) + 13 + 49 blocks, found " + blocks);
        check(items == 94, "expected 20 + 8 modular + two attachments + 13 + 49 + the window trim items, found " + items);
        for (String id : List.of("arch_top_1x1", "arch_top_1x2", "arch_top_2x3", "keyhole_shoulder")) {
            Block block = ForgeRegistries.BLOCKS.getValue(id(id));
            Item item = ForgeRegistries.ITEMS.getValue(id(id));
            check(block instanceof io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock
                    && item instanceof io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem
                    && block.asItem() == item, "modular registration " + id);
            check(ModBlockEntities.archPiece().isValid(block.defaultBlockState()), "modular anchor type " + id);
            check(!ModBlockEntities.archway().isValid(block.defaultBlockState()), "modular piece reused old anchor type " + id);
        }
        for (String id : CANONICAL_BLOCKS) {
            check(ForgeRegistries.BLOCKS.containsKey(id(id)) && ForgeRegistries.ITEMS.containsKey(id(id)), "missing " + id);
            check(ForgeRegistries.BLOCKS.getValue(id(id)).asItem() == ForgeRegistries.ITEMS.getValue(id(id)), id + ": item mapping");
        }
        for (String plain : CANONICAL_BLOCKS.stream().filter(id -> id.startsWith("round_window")).toList()) {
            Item retired = ForgeRegistries.ITEMS.getValue(id(plain + "_cross"));
            Block block = ForgeRegistries.BLOCKS.getValue(id(plain + "_cross"));
            check(retired instanceof io.github.dirtyredz.roundwood.migration.item.RetiredCrossWindowItem
                    && block.asItem() == retired && !ModItems.all().contains(retired) && LegacyContent.items().contains(retired), "retired registration/visibility " + plain);
        }
        check(ModItems.CROSS_BEAMS.get() instanceof io.github.dirtyredz.roundwood.block.attachment.item.StyledAttachmentItem
                && TrimAttachmentItem.slotOf(new ItemStack(ModItems.CROSS_BEAMS.get())).equals(Optional.of(TrimSlot.CROSS_BEAMS)), "cross beams attachment item");
        Item sill = ForgeRegistries.ITEMS.getValue(id("arch_sill_trim"));
        check(sill == ModItems.ARCH_SILL_TRIM.get() && !ForgeRegistries.BLOCKS.containsKey(id("arch_sill_trim"))
                && sill instanceof ArchSillTrimItem && !(sill instanceof net.minecraft.world.item.BlockItem)
                && TrimAttachmentItem.slotOf(new ItemStack(sill)).equals(Optional.of(TrimSlot.SILL)) && ModItems.all().contains(sill)
                && TrimSlot.SILL.item() == sill, "the arch sill is an advertised item without a block, the sill slot's item");
        check(ForgeRegistries.ITEMS.containsKey(id("window_trim")) && !ForgeRegistries.BLOCKS.containsKey(id("window_trim")),
                "the window trim is an item without a block");
        Item windowTrim = LegacyContent.windowTrim();
        check(windowTrim instanceof UniversalTrimItem trim && trim.appearanceFamily() == AppearanceFamily.TRIM && trim.isAlias()
                && TrimAttachmentItem.slotOf(new ItemStack(windowTrim)).equals(Optional.of(TrimSlot.PERIMETER)),
                "the window trim is a hidden Universal Trim");
        check(windowTrim == ForgeRegistries.ITEMS.getValue(id("window_trim")) && LegacyContent.items().contains(windowTrim)
                && !ModItems.all().contains(windowTrim), "the window trim is hidden compatibility content");
        check(windowTrim.getDescriptionId().equals("block.roundwood.window_trim"), "the window trim keeps its own name");
        check(ModItems.ROUND_TRIM.get() instanceof UniversalTrimItem && !((UniversalTrimItem) ModItems.ROUND_TRIM.get()).isAlias(),
                "round_trim is the canonical Universal Trim");
        check(ModBlocks.roundDoor().asItem() == ModItems.ROUND_DOOR.get(), "an alias took the door's item mapping");
        check(ModBlocks.archedDoor().asItem() == ModItems.ARCHED_DOOR.get() && !ModBlocks.archedDoor().defaultBlockState().hasProperty(
                io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock.COLUMN), "the arched door is one item and one column");
        check(ModBlocks.roundWindow().asItem() == ModItems.ROUND_WINDOW.get(), "an alias took the window's item mapping");
        check(ModBlocks.roundTrim().asItem() == ModItems.ROUND_TRIM.get(), "an alias took the trim's item mapping");
        check(ModBlocks.archLeft().asItem() == ModItems.ROUND_WINDOW_ARCH_LEFT.get()
                && ModBlocks.archRight().asItem() == ModItems.ROUND_WINDOW_ARCH_RIGHT.get(), "arch item mappings");
        for (LegacyAppearances.Entry entry : LegacyAppearances.ALL) {
            Block block = ForgeRegistries.BLOCKS.getValue(id(entry.id()));
            Item item = ForgeRegistries.ITEMS.getValue(id(entry.id()));
            check(block != null && AppearanceDefaults.of(block).equals(entry.appearance()), entry.id() + ": alias block look");
            check(item instanceof AppearanceBlockItem alias && alias.isAlias(), entry.id() + ": not a hidden alias item");
            check(entry.family() != AppearanceFamily.TRIM || item instanceof UniversalTrimItem, entry.id() + ": a trim alias is no Universal Trim");
            check(AppearanceStacks.read(new ItemStack(item)).equals(entry.appearance()), entry.id() + ": alias stack look");
            check(((AppearanceBlockItem) item).getBlock() == canonicalBlock(entry.family()),
                    entry.id() + ": alias item does not place the canonical block");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blockEntityTypesCoverTheirFamilyAndItsAliases(GameTestHelper helper) {
        for (Block door : with(ModBlocks.archedDoor(), with(ModBlocks.roundDoor(), LegacyContent.doors()))) {
            check(ModBlockEntities.roundDoor().isValid(door.defaultBlockState()), door + " cannot carry the door entity");
            check(!ModBlockEntities.roundWindow().isValid(door.defaultBlockState())
                    && !ModBlockEntities.roundTrim().isValid(door.defaultBlockState()), door + " valid for another family");
        }
        List<Block> windows = new java.util.ArrayList<>(ModBlocks.windows());
        windows.addAll(LegacyContent.windows());
        windows.addAll(io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility.blocks());
        check(windows.size() == 42, "expected 27 + 15 window blocks, found " + windows.size());
        for (Block window : windows) {
            check(ModBlockEntities.roundWindow().isValid(window.defaultBlockState()), window + " cannot carry the window entity");
            check(!ModBlockEntities.roundDoor().isValid(window.defaultBlockState()), window + " valid for the door entity");
        }
        for (Block trim : with(ModBlocks.roundTrim(), LegacyContent.trims())) {
            check(ModBlockEntities.roundTrim().isValid(trim.defaultBlockState()), trim + " cannot carry the trim entity");
            check(!ModBlockEntities.roundDoor().isValid(trim.defaultBlockState()), trim + " valid for the door entity");
        }
        for (Block archway : ModBlocks.archways()) {
            check(ModBlockEntities.archway().isValid(archway.defaultBlockState()), archway + " cannot carry the archway entity");
            check(!ModBlockEntities.roundDoor().isValid(archway.defaultBlockState()) && !ModBlockEntities.roundTrim().isValid(
                    archway.defaultBlockState()) && !ModBlockEntities.roundWindow().isValid(archway.defaultBlockState()),
                    archway + " valid for another family");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tagsAreLoaded(GameTestHelper helper) {
        for (Block door : with(ModBlocks.archedDoor(), with(ModBlocks.roundDoor(), LegacyContent.doors()))) {
            BlockState state = door.defaultBlockState();
            check(state.is(BlockTags.WOODEN_DOORS) && state.is(BlockTags.DOORS) && state.is(BlockTags.MINEABLE_WITH_AXE), door + " door tags");
        }
        ItemStack arched = new ItemStack(ModItems.ARCHED_DOOR.get());
        check(arched.is(ItemTags.WOODEN_DOORS) && arched.is(ItemTags.DOORS) && arched.is(ModTags.ROUND_DOOR_ITEMS)
                && ModBlocks.archedDoor().defaultBlockState().is(ModTags.ROUND_DOORS), "arched door tags");
        ItemStack door = new ItemStack(ModItems.ROUND_DOOR.get());
        check(door.is(ItemTags.WOODEN_DOORS) && door.is(ItemTags.DOORS) && door.is(ModTags.ROUND_DOOR_ITEMS), "door item tags");
        check(ModBlocks.roundDoor().defaultBlockState().is(ModTags.ROUND_DOORS), "door family tag");
        check(ModBlocks.roundWindow().defaultBlockState().is(ModTags.ROUND_WINDOWS)
                && new ItemStack(ModItems.ROUND_WINDOW.get()).is(ModTags.ROUND_WINDOW_ITEMS), "window family tags");
        for (Block window : ModBlocks.windows()) {
            check(window.defaultBlockState().is(ModTags.ROUND_WINDOWS) && new ItemStack(window).is(ModTags.ROUND_WINDOW_ITEMS)
                    && window.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && !window.defaultBlockState().is(BlockTags.DOORS),
                    window + " window family tags");
        }
        check(ModBlocks.roundTrim().defaultBlockState().is(ModTags.ROUND_TRIMS)
                && new ItemStack(ModItems.ROUND_TRIM.get()).is(ModTags.ROUND_TRIM_ITEMS), "trim family tags");
        check(new ItemStack(LegacyContent.windowTrim()).is(ModTags.ROUND_TRIM_ITEMS)
                && !new ItemStack(LegacyContent.windowTrim()).is(ModTags.ROUND_WINDOW_ITEMS), "window trim item tags");
        for (Block block : with(ModBlocks.roundWindow(), LegacyContent.windows())) {
            check(block.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && !block.defaultBlockState().is(BlockTags.DOORS), block + " window tags");
        }
        for (Block block : with(ModBlocks.roundTrim(), LegacyContent.trims())) {
            BlockState state = block.defaultBlockState();
            check(state.is(BlockTags.MINEABLE_WITH_AXE) && !state.is(BlockTags.MINEABLE_WITH_PICKAXE), block + " trims mine with an axe");
            check(!state.is(BlockTags.DOORS), block + " a trim must not be a door");
        }
        for (Block archway : ModBlocks.archways()) {
            BlockState state = archway.defaultBlockState();
            check(state.is(ModTags.ARCHWAYS) && new ItemStack(archway).is(ModTags.ARCHWAY_ITEMS) && state.is(BlockTags.MINEABLE_WITH_AXE)
                    && !state.is(BlockTags.DOORS) && !state.is(ModTags.ROUND_DOORS) && !new ItemStack(archway).is(ItemTags.DOORS),
                    archway + " archway tags (never a door)");
        }
        for (Block piece : ModBlocks.archPieces()) {
            check(piece.defaultBlockState().is(ModTags.ARCH_PIECES) && new ItemStack(piece).is(ModTags.ARCH_PIECE_ITEMS)
                    && piece.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)
                    && !piece.defaultBlockState().is(BlockTags.DOORS), "modular piece gameplay tags");
        }
        for (Block alias : LegacyContent.blocks()) {
            if (io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility.isRetired(alias.defaultBlockState())) {
                check(alias.defaultBlockState().is(ModTags.ROUND_WINDOWS) && new ItemStack(alias).is(ModTags.ROUND_WINDOW_ITEMS), "retired window lost gameplay tags");
                continue;
            }
            check(!alias.defaultBlockState().is(ModTags.ROUND_DOORS) && !alias.defaultBlockState().is(ModTags.ROUND_WINDOWS)
                    && !alias.defaultBlockState().is(ModTags.ROUND_TRIMS), alias + ": family tags name only the canonical blocks");
        }
        helper.succeed();
    }

    /** Any planks, mixed, make the default (oak, untagged) object; there are exactly five recipes. */
    @GameTest(template = TEMPLATE)
    public static void craftingAcceptsMixedPlanksAndMakesTheDefault(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack o = new ItemStack(Items.OAK_PLANKS);
        ItemStack s = new ItemStack(Items.SPRUCE_PLANKS);
        ItemStack w = new ItemStack(Items.WARPED_PLANKS);
        ItemStack e = ItemStack.EMPTY;
        expect(level, grid(o, s, w, s, new ItemStack(Items.GOLD_NUGGET), o, w, o, s), ModItems.ROUND_DOOR.get(), "mixed-plank door");
        expect(level, grid(e, w, e, s, new ItemStack(Items.GLASS_PANE), o, e, s, e), ModItems.ROUND_WINDOW.get(), "mixed-plank window");
        expect(level, grid(o, e, s, e, e, e, w, e, o), ModItems.ROUND_TRIM.get(), "mixed-plank trim");
        ItemStack g = new ItemStack(Items.GLASS_PANE);
        expect(level, grid(o, s, e, w, g, e, o, e, e), ModItems.ROUND_WINDOW_ARCH_LEFT.get(), "mixed-plank left arch");
        expect(level, grid(e, s, o, e, g, w, e, e, o), ModItems.ROUND_WINDOW_ARCH_LEFT.get(), "mirrored left arch");
        expect(level, grid(o, s, w, e, g, e, e, o, e), ModItems.ROUND_WINDOW_ARCH_RIGHT.get(), "mixed-plank right arch");
        expect(level, grid(s, e, s, e, e, e, s, e, s), ModItems.ROUND_TRIM.get(), "spruce trim");
        check(find(level, grid(o, o, o, o, e, o, o, o, o)).map(r -> !r.getId().getNamespace().equals(Roundwood.MOD_ID)).orElse(true),
                "a ring of eight makes a trim");
        ItemStack b = new ItemStack(Items.BRICKS);
        check(find(level, grid(b, e, b, e, e, e, b, e, b)).isEmpty(), "bricks still craft a trim");
        check(find(level, grid(new ItemStack(ModItems.ROUND_DOOR.get()), new ItemStack(Items.RED_DYE), e, e, e, e, e, e, e)).isEmpty(),
                "a painting recipe is still loaded");
        long count = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(r -> r.getId().getNamespace().equals(Roundwood.MOD_ID)).count();
        check(count == 29, "expected 29 roundwood crafting recipes, loaded " + count);
        ItemStack k = new ItemStack(Items.STICK);
        var ladder = find(level, grid(k, e, k, k, k, k, k, e, k)).orElseThrow();
        check(ladder.assemble(grid(k, e, k, k, k, k, k, e, k), level.registryAccess()).is(Items.LADDER), "beams collided with the vanilla ladder");
        check(find(level, grid(e, o, e, s, k, w, e, o, e)).map(r -> !r.getId().getNamespace().equals(Roundwood.MOD_ID)).orElse(true),
                "the 0.8.0 window trim recipe is still loaded");
        helper.succeed();
    }

    /**
     * Every 0.7.0 shape, in mixed planks, mirrored and moved within the grid where it fits, makes its
     * own default item against everything the server loaded (no vanilla or Roundwood recipe takes it).
     */
    @GameTest(template = TEMPLATE)
    public static void theNewWindowRecipesMatchOnlyThemselves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        expectShape(level, ModItems.window(design(WindowProfile.BOTTOM_LEFT, WindowPattern.PLAIN)), "P ", "PG", "PP");
        expectShape(level, ModItems.window(design(WindowProfile.BOTTOM_RIGHT, WindowPattern.PLAIN)), " P ", " G ", "PPP");
        helper.succeed();
    }

    /** The 0.10.0 shapes likewise, the arch sill and (0.11.0) the arched door: mirrored and moved, each makes only its own item. */
    @GameTest(template = TEMPLATE)
    public static void theWindowShapeRecipesMatchOnlyThemselves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        expectShape(level, ModItems.window(design(WindowProfile.ARCH_BOTTOM_LEFT, WindowPattern.PLAIN)), "PP", "PG", "PP");
        expectShape(level, ModItems.window(design(WindowProfile.ARCH_BOTTOM_RIGHT, WindowPattern.PLAIN)), "PPP", " G ", "PPP");
        expectShape(level, ModItems.window(design(WindowProfile.TOP_LEFT, WindowPattern.PLAIN)), "PPP", "PG ", "P  ");
        expectShape(level, ModItems.window(design(WindowProfile.TOP_RIGHT, WindowPattern.PLAIN)), "PPP", " G ", "  P");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_TOP, WindowPattern.PLAIN)), "PPP", " G ");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_LEFT, WindowPattern.PLAIN)), "P ", "PG", "P ");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_BOTTOM, WindowPattern.PLAIN)), " G ", "PPP");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_RIGHT, WindowPattern.PLAIN)), "GP", " P", " P");
        expectShape(level, ModItems.ARCH_SILL_TRIM.get(), "PPP", "S S");
        expectShape(level, ModItems.ARCHED_DOOR.get(), " P ", "PNP", "PPP");
        expectShape(level, ModItems.CROSS_BEAMS.get(), " S ", "SSS", " S ");
        // 0.12.0
        expectShape(level, ModItems.archPiece(ArchPieceDesign.TOP_1X1), "PPP", "P P");
        expectShape(level, ModItems.archPiece(ArchPieceDesign.TOP_1X2), "PPP", "P P", " S ");
        expectShape(level, ModItems.archPiece(ArchPieceDesign.TOP_2X3), "PPP", "P P", "S S");
        expectShape(level, ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER), "PP", " P");
        for (Item retired : ModItems.archways()) {
            check(level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .noneMatch(recipe -> recipe.getResultItem(level.registryAccess()).is(retired)), "retired archway recipe remains");
        }
        helper.succeed();
    }

    private static WindowDesign design(WindowProfile profile, WindowPattern pattern) {
        return new WindowDesign(profile, pattern);
    }

    /** {@code rows} (P planks, G glass pane, S stick, N gold nugget), and its mirror image, at every position it fits in the 3 x 3 grid. */
    private static void expectShape(ServerLevel level, Item result, String... rows) {
        List<ItemStack> planks = List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.SPRUCE_PLANKS), new ItemStack(Items.WARPED_PLANKS));
        for (boolean mirrored : new boolean[] {false, true}) {
            int width = rows[0].length();
            for (int dx = 0; dx + width <= 3; dx++) {
                for (int dy = 0; dy + rows.length <= 3; dy++) {
                    ItemStack[] stacks = new ItemStack[9];
                    java.util.Arrays.fill(stacks, ItemStack.EMPTY);
                    int n = 0;
                    for (int y = 0; y < rows.length; y++) {
                        for (int x = 0; x < width; x++) {
                            char c = rows[y].charAt(mirrored ? width - 1 - x : x);
                            stacks[(y + dy) * 3 + x + dx] = switch (c) {
                                case 'P' -> planks.get(n++ % planks.size());
                                case 'G' -> new ItemStack(Items.GLASS_PANE);
                                case 'S' -> new ItemStack(Items.STICK);
                                case 'N' -> new ItemStack(Items.GOLD_NUGGET);
                                default -> ItemStack.EMPTY;
                            };
                        }
                    }
                    expect(level, grid(stacks), result, result + (mirrored ? " mirrored" : "") + " at " + dx + "," + dy);
                }
            }
        }
    }

    /** With no appearance in the world, canonical blocks drop their default item; aliases drop the canonical item in their old look. */
    @GameTest(template = TEMPLATE)
    public static void lootIsTheCanonicalItemInTheRightLook(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Block block : ModBlocks.canonical()) {
            List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), helper.getLevel(), pos, null);
            check(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1 && !drops.get(0).hasTag(),
                    block + " loot is " + drops);
        }
        // A window (canonical or alias) with oak trims on both faces also drops one plain oak Universal Trim per face (0.8.0 face pools).
        List<Block> windows = new java.util.ArrayList<>(ModBlocks.windows());
        windows.addAll(LegacyContent.windows());
        for (Block window : windows) {
            net.minecraft.world.level.block.state.BlockState state = window.defaultBlockState().setValue(WindowPaneBlock.TRIMMED, true);
            io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity entity = new io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity(
                    io.github.dirtyredz.roundwood.registry.ModBlockEntities.roundWindow(), pos, state);
            boolean trim=((WindowPaneBlock)window).profile().takesTrim();
            if(trim) check(entity.setTrims(io.github.dirtyredz.roundwood.block.attachment.FaceTrims.both(io.github.dirtyredz.roundwood.appearance.Appearance.DEFAULT)),"trim fixture");
            // an upper arch (0.10.0) with oak sills on both faces also drops one plain Arch Sill per face
            boolean sills = ((WindowPaneBlock) window).profile().takesSill();
            if (sills) {
                entity.setTrims(TrimSlot.SILL, io.github.dirtyredz.roundwood.block.attachment.FaceTrims.both(Appearance.DEFAULT));
            }
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), pos, entity);
            check(drops.size() == 1+(trim?2:0)+(sills?2:0) && drops.stream().filter(d -> d.is(ModItems.ROUND_TRIM.get()) && !d.hasTag()).count() == (trim?2:0)
                            && drops.stream().filter(d -> d.is(ModItems.ARCH_SILL_TRIM.get()) && !d.hasTag()).count() == (sills ? 2 : 0),
                    window + " trimmed loot is " + drops);
        }
        for (LegacyAppearances.Entry entry : LegacyAppearances.ALL) {
            Block alias = ForgeRegistries.BLOCKS.getValue(id(entry.id()));
            List<ItemStack> drops = Block.getDrops(alias.defaultBlockState(), helper.getLevel(), pos, null);
            Item canonical = canonicalBlock(entry.family()).asItem();
            check(drops.size() == 1 && drops.get(0).is(canonical) && AppearanceStacks.read(drops.get(0)).equals(entry.appearance()),
                    entry.id() + " loot is " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeTabHoldsExactlyTheAdvertisedDefaults(GameTestHelper helper) {
        CreativeModeTab tab = ModCreativeTab.ROUNDWOOD.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false,
                helper.getLevel().registryAccess()));
        List<ItemStack> shown = List.copyOf(tab.getDisplayItems());
        List<String> expected = new java.util.ArrayList<>(CANONICAL_BLOCKS);
        expected.add("arch_sill_trim");
        expected.add("cross_beams");
        check(shown.stream().map(stack -> ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath()).toList().equals(expected),
                "tab is not the three doors, 13 plain windows, four full and four thin arch pieces, Universal Trim, Arch Sill and Cross Beams: " + shown);
        check(shown.stream().noneMatch(ItemStack::hasTag), "tab entries carry an appearance: " + shown);
        check(AppearanceStacks.read(shown.get(0)).equals(Appearance.DEFAULT), "the tab's door is not oak");
        helper.succeed();
    }

    /** Viewer lists are built from the tab: each item once, as its oak default (the JEI "no subtype" key). */
    @GameTest(template = TEMPLATE)
    public static void everyAdvertisedItemIsListedOnceAsItsOakDefault(GameTestHelper helper) {
        CreativeModeTab tab = ModCreativeTab.ROUNDWOOD.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false,
                helper.getLevel().registryAccess()));
        List<ItemStack> shown = List.copyOf(tab.getDisplayItems());
        for (Item item : ModItems.all()) {
            List<ItemStack> listed = shown.stream().filter(stack -> stack.getItem() == item).toList();
            check(listed.size() == 1, item + " is listed " + listed.size() + " times in the tab");
            ItemStack stack = listed.get(0);
            check(!stack.hasTag() && AppearanceStacks.read(stack).equals(AppearanceDefaults.of(item))
                    && WindowGlassStacks.key(stack).equals(AppearanceDefaults.of(item).key()), item + " is not its plain default: " + stack);
        }
        check(ModItems.all().size() == 29 && shown.size() == 29, "tab lists " + shown.size() + " items, not the 29 of 0.17.0");
        check(shown.stream().anyMatch(stack -> stack.getItem() == ModItems.CROSS_BEAMS.get()), "Cross Beams is not listed");
        for (Item retired : io.github.dirtyredz.roundwood.migration.LegacyContent.items()) {
            check(shown.stream().noneMatch(stack -> stack.getItem() == retired), "hidden compatibility item " + retired + " is listed");
        }
        for (Item archway : ModItems.archways()) {
            check(archway instanceof io.github.dirtyredz.roundwood.migration.item.RetiredArchwayItem
                    && LegacyContent.items().contains(archway) && !ModItems.all().contains(archway), "retired archway visibility");
            check(AppearanceStacks.read(new ItemStack(archway)).equals(Appearance.DEFAULT), archway + " default is not oak");
        }
        helper.succeed();
    }

    /** The 18 canonical blocks in creative order, written out. */
    private static final List<String> CANONICAL_BLOCKS = List.of("round_door", "arched_door", "double_arched_door", "tall_double_arched_door",
            "round_window", "round_window_arch_left", "round_window_arch_right", "round_window_bottom_left", "round_window_bottom_right", "round_window_arch_bottom_left", "round_window_arch_bottom_right",
            "round_window_top_left", "round_window_top_right",
            "round_window_edge_top", "round_window_edge_left",
            "round_window_edge_bottom", "round_window_edge_right", "frameless_window",
            "arch_top_1x1", "arch_top_1x2", "arch_top_2x3", "keyhole_shoulder",
            "arch_top_1x1_thin", "arch_top_1x2_thin", "arch_top_2x3_thin", "keyhole_shoulder_thin", "round_trim");

    /** The canonical block of a family's 0.4.0 aliases (written out, not read from list positions). */
    private static Block canonicalBlock(io.github.dirtyredz.roundwood.appearance.AppearanceFamily family) {
        return switch (family) {
            case DOOR -> ModBlocks.roundDoor();
            case WINDOW -> ModBlocks.roundWindow();
            case TRIM -> ModBlocks.roundTrim();
        };
    }

    private static List<Block> with(Block canonical, List<? extends Block> aliases) {
        List<Block> out = new java.util.ArrayList<>();
        out.add(canonical);
        out.addAll(aliases);
        return out;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path);
    }

    private static TransientCraftingContainer grid(ItemStack... stacks) {
        TransientCraftingContainer grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < stacks.length; i++) {
            grid.setItem(i, stacks[i].copy());
        }
        return grid;
    }

    private static Optional<CraftingRecipe> find(ServerLevel level, TransientCraftingContainer grid) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
    }

    private static void expect(ServerLevel level, TransientCraftingContainer grid, Item result, String what) {
        var matches = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(candidate -> candidate.matches(grid, level)).toList();
        check(matches.size() == 1, what + ": expected one matching recipe, got " + matches.stream().map(CraftingRecipe::getId).toList());
        Optional<CraftingRecipe> recipe = find(level, grid);
        check(recipe.isPresent(), what + ": no recipe matched");
        ItemStack out = recipe.get().assemble(grid, level.registryAccess());
        check(out.is(result) && out.getCount() == 1 && !out.hasTag(), what + ": produced " + out);
    }
}
