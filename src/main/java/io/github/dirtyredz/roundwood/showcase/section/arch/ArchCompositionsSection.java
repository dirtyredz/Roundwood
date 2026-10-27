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

    public static final int FITTED_F = F + ShowcaseSpacing.GAP + 2;
    public static final int FITTED_LAST_F = FITTED_F + 2 * (ShowcaseSpacing.GAP + 2);
    public static final Appearance FITTED_BODY = Appearance.masonry(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    public static final FaceTrims FITTED_CASINGS = FaceTrims.NONE.with(io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.FRONT,
            Appearance.DEFAULT.painted(net.minecraft.world.item.DyeColor.GREEN)).with(io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.BACK,
            Appearance.wood(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "birch_planks")));

    @Override public String id() { return "arch_compositions"; }
    @Override public void build(ShowcaseCanvas canvas) {
        buildFitted(canvas);
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
    public void buildFitted(ShowcaseCanvas canvas) {
        var blocks = io.github.dirtyredz.roundwood.registry.ModBlocks.doors();
        for (int pose = 0; pose < 3; pose++) for (int index = 0; index < blocks.size(); index++) {
            var thickness = pose == 0 ? io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL
                    : io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.THIN;
            var depth = pose == 1 ? io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.MIDDLE
                    : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT;
            int r = ShowcaseSpacing.FIRST_R + index * ShowcaseSpacing.pitch(3), f = FITTED_F + pose * (ShowcaseSpacing.GAP + 2);
            var block = blocks.get(index);
            canvas.sign(r, f, "Fitted " + block.design().columns() + "x" + block.design().height()/16,
                    thickness.name(), depth.name(), "Leaf: " + leafDepth(pose).name());
            if (canvas.door(block, Appearance.DEFAULT.painted(net.minecraft.world.item.DyeColor.BLUE), r, f,
                    pose == 1 ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT, false, leafDepth(pose)))
                canvas.fittedArch(thickness, depth, FITTED_BODY, FITTED_CASINGS, r, f);
            if(block.design()==io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND && pose==0
                    && canvas.level().getBlockEntity(canvas.pos(r,0,f)) instanceof io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity entity)
                entity.setCamouflage(io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData.free(Blocks.STONE_BRICKS.defaultBlockState()));
        }
    }

    public static io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth leafDepth(int pose) {
        return io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values()[pose == 0 ? 0 : pose == 1 ? 2 : 1];
    }

    private static void columns(ShowcaseCanvas canvas, int r, int opening, int height) {
        for (int y = 0; y < height; y++) {
            canvas.set(r, y, F, Blocks.OAK_PLANKS.defaultBlockState());
            canvas.set(r + opening + 1, y, F, Blocks.OAK_PLANKS.defaultBlockState());
        }
    }
}
