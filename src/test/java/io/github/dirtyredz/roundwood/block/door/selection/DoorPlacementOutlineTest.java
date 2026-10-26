package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the door placement preview draws (0.11.0) for a planned assembly: the cells it will own, the
 * leaf as it will stand (the very edges the swing highlight draws at rest) and the hinge axis, in
 * the anchor cell's frame turned for the facing. The preview's assembly is the placement plan's own,
 * so these are what will be placed.
 */
class DoorPlacementOutlineTest {
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private static DoorLayout.Assembly assembly(Direction facing, DoorHingeSide hinge, DoorDesign design) {
        return new DoorLayout.Assembly(BlockPos.ZERO, facing, hinge, design);
    }

    @Test
    void theFootprintIsTheDesignsCellsFacingNorth() {
        for (DoorDesign design : DoorDesign.values()) {
            List<DoorSwingOutline.Edge> edges = DoorPlacementOutline.footprint(assembly(Direction.NORTH, DoorHingeSide.LEFT, design));
            assertEquals(12, edges.size());
            double[] bounds = bounds(edges);
            assertEquals(0, bounds[0], 1e-9);
            assertEquals(design.width(), bounds[3], 1e-9, design + ": footprint width");
            assertEquals(0, bounds[1], 1e-9);
            assertEquals(design.height(), bounds[4], 1e-9);
            assertEquals(0, bounds[2], 1e-9);
            assertEquals(16, bounds[5], 1e-9);
        }
    }

    @Test
    void everyPartOfThePreviewTurnsWithTheFacing() {
        for (DoorDesign design : DoorDesign.values()) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                for (int turns = 0; turns < 4; turns++) {
                    DoorLayout.Assembly north = assembly(Direction.NORTH, hinge, design);
                    DoorLayout.Assembly turned = assembly(FACINGS.get(turns), hinge, design);
                    int t = turns;
                    assertEquals(DoorPlacementOutline.footprint(north).stream().map(e -> e.rotateY(t)).toList(),
                            DoorPlacementOutline.footprint(turned), design + "/" + hinge + " footprint " + turns);
                    assertEquals(DoorPlacementOutline.hingeAxis(north).rotateY(t), DoorPlacementOutline.hingeAxis(turned));
                    assertEquals(DoorSwingOutline.forCell(design, hinge, 0, 0, 0, t),
                            DoorPlacementOutline.silhouette(turned, false), "the closed silhouette is the highlight's");
                    assertEquals(DoorSwingOutline.forCell(design, hinge, 1, 0, 0, t),
                            DoorPlacementOutline.silhouette(turned, true), "a powered spot previews the open door");
                }
            }
        }
    }

    @Test
    void theHingeAxisStandsAtThePivotFloorToTop() {
        DoorSwingOutline.Edge left = DoorPlacementOutline.hingeAxis(assembly(Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.ARCHED));
        DoorSwingOutline.Edge right = DoorPlacementOutline.hingeAxis(assembly(Direction.NORTH, DoorHingeSide.RIGHT, DoorDesign.ARCHED));
        assertEquals(new DoorSwingOutline.Edge(1.5, 0, 14.5, 1.5, 32, 14.5), left);
        assertEquals(new DoorSwingOutline.Edge(14.5, 0, 14.5, 14.5, 32, 14.5), right);
        assertEquals(30.5, DoorPlacementOutline.hingeAxis(assembly(Direction.NORTH, DoorHingeSide.RIGHT, DoorDesign.ROUND)).x1(), 1e-9);
    }

    @Test
    void theArchedPreviewNeverLeavesItsFootprint() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (boolean open : new boolean[] {false, true}) {
                double[] b = bounds(DoorPlacementOutline.silhouette(assembly(Direction.NORTH, hinge, DoorDesign.ARCHED), open));
                assertTrue(b[0] >= -1e-9 && b[3] <= 16 + 1e-9 && b[1] >= -1e-9 && b[4] <= 32 + 1e-9
                        && b[2] >= -1e-9 && b[5] <= 16 + 1e-9, hinge + " open=" + open + ": " + java.util.Arrays.toString(b));
            }
        }
    }

    /** {minX, minY, minZ, maxX, maxY, maxZ} over every endpoint. */
    private static double[] bounds(List<DoorSwingOutline.Edge> edges) {
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (DoorSwingOutline.Edge e : edges) {
            for (double[] p : new double[][] {{e.x1(), e.y1(), e.z1()}, {e.x2(), e.y2(), e.z2()}}) {
                for (int i = 0; i < 3; i++) {
                    b[i] = Math.min(b[i], p[i]);
                    b[i + 3] = Math.max(b[i + 3], p[i]);
                }
            }
        }
        return b;
    }
}
