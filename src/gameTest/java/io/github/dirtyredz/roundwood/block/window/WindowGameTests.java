package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.core.BlockPos;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.core.Direction;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.gametest.framework.GameTest;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.gametest.framework.GameTestHelper;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.server.level.ServerPlayer;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.item.ItemStack;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.item.Items;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.ClipContext;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.GameType;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.block.Blocks;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.block.Mirror;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.block.PipeBlock;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.block.Rotation;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.level.material.Fluids;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.phys.AABB;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.phys.BlockHitResult;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.phys.HitResult;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.minecraft.world.phys.Vec3;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import java.util.function.Supplier;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.dropped;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.useItemOn;

/** Round windows: orientation, thin-panel collision, pane connections, rotation and water. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowGameTests {
    private WindowGameTests() {
    }

    private static RoundWindowBlock window() {
        return ModBlocks.roundWindow();
    }

    /** Places an item on top of the floor cell below {@code pos}, as a player facing {@code facing}. */
    private static void place(GameTestHelper helper, String name, Direction facing, ItemStack stack, BlockPos pos) {
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, facing);
        check(useItemOn(helper, player, stack, pos.below(), Direction.UP).consumesAction(), name + ": placement failed at " + pos);
    }

    private static BlockState at(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos);
    }

    /** Fresh stacks of a pane, a wall and another window: a placed stack is consumed. */
    @SuppressWarnings("unchecked")
    private static Supplier<ItemStack>[] neighbourStacks() {
        return new Supplier[] {
                (Supplier<ItemStack>) () -> new ItemStack(Items.GLASS_PANE),
                (Supplier<ItemStack>) () -> new ItemStack(Items.COBBLESTONE_WALL),
                (Supplier<ItemStack>) () -> new ItemStack(window())};
    }

    /** True when a thin probe box inside the cell at {@code abs} (block fractions, mid height) collides. */
    private static boolean collides(GameTestHelper helper, BlockPos abs, double x0, double x1, double z0, double z1) {
        AABB box = new AABB(abs.getX() + x0, abs.getY() + 0.3, abs.getZ() + z0, abs.getX() + x1, abs.getY() + 0.7, abs.getZ() + z1);
        return !helper.getLevel().noCollision(box);
    }

    @GameTest(template = TEMPLATE)
    public static void facesThePlacingPlayer(GameTestHelper helper) {
        floor(helper);
        place(helper, "face_n", Direction.NORTH, new ItemStack(window()), new BlockPos(1, 1, 1));
        place(helper, "face_e", Direction.EAST, new ItemStack(window()), new BlockPos(5, 1, 5));
        check(at(helper, new BlockPos(1, 1, 1)).getValue(RoundWindowBlock.AXIS) == Direction.Axis.X, "looking north should give axis x");
        check(at(helper, new BlockPos(5, 1, 5)).getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z, "looking east should give axis z");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void collisionIsAThinPanel(GameTestHelper helper) {
        floor(helper);
        place(helper, "thin", Direction.NORTH, new ItemStack(window()), new BlockPos(4, 1, 4));
        BlockPos abs = helper.absolutePos(new BlockPos(4, 1, 4));
        double[][] probes = {{0.45, 0.45}, {0.01, 0.01}, {0.92, 0.01}, {0.01, 0.92}, {0.92, 0.92}};
        for (double[] probe : probes) {
            AABB panel = new AABB(abs.getX() + probe[0], abs.getY() + probe[1], abs.getZ() + 0.45,
                    abs.getX() + probe[0] + 0.07, abs.getY() + probe[1] + 0.07, abs.getZ() + 0.55);
            check(!helper.getLevel().noCollision(panel), "window should collide at " + probe[0] + "," + probe[1]);
            for (double[] z : new double[][] {{0.05, 0.3}, {0.65, 0.95}}) {
                AABB free = new AABB(abs.getX() + probe[0], abs.getY() + probe[1], abs.getZ() + z[0],
                        abs.getX() + probe[0] + 0.07, abs.getY() + probe[1] + 0.07, abs.getZ() + z[1]);
                check(helper.getLevel().noCollision(free),
                        "unconnected window collides off the panel at " + probe[0] + "," + probe[1] + " z " + z[0]);
            }
        }
        AABB bounds = helper.getLevel().getBlockState(abs).getShape(helper.getLevel(), abs).bounds();
        check(bounds.equals(new AABB(0, 0, 7 / 16.0, 1, 1, 9 / 16.0)), "outline is not the thin panel: " + bounds);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void woodCornersHitRays(GameTestHelper helper) {
        floor(helper);
        place(helper, "corner_ray", Direction.NORTH, new ItemStack(window()), new BlockPos(4, 1, 4)); // axis x: opening runs along z
        BlockPos abs = helper.absolutePos(new BlockPos(4, 1, 4));
        for (double[] corner : new double[][] {{0.05, 0.05}, {0.95, 0.05}, {0.05, 0.95}, {0.95, 0.95}}) {
            Vec3 from = new Vec3(abs.getX() + corner[0], abs.getY() + corner[1], abs.getZ() - 1.0);
            Vec3 to = new Vec3(abs.getX() + corner[0], abs.getY() + corner[1], abs.getZ() + 2.0);
            BlockHitResult hit = helper.getLevel().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            check(hit.getType() == HitResult.Type.BLOCK && helper.getLevel().getBlockState(hit.getBlockPos()).is(window()),
                    "ray through the wooden corner " + corner[0] + "," + corner[1] + " missed the window: " + hit);
        }
        helper.succeed();
    }

    /** 0.13.1: a window keeps no connection to a pane, wall or crossing window, in either placement order, and adds no glass. */
    @GameTest(template = TEMPLATE)
    public static void noNeighbourEverConnectsInEitherPlacementOrder(GameTestHelper helper) {
        floor(helper);
        String[] names = {"pane", "wall", "window"};
        Direction[] facings = {Direction.NORTH, Direction.NORTH, Direction.EAST};
        for (int i = 0; i < names.length; i++) {
            int x = 1 + 3 * i;
            BlockPos a = new BlockPos(x, 1, 2);
            place(helper, "recip_a_" + names[i], Direction.NORTH, new ItemStack(window()), a);
            place(helper, "recip_a_n_" + names[i], facings[i], neighbourStacks()[i].get(), a.north());
            place(helper, "recip_a_e_" + names[i], Direction.NORTH, new ItemStack(Items.GLASS_PANE), a.east());
            check(WindowPaneBlock.connections(at(helper, a)).isEmpty(), names[i] + " a: the window connected: " + at(helper, a));
            BlockPos b = new BlockPos(x, 1, 6);
            place(helper, "recip_b_n_" + names[i], facings[i], neighbourStacks()[i].get(), b.north());
            place(helper, "recip_b_" + names[i], Direction.NORTH, new ItemStack(window()), b);
            place(helper, "recip_b_e_" + names[i], Direction.NORTH, new ItemStack(Items.GLASS_PANE), b.east());
            check(WindowPaneBlock.connections(at(helper, b)).isEmpty(), names[i] + " b: the window connected: " + at(helper, b));
            BlockPos abs = helper.absolutePos(a);
            check(!collides(helper, abs, 0.45, 0.55, 0.05, 0.3) && !collides(helper, abs, 0.6, 0.95, 0.05, 0.3),
                    names[i] + " a: glass reaches towards a neighbour");
        }
        helper.succeed();
    }

    /** 0.13.1: a pane beside a window adds no arm to the window's own collision, across or in its plane. */
    @GameTest(template = TEMPLATE)
    public static void aPaneBesideAWindowAddsNoGeometry(GameTestHelper helper) {
        floor(helper);
        BlockPos pos = new BlockPos(4, 1, 4);
        place(helper, "arm_w", Direction.NORTH, new ItemStack(window()), pos);
        place(helper, "arm_n", Direction.NORTH, new ItemStack(Items.GLASS_PANE), pos.north());
        place(helper, "arm_e", Direction.NORTH, new ItemStack(Items.GLASS_PANE), pos.east());
        BlockPos abs = helper.absolutePos(pos);
        check(!collides(helper, abs, 0.45, 0.55, 0.05, 0.3), "a north arm collides");
        check(!collides(helper, abs, 0.6, 0.95, 0.05, 0.3), "an in-plane connection added geometry");
        check(WindowPaneBlock.connections(at(helper, pos)).isEmpty(), "the window connected: " + at(helper, pos));
        helper.succeed();
    }

    /**
     * Owner report (0.3.1): a full block directly in front of or behind a window grew a glass arm
     * towards it. Faces across the plane connect only to genuine pane neighbours, in either
     * placement order, for both axes.
     */
    @GameTest(template = TEMPLATE)
    public static void solidBlocksOnTheFacesNeverGrowArms(GameTestHelper helper) {
        floor(helper);
        Object[][] cases = {
                {Direction.NORTH, true, new BlockPos(1, 1, 2)}, {Direction.NORTH, false, new BlockPos(4, 1, 2)},
                {Direction.EAST, true, new BlockPos(2, 1, 6)}, {Direction.EAST, false, new BlockPos(6, 1, 6)}};
        for (Object[] c : cases) {
            Direction facing = (Direction) c[0];
            boolean windowFirst = (Boolean) c[1];
            BlockPos pos = (BlockPos) c[2];
            Direction front = facing.getOpposite();
            Direction back = facing;
            String name = "solid_" + facing + "_" + windowFirst;
            if (windowFirst) {
                place(helper, name, facing, new ItemStack(window()), pos);
            }
            helper.setBlock(pos.relative(front), Blocks.STONE);
            helper.setBlock(pos.relative(back), Blocks.OAK_PLANKS);
            if (!windowFirst) {
                place(helper, name, facing, new ItemStack(window()), pos);
            }
            BlockState state = at(helper, pos);
            check(state.is(window()), name + ": window missing");
            check(!connects(state, front) && !connects(state, back), name + ": a solid block on a face made an arm: " + state);
            BlockPos abs = helper.absolutePos(pos);
            boolean north = facing == Direction.NORTH;
            boolean frontArm = north ? collides(helper, abs, 0.45, 0.55, 0.7, 0.95) : collides(helper, abs, 0.05, 0.3, 0.45, 0.55);
            boolean backArm = north ? collides(helper, abs, 0.45, 0.55, 0.05, 0.3) : collides(helper, abs, 0.7, 0.95, 0.45, 0.55);
            check(!frontArm && !backArm, name + ": arm geometry collides towards a solid face neighbour");
        }
        helper.succeed();
    }

    /** 0.13.1: across the plane, panes, bars and crossing windows no longer connect either. */
    @GameTest(template = TEMPLATE)
    public static void paneNeighboursAcrossThePlaneNoLongerConnect(GameTestHelper helper) {
        floor(helper);
        BlockPos pane = new BlockPos(1, 1, 4);
        place(helper, "across_pane_w", Direction.NORTH, new ItemStack(window()), pane);
        place(helper, "across_pane_n", Direction.NORTH, new ItemStack(Items.GLASS_PANE), pane.north());
        BlockPos bars = new BlockPos(3, 1, 4);
        place(helper, "across_bars_w", Direction.NORTH, new ItemStack(window()), bars);
        place(helper, "across_bars_s", Direction.NORTH, new ItemStack(Items.IRON_BARS), bars.south());
        BlockPos crossing = new BlockPos(5, 1, 4);
        place(helper, "across_cross_w", Direction.NORTH, new ItemStack(window()), crossing);
        place(helper, "across_cross_n", Direction.EAST, new ItemStack(window()), crossing.north());
        check(at(helper, crossing.north()).getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z, "setup: crossing window axis");
        for (BlockPos pos : new BlockPos[] {pane, bars, crossing, crossing.north()}) {
            check(WindowPaneBlock.connections(at(helper, pos)).isEmpty(), "a window connected at " + pos + ": " + at(helper, pos));
        }
        helper.succeed();
    }

    private static boolean connects(BlockState state, Direction direction) {
        return state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction));
    }

    @GameTest(template = TEMPLATE)
    public static void rotationAndMirrorTurnTheAxis(GameTestHelper helper) {
        floor(helper);
        BlockPos pos = new BlockPos(4, 1, 4);
        place(helper, "rot_w", Direction.NORTH, new ItemStack(window()), pos);
        place(helper, "rot_n", Direction.NORTH, new ItemStack(Items.GLASS_PANE), pos.north());
        BlockState state = at(helper, pos);
        check(state.getValue(RoundWindowBlock.AXIS) == Direction.Axis.X, "setup: " + state);
        BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
        check(rotated.getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z, "rotation did not turn the axis: " + rotated);
        BlockState mirrored = state.mirror(Mirror.LEFT_RIGHT);
        check(mirrored.getValue(RoundWindowBlock.AXIS) == Direction.Axis.X, "mirror changed the axis: " + mirrored);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void waterloggingBucketsAndWaterRestoration(GameTestHelper helper) {
        floor(helper);
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, Blocks.WATER);
        // Placing into a source block waterlogs.
        ServerPlayer player = player(helper, "water", GameType.SURVIVAL, Direction.NORTH);
        check(useItemOn(helper, player, new ItemStack(window()), pos.below(), Direction.UP).consumesAction(), "placement into water failed");
        check(at(helper, pos).is(window()) && at(helper, pos).getValue(BlockStateProperties.WATERLOGGED), "not waterlogged in a source");
        // Bucket out, bucket in.
        BlockPos abs = helper.absolutePos(pos);
        ItemStack bucket = window().pickupBlock(null, helper.getLevel(), abs, at(helper, pos));
        check(bucket.is(Items.WATER_BUCKET) && !at(helper, pos).getValue(BlockStateProperties.WATERLOGGED), "bucket did not drain the window");
        check(window().placeLiquid(helper.getLevel(), abs, at(helper, pos), Fluids.WATER.getSource(false)), "bucket did not fill the window");
        check(at(helper, pos).getValue(BlockStateProperties.WATERLOGGED), "window not waterlogged after filling");
        // Breaking a waterlogged window leaves the water and drops the window (no Silk Touch needed).
        clearDrops(helper);
        player.gameMode.destroyBlock(abs);
        check(helper.getLevel().getFluidState(abs).isSource() && helper.getLevel().getFluidState(abs).is(Fluids.WATER), "water not restored");
        check(dropped(helper, window().asItem()) == 1, "window did not drop itself: " + dropped(helper, window().asItem()));
        helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }
}
