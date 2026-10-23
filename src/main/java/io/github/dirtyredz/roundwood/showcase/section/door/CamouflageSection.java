package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageMaterial;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** One camouflage example: a closed oak round door with stone-brick corners. Occupies {@code r 12..14, f 21..22}. */
public final class CamouflageSection implements ShowcaseSection {
    public static final Block MATERIAL = Blocks.STONE_BRICKS;
    public static final int R = 13;
    public static final int F = 22;

    @Override
    public String id() {
        return "camouflage";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(R, F - 1, "Camouflage", MATERIAL.getName().getString());
        if (canvas.door(Appearance.DEFAULT, R, F, DoorHingeSide.LEFT, false)
                && CamouflageMaterial.isValid(MATERIAL.defaultBlockState())
                && canvas.level().getBlockEntity(canvas.pos(R, 0, F)) instanceof RoundDoorBlockEntity entity) {
            entity.setCamouflage(CamouflageData.free(MATERIAL.defaultBlockState()));
        } else {
            canvas.problem("camouflage not applied at " + canvas.pos(R, 0, F).toShortString());
        }
    }
}
