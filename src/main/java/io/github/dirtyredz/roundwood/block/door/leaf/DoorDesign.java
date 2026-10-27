package io.github.dirtyredz.roundwood.block.door.leaf;

import io.github.dirtyredz.roundwood.geometry.ArchedProfile;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;

import java.util.List;

/**
 * The fixed door designs (0.11.0): what a leaf's outline is, how many columns of cells the door
 * owns and where its hardware sits. Everything design-specific downstream (leaf, collision,
 * outline, camouflage corners, casing, models, caches) is keyed by this. Authored like the round
 * door: facing north, x across from the left edge, y up from the floor, in pixels. Plain Java.
 * <ul>
 *     <li>{@link #ROUND}: the 32-pixel disc over a 2 x 2 assembly (unchanged since 0.4.0);</li>
 *     <li>{@link #ARCHED}: 16 x 32 over a 1 x 2 assembly, straight sides and a flat bottom, a
 *     radius-eight crown centred at (8, 24).</li>
 * </ul>
 */
public enum DoorDesign {
    ROUND("round_door", 2, 32) {
        @Override
        public boolean inLeaf(int u, int v) {
            return PixelCircle.inDisc(WIDTH_ROUND, WIDTH_ROUND, u, v);
        }
    },
    ARCHED("arched_door", 1, 32) {
        @Override
        public boolean inLeaf(int u, int v) {
            return u >= 0 && u < 16 && v >= 0 && v < HEIGHT && ARCH.contains(u, v);
        }
    },
    DOUBLE_ARCHED("double_arched_door", 3, 48),
    TALL_DOUBLE_ARCHED("tall_double_arched_door", 3, 64);

    /** Historical single-door height; paired designs carry their own height. */
    public static final int HEIGHT = 32;
    /** The arched leaf's crown starts here (its centre height). */
    public static final int ARCH_SPRING = 24;
    private static final int WIDTH_ROUND = 32;
    private static final ArchedProfile ARCH = new ArchedProfile(0, 16, ARCH_SPRING);

    private final String id;
    private final int columns;
    private final int height;
    private List<PixelCircle.Rect> rects;

    DoorDesign(String id, int columns, int height) {
        this.id = id;
        this.columns = columns;
        this.height = height;
    }

    /** True when pixel (u, v) of the door's plane is leaf. */
    public boolean inLeaf(int u, int v) {
        int crown = height - 32;
        return u >= 0 && u < width() && v >= 0 && v < height
                && (v < crown || !io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign.TOP_2X3.top().contains(u, v - crown));
    }

    /** The block id the canonical door of this design is registered under (and its model names start with). */
    public String id() {
        return id;
    }

    /** Columns of owned cells; height() determines the row count. */
    public int columns() {
        return columns;
    }

    /** Height of the owned footprint in pixels. */
    public int height() { return height; }
    public boolean doubleLeaf() { return columns == 3; }

    public int width() {
        return 16 * columns;
    }

    /** The leaf's pixels merged into rectangles, x across, y up. */
    public synchronized List<PixelCircle.Rect> leafRects() {
        if (rects == null) {
            rects = PixelCircle.rasterize(width(), height(), this::inLeaf);
        }
        return rects;
    }

}
