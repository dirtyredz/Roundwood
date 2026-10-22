package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;

import java.util.List;

/**
 * The retained legacy exhibit (0.8.0): the four masonry frames standing free, as worlds from 0.7.0
 * and earlier may still hold them (the door trim item no longer places them; frames fitted to a door
 * became door trims on upgrade). Nothing behind them, so the radial brickwork, the open centre and
 * both faces can be inspected. Each has a sign to its left. Occupies {@code r 3..35, f 78}.
 */
public final class BrickArchesSection implements ShowcaseSection {
    public static final int F = 78;
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
        List<Appearance> masonry = canvas.masonry();
        for (int i = 0; i < masonry.size(); i++) {
            canvas.trim(masonry.get(i), r(i), F);
            canvas.sign(r(i), F, ShowcaseSamples.label(masonry.get(i)), "legacy frame", "(older saves)");
        }
    }
}
