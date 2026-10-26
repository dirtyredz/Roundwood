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
        return DoorSwingOutline.at(assembly.design(), assembly.hinge(), open ? 1 : 0).stream()
                .map(e -> e.translate(0, 0, assembly.depth().offset()).rotateY(turns(assembly))).toList();
    }

    /** {@code assembly}'s cells: the twelve edges of their joint box. */
    public static List<DoorSwingOutline.Edge> footprint(DoorLayout.Assembly assembly) {
        double w = assembly.design().width();
        double h = assembly.design().height();
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
        return new DoorSwingOutline.Edge(x, 0, DoorGeometry.PIVOT_Z + assembly.depth().offset(), x, assembly.design().height(), DoorGeometry.PIVOT_Z + assembly.depth().offset())
                .rotateY(turns(assembly));
    }

    public static List<DoorSwingOutline.Edge> hingeAxes(DoorLayout.Assembly assembly) {
        if (!assembly.design().doubleLeaf()) return List.of(hingeAxis(assembly));
        return java.util.Arrays.stream(net.minecraft.world.level.block.state.properties.DoorHingeSide.values())
                .map(side -> hingeAxis(new DoorLayout.Assembly(assembly.anchor(), assembly.facing(), side, assembly.design(), assembly.depth()))).toList();
    }

    private static int turns(DoorLayout.Assembly assembly) {
        return VoxelShapeFactory.quarterTurns(assembly.facing());
    }
}
