package io.github.dirtyredz.roundwood.showcase.section.arch;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/** Three tops and a reflected shoulder pair; one masonry contour and independent casing looks. */
public final class ArchPiecesSection implements ShowcaseSection {
    public static final int F = ShowcaseSpacing.ROW_ARCH_PIECES;
    public static final int SMALL_R = ShowcaseSpacing.FIRST_R;
    public static final int MEDIUM_R = SMALL_R + ShowcaseSpacing.pitch(1);
    public static final int LARGE_R = MEDIUM_R + ShowcaseSpacing.pitch(2);
    public static final int SHOULDERS_R = LARGE_R + ShowcaseSpacing.pitch(3);
    public static final int THIN_R = SHOULDERS_R + ShowcaseSpacing.pitch(2);
    public static final int R_MAX = THIN_R + 3 * ShowcaseSpacing.pitch(3) + 1;
    public static final Appearance BRICK = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    public static final Appearance CHERRY = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "cherry_planks"));
    public static final FaceTrims CASINGS = FaceTrims.NONE.with(AttachmentFace.FRONT, CHERRY)
            .with(AttachmentFace.BACK, Appearance.DEFAULT.painted(DyeColor.BLUE));

    @Override public String id() { return "arch_pieces"; }
    @Override public void build(ShowcaseCanvas canvas) {
        for (var design : ArchPieceDesign.values()) {
            int r = THIN_R + design.ordinal() * ShowcaseSpacing.pitch(3);
            canvas.sign(r, F, "Thin " + design.id().replace("arch_", ""), "Top: BACK", "Mid: MIDDLE", "Low: FRONT");
            for (var depth : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.values()) {
                canvas.archPiece(design, Side.LEFT, io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.THIN,
                        depth, Appearance.DEFAULT, FaceTrims.NONE, r, 2 * depth.ordinal(), F);
                if (design.shoulder()) canvas.archPiece(design, Side.RIGHT, io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.THIN,
                        depth, Appearance.DEFAULT, FaceTrims.NONE, r + 1, 2 * depth.ordinal(), F);
            }
        }
        canvas.sign(SMALL_R, F, "Arch top 1x1", "Upper door corners");
        canvas.archPiece(ArchPieceDesign.TOP_1X1, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE, SMALL_R, 0, F);
        canvas.sign(MEDIUM_R, F, "Arch top 1x2", "Cherry front", "Blue back");
        canvas.archPiece(ArchPieceDesign.TOP_1X2, Side.LEFT, Appearance.DEFAULT, CASINGS, MEDIUM_R, 0, F);
        canvas.sign(LARGE_R, F, "Arch top 2x3", "Masonry contour");
        canvas.archPiece(ArchPieceDesign.TOP_2X3, Side.LEFT, BRICK, FaceTrims.both(BRICK), LARGE_R, 0, F);
        canvas.sign(SHOULDERS_R, F, "Shoulder pair", "Full-height bases");
        canvas.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE, SHOULDERS_R, 0, F);
        canvas.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER, Side.RIGHT, Appearance.DEFAULT, FaceTrims.NONE, SHOULDERS_R + 1, 0, F);
    }
}
