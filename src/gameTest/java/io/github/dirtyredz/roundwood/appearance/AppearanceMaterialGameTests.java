package io.github.dirtyredz.roundwood.appearance;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.appearance.material.PlankMaterials;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.MUSHROOM;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.trimLook;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.frontHit;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/**
 * Which items count as wood and which as masonry (tag based, so modded woods work), and that the
 * acceptance holds end to end through a real gesture.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AppearanceMaterialGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Item> VANILLA_PLANKS = List.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS,
            Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS,
            Items.BAMBOO_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS);
    private static final List<Item> BRICK_ITEMS = List.of(Items.BRICKS, Items.STONE_BRICKS, Items.DEEPSLATE_BRICKS, Items.MUD_BRICKS);

    private AppearanceMaterialGameTests() {
    }

    private static ResourceLocation blockId(Item item) {
        return BuiltInRegistries.BLOCK.getKey(((BlockItem) item).getBlock());
    }

    private static ItemStack withTag(Item item, String key) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().put(key, new CompoundTag());
        return stack;
    }

    @GameTest(template = TEMPLATE)
    public static void everyVanillaPlankIsAcceptedAsItsOwnBlock(GameTestHelper helper) {
        check(VANILLA_PLANKS.size() == 11, "expected 11 vanilla plank items");
        for (Item item : VANILLA_PLANKS) {
            Optional<ResourceLocation> found = PlankMaterials.fromStack(new ItemStack(item));
            check(found.isPresent() && found.get().equals(blockId(item)), item + ": read as " + found);
            check(PlankMaterials.present().contains(blockId(item)), item + ": missing from present()");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aTaggedModdedWoodStandInIsAccepted(GameTestHelper helper) {
        Optional<ResourceLocation> found = PlankMaterials.fromStack(new ItemStack(Items.MUSHROOM_STEM));
        check(found.equals(Optional.of(blockId(Items.MUSHROOM_STEM))), "mushroom stem read as " + found);
        check(PlankMaterials.present().contains(ResourceLocation.fromNamespaceAndPath("minecraft", "mushroom_stem")),
                "mushroom_stem missing from present()");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nonPlanksAndLoadedStacksAreRefusedAsWood(GameTestHelper helper) {
        check(PlankMaterials.fromStack(new ItemStack(Items.STONE)).isEmpty(), "stone accepted as wood");
        check(PlankMaterials.fromStack(new ItemStack(Items.BRICKS)).isEmpty(), "bricks accepted as wood");
        check(PlankMaterials.fromStack(ItemStack.EMPTY).isEmpty(), "an empty stack accepted as wood");
        check(PlankMaterials.fromStack(withTag(Items.OAK_PLANKS, "BlockEntityTag")).isEmpty(), "planks with BlockEntityTag accepted");
        check(PlankMaterials.fromStack(withTag(Items.OAK_PLANKS, "BlockStateTag")).isEmpty(), "planks with BlockStateTag accepted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void exactlyTheFourBrickBlocksAreMasonry(GameTestHelper helper) {
        for (Item item : BRICK_ITEMS) {
            Optional<ResourceLocation> found = MasonryMaterials.fromStack(new ItemStack(item));
            check(found.equals(Optional.of(blockId(item))), item + ": read as " + found);
            check(MasonryMaterials.blocks().contains(blockId(item)), item + ": missing from blocks()");
            check(MasonryMaterials.fromStack(withTag(item, "BlockEntityTag")).isEmpty(), item + " with BlockEntityTag accepted");
        }
        check(MasonryMaterials.blocks().size() == 4, "expected four masonry blocks, got " + MasonryMaterials.blocks());
        check(MasonryMaterials.fromStack(new ItemStack(Items.COBBLESTONE)).isEmpty(), "cobblestone accepted as masonry");
        check(MasonryMaterials.fromStack(new ItemStack(Items.OAK_PLANKS)).isEmpty(), "planks accepted as masonry");
        check(MasonryMaterials.fromStack(ItemStack.EMPTY).isEmpty(), "an empty stack accepted as masonry");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aTaggedWoodStandInRestylesATrimThroughARealGesture(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "material_tag_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, placer, new ItemStack(ModItems.ROUND_TRIM.get()), ANCHOR).consumesAction(), "trim placement");
        TrimLayout.Assembly trim = trimAssembly(helper, ANCHOR, Direction.NORTH);
        BlockHitResult hit = frontHit(trim, 0.05, 1.0);
        ServerPlayer player = player(helper, "material_tag", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stems = new ItemStack(Items.MUSHROOM_STEM, 2);
        check(use(helper, player, stems, ItemStack.EMPTY, hit, true).consumesAction(), "the gesture was not handled");
        check(trimLook(helper, trim).equals(MUSHROOM), "the trim shows " + trimLook(helper, trim).key());
        check(stems.getCount() == 1, "expected one stem consumed, left " + stems.getCount());
        assertTrim(helper, trim, oakTrim());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stoneIsNotAGestureAndLeavesTheTrimAlone(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "material_stone_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, placer, new ItemStack(ModItems.ROUND_TRIM.get()), ANCHOR).consumesAction(), "trim placement");
        TrimLayout.Assembly trim = trimAssembly(helper, ANCHOR, Direction.NORTH);
        ServerPlayer player = player(helper, "material_stone", GameType.SURVIVAL, Direction.NORTH);
        // Whatever vanilla then does with the stone (placing it beside the frame), the frame is untouched.
        use(helper, player, new ItemStack(Items.STONE, 2), ItemStack.EMPTY, frontHit(trim, 0.05, 1.0), true);
        check(trimLook(helper, trim).equals(Appearance.DEFAULT), "stone restyled the trim: " + trimLook(helper, trim).key());
        check(!trimLook(helper, trim).equals(BRICKS), "stone made the trim masonry");
        assertTrim(helper, trim, oakTrim());
        helper.succeed();
    }
}
