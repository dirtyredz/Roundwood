package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * What the door placement preview draws for a planned assembly (0.11.0): the leaf as it will stand
 * (its silhouette, the same edges the swinging highlight uses), the cells it will own, and the hinge
 * it will turn on. Pixels relative to the anchor cell's origin, turned for the facing: the frame
 * {@link DoorSwingOutline#forCell} gives for the anchor. Read from the placement plan's assembly
 * ({@code assembly/DoorPlacement}), never decided here. Plain Java.
 */
public final class DoorPlacementOutline {
    private DoorPlacementOutline() {
    }

    /** {@code assembly}'s leaf at rest, open or closed. */
    public static List<DoorSwingOutline.Edge> silhouette(DoorLayout.Assembly assembly, boolean open) {
        return DoorSwingOutline.forCell(assembly.design(), assembly.hinge(), open ? 1 : 0, 0, 0, turns(assembly));
    }

    /** {@code assembly}'s cells: the twelve edges of their joint box. */
    public static List<DoorSwingOutline.Edge> footprint(DoorLayout.Assembly assembly) {
        double w = assembly.design().width();
        double h = DoorDesign.HEIGHT;
        List<DoorSwingOutline.Edge> edges = new ArrayList<>(12);
        for (double y : new double[] {0, h}) {
            edges.add(new DoorSwingOutline.Edge(0, y, 0, w, y, 0));
            edges.add(new DoorSwingOutline.Edge(0, y, 16, w, y, 16));
            edges.add(new DoorSwingOutline.Edge(0, y, 0, 0, y, 16));
            edges.add(new DoorSwingOutline.Edge(w, y, 0, w, y, 16));
        }
        for (double x : new double[] {0, w}) {
            for (double z : new double[] {0, 16}) {
                edges.add(new DoorSwingOutline.Edge(x, 0, z, x, h, z));
            }
        }
        return edges.stream().map(edge -> edge.rotateY(turns(assembly))).toList();
    }

    /** {@code assembly}'s hinge: the leaf's swing axis, floor to top. */
    public static DoorSwingOutline.Edge hingeAxis(DoorLayout.Assembly assembly) {
        double x = DoorGeometry.pivotX(assembly.design(), assembly.hinge());
        return new DoorSwingOutline.Edge(x, 0, DoorGeometry.PIVOT_Z, x, DoorDesign.HEIGHT, DoorGeometry.PIVOT_Z)
                .rotateY(turns(assembly));
    }

    private static int turns(DoorLayout.Assembly assembly) {
        return VoxelShapeFactory.quarterTurns(assembly.facing());
    }
}
