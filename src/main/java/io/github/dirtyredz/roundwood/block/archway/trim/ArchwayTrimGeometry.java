package io.github.dirtyredz.roundwood.block.archway.trim;

import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayGeometry;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.geometry.Box;

import java.util.List;

/**
 * An archway's face casings (0.12.0), in the archway's authored frame (facing north, front at
 * {@code z 16}): a Universal Trim covers the whole frame face it is fitted to ({@link #inCasing}:
 * the frame's own pixels, jambs and crown, and on the keyhole its shoulders), {@link AttachmentFace#PROUD} pixel
 * proud of it, FRONT at {@code z 16..17} and BACK at {@code -1..0}. Casings are decorative: they never
 * collide and are never targeted themselves; a click on the frame face under one addresses it
 * ({@link ArchwayTrimTarget}). Plain Java.
 */
public final class ArchwayTrimGeometry {
    /** Where a hit's depth decides the face ({@link AttachmentFace#at}): the middle of the frame, 8. */
    public static final double MIDDLE = (ArchwayDesign.BACK + ArchwayDesign.FRONT) / 2;

    private ArchwayTrimGeometry() {
    }

    /** True when pixel (u, v) of the archway's plane carries casing: the frame face. */
    public static boolean inCasing(ArchwayDesign design, int u, int v) {
        return design.inFrame(u, v);
    }

    /** The depth range {@code {back, front}} of {@code face}'s casing. */
    public static double[] depth(AttachmentFace face) {
        return face == AttachmentFace.FRONT
                ? new double[] {ArchwayDesign.FRONT, ArchwayDesign.FRONT + AttachmentFace.PROUD}
                : new double[] {ArchwayDesign.BACK - AttachmentFace.PROUD, ArchwayDesign.BACK};
    }

    /** {@code face}'s whole casing in assembly coordinates. */
    public static List<Box> casing(ArchwayDesign design, AttachmentFace face) {
        double[] z = depth(face);
        return ArchwayGeometry.rects(design).stream().map(rect -> rect.extrude(z[0], z[1])).toList();
    }
}
