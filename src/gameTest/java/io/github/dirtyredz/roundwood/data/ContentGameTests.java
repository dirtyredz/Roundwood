package io.github.dirtyredz.roundwood.data;

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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * Registrations, tags, recipes, loot and the creative tab, as loaded by a dedicated server: five
 * advertised entries (door, round window, the two arch pieces, trim), 49 hidden 0.4.0 aliases that
 * keep their looks, and nothing else.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentGameTests {
    private ContentGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void fiveEntriesAndFortyNineHiddenAliasesAreRegistered(GameTestHelper helper) {
        long blocks = ForgeRegistries.BLOCKS.getKeys().stream().filter(k -> k.getNamespace().equals(Roundwood.MOD_ID)).count();
        long items = ForgeRegistries.ITEMS.getKeys().stream().filter(k -> k.getNamespace().equals(Roundwood.MOD_ID)).count();
        check(blocks == 54, "expected 5 + 49 blocks, found " + blocks);
        check(items == 54, "expected 5 + 49 items, found " + items);
        for (String id : List.of("round_door", "round_window", "round_window_arch_left", "round_window_arch_right", "round_trim")) {
            check(ForgeRegistries.BLOCKS.containsKey(id(id)) && ForgeRegistries.ITEMS.containsKey(id(id)), "missing " + id);
        }
        check(ModBlocks.roundDoor().asItem() == ModItems.ROUND_DOOR.get(), "an alias took the door's item mapping");
        check(ModBlocks.roundWindow().asItem() == ModItems.ROUND_WINDOW.get(), "an alias took the window's item mapping");
        check(ModBlocks.roundTrim().asItem() == ModItems.ROUND_TRIM.get(), "an alias took the trim's item mapping");
        check(ModBlocks.archLeft().asItem() == ModItems.ROUND_WINDOW_ARCH_LEFT.get()
                && ModBlocks.archRight().asItem() == ModItems.ROUND_WINDOW_ARCH_RIGHT.get(), "arch item mappings");
        for (LegacyAppearances.Entry entry : LegacyAppearances.ALL) {
            Block block = ForgeRegistries.BLOCKS.getValue(id(entry.id()));
            Item item = ForgeRegistries.ITEMS.getValue(id(entry.id()));
            check(block != null && AppearanceDefaults.of(block).equals(entry.appearance()), entry.id() + ": alias block look");
            check(item instanceof AppearanceBlockItem alias && alias.isAlias(), entry.id() + ": not a hidden alias item");
            check(AppearanceStacks.read(new ItemStack(item)).equals(entry.appearance()), entry.id() + ": alias stack look");
            check(((AppearanceBlockItem) item).getBlock() == canonicalBlock(entry.family()),
                    entry.id() + ": alias item does not place the canonical block");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blockEntityTypesCoverTheirFamilyAndItsAliases(GameTestHelper helper) {
        for (Block door : with(ModBlocks.roundDoor(), LegacyContent.doors())) {
            check(ModBlockEntities.roundDoor().isValid(door.defaultBlockState()), door + " cannot carry the door entity");
            check(!ModBlockEntities.roundWindow().isValid(door.defaultBlockState())
                    && !ModBlockEntities.roundTrim().isValid(door.defaultBlockState()), door + " valid for another family");
        }
        List<Block> windows = with(ModBlocks.roundWindow(), LegacyContent.windows());
        windows.addAll(ModBlocks.arches());
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
        for (Block door : with(ModBlocks.roundDoor(), LegacyContent.doors())) {
            BlockState state = door.defaultBlockState();
            check(state.is(BlockTags.WOODEN_DOORS) && state.is(BlockTags.DOORS) && state.is(BlockTags.MINEABLE_WITH_AXE), door + " door tags");
        }
        ItemStack door = new ItemStack(ModItems.ROUND_DOOR.get());
        check(door.is(ItemTags.WOODEN_DOORS) && door.is(ItemTags.DOORS) && door.is(ModTags.ROUND_DOOR_ITEMS), "door item tags");
        check(ModBlocks.roundDoor().defaultBlockState().is(ModTags.ROUND_DOORS), "door family tag");
        check(ModBlocks.roundWindow().defaultBlockState().is(ModTags.ROUND_WINDOWS)
                && new ItemStack(ModItems.ROUND_WINDOW.get()).is(ModTags.ROUND_WINDOW_ITEMS), "window family tags");
        for (Block arch : ModBlocks.arches()) {
            check(arch.defaultBlockState().is(ModTags.ROUND_WINDOWS) && new ItemStack(arch).is(ModTags.ROUND_WINDOW_ITEMS)
                    && arch.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && !arch.defaultBlockState().is(BlockTags.DOORS),
                    arch + " window family tags");
        }
        check(ModBlocks.roundTrim().defaultBlockState().is(ModTags.ROUND_TRIMS)
                && new ItemStack(ModItems.ROUND_TRIM.get()).is(ModTags.ROUND_TRIM_ITEMS), "trim family tags");
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
        check(count == 5, "expected 5 roundwood crafting recipes, loaded " + count);
        helper.succeed();
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
    public static void creativeTabHoldsExactlyTheFiveDefaults(GameTestHelper helper) {
        CreativeModeTab tab = ModCreativeTab.ROUNDWOOD.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false,
                helper.getLevel().registryAccess()));
        List<ItemStack> shown = List.copyOf(tab.getDisplayItems());
        check(shown.stream().map(ItemStack::getItem).toList().equals(List.of(ModItems.ROUND_DOOR.get(), ModItems.ROUND_WINDOW.get(),
                ModItems.ROUND_WINDOW_ARCH_LEFT.get(), ModItems.ROUND_WINDOW_ARCH_RIGHT.get(), ModItems.ROUND_TRIM.get())),
                "tab is not door, window, left arch, right arch, trim: " + shown);
        check(shown.stream().noneMatch(ItemStack::hasTag), "tab entries carry an appearance: " + shown);
        check(AppearanceStacks.read(shown.get(0)).equals(Appearance.DEFAULT), "the tab's door is not oak");
        helper.succeed();
    }

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
