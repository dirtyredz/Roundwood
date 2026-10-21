package io.github.dirtyredz.roundwood.block.window;

import com.mojang.logging.LogUtils;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;

/**
 * Every way windows meet each other and vanilla panes (0.6.1 owner ruling): a positioned window
 * (flush, floor or ceiling, round or arch) keeps no connection and its shape is its slab, whoever
 * its neighbours are; a centred window keeps the 0.5.0 arms only towards panes, bars, walls and
 * centred windows crossing it. Each scenario is settled by the real neighbour-update path, then the
 * final states are logged ({@code ROUNDWOOD_SWEEP|scenario|dx,dz|state}) so the preview sheet renders
 * exactly what the game computed.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowJunctionSweepGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    private WindowJunctionSweepGameTests() {
    }

    /** One block of a scenario at (dx, dz) from its slot, and, for a centred window, the sides it must keep. */
    private record Cell(int dx, int dz, BlockState state, Set<Direction> expected) {
    }

    private record Scenario(String name, List<Cell> cells) {
    }

    private static final class Builder {
        private final String name;
        private final List<Cell> cells = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        Builder at(int dx, int dz, BlockState state, Direction... expected) {
            Set<Direction> sides = EnumSet.noneOf(Direction.class);
            sides.addAll(List.of(expected));
            cells.add(new Cell(dx, dz, state, sides));
            return this;
        }

        Scenario done() {
            return new Scenario(name, List.copyOf(cells));
        }
    }

    private static Builder scenario(String name) {
        return new Builder(name);
    }

    private static BlockState flush(Direction side) {
        return ModBlocks.roundWindow().defaultBlockState().setValue(RoundWindowBlock.POSITION, WindowPosition.flush(side));
    }

    /** A centred round window; axis x lies in the x-y plane (normal z). */
    private static BlockState centred(Direction.Axis axis) {
        return ModBlocks.roundWindow().defaultBlockState().setValue(RoundWindowBlock.POSITION, WindowPosition.CENTER)
                .setValue(RoundWindowBlock.AXIS, axis);
    }

    private static BlockState arch(OrientedWindowBlock block, WindowPose pose) {
        return block.defaultBlockState().setValue(OrientedWindowBlock.ORIENTATION, WindowOrientation.of(pose))
                .setValue(OrientedWindowBlock.DEPTH, pose.depth());
    }

    private static BlockState pane() {
        return Blocks.GLASS_PANE.defaultBlockState();
    }

    /** Every combination, in groups of at most four (one test each). */
    static List<Scenario> scenarios() {
        WindowPose centredSouth = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        WindowPose flushNorth = WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE);
        WindowPose flushWest = WindowPose.upright(Direction.EAST, WindowDepth.NEGATIVE);
        WindowPose ceiling = new WindowPose(Direction.DOWN, Direction.NORTH, WindowDepth.POSITIVE);
        return List.of(
                // The owner's 0.6.0 screenshot: two flush windows meeting in an L (outer corner).
                scenario("flush L corner").at(0, 0, flush(Direction.NORTH)).at(1, 0, flush(Direction.WEST)).done(),
                scenario("flush L inner").at(0, 0, flush(Direction.SOUTH)).at(1, 0, flush(Direction.WEST)).done(),
                scenario("flush row").at(0, 0, flush(Direction.NORTH)).at(1, 0, flush(Direction.NORTH))
                        .at(2, 0, flush(Direction.NORTH)).done(),
                scenario("flush step").at(0, 0, flush(Direction.NORTH)).at(1, 0, flush(Direction.SOUTH)).done(),
                scenario("flush pane inline").at(0, 0, flush(Direction.NORTH)).at(1, 0, pane()).done(),
                scenario("flush pane front back").at(0, 0, flush(Direction.NORTH)).at(0, 1, pane()).at(0, -1, pane()).done(),
                scenario("flush bars").at(0, 0, flush(Direction.NORTH)).at(1, 0, Blocks.IRON_BARS.defaultBlockState()).done(),
                scenario("ceiling floor pane").at(0, 0, flush(Direction.UP)).at(1, 0, pane()).at(2, 0, flush(Direction.DOWN)).done(),
                // Centred beside flush: the centred window's in-plane side follows vanilla (no geometry).
                scenario("flush beside centred").at(0, 0, flush(Direction.NORTH))
                        .at(1, 0, centred(Direction.Axis.X), Direction.WEST).done(),
                // Across a centred window's plane: a flush window is not a junction, a pane is.
                scenario("centred across flush").at(0, 0, centred(Direction.Axis.X)).at(0, 1, flush(Direction.NORTH)).done(),
                scenario("centred across pane").at(0, 0, centred(Direction.Axis.X), Direction.SOUTH).at(0, 1, pane()).done(),
                scenario("centred T").at(0, 0, centred(Direction.Axis.X), Direction.SOUTH)
                        .at(0, 1, centred(Direction.Axis.Z), Direction.NORTH).done(),
                scenario("centred row").at(0, 0, centred(Direction.Axis.X), Direction.EAST)
                        .at(1, 0, centred(Direction.Axis.X), Direction.EAST, Direction.WEST)
                        .at(2, 0, centred(Direction.Axis.X), Direction.WEST).done(),
                scenario("arch pair pane").at(0, 0, arch(ModBlocks.archLeft(), centredSouth), Direction.EAST, Direction.SOUTH)
                        .at(1, 0, arch(ModBlocks.archRight(), centredSouth), Direction.WEST).at(0, 1, pane()).done(),
                scenario("arch flush L").at(0, 0, arch(ModBlocks.archLeft(), flushNorth))
                        .at(1, 0, arch(ModBlocks.archRight(), flushWest)).done(),
                scenario("arch flush pair pane").at(0, 0, arch(ModBlocks.archLeft(), flushNorth))
                        .at(1, 0, arch(ModBlocks.archRight(), flushNorth)).at(2, 0, pane()).done(),
                scenario("arch ceiling pane").at(0, 0, arch(ModBlocks.archLeft(), ceiling)).at(1, 0, pane()).done(),
                scenario("round and arch L").at(0, 0, flush(Direction.NORTH))
                        .at(1, 0, arch(ModBlocks.archLeft(), flushWest)).done());
    }

    @GameTest(template = TEMPLATE)
    public static void flushCornersRowsAndSteps(GameTestHelper helper) {
        sweep(helper, 0);
    }

    @GameTest(template = TEMPLATE)
    public static void positionedWindowsBesidePanesAndBars(GameTestHelper helper) {
        sweep(helper, 1);
    }

    @GameTest(template = TEMPLATE)
    public static void centredWindowsBesidePositionedOnesPanesAndEachOther(GameTestHelper helper) {
        sweep(helper, 2);
    }

    @GameTest(template = TEMPLATE)
    public static void centredRowsAndArchCombinations(GameTestHelper helper) {
        sweep(helper, 3);
    }

    @GameTest(template = TEMPLATE)
    public static void archCeilingAndMixedCorners(GameTestHelper helper) {
        sweep(helper, 4);
    }

    /**
     * Group {@code group} of {@link #scenarios()}: up to four, one per corner of the 9 x 9 floor (each
     * at most 3 x 3, two empty cells apart); every one settled, logged and checked.
     */
    private static void sweep(GameTestHelper helper, int group) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Scenario> all = scenarios();
        List<Scenario> scenarios = all.subList(4 * group, Math.min(all.size(), 4 * group + 4));
        for (int i = 0; i < scenarios.size(); i++) {
            Scenario scenario = scenarios.get(i);
            BlockPos slot = new BlockPos(5 * (i % 2), 1, 1 + 5 * (i / 2));
            Map<BlockPos, Cell> placed = new LinkedHashMap<>();
            for (Cell cell : scenario.cells()) {
                BlockPos pos = helper.absolutePos(slot.offset(cell.dx(), 0, cell.dz()));
                level.setBlock(pos, cell.state(), Block.UPDATE_ALL);
                placed.put(pos, cell);
            }
            // Settle every block (its own sides too) through the neighbour-update path the game uses.
            for (int pass = 0; pass < 2; pass++) {
                for (BlockPos pos : placed.keySet()) {
                    BlockState state = level.getBlockState(pos);
                    level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), Block.UPDATE_ALL);
                }
            }
            for (Map.Entry<BlockPos, Cell> entry : placed.entrySet()) {
                BlockState state = level.getBlockState(entry.getKey());
                Cell cell = entry.getValue();
                LOGGER.info("ROUNDWOOD_SWEEP|{}|{},{}|{}", scenario.name(), cell.dx(), cell.dz(), state);
                if (state.getBlock() instanceof WindowPaneBlock window) {
                    verify(scenario.name() + " " + cell.dx() + "," + cell.dz(), level, entry.getKey(), state, window, cell.expected());
                }
            }
        }
        helper.succeed();
    }

    private static void verify(String where, ServerLevel level, BlockPos pos, BlockState state, WindowPaneBlock window,
                               Set<Direction> expected) {
        WindowPose pose = window.pose(state);
        Set<Direction> connected = WindowPaneBlock.connections(state);
        VoxelShape shape = state.getShape(level, pos);
        if (!WindowPaneGeometry.bridges(pose)) {
            check(connected.isEmpty(), where + ": a positioned window kept connections " + connected);
            check(onlyInside(shape, List.of(pose.slab())), where + ": a positioned window's shape leaves its slab");
            return;
        }
        // In-plane connections are vanilla's and add nothing; only the sides across the plane are judged here.
        Set<Direction> across = EnumSet.noneOf(Direction.class);
        connected.stream().filter(side -> side.getAxis() == pose.normal()).forEach(across::add);
        Set<Direction> want = EnumSet.noneOf(Direction.class);
        expected.stream().filter(side -> side.getAxis() == pose.normal()).forEach(want::add);
        check(across.equals(want), where + ": across-plane connections " + across + ", expected " + want);
        List<Box> allowed = new ArrayList<>(List.of(pose.slab()));
        want.forEach(side -> allowed.add(WindowPaneGeometry.armBox(side)));
        check(onlyInside(shape, allowed), where + ": glass outside the slab and its arms");
    }

    private static boolean onlyInside(VoxelShape shape, List<Box> allowed) {
        return !Shapes.joinIsNotEmpty(shape, VoxelShapeFactory.shape(allowed), BooleanOp.ONLY_FIRST);
    }
}
