package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collection;
import java.util.List;

/**
 * Converts pixel boxes to Minecraft voxel shapes. The only geometry class that builds Minecraft
 * shapes ({@code Shapes}, {@code VoxelShape}); {@code Surface}, {@code FaceUv} and {@code Box} use
 * only the plain {@code Direction} enum.
 */
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

    /** {@code side} turned like {@link Box#rotateY} (vertical sides stay). */
    public static Direction turn(Direction side, int turns) {
        Direction out = side;
        if (out.getAxis().isHorizontal()) {
            for (int i = 0; i < Math.floorMod(turns, 4); i++) {
                out = out.getClockWise();
            }
        }
        return out;
    }

    public static List<Box> rotate(Collection<Box> boxes, int quarterTurns) {
        return boxes.stream().map(box -> box.rotateY(quarterTurns)).toList();
    }
}
