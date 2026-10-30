package io.github.dirtyredz.roundwood.appearance;


import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorSwing;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.MUSHROOM;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.STONE_BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.oakPainted;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/**
 * An appearance survives everything it travels through: item stacks (including damaged or foreign
 * tags), world saves and client update tags (where a return to the default must also arrive).
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AppearancePersistenceGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private AppearancePersistenceGameTests() {
    }

    private static List<Item> canonicalItems() {
        return List.of(ModItems.ROUND_DOOR.get(), ModItems.ROUND_WINDOW.get(), ModItems.ROUND_TRIM.get());
    }

    private static List<Appearance> woodLooks() {
        List<Appearance> looks = new ArrayList<>(List.of(Appearance.DEFAULT, SPRUCE, MUSHROOM));
        for (DyeColor dye : DyeColor.values()) {
            looks.add(oakPainted(dye));
        }
        return looks;
    }

    private static ItemStack withAppearanceTag(Item item, CompoundTag appearance) {
        ItemStack stack = new ItemStack(item);
        io.github.dirtyredz.roundwood.appearance.item.StackPayloads.putCustom(stack, AppearanceNbt.TAG, appearance);
        return stack;
    }

    private static CompoundTag raw(String kind, String material, String paint) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        if (kind != null) {
            tag.putString("Kind", kind);
        }
        if (material != null) {
            tag.putString("Material", material);
        }
        if (paint != null) {
            tag.putString("Paint", paint);
        }
        return tag;
    }

    @GameTest(template = TEMPLATE)
    public static void itemsRoundTripEveryLook(GameTestHelper helper) {
        for (Item item : canonicalItems()) {
            List<Appearance> looks = new ArrayList<>(woodLooks());
            if (item == ModItems.ROUND_TRIM.get()) {
                looks.add(BRICKS);
                looks.add(STONE_BRICKS);
            }
            for (Appearance look : looks) {
                ItemStack stack = AppearanceStacks.create(item, look);
                check(AppearanceStacks.read(stack).equals(look), item + ": " + look.key() + " read back as "
                        + AppearanceStacks.read(stack).key());
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void defaultStacksCarryNoTagAndStackWithPlainItems(GameTestHelper helper) {
        for (Item item : canonicalItems()) {
            ItemStack made = AppearanceStacks.create(item, Appearance.DEFAULT);
            check(made.getComponentsPatch().isEmpty(), item + ": a default stack has a tag " + made.getComponentsPatch());
            check(ItemStack.isSameItemSameComponents(new ItemStack(item), made), item + ": default stack does not stack with a plain one");
            ItemStack styled = AppearanceStacks.create(item, SPRUCE);
            check(!styled.getComponentsPatch().isEmpty(), item + ": a styled stack has no tag");
            AppearanceStacks.write(styled, Appearance.DEFAULT);
            check(styled.getComponentsPatch().isEmpty(), item + ": writing the default left a tag " + styled.getComponentsPatch());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void masonryOnAWoodOnlyItemReadsAsDefault(GameTestHelper helper) {
        for (Item item : List.of(ModItems.ROUND_DOOR.get(), ModItems.ROUND_WINDOW.get())) {
            ItemStack stack = new ItemStack(item);
            AppearanceStacks.write(stack, BRICKS);
            check(AppearanceStacks.read(stack).equals(Appearance.DEFAULT), item + ": masonry read as " + AppearanceStacks.read(stack).key());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void malformedTagsReadAsDefaultAndUnavailableDonorsSurvive(GameTestHelper helper) {
        Item trim = ModItems.ROUND_TRIM.get();
        check(AppearanceStacks.read(withAppearanceTag(trim, raw("bogus", "minecraft:spruce_planks", null))).equals(Appearance.DEFAULT),
                "an unknown kind did not read as the default");
        check(AppearanceStacks.read(withAppearanceTag(trim, raw("wood", "not a valid id!!", null))).equals(Appearance.DEFAULT),
                "an unparseable material did not read as the default");
        check(AppearanceStacks.read(withAppearanceTag(trim, raw("wood", null, null))).equals(Appearance.DEFAULT),
                "a missing material did not read as the default");
        ResourceLocation fancy = ResourceLocation.fromNamespaceAndPath("othermod", "fancy_planks");
        check(!BuiltInRegistries.BLOCK.containsKey(fancy), "othermod:fancy_planks is unexpectedly registered");
        Appearance kept = AppearanceStacks.read(withAppearanceTag(trim, raw("wood", "othermod:fancy_planks", null)));
        check(kept.equals(Appearance.wood(fancy)), "an unavailable donor was not preserved: " + kept.key());
        Appearance unpainted = AppearanceStacks.read(withAppearanceTag(trim, raw("wood", "minecraft:spruce_planks", "neon")));
        check(unpainted.equals(SPRUCE) && !unpainted.isPainted(), "an unknown dye was not read as unpainted: " + unpainted.key());
        Appearance painted = AppearanceStacks.read(withAppearanceTag(trim, raw("wood", "minecraft:spruce_planks", "red")));
        check(painted.equals(SPRUCE.painted(DyeColor.RED)), "a valid dye was lost: " + painted.key());
        helper.succeed();
    }

    // ---- block entities ----

    private static Appearance look(BlockEntity entity) {
        if (entity instanceof RoundDoorBlockEntity door) {
            return door.appearance();
        } else if (entity instanceof RoundTrimBlockEntity trim) {
            return trim.appearance();
        } else if (entity instanceof RoundWindowBlockEntity window) {
            return window.appearance();
        }
        throw new IllegalStateException("not a Roundwood entity: " + entity);
    }

    private static void style(BlockEntity entity, Appearance next) {
        if (entity instanceof RoundDoorBlockEntity door) {
            door.setAppearance(next);
        } else if (entity instanceof RoundTrimBlockEntity trim) {
            trim.setAppearance(next);
        } else if (entity instanceof RoundWindowBlockEntity window) {
            window.setAppearance(next);
        } else {
            throw new IllegalStateException("not a Roundwood entity: " + entity);
        }
    }

    private static BlockEntity fresh(BlockEntity entity) {
        BlockEntity copy = entity.getType().create(entity.getBlockPos(), entity.getBlockState());
        check(copy != null, "could not create a fresh entity");
        return copy;
    }

    /** Save/load, then update tag round trips, then the default clearing paint on a client that showed red. */
    private static void exerciseEntity(BlockEntity entity, String what) {
        check(entity != null, what + ": no block entity");
        style(entity, oakPainted(DyeColor.RED));
        check(look(entity).equals(oakPainted(DyeColor.RED)), what + ": setAppearance did not stick");
        BlockPos pos = entity.getBlockPos();
        BlockState state = entity.getBlockState();

        BlockEntity loaded = BlockEntity.loadStatic(pos, state, entity.saveWithFullMetadata(TestRegistries.get()), TestRegistries.get());
        check(loaded != null && look(loaded).equals(oakPainted(DyeColor.RED)), what + ": save/load lost the look");

        BlockEntity client = fresh(entity);
        client.loadWithComponents(entity.getUpdateTag(TestRegistries.get()), TestRegistries.get());
        check(look(client).equals(oakPainted(DyeColor.RED)), what + ": update tag lost the look");

        style(entity, Appearance.DEFAULT);
        CompoundTag cleared = entity.getUpdateTag(TestRegistries.get());
        check(!cleared.contains(AppearanceNbt.TAG), what + ": a default look is still in the update tag " + cleared);
        client.loadWithComponents(cleared, TestRegistries.get());
        check(look(client).equals(Appearance.DEFAULT), what + ": paint removal did not reach the client, shows " + look(client).key());
        check(!entity.saveWithFullMetadata(TestRegistries.get()).contains(AppearanceNbt.TAG), what + ": a default look is still saved");
    }

    @GameTest(template = TEMPLATE)
    public static void doorAppearanceSurvivesSaveAndUpdate(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "persist_door", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, ModItems.ROUND_DOOR.get(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "door placement");
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
        exerciseEntity(anchorEntity(helper, door), "door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void trimAppearanceSurvivesSaveAndUpdate(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "persist_trim", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, placer, new ItemStack(ModItems.ROUND_TRIM.get()), ANCHOR).consumesAction(), "trim placement");
        TrimLayout.Assembly trim = trimAssembly(helper, ANCHOR, Direction.NORTH);
        exerciseEntity(helper.getLevel().getBlockEntity(trim.anchor()), "trim");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void windowAppearanceSurvivesSaveAndUpdate(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(ANCHOR, ModBlocks.roundWindow().defaultBlockState());
        exerciseEntity(helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR)), "window");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void loadingAnUpdateNeverResetsTheDoorSwing(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "persist_swing", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, ModItems.ROUND_DOOR.get(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "door placement");
        RoundDoorBlockEntity entity = anchorEntity(helper, assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT));
        entity.setAppearance(oakPainted(DyeColor.BLUE));
        DoorSwing swing = entity.swing();
        double now = helper.getLevel().getGameTime();
        double before = swing.progress(now);
        entity.loadWithComponents(entity.getUpdateTag(TestRegistries.get()), TestRegistries.get());
        check(entity.swing() == swing, "loading an update replaced the swing object");
        check(swing.progress(now) == before, "loading an update changed the swing progress");
        check(entity.appearance().equals(oakPainted(DyeColor.BLUE)), "the update lost the look");
        helper.succeed();
    }
}
