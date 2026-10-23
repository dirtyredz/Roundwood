package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;

/**
 * Every look once, on one representative piece, in one row: the ordinary archway (the one
 * piece that takes wood, paint and masonry alike) in every wood (the eleven vanilla ones, then up to
 * eight present modded planks), the sixteen paints on oak and the four masonry materials, each with
 * a sign beside it. Archways stand side by side, two columns each. Occupies {@code r 0..78, f 3..4}
 * for the most looks the samples allow (39); the section's rectangle is checked to hold nothing else.
 */
public final class MaterialsSection implements ShowcaseSection {
    public static final int F = 4;
    /** The last column the section can use: 39 looks, two columns each, from column 1. */
    public static final int R_MAX = 78;
    private static final int R0 = 1;
    private static final int PITCH = 2;

    @Override
    public String id() {
        return "materials";
    }

    /** Frame {@code r} of the left column of the archway showing look {@code index}. */
    public static int r(int index) {
        return R0 + PITCH * index;
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        var looks = canvas.samples();
        for (int i = 0; i < looks.size(); i++) {
            Appearance look = looks.get(i);
            canvas.sign(r(i), F - 1, ShowcaseSamples.label(look));
            canvas.archway(ModBlocks.archway(ArchwayDesign.ORDINARY), look, FaceTrims.NONE, r(i), F);
        }
    }
}
