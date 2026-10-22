package io.github.dirtyredz.roundwood.data;

import io.github.dirtyredz.roundwood.block.attachment.item.ArchSillTrimItem;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
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

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * Registrations, tags, recipes, loot and the creative tab, as loaded by a dedicated server: 29
 * canonical blocks (round and (0.11.0) arched door, 26 windows, round trim), 30 advertised items (0.9.0: the Universal Trim is
 * {@code round_trim}; 0.10.0: the Arch Sill, an item only), 49 hidden 0.4.0 aliases that keep their
 * looks, the hidden 0.8.0 window trim (an item only), and nothing else.
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
        check(blocks == 78, "expected 29 + 49 blocks, found " + blocks);
        check(items == 80, "expected 29 + the arch sill + 49 + the window trim items, found " + items);
        for (String id : CANONICAL_BLOCKS) {
            check(ForgeRegistries.BLOCKS.containsKey(id(id)) && ForgeRegistries.ITEMS.containsKey(id(id)), "missing " + id);
            check(ForgeRegistries.BLOCKS.getValue(id(id)).asItem() == ForgeRegistries.ITEMS.getValue(id(id)), id + ": item mapping");
        }
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
        check(windows.size() == 41, "expected 26 + 15 window blocks, found " + windows.size());
        for (Block window : windows) {
            check(ModBlockEntities.roundWindow().isValid(window.defaultBlockState()), window + " cannot carry the window entity");
            check(!ModBlockEntities.roundDoor().isValid(window.defaultBlockState()), window + " valid for the door entity");
        }
        for (Block trim : with(ModBlocks.roundTrim(), LegacyContent.trims())) {
            check(ModBlockEntities.roundTrim().isValid(trim.defaultBlockState()), trim + " cannot carry the trim entity");
            check(!ModBlockEntities.roundDoor().isValid(trim.defaultBlockState()), trim + " valid for the door entity");
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
        for (Block alias : LegacyContent.blocks()) {
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
        check(count == 30, "expected 30 roundwood crafting recipes, loaded " + count);
        ItemStack k = new ItemStack(Items.STICK);
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
        expectShape(level, ModItems.window(design(WindowProfile.ROUND, WindowPattern.CROSS)), "SPS", "PGP", "SPS");
        expectShape(level, ModItems.window(design(WindowProfile.ARCH_LEFT, WindowPattern.CROSS)), "PPS", "PGS", "P  ");
        expectShape(level, ModItems.window(design(WindowProfile.ARCH_RIGHT, WindowPattern.CROSS)), "PPP", "SGS", " P ");
        expectShape(level, ModItems.window(design(WindowProfile.BOTTOM_LEFT, WindowPattern.CROSS)), "P  ", "PGS", "PPS");
        expectShape(level, ModItems.window(design(WindowProfile.BOTTOM_RIGHT, WindowPattern.CROSS)), " P ", "SGS", "PPP");
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
        expectShape(level, ModItems.window(design(WindowProfile.ARCH_BOTTOM_LEFT, WindowPattern.CROSS)), "PP ", "PGS", "PPS");
        expectShape(level, ModItems.window(design(WindowProfile.ARCH_BOTTOM_RIGHT, WindowPattern.CROSS)), "PPP", "SGS", "PPP");
        expectShape(level, ModItems.window(design(WindowProfile.TOP_LEFT, WindowPattern.CROSS)), "PPP", "PGS", "PS ");
        expectShape(level, ModItems.window(design(WindowProfile.TOP_RIGHT, WindowPattern.CROSS)), "PPP", "SGS", "  P");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_TOP, WindowPattern.CROSS)), "PPP", "SGS");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_LEFT, WindowPattern.CROSS)), "PS", "PG", "PS");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_BOTTOM, WindowPattern.CROSS)), "SGS", "PPP");
        expectShape(level, ModItems.window(design(WindowProfile.EDGE_RIGHT, WindowPattern.CROSS)), "GP", "SP", "SP");
        expectShape(level, ModItems.ARCH_SILL_TRIM.get(), "PPP", "S S");
        expectShape(level, ModItems.ARCHED_DOOR.get(), " P ", "PNP", "PPP");
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
            entity.setTrims(io.github.dirtyredz.roundwood.block.attachment.FaceTrims.both(io.github.dirtyredz.roundwood.appearance.Appearance.DEFAULT));
            // an upper arch (0.10.0) with oak sills on both faces also drops one plain Arch Sill per face
            boolean sills = ((WindowPaneBlock) window).profile().takesSill();
            if (sills) {
                entity.setTrims(TrimSlot.SILL, io.github.dirtyredz.roundwood.block.attachment.FaceTrims.both(Appearance.DEFAULT));
            }
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), pos, entity);
            check(drops.size() == (sills ? 5 : 3) && drops.stream().filter(d -> d.is(ModItems.ROUND_TRIM.get()) && !d.hasTag()).count() == 2
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
        check(shown.stream().map(stack -> ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath()).toList().equals(expected),
                "tab is not the two doors, the 26 windows (each plain then crossed), Universal Trim, Arch Sill: " + shown);
        check(shown.stream().noneMatch(ItemStack::hasTag), "tab entries carry an appearance: " + shown);
        check(AppearanceStacks.read(shown.get(0)).equals(Appearance.DEFAULT), "the tab's door is not oak");
        helper.succeed();
    }

    /** The 29 canonical blocks in creative order, written out. */
    private static final List<String> CANONICAL_BLOCKS = List.of("round_door", "arched_door",
            "round_window", "round_window_cross", "round_window_arch_left", "round_window_arch_left_cross",
            "round_window_arch_right", "round_window_arch_right_cross", "round_window_bottom_left", "round_window_bottom_left_cross",
            "round_window_bottom_right", "round_window_bottom_right_cross",
            "round_window_arch_bottom_left", "round_window_arch_bottom_left_cross", "round_window_arch_bottom_right",
            "round_window_arch_bottom_right_cross", "round_window_top_left", "round_window_top_left_cross", "round_window_top_right",
            "round_window_top_right_cross", "round_window_edge_top", "round_window_edge_top_cross", "round_window_edge_left",
            "round_window_edge_left_cross", "round_window_edge_bottom", "round_window_edge_bottom_cross", "round_window_edge_right",
            "round_window_edge_right_cross", "round_trim");

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
        Optional<CraftingRecipe> recipe = find(level, grid);
        check(recipe.isPresent(), what + ": no recipe matched");
        ItemStack out = recipe.get().assemble(grid, level.registryAccess());
        check(out.is(result) && out.getCount() == 1 && !out.hasTag(), what + ": produced " + out);
    }
}
