package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.ArchedProfile;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import io.github.dirtyredz.roundwood.geometry.RingGeometry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.dirtyredz.roundwood.geometry.PixelSlab;
import io.github.dirtyredz.roundwood.geometry.Surface;

/**
 * The door's trim attachment (0.8.0) in the door's authored frame (facing north; x across
 * 0..width, y up 0..32, z the depth with the front face, the placing player's side, at 16): a
 * two-pixel casing one pixel proud of the closed leaf ({@code z 13..16}) on each face, FRONT at
 * {@code 16..17} (where a 0.7.0 fitted frame stood) and BACK at {@code 12..13}. Its outline follows
 * the design ({@link #inCasing}): the round door's 32/28 ring ({@link RingGeometry}); the arched
 * door's (0.11.0) jambs and crown, the leaf's own outline minus the same arch two pixels in, open at
 * the floor (no threshold). The casings are static: they never swing with the leaf, never collide,
 * and the FRONT casing's one-pixel projection into the cells in front is render-only. Rays reach
 * them through in-cell proxies ({@link #target}): the FRONT proxy is the casing's footprint just
 * inside the door cells. Plain Java.
 */
public final class DoorTrimGeometry {
    /** Where a hit's depth decides the face ({@link AttachmentFace#at}): the middle of the closed leaf, 14.5. */
    public static final double MIDDLE = (DoorGeometry.BACK + DoorGeometry.FRONT) / 2;
    /** The casing's width in pixels, on every design. */
    public static final int WIDTH = RingGeometry.WIDTH;
    /** The arched door's casing's inner edge: its leaf's arch, {@link #WIDTH} pixels in at the sides and the crown. */
    private static final ArchedProfile ARCHED_INNER = new ArchedProfile(WIDTH, 16 - WIDTH, DoorDesign.ARCH_SPRING);

    private record Casing(List<Box> front, List<Box> back, List<Box> frontProxy) {
    }

    private static final Map<DoorDesign, Casing> CASINGS = new EnumMap<>(DoorDesign.class);

    static {
        for (DoorDesign design : DoorDesign.values()) {
            List<PixelCircle.Rect> rects = PixelCircle.rasterize(design.width(), design.height(), (u, v) -> inCasing(design, u, v));
            CASINGS.put(design, new Casing(extrude(rects, DoorGeometry.FRONT, DoorGeometry.FRONT + AttachmentFace.PROUD),
                    extrude(rects, DoorGeometry.BACK - AttachmentFace.PROUD, DoorGeometry.BACK),
                    extrude(rects, DoorGeometry.FRONT - AttachmentFace.PROUD, DoorGeometry.FRONT)));
        }
    }

    private DoorTrimGeometry() {
    }

    /** True when pixel (u, v) of the door's plane is casing. */
    public static boolean inCasing(DoorDesign design, int u, int v) {
        return switch (design) {
            case ROUND -> RingGeometry.inRing(u, v);
            case DOUBLE_ARCHED, TALL_DOUBLE_ARCHED -> design.inLeaf(u, v) && !new ArchedProfile(WIDTH, design.width() - WIDTH, design.height() - 32).contains(u, v);
            case ARCHED -> design.inLeaf(u, v) && !ARCHED_INNER.contains(u, v);
        };
    }

    /** The exact surfaces supplied to the casing model baker; targets retain the authored slab. */
    public static List<Surface> surfaces(DoorDesign design, AttachmentFace face, int column, int row) {
        Box slab = ring(design, face).get(0);
        return PixelSlab.cellFaces((u,v) -> inCasing(design,u,v),
                column * 16, row * 16, slab.minZ(), slab.maxZ(), "").stream()
                .map(Surface::separated).toList();
    }

    /** The casing drawn on {@code face}. */
    public static List<Box> ring(DoorDesign design, AttachmentFace face) {
        Casing casing = CASINGS.get(design);
        return face == AttachmentFace.FRONT ? casing.front() : casing.back();
    }

    /** What rays hit for {@code face}'s casing, inside the door's own cells. */
    public static List<Box> target(DoorDesign design, AttachmentFace face) {
        Casing casing = CASINGS.get(design);
        return face == AttachmentFace.FRONT ? casing.frontProxy() : casing.back();
    }

    /** Keep the back-flush casing target inside its owning cell, like the historical front proxy. */
    public static List<Box> target(DoorDesign design, AttachmentFace face, double offset) {
        var boxes = target(design, face);
        return face == AttachmentFace.BACK && offset == -13 ? boxes.stream().map(b -> b.translate(0, 0, 1)).toList() : boxes;
    }

    /** True when an authored point lies on the casing's footprint (any depth). */
    public static boolean onRing(DoorDesign design, double x, double y) {
        return inPlane(design, x, y) && inCasing(design, (int) Math.floor(x), (int) Math.floor(y));
    }

    /** True when an authored point lies inside the casing's outer edge (the casing, or the leaf it frames). */
    public static boolean withinRing(DoorDesign design, double x, double y) {
        return inPlane(design, x, y) && design.inLeaf((int) Math.floor(x), (int) Math.floor(y));
    }

    /** True when depth {@code z} lies in a casing's in-cell target for {@code face} (an open door is aimed at there). */
    public static boolean inTargetDepth(AttachmentFace face, double z) {
        return face == AttachmentFace.FRONT
                ? z >= DoorGeometry.FRONT - AttachmentFace.PROUD && z <= DoorGeometry.FRONT
                : z >= DoorGeometry.BACK - AttachmentFace.PROUD && z <= DoorGeometry.BACK;
    }

    public static boolean inTargetDepth(AttachmentFace face, double z, double offset) {
        return inTargetDepth(face, face == AttachmentFace.BACK && offset == -13 ? z - 1 : z);
    }

    private static boolean inPlane(DoorDesign design, double x, double y) {
        return x >= 0 && x < design.width() && y >= 0 && y < design.height();
    }

    private static List<Box> extrude(List<PixelCircle.Rect> rects, double z1, double z2) {
        return rects.stream().map(rect -> rect.extrude(z1, z2)).toList();
    }
}
