package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.List;

/**
 * Every dye (0.8.0): per dye a closed door painted and wearing a painted door trim on its front, an
 * oak window whose glass is tinted that dye, and a window with a painted frame, with a sign in front.
 * Four dyes per row, four rows running away from the builder. Occupies {@code r 32..71, f 20..34}.
 */
public final class PaintsSection implements ShowcaseSection {
    public static final int PER_ROW = 4;
    private static final int R0 = 32;
    private static final int PITCH = 11;
    private static final int F0 = 21;
    private static final int ROW_PITCH = 4;
    /** Offsets from the dye's door: the tinted-glass window and the painted window. */
    public static final int GLASS_OFFSET = 3;
    public static final int WINDOW_OFFSET = 6;

    @Override
    public String id() {
        return "paints";
    }

    /** Frame {@code r} of dye {@code index}'s door (its left column). */
    public static int r(int index) {
        return R0 + PITCH * (index % PER_ROW);
    }

    /** Frame {@code f} of dye {@code index}'s door and windows; the sign is one nearer. */
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
            canvas.sign(r, f - 1, ShowcaseSamples.label(paint), "door and trim,", "glass, window");
            if (canvas.door(paint, r, f, DoorHingeSide.LEFT, false)) {
                canvas.doorTrim(paint, r, f, AttachmentFace.FRONT);
            }
            canvas.window(ShowcaseWindow.of(WindowDesign.plain(WindowProfile.ROUND), WindowPosition.CENTER.pose(canvas.acrossAxis()),
                    r + GLASS_OFFSET, 0, f, Appearance.DEFAULT).glass(paint.paint().orElseThrow()));
            canvas.window(paint, r + WINDOW_OFFSET, 0, f, canvas.acrossAxis(), false);
        }
    }
}
