package io.github.dirtyredz.roundwood.block.door.lifecycle;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorColumn;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.fixture.PlacementCanceller;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assertOneAnchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.blockEntities;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.useItemOn;

/** The anchor block entity's life: created with the door, gone with it, persisted, and carried through structure transforms. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorLifecycleGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90};
    private static final Mirror[] MIRRORS = {Mirror.NONE, Mirror.LEFT_RIGHT};

    private DoorLifecycleGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void oneAnchorEntityAfterPlacementAndNoneAfterRemoval(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            ServerPlayer player = player(helper, "lifecycle_" + hinge, GameType.SURVIVAL, Direction.NORTH);
            check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR, hinge).consumesAction(), hinge + ": placement failed");
            DoorLayout.Assembly assembly = assembly(helper, ANCHOR, Direction.NORTH, hinge);
            assertOneAnchorEntity(helper, assembly);
            helper.setBlock(ANCHOR.above(), Blocks.AIR); // removing any part removes the whole door
            assertNoDoor(helper, assembly);
            check(blockEntities(helper, assembly) == 0, hinge + ": block entity left after removal");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledPlacementLeavesNoBlockEntity(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "cancelled", GameType.SURVIVAL, Direction.NORTH);
        check(PlacementCanceller.PLAYER.equals(player.getName().getString()), "fixture player is not the cancelled one");
        ItemStack stack = new ItemStack(oakDoor().asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        useItemOn(helper, player, stack, ANCHOR.below(), Direction.UP);
        DoorLayout.Assembly assembly = assembly(helper, ANCHOR, Direction.NORTH);
        assertNoDoor(helper, assembly);
        check(blockEntities(helper, assembly) == 0, "block entity left after a cancelled placement");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openAndHingeSurviveASaveLoadRoundTrip(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            ServerPlayer player = player(helper, "roundtrip_" + hinge, GameType.SURVIVAL, Direction.NORTH);
            check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR, hinge).consumesAction(), hinge + ": placement failed");
            BlockPos anchor = helper.absolutePos(ANCHOR);
            oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(anchor), anchor, true);
            BlockState state = helper.getLevel().getBlockState(anchor);
            BlockEntity entity = helper.getLevel().getBlockEntity(anchor);
            check(entity instanceof RoundDoorBlockEntity, hinge + ": no block entity at the anchor");
            CompoundTag tag = entity.saveWithFullMetadata();
            BlockEntity copy = BlockEntity.loadStatic(anchor, state, tag);
            check(copy instanceof RoundDoorBlockEntity, hinge + ": saved entity did not load back as a round door entity: " + copy);
            check(copy.getType() == ModBlockEntities.ROUND_DOOR.get(), hinge + ": wrong block entity type after load");
            check(copy.getBlockState().getValue(DoorBlock.OPEN) && copy.getBlockState().getValue(DoorBlock.HINGE) == hinge,
                    hinge + ": open or hinge lost across the round trip: " + copy.getBlockState());
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void leftHingedDoorSurvivesStructureTransforms(GameTestHelper helper) {
        transformed(helper, DoorHingeSide.LEFT, 0);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void rightHingedDoorSurvivesStructureTransforms(GameTestHelper helper) {
        transformed(helper, DoorHingeSide.RIGHT, 0);
    }

    /**
     * Builds a door in the source corner, copies it to a template and places it at the centre under
     * rotation / mirror combination {@code index}, then checks the result and moves to the next combination.
     */
    private static void transformed(GameTestHelper helper, DoorHingeSide hinge, int index) {
        if (index == 0) {
            floor(helper);
            ServerPlayer player = player(helper, "structure_" + hinge, GameType.SURVIVAL, Direction.NORTH);
            check(placeDoor(helper, player, oakDoor().asItem(), new BlockPos(1, 1, 1), hinge).consumesAction(), "source placement failed");
        }
        Rotation rotation = ROTATIONS[index / MIRRORS.length];
        Mirror mirror = MIRRORS[index % MIRRORS.length];
        String name = hinge + "/" + rotation + "/" + mirror;
        ServerLevel level = helper.getLevel();
        BlockPos from = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos dest = helper.absolutePos(new BlockPos(5, 1, 5));
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, from, new Vec3i(3, 2, 3), false, Blocks.STRUCTURE_VOID);
        template.placeInWorld(level, dest, dest, new StructurePlaceSettings().setRotation(rotation).setMirror(mirror),
                level.getRandom(), Block.UPDATE_CLIENTS);

        DoorLayout.Assembly assembly = findPlacedAssembly(helper, dest);
        check(assembly != null, name + ": no left-column lower part found at the destination");
        check(assembly.complete(level, oakDoor()), name + ": placed assembly is incomplete");
        assertOneAnchorEntity(helper, assembly);
        helper.runAfterDelay(3, () -> {
            check(assembly.complete(level, oakDoor()), name + ": integrity check removed or altered the placed door");
            assertOneAnchorEntity(helper, assembly);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = 0; y <= 1; y++) {
                        level.removeBlock(dest.offset(x, y, z), false);
                    }
                }
            }
            if (index + 1 < ROTATIONS.length * MIRRORS.length) {
                transformed(helper, hinge, index + 1);
            } else {
                helper.succeed();
            }
        });
    }

    /** The assembly whose left-column lower part lies in the 5 x 5 box around {@code dest}; the anchor may have moved. */
    private static DoorLayout.Assembly findPlacedAssembly(GameTestHelper helper, BlockPos dest) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = dest.offset(x, 0, z);
                BlockState state = helper.getLevel().getBlockState(pos);
                if (state.is(oakDoor()) && state.getValue(RoundDoorBlock.COLUMN) == DoorColumn.LEFT
                        && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    return DoorLayout.of(pos, state);
                }
            }
        }
        return null;
    }
}
