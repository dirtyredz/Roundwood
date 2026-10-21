package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** A lever on the floor beside a closed door: flip it and the door opens. Occupies {@code r 27..29, f 23..25}. */
public final class RedstoneSection implements ShowcaseSection {
    public static final int DOOR_R = 28;
    public static final int DOOR_F = 24;
    /** The lever sits beside the door's left column. */
    public static final int LEVER_R = DOOR_R - 1;

    @Override
    public String id() {
        return "redstone";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(DOOR_R, DOOR_F - 1, "Redstone", "flip the lever");
        canvas.door(ModBlocks.door(FinishCatalog.PAINT_BASE), DOOR_R, DOOR_F, DoorHingeSide.LEFT, false);
        canvas.set(LEVER_R, 0, DOOR_F, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, canvas.frame().forward()));
    }
}
