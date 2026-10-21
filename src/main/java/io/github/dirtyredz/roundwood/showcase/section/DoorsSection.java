package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/**
 * Every door finish, four doors each: closed and open, hinge left and right (variants 0 to 3, left
 * to right), each with a sign. Two finishes per band; bands run away from the builder. Occupies
 * {@code r 0..25, f 0..31}.
 */
public final class DoorsSection implements ShowcaseSection {
    private static final int STRIP_PITCH = 13;
    private static final int DOOR_PITCH = 3;
    private static final int BAND_PITCH = 4;

    @Override
    public String id() {
        return "doors";
    }

    /** Variants: 0 closed/left, 1 closed/right, 2 open/left, 3 open/right. */
    public static DoorHingeSide hinge(int variant) {
        return variant % 2 == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
    }

    public static boolean open(int variant) {
        return variant >= 2;
    }

    /** Frame {@code r} of the door's left column. */
    public static int r(int finishIndex, int variant) {
        return finishIndex % 2 * STRIP_PITCH + variant * DOOR_PITCH;
    }

    /** Frame {@code f} of the door's cells; its sign is one block nearer. */
    public static int f(int finishIndex) {
        return finishIndex / 2 * BAND_PITCH + 1;
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        for (int i = 0; i < FinishCatalog.ALL.size(); i++) {
            Finish finish = FinishCatalog.ALL.get(i);
            for (int variant = 0; variant < 4; variant++) {
                int r = r(i, variant);
                canvas.door(ModBlocks.door(finish), r, f(i), hinge(variant), open(variant));
                canvas.sign(r, f(i) - 1, finish.displayName(), open(variant) ? "open" : "closed",
                        hinge(variant) == DoorHingeSide.LEFT ? "hinge left" : "hinge right");
            }
        }
    }
}
