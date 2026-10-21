package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.RingGeometry;

import java.util.List;

/**
 * The door's trim attachment (0.8.0) in the door's authored frame (facing north; x across 0..32, y up
 * 0..32, z the depth with the front face, the placing player's side, at 16): the 32/28 ring
 * ({@link RingGeometry}) one pixel proud of the closed leaf ({@code z 13..16}) on each face, FRONT at
 * {@code 16..17} (where a 0.7.0 fitted frame stood) and BACK at {@code 12..13}. The rings are static
 * casing around the doorway: they never swing with the leaf, never collide, and the FRONT ring's
 * one-pixel projection into the cells in front is render-only. Rays reach them through in-cell
 * proxies ({@link #target}): the FRONT proxy is the ring's footprint just inside the door cells.
 * Plain Java.
 */
public final class DoorTrimGeometry {
    /** Where a hit's depth decides the face: the middle of the closed leaf. */
    public static final double MIDDLE = (DoorGeometry.BACK + DoorGeometry.FRONT) / 2;
    public static final double PROUD = 1;

    private static final List<Box> FRONT_RING = RingGeometry.extrude(DoorGeometry.FRONT, DoorGeometry.FRONT + PROUD);
    private static final List<Box> BACK_RING = RingGeometry.extrude(DoorGeometry.BACK - PROUD, DoorGeometry.BACK);
    private static final List<Box> FRONT_PROXY = RingGeometry.extrude(DoorGeometry.FRONT - PROUD, DoorGeometry.FRONT);

    private DoorTrimGeometry() {
    }

    /** The ring drawn on {@code face}. */
    public static List<Box> ring(AttachmentFace face) {
        return face == AttachmentFace.FRONT ? FRONT_RING : BACK_RING;
    }

    /** What rays hit for {@code face}'s ring, inside the door's own cells. */
    public static List<Box> target(AttachmentFace face) {
        return face == AttachmentFace.FRONT ? FRONT_PROXY : BACK_RING;
    }

    /** The face a point at depth {@code z} addresses. */
    public static AttachmentFace face(double z) {
        return z >= MIDDLE ? AttachmentFace.FRONT : AttachmentFace.BACK;
    }

    /** True when an authored point lies on the ring's footprint (any depth). */
    public static boolean onRing(double x, double y) {
        return inPlane(x, y) && RingGeometry.inRing((int) Math.floor(x), (int) Math.floor(y));
    }

    /** True when an authored point lies inside the ring's outer edge (the ring, or the leaf it frames). */
    public static boolean withinRing(double x, double y) {
        return inPlane(x, y) && RingGeometry.inOuterDisc((int) Math.floor(x), (int) Math.floor(y));
    }

    /** True when depth {@code z} lies in a ring's in-cell target for {@code face} (an open door is aimed at there). */
    public static boolean inTargetDepth(AttachmentFace face, double z) {
        return face == AttachmentFace.FRONT
                ? z >= DoorGeometry.FRONT - PROUD && z <= DoorGeometry.FRONT
                : z >= DoorGeometry.BACK - PROUD && z <= DoorGeometry.BACK;
    }

    private static boolean inPlane(double x, double y) {
        return x >= 0 && x < RingGeometry.OUTER && y >= 0 && y < RingGeometry.OUTER;
    }
}
