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
    ROUND("round_door", 2, 4, 15) {
        @Override
        public boolean inLeaf(int u, int v) {
            return PixelCircle.inDisc(WIDTH_ROUND, WIDTH_ROUND, u, v);
        }
    },
    ARCHED("arched_door", 1, 2, 11) {
        @Override
        public boolean inLeaf(int u, int v) {
            return u >= 0 && u < 16 && v >= 0 && v < HEIGHT && ARCH.contains(u, v);
        }
    },
    DOUBLE_ARCHED("double_arched_door", 3, 2, 20) {
        @Override public boolean inLeaf(int u, int v) {
            return u >= 0 && u < 48 && v >= 0 && v < 48 && (v < 16 ||
                    !io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign.TOP_2X3.top().contains(u, v - 16));
        }
    };

    /** Every design is two blocks tall. */
    public static final int HEIGHT = 32;
    /** The arched leaf's crown starts here (its centre height). */
    public static final int ARCH_SPRING = 24;
    private static final int WIDTH_ROUND = 32;
    private static final ArchedProfile ARCH = new ArchedProfile(0, 16, ARCH_SPRING);
    /** The rows the two hinge straps run along (both designs). */
    private static final int[] STRAP_ROWS = {8, 23};
    private static final int STRAP_LENGTH = 7;

    private final String id;
    private final int columns;
    private final int strapFrom;
    private final int knobX;
    private List<PixelCircle.Rect> rects;

    DoorDesign(String id, int columns, int strapFrom, int knobX) {
        this.id = id;
        this.columns = columns;
        this.strapFrom = strapFrom;
        this.knobX = knobX;
    }

    /** True when pixel (u, v) of the door's plane is leaf. */
    public abstract boolean inLeaf(int u, int v);

    /** The block id the canonical door of this design is registered under (and its model names start with). */
    public String id() {
        return id;
    }

    /** Columns of cells: 2 (round) or 1 (arched); every design has two rows. */
    public int columns() {
        return columns;
    }

    /** Width in pixels (16 per column). */
    public int height() { return this == DOUBLE_ARCHED ? 48 : HEIGHT; }
    public boolean doubleLeaf() { return this == DOUBLE_ARCHED; }

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

    /** The rows the hinge straps run along. */
    public int[] strapRows() {
        return STRAP_ROWS.clone();
    }

    /** Where a left-hinge strap starts (x, pixels); it runs {@link #strapLength} towards the free edge. */
    public int strapFrom() {
        return strapFrom;
    }

    public int strapLength() {
        return STRAP_LENGTH;
    }

    /** The left edge of the 2 x 2 knob for a left hinge (the round door's sits in the middle, the arched one's near the free edge). */
    public int knobX() {
        return knobX;
    }

    /** The bottom edge of the knob: mid-height on both designs. */
    public int knobY() {
        return height() / 2 - 1;
    }
}
