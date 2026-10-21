package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageMaterial;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.List;

/**
 * Oak doors with corner camouflage: one door per material, closed (front row) and open (back row).
 * Occupies {@code r 28..39, f 0..6}.
 */
public final class CamouflageSection implements ShowcaseSection {
    public static final List<Block> MATERIALS = List.of(Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.GRASS_BLOCK, Blocks.WHITE_WOOL);
    private static final int R0 = 28;
    private static final int PITCH = 3;
    private static final int[] ROW_F = {1, 5};

    @Override
    public String id() {
        return "camouflage";
    }

    /** Frame {@code r} of the left column of the door with material {@code index}. */
    public static int r(int index) {
        return R0 + index * PITCH;
    }

    /** Frame {@code f} of the cells of the closed (0) or open (1) row. */
    public static int f(int row) {
        return ROW_F[row];
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < MATERIALS.size(); i++) {
                Block material = MATERIALS.get(i);
                int r = r(i);
                canvas.sign(r, f(row) - 1, "Camouflage", material.getName().getString(), row == 0 ? "closed" : "open");
                if (canvas.door(ModBlocks.door(FinishCatalog.PAINT_BASE), r, f(row), DoorHingeSide.LEFT, row == 1)
                        && CamouflageMaterial.isValid(material.defaultBlockState())
                        && canvas.level().getBlockEntity(canvas.pos(r, 0, f(row))) instanceof RoundDoorBlockEntity entity) {
                    entity.setCamouflage(CamouflageData.free(material.defaultBlockState()));
                } else {
                    canvas.problem("camouflage not applied at " + canvas.pos(r, 0, f(row)).toShortString());
                }
            }
        }
    }
}
