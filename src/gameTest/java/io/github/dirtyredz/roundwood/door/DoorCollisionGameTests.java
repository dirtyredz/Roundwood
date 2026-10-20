package io.github.dirtyredz.roundwood.door;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * World collision and ray hits, measured through the level (not the shape objects). Coordinates
 * are computed in the assembly's own frame: "across" runs from the left to the right column, "out"
 * points towards the side the placing player stood on.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorCollisionGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private DoorCollisionGameTests() {
    }

    /** A point in the assembly's frame: {@code a} across (0..2), {@code h} up (0..2), {@code d} depth into the cells (0 = front face). */
    private static Vec3 point(DoorLayout.Assembly assembly, double a, double h, double d) {
        Direction facing = assembly.facing();
        Direction across = facing.getClockWise();
        Direction out = facing.getOpposite();
        BlockPos anchor = assembly.anchor();
        // Start at the anchor cell's front-left-bottom corner.
        double x = anchor.getX() + (across.getStepX() < 0 || out.getStepX() > 0 ? 1 : 0);
        double z = anchor.getZ() + (across.getStepZ() < 0 || out.getStepZ() > 0 ? 1 : 0);
        x += across.getStepX() * a - out.getStepX() * d;
        z += across.getStepZ() * a - out.getStepZ() * d;
        return new Vec3(x, anchor.getY() + h, z);
    }

    private static boolean free(GameTestHelper helper, AABB box) {
        return helper.getLevel().noCollision(box);
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, Direction facing, BlockPos anchor, String name) {
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, facing);
        check(placeDoor(helper, player, oakDoor().asItem(), anchor).consumesAction(), facing + ": placement failed");
        return assembly(helper, anchor, facing);
    }

    @GameTest(template = TEMPLATE)
    public static void closedBlocksTheCentreButNotTheCorners(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            DoorLayout.Assembly assembly = place(helper, facing, ANCHOR, "closed_" + facing);
            // A thin probe through the slab (front 3/16) at the middle of the door: blocked.
            check(!free(helper, probe(assembly, 1.0, 1.0)), facing + ": centre not solid");
            check(!free(helper, probe(assembly, 0.1, 1.0)), facing + ": disc should reach the outer edge at mid height");
            // The square's corners outside the circle are open.
            check(free(helper, probe(assembly, 0.05, 0.05)), facing + ": lower-left corner should be open");
            check(free(helper, probe(assembly, 1.95, 1.95)), facing + ": upper-right corner should be open");
            // Behind the slab the cells are empty.
            check(free(helper, new AABB(point(assembly, 0.2, 0.2, 0.25), point(assembly, 1.8, 1.8, 0.95))), facing + ": cell behind the slab");
            helper.setBlock(ANCHOR, net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openLeavesLetAPlayerThroughBothColumns(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            DoorLayout.Assembly assembly = place(helper, facing, ANCHOR, "open_" + facing);
            oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(assembly.anchor()), assembly.anchor(), true);
            // A player-sized box (0.6 wide, 1.8 tall) through each column, across the whole depth.
            for (double centre : new double[] {0.5, 1.5}) {
                AABB body = new AABB(point(assembly, centre - 0.3, 0.01, 0.0), point(assembly, centre + 0.3, 1.81, 1.0));
                check(free(helper, body), facing + ": player cannot pass column at " + centre);
            }
            // The clear width between the open leaves is at least 26/16.
            AABB gap = new AABB(point(assembly, 3.0 / 16 + 0.001, 0.1, 0.1), point(assembly, 2 - 3.0 / 16 - 0.001, 1.9, 0.9));
            check(free(helper, gap), facing + ": less than 26/16 clear");
            // Each open leaf lies along its outer edge.
            check(!free(helper, new AABB(point(assembly, 0.0, 1.0, 0.4), point(assembly, 0.1, 1.05, 0.6))), facing + ": left leaf missing");
            check(!free(helper, new AABB(point(assembly, 1.9, 1.0, 0.4), point(assembly, 2.0, 1.05, 0.6))), facing + ": right leaf missing");
            helper.setBlock(ANCHOR, net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void raysHitTheDiscAndMissTheCorners(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, Direction.NORTH, ANCHOR, "rays");
        BlockHitResult centre = ray(helper, assembly, 1.0, 1.0);
        check(centre.getType() == HitResult.Type.BLOCK && helper.getLevel().getBlockState(centre.getBlockPos()).getBlock() instanceof RoundDoorBlock,
                "ray at the centre did not hit the door");
        BlockHitResult corner = ray(helper, assembly, 0.05, 1.95);
        check(corner.getType() == HitResult.Type.MISS
                        || !(helper.getLevel().getBlockState(corner.getBlockPos()).getBlock() instanceof RoundDoorBlock),
                "ray through the empty corner hit the door");
        // Outline bounds stay within each cell.
        for (BlockPos pos : assembly.positions()) {
            AABB bounds = helper.getLevel().getBlockState(pos).getShape(helper.getLevel(), pos, CollisionContext.empty()).bounds();
            check(bounds.minX >= 0 && bounds.minY >= 0 && bounds.minZ >= 0 && bounds.maxX <= 1 && bounds.maxY <= 1 && bounds.maxZ <= 1,
                    "outline leaves its cell at " + pos + ": " + bounds);
            check(!helper.getLevel().getBlockState(pos).getValue(DoorBlock.OPEN), "unexpected open state");
        }
        helper.succeed();
    }

    /** A thin probe through the closed slab (depth 0..0.19) around a point of the door plane. */
    private static AABB probe(DoorLayout.Assembly assembly, double a, double h) {
        return new AABB(point(assembly, a - 0.02, h - 0.02, 0.0), point(assembly, a + 0.02, h + 0.02, 0.19));
    }

    private static BlockHitResult ray(GameTestHelper helper, DoorLayout.Assembly assembly, double a, double h) {
        Vec3 from = point(assembly, a, h, -1.0);
        Vec3 to = point(assembly, a, h, 1.5);
        return helper.getLevel().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null));
    }
}
