package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageMaterial;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowMuntinsSection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** One camouflage example: a closed oak round door with stone-brick corners, in the arches' row (two columns wide). */
public final class CamouflageSection implements ShowcaseSection {
    public static final Block MATERIAL = Blocks.STONE_BRICKS;
    public static final int R = WindowMuntinsSection.NEXT_R;
    public static final int F = WindowArchesSection.ROW_F;
    /** The column the next sample in this row starts in. */
    public static final int NEXT_R = R + ShowcaseSpacing.pitch(2);

    @Override
    public String id() {
        return "camouflage";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(R, F, "Camouflage", MATERIAL.getName().getString());
        if (canvas.door(Appearance.DEFAULT, R, F, DoorHingeSide.LEFT, false)
                && CamouflageMaterial.isValid(MATERIAL.defaultBlockState())
                && canvas.level().getBlockEntity(canvas.pos(R, 0, F)) instanceof RoundDoorBlockEntity entity) {
            entity.setCamouflage(CamouflageData.free(MATERIAL.defaultBlockState()));
        } else {
            canvas.problem("camouflage not applied at " + canvas.pos(R, 0, F).toShortString());
        }
    }
}
