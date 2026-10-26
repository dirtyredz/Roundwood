package io.github.dirtyredz.roundwood.block.arch.geometry;

/** Shared translation for generated and runtime icons; authored geometry and UVs stay unchanged. */
public final class ArchPieceItemGeometry {
    private ArchPieceItemGeometry() {}

    /** The 48-pixel icon occupies the complete legal model interval [-16,32]. */
    public static int offsetX(ArchPieceDesign design) { return design.width() > 32 ? -16 : 0; }
}
