package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.List;

/**
 * Every paint on all three families: per dye a closed door, a freestanding trim beside it and a
 * window, with a sign in front. Four dyes per row, four rows running away from the builder.
 * Occupies {@code r 32..71, f 20..34}.
 */
public final class PaintsSection implements ShowcaseSection {
    public static final int PER_ROW = 4;
    private static final int R0 = 32;
    private static final int PITCH = 11;
    private static final int F0 = 21;
    private static final int ROW_PITCH = 4;
    /** Offsets from the dye's door: the trim's left column and the window. */
    public static final int TRIM_OFFSET = 3;
    public static final int WINDOW_OFFSET = 6;

    @Override
    public String id() {
        return "paints";
    }

    /** Frame {@code r} of dye {@code index}'s door (its left column). */
    public static int r(int index) {
        return R0 + PITCH * (index % PER_ROW);
    }

    /** Frame {@code f} of dye {@code index}'s door, trim and window; the sign is one nearer. */
    public static int f(int index) {
        return F0 + ROW_PITCH * (index / PER_ROW);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        List<Appearance> paints = canvas.paints();
        for (int i = 0; i < paints.size(); i++) {
            Appearance paint = paints.get(i);
            int r = r(i);
            int f = f(i);
            canvas.sign(r, f - 1, ShowcaseCanvas.label(paint), "door, trim", "and window");
            canvas.door(paint, r, f, DoorHingeSide.LEFT, false);
            canvas.trim(paint, r + TRIM_OFFSET, f);
            canvas.window(paint, r + WINDOW_OFFSET, 0, f, canvas.acrossAxis(), false);
        }
    }
}
