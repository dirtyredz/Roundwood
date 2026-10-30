package io.github.dirtyredz.roundwood.block.door;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorColumn;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.fixture.door.DoorFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;

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
        return place(helper, facing, anchor, name, DoorHingeSide.LEFT);
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, Direction facing, BlockPos anchor, String name, DoorHingeSide hinge) {
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, facing);
        check(placeDoor(helper, player, oakDoor().asItem(), anchor, hinge).consumesAction(), facing + ": placement failed");
        return assembly(helper, anchor, facing, hinge);
    }

    private static void open(GameTestHelper helper, DoorLayout.Assembly assembly) {
        oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(assembly.anchor()), assembly.anchor(), true);
    }

    /** The column the leaf turns on: the hinge side. */
    private static DoorColumn hingeColumn(DoorLayout.Assembly assembly) {
        return assembly.hinge() == DoorHingeSide.LEFT ? DoorColumn.LEFT : DoorColumn.RIGHT;
    }

    /** Across-coordinate of the middle of the hinge-side slab. */
    private static double slabCentre(DoorLayout.Assembly assembly) {
        return assembly.hinge() == DoorHingeSide.LEFT ? 0.05 : 1.95;
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
            // Camouflaged corners are decorative: the same probes, the same answers.
            DoorFixtures.anchorEntity(helper, assembly).setCamouflage(CamouflageData.free(Blocks.STONE.defaultBlockState()));
            check(!free(helper, probe(assembly, 1.0, 1.0)), facing + ": camouflaged centre not solid");
            check(free(helper, probe(assembly, 0.05, 0.05)), facing + ": camouflaged corner collides");
            check(free(helper, probe(assembly, 1.95, 1.95)), facing + ": camouflaged corner collides");
            helper.setBlock(ANCHOR, net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openLeavesLetAPlayerThroughBothColumns(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (Direction facing : FACINGS) {
                String name = facing + "/" + hinge;
                DoorLayout.Assembly assembly = place(helper, facing, ANCHOR, "open_" + name, hinge);
                open(helper, assembly);
                boolean left = hinge == DoorHingeSide.LEFT;
                // A player-sized box (0.6 wide, 1.8 tall) through each column, across the whole depth.
                for (double centre : new double[] {0.5, 1.5}) {
                    AABB body = new AABB(point(assembly, centre - 0.3, 0.01, 0.0), point(assembly, centre + 0.3, 1.81, 1.0));
                    check(free(helper, body), name + ": player cannot pass column at " + centre);
                }
                // The clear width beside the single retained segment is 29/16.
                double from = left ? 3.0 / 16 + 0.001 : 0.001;
                double to = left ? 1.999 : 2 - 3.0 / 16 - 0.001;
                check(free(helper, new AABB(point(assembly, from, 0.1, 0.1), point(assembly, to, 1.9, 0.9))), name + ": less than 29/16 clear");
                // The hinge-side slab collides; the far side has nothing.
                double slab = slabCentre(assembly);
                check(!free(helper, new AABB(point(assembly, slab - 0.02, 0.98, 0.4), point(assembly, slab + 0.02, 1.02, 0.6))),
                        name + ": hinge-side slab missing");
                // The non-hinge column has no shape at all, in either row.
                for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
                    if (part.column != hingeColumn(assembly)) {
                        BlockPos pos = assembly.pos(part);
                        BlockState state = helper.getLevel().getBlockState(pos);
                        // The outline is still the whole door (it is selection only); collision is empty.
                        check(!state.getShape(helper.getLevel(), pos).isEmpty(), name + ": no whole-door outline at " + part);
                        check(state.getCollisionShape(helper.getLevel(), pos).isEmpty(), name + ": collision not empty at " + part);
                    }
                }
                // The outer half of the leaf projects into the cell in front of the hinge column: visual only.
                BlockPos hingeLower = assembly.pos(DoorLayout.Part.of(hingeColumn(assembly), DoubleBlockHalf.LOWER));
                check(helper.getLevel().getBlockState(hingeLower.relative(facing)).isAir(), name + ": a block appeared in the projection cell");
                double a0 = left ? 0.001 : 1.001;
                check(free(helper, new AABB(point(assembly, a0, 0.001, 1.001), point(assembly, a0 + 0.998, 1.999, 1.999))),
                        name + ": the projected half of the leaf collides");
                helper.setBlock(ANCHOR, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openRaysHitOnlyTheRetainedSegment(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (Direction facing : FACINGS) {
                String name = facing + "/" + hinge;
                DoorLayout.Assembly assembly = place(helper, facing, ANCHOR, "open_ray_" + name, hinge);
                open(helper, assembly);
                // Along the facing axis through the hinge-side slab: the retained open segment is clickable.
                BlockHitResult slab = ray(helper, assembly, slabCentre(assembly), 1.0);
                check(slab.getType() == HitResult.Type.BLOCK
                                && helper.getLevel().getBlockState(slab.getBlockPos()).getBlock() instanceof RoundDoorBlock,
                        name + ": ray at the hinge-side slab did not hit the door");
                // Across the plane through the projection region (d = 1.5): nothing there to hit.
                BlockHitResult projection = helper.getLevel().clip(new ClipContext(point(assembly, -1.0, 1.0, 1.5), point(assembly, 3.0, 1.0, 1.5),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null));
                check(projection.getType() == HitResult.Type.MISS
                                || !(helper.getLevel().getBlockState(projection.getBlockPos()).getBlock() instanceof RoundDoorBlock),
                        name + ": ray through the projection region hit the door");
                // Along the facing axis through the empty non-hinge column.
                BlockHitResult empty = ray(helper, assembly, hinge == DoorHingeSide.LEFT ? 1.5 : 0.5, 1.0);
                check(empty.getType() == HitResult.Type.MISS
                                || !(helper.getLevel().getBlockState(empty.getBlockPos()).getBlock() instanceof RoundDoorBlock),
                        name + ": ray through the non-hinge column hit the door");
                helper.setBlock(ANCHOR, Blocks.AIR);
            }
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
        // Every part outlines the same whole door in world space; collision stays inside each cell.
        AABB expected = null;
        for (BlockPos pos : assembly.positions()) {
            BlockState state = helper.getLevel().getBlockState(pos);
            AABB outline = state.getShape(helper.getLevel(), pos, CollisionContext.empty()).bounds().move(pos);
            if (expected == null) {
                expected = outline;
            }
            check(outline.equals(expected), "part at " + pos + " outlines " + outline + ", not " + expected);
            AABB collision = state.getCollisionShape(helper.getLevel(), pos).bounds();
            check(collision.minX >= 0 && collision.minY >= 0 && collision.minZ >= 0 && collision.maxX <= 1 && collision.maxY <= 1
                    && collision.maxZ <= 1, "collision leaves its cell at " + pos + ": " + collision);
            check(!state.getValue(DoorBlock.OPEN), "unexpected open state");
        }
        check(Math.abs(expected.getXsize() + expected.getZsize() - (2 + 3.0 / 16)) < 1e-6 && Math.abs(expected.getYsize() - 2) < 1e-6,
                "whole-door outline has the wrong size: " + expected);
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
