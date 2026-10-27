package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Fitted top volumes and only their exposed surfaces, in the top's authored frame. */
public final class DoorArchGeometry {
    public enum Layer { BODY, FRONT_CASING, BACK_CASING }
    private record Key(DoorDesign door, ArchPieceThickness thickness, ArchPieceDepth depth, Layer layer, int column, int row) {}
    private static final ConcurrentHashMap<Key, List<Surface>> SURFACES = new ConcurrentHashMap<>();
    private record VolumeKey(DoorDesign door, ArchPieceThickness thickness, ArchPieceDepth depth, Layer layer) {}
    private static final ConcurrentHashMap<VolumeKey, List<Box>> VOLUMES = new ConcurrentHashMap<>();
    private DoorArchGeometry() {}

    public static List<Box> boxes(DoorDesign door, ArchPieceThickness thickness, ArchPieceDepth depth, Layer layer) {
        return VOLUMES.computeIfAbsent(new VolumeKey(door, thickness, depth, layer), key -> buildBoxes(door, thickness, depth, layer));
    }
    private static List<Box> buildBoxes(DoorDesign door, ArchPieceThickness thickness, ArchPieceDepth depth, Layer layer) {
        var top = DoorArchFitPlan.topFor(door);
        var result = new ArrayList<Box>();
        for (int x = 0; x < top.width(); x++) for (int y = 0; y < top.height(); y++) {
            double[] band = band(door, thickness, depth, layer, x, y);
            if (band != null) result.add(new Box(x, y, band[0], x + 1, y + 1, band[1]));
        }
        return List.copyOf(result);
    }

    private static double[] band(DoorDesign door, ArchPieceThickness thickness, ArchPieceDepth depth, Layer layer, int x, int y) {
        if (!DoorArchFitPlan.topFor(door).contains(Side.LEFT, x, y)) return null;
        // Identical to the standalone arch piece: no rebate for the door leaf.
        double back = thickness.back(depth);
        double front = thickness.front(depth);
        return switch (layer) {
            case BODY -> new double[]{back, front};
            case FRONT_CASING -> new double[]{front, front + 1};
            case BACK_CASING -> new double[]{back - 1, back};
        };
    }

    public static List<Surface> surfaces(DoorDesign door, ArchPieceThickness thickness, ArchPieceDepth depth, Layer layer, int column, int row) {
        return SURFACES.computeIfAbsent(new Key(door, thickness, depth, layer, column, row), DoorArchGeometry::surfaces);
    }
    private static List<Surface> surfaces(Key key) {
        var out = new ArrayList<Surface>();
        for (int x = key.column * 16; x < (key.column + 1) * 16; x++) for (int y = key.row * 16; y < (key.row + 1) * 16; y++) {
            var band = band(key.door, key.thickness, key.depth, key.layer, x, y);
            if (band == null) continue;
            add(out, key, x, y, band[0], band[1], Direction.NORTH);
            add(out, key, x, y, band[0], band[1], Direction.SOUTH);
            for (var face : new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}) {
                var neighbour = band(key.door, key.thickness, key.depth, key.layer, x + face.getStepX(), y + face.getStepY());
                if (neighbour == null || neighbour[1] <= band[0] || neighbour[0] >= band[1]) {
                    add(out, key, x, y, band[0], band[1], face);
                } else {
                    if (neighbour[0] > band[0]) add(out, key, x, y, band[0], neighbour[0], face);
                    if (neighbour[1] < band[1]) add(out, key, x, y, neighbour[1], band[1], face);
                }
            }
        }
        return List.copyOf(out);
    }
    private static void add(List<Surface> out, Key key, int x, int y, double back, double front, Direction face) {
        // Side UVs must not cross a donor tile, including rear casings straddling z=0.
        if (face.getAxis() != Direction.Axis.Z) {
            double boundary = (Math.floor(back / 16) + 1) * 16;
            if (boundary < front) {
                add(out, key, x, y, back, boundary, face);
                add(out, key, x, y, boundary, front, face);
                return;
            }
        }
        var surface = new Surface(new Box(x - key.column * 16, y - key.row * 16, back,
                x - key.column * 16 + 1, y - key.row * 16 + 1, front), face, "");
        out.add(key.layer == Layer.BODY ? surface : surface.separated());
    }

    /** Whole static frame in door coordinates; target proxies remain in occupied depth cells. */
    public static List<Box> selection(DoorDesign door, DoorArchAttachment attachment, boolean target) {
        var out = new ArrayList<Box>();
        addSelection(out, door, attachment, Layer.BODY, target);
        if (attachment.casings().has(AttachmentFace.FRONT)) addSelection(out, door, attachment, Layer.FRONT_CASING, target);
        if (attachment.casings().has(AttachmentFace.BACK)) addSelection(out, door, attachment, Layer.BACK_CASING, target);
        return List.copyOf(out);
    }
    private static void addSelection(List<Box> out, DoorDesign door, DoorArchAttachment attachment, Layer layer, boolean target) {
        int offset = 16 * DoorArchFitPlan.topOffset(door);
        for (var box : boxes(door, attachment.thickness(), attachment.depth(), layer)) {
            double back = box.minZ(), front = box.maxZ();
            if (target && front <= 0) { back = 0; front = 1; }
            else if (target && back >= 16) { back = 15; front = 16; }
            else if (target) { back = Math.max(0, back); front = Math.min(16, front); }
            out.add(new Box(box.minX(), box.minY() + offset, back, box.maxX(), box.maxY() + offset, front));
        }
    }
}
