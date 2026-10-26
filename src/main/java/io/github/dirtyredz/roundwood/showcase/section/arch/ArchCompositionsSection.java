package io.github.dirtyredz.roundwood.showcase.section.arch;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import net.minecraft.world.level.block.Blocks;

/** Full-block columns sit outside the opening; no hidden posts or ownership between pieces. */
public final class ArchCompositionsSection implements ShowcaseSection {
    public static final int F = ShowcaseSpacing.ROW_ARCH_COMPOSITIONS;
    public static final int ORDINARY_R = ShowcaseSpacing.FIRST_R;
    public static final int KEYHOLE_R = ORDINARY_R + ShowcaseSpacing.pitch(4);
    public static final int TALL_R = KEYHOLE_R + ShowcaseSpacing.pitch(4);
    public static final int R_MAX = TALL_R + 4;

    @Override public String id() { return "arch_compositions"; }
    @Override public void build(ShowcaseCanvas canvas) {
        canvas.sign(ORDINARY_R, F, "Ordinary opening", "Flush spring edges");
        columns(canvas, ORDINARY_R, 2, 2);
        canvas.archPiece(ArchPieceDesign.TOP_1X2, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE, ORDINARY_R + 1, 1, F);
        canvas.sign(KEYHOLE_R, F, "Keyhole opening", "Full-height shoulders");
        columns(canvas, KEYHOLE_R, 2, 2);
        canvas.archPiece(ArchPieceDesign.TOP_1X2, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE, KEYHOLE_R + 1, 1, F);
        canvas.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE, KEYHOLE_R + 1, 0, F);
        canvas.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER, Side.RIGHT, Appearance.DEFAULT, FaceTrims.NONE, KEYHOLE_R + 2, 0, F);
        canvas.sign(TALL_R, F, "Taller opening", "Three-wide top", "Spring height 3");
        columns(canvas, TALL_R, 3, 5);
        canvas.archPiece(ArchPieceDesign.TOP_2X3, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE, TALL_R + 1, 3, F);
    }
    private static void columns(ShowcaseCanvas canvas, int r, int opening, int height) {
        for (int y = 0; y < height; y++) {
            canvas.set(r, y, F, Blocks.OAK_PLANKS.defaultBlockState());
            canvas.set(r + opening + 1, y, F, Blocks.OAK_PLANKS.defaultBlockState());
        }
    }
}
