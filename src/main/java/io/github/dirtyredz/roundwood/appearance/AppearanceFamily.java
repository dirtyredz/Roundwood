package io.github.dirtyredz.roundwood.appearance;

/** The three appearance-bearing families, and which material kinds each accepts. */
public enum AppearanceFamily {
    DOOR, WINDOW, TRIM;

    /** Every family accepts wood and block surfaces; trims also take masonry. */
    public boolean accepts(Appearance.Kind kind) {
        return kind == Appearance.Kind.WOOD || kind == Appearance.Kind.BLOCK_SURFACE || this == TRIM;
    }

    public boolean accepts(Appearance appearance) {
        return accepts(appearance.kind());
    }
}
