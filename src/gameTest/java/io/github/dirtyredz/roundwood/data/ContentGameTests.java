package io.github.dirtyredz.roundwood.data;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModCreativeTab;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.registry.ModTags;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
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
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/** Registrations, tags, recipes, loot and the creative tab, as loaded by a dedicated server. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentGameTests {
    private ContentGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void thirtyBlocksAndItemsAreRegistered(GameTestHelper helper) {
        long blocks = ForgeRegistries.BLOCKS.getKeys().stream().filter(k -> k.getNamespace().equals(Roundwood.MOD_ID)).count();
        long items = ForgeRegistries.ITEMS.getKeys().stream().filter(k -> k.getNamespace().equals(Roundwood.MOD_ID)).count();
        check(blocks == 30, "expected 30 blocks, found " + blocks);
        check(items == 30, "expected 30 items, found " + items);
        for (Finish finish : FinishCatalog.ALL) {
            check(ForgeRegistries.BLOCKS.containsKey(id(finish.doorId())), "missing " + finish.doorId());
            check(ForgeRegistries.BLOCKS.containsKey(id(finish.windowId())), "missing " + finish.windowId());
            check(ModBlocks.door(finish).asItem() != Items.AIR && ModBlocks.window(finish).asItem() != Items.AIR, "missing item for " + finish.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tagsAreLoaded(GameTestHelper helper) {
        for (Block door : ModBlocks.doors()) {
            check(door.defaultBlockState().is(BlockTags.WOODEN_DOORS) && door.defaultBlockState().is(BlockTags.DOORS), door + " not a door");
            check(door.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && door.defaultBlockState().is(ModTags.ROUND_DOORS), door + " tags");
            ItemStack stack = new ItemStack(door);
            check(stack.is(ItemTags.WOODEN_DOORS) && stack.is(ItemTags.DOORS) && stack.is(ModTags.ROUND_DOOR_ITEMS), door + " item tags");
        }
        for (Block window : ModBlocks.windows()) {
            check(window.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && window.defaultBlockState().is(ModTags.ROUND_WINDOWS), window + " tags");
            check(!window.defaultBlockState().is(BlockTags.DOORS), window + " must not be a door");
            check(new ItemStack(window).is(ModTags.ROUND_WINDOW_ITEMS), window + " item tag");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void craftingRecipesProduceTheRightItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (Finish finish : FinishCatalog.NATURAL) {
            ItemStack p = new ItemStack(finish.planks());
            ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);
            expect(level, grid(p, p, p, p, nugget, p, p, p, p), ModBlocks.door(finish).asItem(), finish.id() + " door");
            ItemStack e = ItemStack.EMPTY;
            expect(level, grid(e, p, e, p, new ItemStack(Items.GLASS_PANE), p, e, p, e), ModBlocks.window(finish).asItem(), finish.id() + " window");
        }
        long count = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(r -> r.getId().getNamespace().equals(Roundwood.MOD_ID)).count();
        check(count == 30, "expected 30 roundwood crafting recipes, loaded " + count);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void paintingAndRepainting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Finish red = FinishCatalog.PAINTED.get(2);
        Finish green = FinishCatalog.PAINTED.get(0);
        Item redDye = DyeItem.byColor(red.paint().orElseThrow());
        ItemStack e = ItemStack.EMPTY;
        expect(level, grid(new ItemStack(ModBlocks.door(FinishCatalog.PAINT_BASE)), new ItemStack(redDye), e, e, e, e, e, e, e),
                ModBlocks.door(red).asItem(), "oak door + red dye");
        expect(level, grid(new ItemStack(ModBlocks.door(green)), new ItemStack(redDye), e, e, e, e, e, e, e),
                ModBlocks.door(red).asItem(), "green door + red dye");
        expect(level, grid(new ItemStack(ModBlocks.window(FinishCatalog.PAINT_BASE)), new ItemStack(redDye), e, e, e, e, e, e, e),
                ModBlocks.window(red).asItem(), "oak window + red dye");
        check(find(level, grid(new ItemStack(ModBlocks.door(red)), new ItemStack(redDye), e, e, e, e, e, e, e)).isEmpty(),
                "a red door can be 'repainted' red");
        check(find(level, grid(new ItemStack(ModBlocks.door(FinishCatalog.NATURAL.get(1))), new ItemStack(redDye), e, e, e, e, e, e, e)).isEmpty(),
                "a spruce door can be painted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyBlockDropsItself(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Block block : ModBlocks.all()) {
            List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), helper.getLevel(), pos, null);
            check(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1,
                    block + " loot is " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeTabHoldsEveryItemOnce(GameTestHelper helper) {
        CreativeModeTab tab = ModCreativeTab.ROUNDWOOD.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false,
                helper.getLevel().registryAccess()));
        List<Item> shown = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
        Set<Item> unique = new HashSet<>(shown);
        check(shown.size() == 30 && unique.size() == 30, "tab shows " + shown.size() + " stacks, " + unique.size() + " distinct");
        check(unique.containsAll(ModItems.all()), "tab is missing items");
        helper.succeed();
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
        check(out.is(result) && out.getCount() == 1, what + ": produced " + out);
    }
}
