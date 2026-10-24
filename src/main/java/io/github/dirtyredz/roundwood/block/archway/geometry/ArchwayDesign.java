package io.github.dirtyredz.roundwood.block.archway.geometry;

import io.github.dirtyredz.roundwood.geometry.ArchedProfile;

import java.util.Locale;

/**
 * The two archway designs (0.12.0), each a fixed profile in the 32 x 32 pixel plane of its 2 x 2
 * assembly (x across from the left as seen from the front, y up from the floor), authored like the
 * doors: facing north, the front (the placing player's side) at {@code z 16}. Both fill the square
 * footprint outside the opening (0.13.2), with a radius-14 upper opening. Below the spring line:
 * <ul>
 *     <li>{@link #ORDINARY}: the opening runs straight down, {@code x 2..30}, two-pixel jambs;</li>
 *     <li>{@link #KEYHOLE}: shoulders narrow it to a twenty-pixel lower passage ({@link KeyholeGeometry}).</li>
 * </ul>
 * The frame is whatever lies inside the square footprint and outside the opening, full block deep
 * ({@code z 0..16}); there is no threshold (the opening reaches the floor). Pixels follow the
 * pixel-centre convention of {@link ArchedProfile}. The id names the block and item. Plain Java.
 */
public enum ArchwayDesign {
    ORDINARY,
    KEYHOLE;

    /** The assembly's width and height in pixels. */
    public static final int SIZE = 32;
    /** Where the straight jambs end and both crowns are centred. */
    public static final int SPRING = 16;
    /** The ordinary opening's left side below the spring line. */
    public static final int OPENING_LEFT = 2;
    /** The ordinary opening (also every design's opening above the spring line): {@code x 2..30}, the radius-14 crown. */
    public static final ArchedProfile OPENING = new ArchedProfile(OPENING_LEFT, SIZE - OPENING_LEFT, SPRING);
    /** The frame's depth: a full block. */
    public static final double BACK = 0;
    public static final double FRONT = 16;

    /** The block and item id: {@code archway}, {@code keyhole_archway}. */
    public String id() {
        return this == ORDINARY ? "archway" : name().toLowerCase(Locale.ROOT) + "_archway";
    }

    /** True when pixel (u, v) of the assembly plane is open (the passage under the crown). */
    public boolean inOpening(int u, int v) {
        return this == ORDINARY ? OPENING.contains(u, v) : KeyholeGeometry.inOpening(u, v);
    }

    /** True when pixel (u, v) of the assembly plane is frame: inside the square footprint, outside the opening. */
    public boolean inFrame(int u, int v) {
        return u >= 0 && u < SIZE && v >= 0 && v < SIZE && !inOpening(u, v);
    }
}
