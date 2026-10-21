package io.github.dirtyredz.roundwood.appearance;

/** The three appearance-bearing families, and which material kinds each accepts. */
public enum AppearanceFamily {
    DOOR, WINDOW, TRIM;

    /** Doors and windows are wood (bare or painted); trims also take masonry. */
    public boolean accepts(Appearance.Kind kind) {
        return kind == Appearance.Kind.WOOD || this == TRIM;
    }

    public boolean accepts(Appearance appearance) {
        return accepts(appearance.kind());
    }
}
