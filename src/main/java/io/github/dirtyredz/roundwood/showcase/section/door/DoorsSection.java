package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.List;

/**
 * Every wood sample (the eleven vanilla woods, then up to eight present modded planks), four doors
 * each: closed and open, hinge left and right (variants 0 to 3, left to right), each with a sign.
 * Two woods per band; bands run away from the builder. Occupies {@code r 0..25, f 0..38}.
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
    public static int r(int woodIndex, int variant) {
        return woodIndex % 2 * STRIP_PITCH + variant * DOOR_PITCH;
    }

    /** Frame {@code f} of the door's cells; its sign is one block nearer. */
    public static int f(int woodIndex) {
        return woodIndex / 2 * BAND_PITCH + 1;
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        List<Appearance> woods = canvas.woods();
        for (int i = 0; i < woods.size(); i++) {
            Appearance wood = woods.get(i);
            for (int variant = 0; variant < 4; variant++) {
                int r = r(i, variant);
                canvas.door(wood, r, f(i), hinge(variant), open(variant));
                canvas.sign(r, f(i) - 1, ShowcaseSamples.label(wood), open(variant) ? "open" : "closed",
                        hinge(variant) == DoorHingeSide.LEFT ? "hinge left" : "hinge right");
            }
        }
    }
}
