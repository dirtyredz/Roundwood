package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.fixture.WindowFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Supplier;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * Flush and floor/ceiling round windows among other blocks: they never connect or grow glass (in both
 * placement orders; parallel windows face to face, coplanar windows, solid blocks), culling, outlines
 * and water.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowPositionGameTests {
    private WindowPositionGameTests() {
    }

    /** Places a round window on the floor below {@code pos} in the given zone of its top face. */
    private static BlockState placeWindow(GameTestHelper helper, ServerPlayer player, BlockPos pos, WindowTargetZones.Zone zone) {
        check(WindowFixtures.place(player, new ItemStack(ModBlocks.roundWindow()), WindowFixtures.zone(helper, pos.below(), Direction.UP, zone))
                .consumesAction(), "window not placed at " + pos);
        return helper.getBlockState(pos);
    }

    /** Places any block item through the item path on the floor below {@code pos}. */
    private static void placeItem(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        check(WindowFixtures.place(player, stack, WindowFixtures.zone(helper, pos.below(), Direction.UP, WindowTargetZones.Zone.CENTER))
                .consumesAction(), stack + " not placed at " + pos);
    }

    private static boolean connects(BlockState state, Direction side) {
        return state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
    }

    /** True when a probe box (block fractions inside the cell at relative {@code pos}) collides. */
    private static boolean collides(GameTestHelper helper, BlockPos pos, double x0, double y0, double z0, double x1, double y1, double z1) {
        BlockPos abs = helper.absolutePos(pos);
        return !helper.getLevel().noCollision(new AABB(abs.getX() + x0, abs.getY() + y0, abs.getZ() + z0,
                abs.getX() + x1, abs.getY() + y1, abs.getZ() + z1));
    }

    @SuppressWarnings("unchecked")
    private static Supplier<ItemStack>[] junctions() {
        return new Supplier[] {
                (Supplier<ItemStack>) () -> new ItemStack(Items.GLASS_PANE),
                (Supplier<ItemStack>) () -> new ItemStack(Items.IRON_BARS),
                (Supplier<ItemStack>) () -> new ItemStack(Items.COBBLESTONE_WALL)};
    }

    /**
     * A far-flush window keeps no connection to panes, bars or walls, whichever is placed first, and
     * grows nothing back to the middle (0.6.1; the full combination sweep is {@link WindowJunctionSweepGameTests}).
     */
    @GameTest(template = TEMPLATE)
    public static void positionedWindowsNeverConnectInEitherOrder(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "pos_join", GameType.CREATIVE, Direction.NORTH);
        Supplier<ItemStack>[] stacks = junctions();
        for (int i = 0; i < stacks.length; i++) {
            BlockPos a = new BlockPos(1, 1, 1 + 3 * i);
            BlockState window = placeWindow(helper, player, a, WindowTargetZones.Zone.V_MIN);
            check(window.getValue(RoundWindowBlock.POSITION) == WindowPosition.NORTH, "setup: flush north");
            placeItem(helper, player, stacks[i].get(), a.east());
            check(WindowPaneBlock.connections(helper.getBlockState(a)).isEmpty(), i + " a: a flush window kept a connection");
            check(!collides(helper, a, 0.45, 0.3, 0.45, 0.55, 0.7, 0.55), i + " a: glass grew in the middle");
            check(!collides(helper, a, 0.7, 0.3, 0.45, 0.95, 0.7, 0.55), i + " a: glass grew towards the neighbour");
            BlockPos b = new BlockPos(5, 1, 1 + 3 * i);
            placeItem(helper, player, stacks[i].get(), b.south());
            placeWindow(helper, player, b, WindowTargetZones.Zone.V_MIN);
            check(WindowPaneBlock.connections(helper.getBlockState(b)).isEmpty(), i + " b: a flush window kept a connection");
            check(!collides(helper, b, 0.45, 0.3, 0.45, 0.55, 0.7, 0.55), i + " b: glass grew in the middle");
        }
        // A ceiling window with a pane beyond its north edge: no fin.
        BlockPos ceiling = new BlockPos(7, 1, 4);
        placeItem(helper, player, new ItemStack(Items.GLASS_PANE), ceiling.north());
        BlockState hung = placeFromWall(helper, player, ceiling);
        check(hung.getValue(RoundWindowBlock.POSITION) == WindowPosition.UP && WindowPaneBlock.connections(hung).isEmpty(),
                "ceiling window " + hung);
        check(!collides(helper, ceiling, 0.45, 0.1, 0.1, 0.55, 0.3, 0.3), "a fin hangs below the ceiling pane");
        helper.succeed();
    }

    /** A ceiling window in {@code pos}: the top zone of the south face of a stone block placed north of it. */
    private static BlockState placeFromWall(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos wall = pos.south();
        helper.setBlock(wall, Blocks.STONE);
        check(WindowFixtures.place(player, new ItemStack(ModBlocks.roundWindow()), WindowFixtures.zone(helper, wall, Direction.NORTH,
                WindowTargetZones.Zone.V_MAX)).consumesAction(), "ceiling window not placed");
        helper.setBlock(wall, Blocks.AIR);
        return helper.getBlockState(pos);
    }

    /** Face to face, coplanar, or solid: no connection, no glass. */
    @GameTest(template = TEMPLATE)
    public static void parallelCoplanarAndSolidNeighboursNeverBridge(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "pos_never", GameType.CREATIVE, Direction.NORTH);
        // A flush window with a centred parallel window in front of its flush face.
        BlockPos flush = new BlockPos(1, 1, 2);
        placeWindow(helper, player, flush.north(), WindowTargetZones.Zone.CENTER);
        BlockState parallel = placeWindow(helper, player, flush, WindowTargetZones.Zone.V_MAX);
        check(parallel.getValue(RoundWindowBlock.POSITION) == WindowPosition.SOUTH, "setup: flush south");
        check(!connects(parallel, Direction.NORTH) && !connects(helper.getBlockState(flush.north()), Direction.SOUTH),
                "parallel windows face to face joined");
        // Two flush windows continuing one plane.
        BlockPos first = new BlockPos(4, 1, 2);
        placeWindow(helper, player, first, WindowTargetZones.Zone.V_MIN);
        BlockState second = placeWindow(helper, player, first.east(), WindowTargetZones.Zone.V_MIN);
        check(!connects(second, Direction.WEST) && !connects(helper.getBlockState(first), Direction.EAST), "coplanar flush windows bridged");
        check(!collides(helper, first, 0.45, 0.3, 0.45, 0.55, 0.7, 0.55), "coplanar flush windows grew glass");
        // Two ceiling windows side by side.
        BlockPos c1 = new BlockPos(1, 1, 6);
        placeFromWall(helper, player, c1);
        BlockState c2 = placeFromWall(helper, player, c1.east());
        check(!connects(c2, Direction.WEST), "coplanar ceiling windows grew fins");
        // Stone beside and behind a flush window.
        BlockPos solid = new BlockPos(6, 1, 6);
        helper.setBlock(solid.east(), Blocks.STONE);
        helper.setBlock(solid.south(), Blocks.OAK_PLANKS);
        BlockState lone = placeWindow(helper, player, solid, WindowTargetZones.Zone.V_MIN);
        check(!connects(lone, Direction.EAST) && !connects(lone, Direction.SOUTH), "a solid block made a flush window grow glass: " + lone);
        check(!collides(helper, solid, 0.45, 0.3, 0.45, 0.55, 0.7, 0.55), "solid neighbours made a post");
        helper.succeed();
    }

    /** Faces are hidden only where the neighbouring window covers them. */
    @GameTest(template = TEMPLATE)
    public static void cullingFollowsTheGeometry(GameTestHelper helper) {
        RoundWindowBlock block = ModBlocks.roundWindow();
        BlockState x = block.defaultBlockState();
        BlockState z = x.setValue(RoundWindowBlock.AXIS, Direction.Axis.Z);
        BlockState north = x.setValue(RoundWindowBlock.POSITION, WindowPosition.NORTH);
        BlockState south = x.setValue(RoundWindowBlock.POSITION, WindowPosition.SOUTH);
        check(x.skipRendering(x, Direction.UP), "stacked centred windows in one plane should hide their shared faces");
        check(!x.skipRendering(z, Direction.UP), "crossed windows hid faces they do not cover");
        check(north.skipRendering(north, Direction.UP), "stacked north-flush windows should hide their shared faces");
        check(!north.skipRendering(x, Direction.UP) && !x.skipRendering(north, Direction.UP), "flush and centred windows hid each other");
        check(north.skipRendering(north, Direction.EAST), "coplanar flush windows should hide their touching edges");
        check(!north.skipRendering(south, Direction.EAST), "windows on opposite sides hid their edges");
        check(!x.skipRendering(Blocks.GLASS_PANE.defaultBlockState(), Direction.EAST), "a glass pane hid a window face");
        BlockState arch = ModBlocks.archLeft().defaultBlockState();
        check(!x.skipRendering(arch, Direction.UP), "an arch above hid a round window's top it only partly covers");
        helper.succeed();
    }

    /** Outline and collision are the posed slab (plus junctions); a flush waterlogged window leaves its water when broken. */
    @GameTest(template = TEMPLATE)
    public static void outlinesFollowThePoseAndWaterStays(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "pos_shape", GameType.SURVIVAL, Direction.NORTH);
        List<Object[]> cases = List.of(
                new Object[] {WindowTargetZones.Zone.V_MIN, new AABB(0, 0, 0, 1, 1, 2 / 16.0)},
                new Object[] {WindowTargetZones.Zone.V_MAX, new AABB(0, 0, 14 / 16.0, 1, 1, 1)},
                new Object[] {WindowTargetZones.Zone.U_MIN, new AABB(0, 0, 0, 2 / 16.0, 1, 1)},
                new Object[] {WindowTargetZones.Zone.U_MAX, new AABB(14 / 16.0, 0, 0, 1, 1, 1)});
        for (int i = 0; i < cases.size(); i++) {
            BlockPos pos = new BlockPos(1 + 2 * i, 1, 1);
            placeWindow(helper, player, pos, (WindowTargetZones.Zone) cases.get(i)[0]);
            BlockPos abs = helper.absolutePos(pos);
            AABB bounds = helper.getLevel().getBlockState(abs).getShape(helper.getLevel(), abs).bounds();
            check(bounds.equals(cases.get(i)[1]), cases.get(i)[0] + ": outline " + bounds);
        }
        BlockPos wet = new BlockPos(4, 1, 5);
        helper.setBlock(wet, Blocks.WATER);
        BlockState placed = placeWindow(helper, player, wet, WindowTargetZones.Zone.V_MAX);
        check(placed.getValue(BlockStateProperties.WATERLOGGED), "flush window in water not waterlogged");
        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(wet));
        check(helper.getLevel().getFluidState(helper.absolutePos(wet)).isSource(), "water not restored");
        helper.setBlock(wet, Blocks.AIR);
        clearDrops(helper);
        helper.succeed();
    }

    /** A neighbour update schedules the waterlogged window's water tick (inherited pane behaviour, kept by the filter). */
    @GameTest(template = TEMPLATE)
    public static void neighbourUpdatesKeepWaterTicking(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "pos_tick", GameType.CREATIVE, Direction.NORTH);
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, Blocks.WATER);
        placeWindow(helper, player, pos, WindowTargetZones.Zone.U_MAX);
        helper.setBlock(pos.west(), Blocks.STONE);
        BlockPos abs = helper.absolutePos(pos);
        check(helper.getLevel().getFluidTicks().hasScheduledTick(abs, Fluids.WATER), "no water tick after a neighbour update");
        // Remove it before the tick runs, so no water spreads into other tests.
        helper.getLevel().setBlock(abs, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.succeed();
    }
}
