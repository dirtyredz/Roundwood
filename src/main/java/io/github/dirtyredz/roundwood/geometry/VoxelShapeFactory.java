package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collection;
import java.util.List;

/** Converts pixel boxes to Minecraft voxel shapes; the only geometry class that touches Minecraft. */
public final class VoxelShapeFactory {
    private VoxelShapeFactory() {
    }

    public static VoxelShape shape(Collection<Box> boxes) {
        VoxelShape shape = Shapes.empty();
        for (Box box : boxes) {
            shape = Shapes.or(shape, Shapes.box(
                    box.minX() / Box.BLOCK, box.minY() / Box.BLOCK, box.minZ() / Box.BLOCK,
                    box.maxX() / Box.BLOCK, box.maxY() / Box.BLOCK, box.maxZ() / Box.BLOCK));
        }
        return shape.optimize();
    }

    /** Quarter turns that take geometry authored facing north to {@code facing}. */
    public static int quarterTurns(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    public static List<Box> rotate(Collection<Box> boxes, int quarterTurns) {
        return boxes.stream().map(box -> box.rotateY(quarterTurns)).toList();
    }
}
