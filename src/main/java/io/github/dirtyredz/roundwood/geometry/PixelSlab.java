package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * A flat pixel mask extruded between two depths, cut into one cell's visible surfaces (extracted in
 * 0.11.0 from the door's camouflage corners, which the arched door's casing now shares). The mask is
 * read in assembly pixels, so a side face is drawn only where the neighbouring pixel, in this cell or
 * the next, is outside the mask: no internal faces between merged pieces or across cell seams. Plain
 * Java ({@link Direction} only).
 */
public final class PixelSlab {
    private PixelSlab() {
    }

    /**
     * The surfaces of the mask's part inside the cell whose lower-left assembly pixel is
     * ({@code u0}, {@code v0}), cell-local (0..16), facing north: front ({@code SOUTH}, z =
     * {@code front}) and back ({@code NORTH}) of every merged piece, and each side only where it is
     * exposed. Deterministic order: fronts and backs by piece, then sides by line.
     */
    public static List<Surface> cellFaces(PixelCircle.PixelTest mask, int u0, int v0, double back, double front, String texture) {
        List<Surface> faces = new ArrayList<>();
        for (PixelCircle.Rect rect : PixelCircle.rasterize(16, 16, (u, v) -> mask.contains(u0 + u, v0 + v))) {
            faces.add(new Surface(rect.extrude(back, front), Direction.NORTH, texture));
            faces.add(new Surface(rect.extrude(back, front), Direction.SOUTH, texture));
        }
        for (int line = 0; line < 16; line++) {
            int l = line;
            // Rows for the top/bottom faces, columns for the side faces; each a strip of exposed pixels.
            strips(faces, Direction.UP, texture, i -> exposed(mask, u0 + i, v0 + l, 0, 1),
                    (a, b) -> new Box(a, l, back, b, l + 1, front));
            strips(faces, Direction.DOWN, texture, i -> exposed(mask, u0 + i, v0 + l, 0, -1),
                    (a, b) -> new Box(a, l, back, b, l + 1, front));
            strips(faces, Direction.EAST, texture, i -> exposed(mask, u0 + l, v0 + i, 1, 0),
                    (a, b) -> new Box(l, a, back, l + 1, b, front));
            strips(faces, Direction.WEST, texture, i -> exposed(mask, u0 + l, v0 + i, -1, 0),
                    (a, b) -> new Box(l, a, back, l + 1, b, front));
        }
        return faces;
    }

    /** A mask pixel whose neighbour in direction (du, dv) is outside the mask: its face on that side shows. */
    private static boolean exposed(PixelCircle.PixelTest mask, int u, int v, int du, int dv) {
        return mask.contains(u, v) && !mask.contains(u + du, v + dv);
    }

    private interface StripBox {
        Box of(int from, int to);
    }

    /** Adds one face per maximal run of exposed pixels along a 16-pixel line. */
    private static void strips(List<Surface> out, Direction side, String texture, IntPredicate exposed, StripBox box) {
        int i = 0;
        while (i < 16) {
            if (!exposed.test(i)) {
                i++;
                continue;
            }
            int start = i;
            while (i < 16 && exposed.test(i)) {
                i++;
            }
            out.add(new Surface(box.of(start, i), side, texture));
        }
    }
}
