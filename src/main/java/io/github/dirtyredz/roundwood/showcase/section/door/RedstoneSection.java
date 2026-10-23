package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowArchesSection;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * A lever on the floor beside a closed door: flip it and the door opens. One sample three columns wide
 * (the lever, then the two-wide door) at the end of the arches' row, its sign two columns left of the lever.
 */
public final class RedstoneSection implements ShowcaseSection {
    /** The lever is the sample's left edge, in the column after the camouflage sample's gap. */
    public static final int LEVER_R = CamouflageSection.NEXT_R;
    /** The door stands beside the lever, whose column is the door's left. */
    public static final int DOOR_R = LEVER_R + 1;
    public static final int DOOR_F = WindowArchesSection.ROW_F;
    /** The last column of the sample: the door is two wide. */
    public static final int R_MAX = DOOR_R + 1;

    @Override
    public String id() {
        return "redstone";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        canvas.sign(LEVER_R, DOOR_F, "Redstone", "flip the lever");
        canvas.door(Appearance.DEFAULT, DOOR_R, DOOR_F, DoorHingeSide.LEFT, false);
        canvas.set(LEVER_R, 0, DOOR_F, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, canvas.frame().forward()));
    }
}
