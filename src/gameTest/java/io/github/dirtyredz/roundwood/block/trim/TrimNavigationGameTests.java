package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.point;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.rel;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.brickTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fitToDoor;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fittedTo;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/**
 * The frame is decorative and never obstructs: no collision, nothing stands on it, it is pathable, it
 * leaves a fitted door's own collision untouched, and mobs walk straight through the aperture.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimNavigationGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 4);
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private TrimNavigationGameTests() {
    }

    private static boolean free(GameTestHelper helper, AABB box) {
        return helper.getLevel().noCollision(box);
    }

    @GameTest(template = TEMPLATE)
    public static void nothingCollidesAnywhereInTheFourCells(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            TrimLayout.Assembly assembly = trimAssembly(helper, new BlockPos(4, 1, 4), facing);
            check(TrimPlacement.placeAt(helper.getLevel(), brickTrim(), assembly), facing + ": placement failed");
            for (BlockPos pos : assembly.positions()) {
                check(free(helper, new AABB(pos).deflate(0.001)), facing + ": whole cell " + pos + " collides");
                // Thin probes across the cell, front to back and side to side, to catch any ring segment.
                for (double u = 0.05; u < 1; u += 0.15) {
                    for (double v = 0.05; v < 1; v += 0.15) {
                        check(free(helper, new AABB(pos.getX() + u - 0.02, pos.getY() + v - 0.02, pos.getZ() + 0.05,
                                pos.getX() + u + 0.02, pos.getY() + v + 0.02, pos.getZ() + 0.95)),
                                facing + ": z-probe collides in " + pos + " at " + u + "," + v);
                        check(free(helper, new AABB(pos.getX() + 0.05, pos.getY() + v - 0.02, pos.getZ() + u - 0.02,
                                pos.getX() + 0.95, pos.getY() + v + 0.02, pos.getZ() + u + 0.02)),
                                facing + ": x-probe collides in " + pos + " at " + u + "," + v);
                    }
                }
            }
            helper.setBlock(rel(helper, assembly.anchor()), Blocks.AIR); // removes the whole frame
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nothingCanStandOnTheFrameAndItIsPathable(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            TrimLayout.Assembly assembly = trimAssembly(helper, new BlockPos(4, 1, 4), facing);
            check(TrimPlacement.placeAt(helper.getLevel(), brickTrim(), assembly), facing + ": placement failed");
            for (BlockPos pos : assembly.positions()) {
                BlockState state = helper.getLevel().getBlockState(pos);
                check(state.getCollisionShape(helper.getLevel(), pos).isEmpty(), facing + ": collision shape not empty at " + pos);
                check(state.getBlockSupportShape(helper.getLevel(), pos).isEmpty(), facing + ": support shape not empty at " + pos);
                check(!state.isFaceSturdy(helper.getLevel(), pos, Direction.UP), facing + ": top face is sturdy at " + pos);
                check(state.isPathfindable(helper.getLevel(), pos, PathComputationType.LAND), facing + ": not pathfindable at " + pos);
            }
            helper.setBlock(rel(helper, assembly.anchor()), Blocks.AIR);
        }
        helper.succeed();
    }

    /** A closed door at (3,1,4) facing north with the frame fitted in front of it (z = 5). */
    private static DoorLayout.Assembly doorWithFrame(GameTestHelper helper, String name) {
        floor(helper);
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "door placement failed");
        return assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
    }

    private static List<List<AABB>> collisions(GameTestHelper helper, DoorLayout.Assembly door) {
        List<List<AABB>> shapes = new ArrayList<>();
        for (BlockPos pos : door.positions()) {
            VoxelShape shape = helper.getLevel().getBlockState(pos).getCollisionShape(helper.getLevel(), pos);
            shapes.add(shape.toAabbs());
        }
        return shapes;
    }

    private static void open(GameTestHelper helper, DoorLayout.Assembly door) {
        oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(door.anchor()), door.anchor(), true);
    }

    @GameTest(template = TEMPLATE)
    public static void aFittedFrameLeavesTheDoorCollisionUnchanged(GameTestHelper helper) {
        DoorLayout.Assembly door = doorWithFrame(helper, "fitted_collision");
        List<List<AABB>> closedBefore = collisions(helper, door);
        ServerPlayer fitter = player(helper, "fitted_collision_fitter", GameType.SURVIVAL, Direction.NORTH);
        check(fitToDoor(helper, fitter, new ItemStack(oakTrim()), door, 0.5, 0.5).consumesAction(), "fitting failed");
        TrimLayout.Assembly frame = fittedTo(door);
        assertTrim(helper, frame, oakTrim());
        check(collisions(helper, door).equals(closedBefore), "closed door collision changed by the fitted frame");

        open(helper, door);
        List<List<AABB>> openWith = collisions(helper, door);
        // 29/16 clear beside the retained segment of the open door, with the frame in front.
        check(free(helper, new AABB(point(door, 3.0 / 16 + 0.001, 0.1, 0.1), point(door, 1.999, 1.9, 0.9))), "less than 29/16 clear with the frame");
        // A player-sized box through the frame's aperture and on through the open door.
        for (double centre : new double[] {0.5, 1.5}) {
            check(free(helper, new AABB(point(door, centre - 0.3, 0.01, -1.0), point(door, centre + 0.3, 1.81, 1.0))),
                    "player cannot pass the frame and open door at " + centre);
        }
        check(free(helper, new AABB(point(door, 0.05, 0.05, -0.95), point(door, 1.95, 1.95, -0.05))), "the fitted frame collides");

        helper.setBlock(rel(helper, frame.anchor()), Blocks.AIR); // removes the frame only
        check(collisions(helper, door).equals(openWith), "open door collision differs with and without the frame");
        helper.succeed();
    }

    /** A corridor (x 3..4, walls at x 2 and 5) along z, as the door suite builds it, with a lintel row at z = 4. */
    private static void corridor(GameTestHelper helper) {
        floor(helper);
        for (int z = 0; z < 9; z++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(2, y, z), Blocks.STONE);
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksNorthToSouthThroughADoorWithAFittedFrame(GameTestHelper helper) {
        corridor(helper);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        DoorLayout.Assembly door = doorWithFrame(helper, "walk_fitted_south");
        fitFrame(helper, door, "walk_fitted_south_fitter");
        walkThrough(helper, door, new BlockPos(3, 1, 1), new BlockPos(3, 1, 7), true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksSouthToNorthThroughADoorWithAFittedFrame(GameTestHelper helper) {
        corridor(helper);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        DoorLayout.Assembly door = doorWithFrame(helper, "walk_fitted_north");
        fitFrame(helper, door, "walk_fitted_north_fitter");
        walkThrough(helper, door, new BlockPos(4, 1, 7), new BlockPos(4, 1, 1), true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void villagerWalksThroughAFreestandingBrickFrame(GameTestHelper helper) {
        corridor(helper);
        ServerPlayer player = player(helper, "walk_freestanding", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, player, new ItemStack(brickTrim()), ANCHOR).consumesAction(), "frame placement failed");
        assertTrim(helper, trimAssembly(helper, ANCHOR, Direction.NORTH), brickTrim());
        walkThrough(helper, null, new BlockPos(3, 1, 1), new BlockPos(3, 1, 7), false);
    }

    private static void fitFrame(GameTestHelper helper, DoorLayout.Assembly door, String name) {
        ServerPlayer fitter = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(fitToDoor(helper, fitter, new ItemStack(oakTrim()), door, 0.5, 0.5).consumesAction(), "fitting failed");
    }

    /** Sends a villager from {@code start} to {@code target}; with a door, also requires that it opened. */
    private static void walkThrough(GameTestHelper helper, DoorLayout.Assembly door, BlockPos start, BlockPos target, boolean needsOpen) {
        Villager villager = helper.spawn(EntityType.VILLAGER, start);
        BlockPos absoluteTarget = helper.absolutePos(target);
        AtomicBoolean sawOpen = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty() && !arrived(helper, villager, target)) {
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(absoluteTarget, 0.6F, 0));
            }
            if (door != null && helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN)) {
                sawOpen.set(true);
            }
        });
        // Villager walks are known to be intermittent: same budget as the door suite.
        helper.succeedWhen(() -> {
            check(arrived(helper, villager, target), "villager has not reached the far side: at " + rel(helper, villager.blockPosition())
                    + ", path " + (villager.getNavigation().getPath() == null ? "none" : villager.getNavigation().getPath().getTarget() + " done=" + villager.getNavigation().getPath().isDone())
                    + ", door open " + (door != null && helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN)));
            check(!needsOpen || sawOpen.get(), "the door never opened");
        });
    }

    private static boolean arrived(GameTestHelper helper, Villager villager, BlockPos target) {
        Vec3 origin = Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO));
        Vec3 rel = villager.position().subtract(origin);
        // Fitted frames add one cell south of the wall, so "past" means beyond z = 5.5 going south.
        boolean pastWall = target.getZ() > ANCHOR.getZ() ? rel.z > ANCHOR.getZ() + 2.5 : rel.z < ANCHOR.getZ() - 0.5;
        return pastWall && Math.abs(rel.z - (target.getZ() + 0.5)) < 1.5;
    }
}
