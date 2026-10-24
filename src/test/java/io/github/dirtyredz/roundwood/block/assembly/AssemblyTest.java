package io.github.dirtyredz.roundwood.block.assembly;

import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The shared assembly contract as the three families implement it: the part order (lower-left,
 * upper-left, lower-right, upper-right; the arched door only the first two), the two- and four-cell
 * footprints in every facing, and the authored frame every part's cell maps into. Exact membership
 * reads block states and is pinned by the GameTests ({@code AssemblyAnchorGameTests},
 * {@code AssemblyLifecycleGameTests}).
 */
class AssemblyTest {
    private static final BlockPos ANCHOR = new BlockPos(10, 64, -7);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    @Test
    void partsComeInTheSharedOrder() {
        assertEquals(List.of(DoorLayout.Part.LOWER_LEFT, DoorLayout.Part.UPPER_LEFT, DoorLayout.Part.LOWER_RIGHT,
                DoorLayout.Part.UPPER_RIGHT), door(Direction.NORTH, DoorDesign.ROUND).parts());
        assertEquals(List.of(DoorLayout.Part.LOWER_LEFT, DoorLayout.Part.UPPER_LEFT), door(Direction.NORTH, DoorDesign.ARCHED).parts());
        assertEquals(List.of(ArchwayPart.LOWER_LEFT, ArchwayPart.UPPER_LEFT, ArchwayPart.LOWER_RIGHT, ArchwayPart.UPPER_RIGHT),
                archway(Direction.NORTH).parts());
        assertEquals(List.of(Quarter.LOWER_LEFT, Quarter.UPPER_LEFT, Quarter.LOWER_RIGHT, Quarter.UPPER_RIGHT),
                frame(Direction.NORTH).parts());
    }

    @Test
    void footprintsAreTwoOrFourDistinctCellsFromTheAnchor() {
        for (Direction facing : FACINGS) {
            assertFootprint(door(facing, DoorDesign.ROUND), 4, facing);
            assertFootprint(door(facing, DoorDesign.ARCHED), 2, facing);
            assertFootprint(archway(facing), 4, facing);
            assertFootprint(frame(facing), 4, facing);
        }
    }

    @Test
    void everyCellMapsIntoTheAuthoredFrame() {
        for (Direction facing : FACINGS) {
            assertAuthored(door(facing, DoorDesign.ROUND), part -> new int[] {part.column.index(), part.row()});
            assertAuthored(door(facing, DoorDesign.ARCHED), part -> new int[] {part.column.index(), part.row()});
            assertAuthored(archway(facing), part -> new int[] {part.column, part.row});
            assertAuthored(frame(facing), part -> new int[] {part.column, part.row});
        }
    }

    @Test
    void recordIdentityIsTheFamilysLayout() {
        assertEquals(door(Direction.EAST, DoorDesign.ROUND), door(Direction.EAST, DoorDesign.ROUND));
        assertNotEquals(door(Direction.EAST, DoorDesign.ROUND), door(Direction.EAST, DoorDesign.ARCHED));
        assertNotEquals(new DoorLayout.Assembly(ANCHOR, Direction.EAST, DoorHingeSide.LEFT, DoorDesign.ROUND),
                new DoorLayout.Assembly(ANCHOR, Direction.EAST, DoorHingeSide.RIGHT, DoorDesign.ROUND));
        assertNotEquals(archway(Direction.EAST), new ArchwayLayout.Assembly(ANCHOR, Direction.EAST, ArchwayDesign.KEYHOLE));
        assertEquals(frame(Direction.SOUTH), frame(Direction.SOUTH));
    }

    private static DoorLayout.Assembly door(Direction facing, DoorDesign design) {
        return new DoorLayout.Assembly(ANCHOR, facing, DoorHingeSide.LEFT, design);
    }

    private static ArchwayLayout.Assembly archway(Direction facing) {
        return new ArchwayLayout.Assembly(ANCHOR, facing, ArchwayDesign.ORDINARY);
    }

    private static TrimLayout.Assembly frame(Direction facing) {
        return new TrimLayout.Assembly(ANCHOR, facing);
    }

    private static void assertFootprint(Assembly<?> assembly, int cells, Direction facing) {
        List<BlockPos> positions = assembly.positions();
        assertEquals(cells, positions.size(), facing + ": cells");
        assertEquals(cells, new HashSet<>(positions).size(), facing + ": distinct cells");
        assertEquals(ANCHOR, positions.get(0), facing + ": the anchor comes first");
        for (BlockPos pos : positions) {
            BlockPos offset = pos.subtract(ANCHOR);
            int across = offset.getX() * facing.getClockWise().getStepX() + offset.getZ() * facing.getClockWise().getStepZ();
            assertEquals(0, offset.getX() * facing.getStepX() + offset.getZ() * facing.getStepZ(), facing + ": a cell off the plane");
            assertEquals(true, across == 0 || across == 1, facing + ": a cell outside the two columns");
            assertEquals(true, offset.getY() == 0 || offset.getY() == 1, facing + ": a cell outside the two rows");
        }
    }

    /** Each part's cell centre is at (column * 16 + 8, row * 16 + 8, 8); the face toward the placer is at z 16. */
    private static <P> void assertAuthored(Assembly<P> assembly, Function<P, int[]> columnRow) {
        for (P part : assembly.parts()) {
            int[] cr = columnRow.apply(part);
            Vec3 centre = Vec3.atCenterOf(assembly.pos(part));
            assertVec(new Vec3(cr[0] * 16 + 8, cr[1] * 16 + 8, 8), assembly.toAuthored(centre), assembly + " " + part);
            Vec3 front = centre.relative(assembly.facing().getOpposite(), 0.5);
            assertVec(new Vec3(cr[0] * 16 + 8, cr[1] * 16 + 8, 16), assembly.toAuthored(front), assembly + " " + part + " front");
        }
    }

    private static void assertVec(Vec3 expected, Vec3 actual, String what) {
        assertEquals(expected.x, actual.x, 1e-9, what + " x");
        assertEquals(expected.y, actual.y, 1e-9, what + " y");
        assertEquals(expected.z, actual.z, 1e-9, what + " z");
    }
}
