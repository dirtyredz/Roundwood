package io.github.dirtyredz.roundwood.block.window.geometry.face;

import java.util.Locale;

/**
 * Whether a window piece's glazing is one sheet ({@link #PLAIN}) or crossed by wooden beams
 * ({@link #CROSS}, 0.7.0). Fixed per block registration (an explicit item, not a toggle), so it is
 * never a blockstate value; the beams are drawn by {@link WindowMuntinGeometry}.
 */
public enum WindowPattern {
    PLAIN(""),
    CROSS("_cross");

    private final String suffix;

    WindowPattern(String suffix) {
        this.suffix = suffix;
    }

    /** Appended to the profile's id for the registry name and model stems. */
    public String suffix() {
        return suffix;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
