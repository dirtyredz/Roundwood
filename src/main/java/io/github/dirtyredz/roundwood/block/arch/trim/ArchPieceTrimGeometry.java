package io.github.dirtyredz.roundwood.block.arch.trim;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.geometry.Box;
import java.util.List;

import io.github.dirtyredz.roundwood.geometry.Surface;

/** Whole-mask decorative casings, one pixel proud; never added to collision or support. */
public final class ArchPieceTrimGeometry {
    public static final double MIDDLE = 8;
    private ArchPieceTrimGeometry() {}

    public static double[] depth(AttachmentFace face) {
        return depth(face, 0, 16);
    }
    public static double[] depth(AttachmentFace face, double back, double front) {
        return face == AttachmentFace.FRONT ? new double[]{front, front + AttachmentFace.PROUD}
                : new double[]{back - AttachmentFace.PROUD, back};
    }

    public static List<Surface> surfaces(ArchPieceDesign design, Side side,
            int column, int row, double back, double front, String texture) {
        return ArchPieceGeometry.surfaces(design, side, column, row, back, front, texture);
    }

    public static List<Box> casing(ArchPieceDesign design, Side side, AttachmentFace face) {
        double[] depth = depth(face);
        return ArchPieceGeometry.body(design, side).stream()
                .map(box -> new Box(box.minX(), box.minY(), depth[0], box.maxX(), box.maxY(), depth[1])).toList();
    }
}
