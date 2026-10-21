package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.variant.TrimMaterial;

/**
 * The masonry frames standing free, nothing behind them, so the radial brickwork, the open centre
 * and both faces can be inspected (walk round the back). Each has a sign to its left. Occupies
 * {@code r 3..35, f 66}.
 */
public final class BrickArchesSection implements ShowcaseSection {
    public static final int F = 66;
    private static final int R0 = 4;
    private static final int PITCH = 10;

    @Override
    public String id() {
        return "brick arches";
    }

    /** Frame {@code r} of the left column of masonry frame {@code index}. */
    public static int r(int index) {
        return R0 + PITCH * index;
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        for (int i = 0; i < FinishCatalog.MASONRY.size(); i++) {
            TrimMaterial material = FinishCatalog.MASONRY.get(i);
            canvas.trim(ModBlocks.trim(material), r(i), F);
            canvas.sign(r(i) - 1, F, material.displayName(), "round trim", "freestanding");
        }
    }
}
