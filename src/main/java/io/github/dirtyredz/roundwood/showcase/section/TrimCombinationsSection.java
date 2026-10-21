package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.List;

/**
 * Every wood and masonry trim fitted around three representative doors: blue-painted (closed, hinge
 * left), oak with stone-brick camouflage (closed, hinge right) and red-painted (open, hinge
 * alternating by material). Five materials per band, bands running away from the builder; each door
 * has its frame one block nearer the builder and a sign one further. Painted trims are in
 * {@link PaintsSection}. Occupies {@code r 0..57, f 42..73} (an open leaf projects to {@code f + 1}).
 */
public final class TrimCombinationsSection implements ShowcaseSection {
    public static final int SAMPLES = 3;
    private static final int PER_BAND = 5;
    private static final int GROUP_PITCH = 12;
    private static final int SAMPLE_PITCH = 4;
    private static final int F0 = 44;
    private static final int BAND_PITCH = 7;

    @Override
    public String id() {
        return "trim combinations";
    }

    /** The trim materials shown, in order: every wood sample, then the masonry. */
    public static List<Appearance> materials(ShowcaseCanvas canvas) {
        List<Appearance> out = new ArrayList<>(canvas.woods());
        out.addAll(canvas.masonry());
        return out;
    }

    /** Frame {@code r} of the door's (and its frame's) left column. */
    public static int r(int material, int sample) {
        return GROUP_PITCH * (material % PER_BAND) + SAMPLE_PITCH * sample;
    }

    /** Frame {@code f} of the door's cells; the frame is at {@code f - 1}, the sign at {@code f - 2}. */
    public static int f(int material) {
        return F0 + BAND_PITCH * (material / PER_BAND);
    }

    /** The door each sample shows. */
    public static Appearance door(int sample) {
        return switch (sample) {
            case 0 -> Appearance.DEFAULT.painted(DyeColor.BLUE);
            case 1 -> Appearance.DEFAULT;
            default -> Appearance.DEFAULT.painted(DyeColor.RED);
        };
    }

    public static boolean open(int sample) {
        return sample == 2;
    }

    public static DoorHingeSide hinge(int material, int sample) {
        return switch (sample) {
            case 0 -> DoorHingeSide.LEFT;
            case 1 -> DoorHingeSide.RIGHT;
            default -> material % 2 == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
        };
    }

    /** The oak sample wears stone-brick camouflage. */
    public static boolean camouflaged(int sample) {
        return sample == 1;
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        List<Appearance> materials = materials(canvas);
        for (int i = 0; i < materials.size(); i++) {
            Appearance material = materials.get(i);
            for (int j = 0; j < SAMPLES; j++) {
                int r = r(i, j);
                int f = f(i);
                boolean placed = canvas.door(door(j), r, f, hinge(i, j), open(j));
                if (placed && camouflaged(j)) {
                    if (canvas.level().getBlockEntity(canvas.pos(r, 0, f)) instanceof RoundDoorBlockEntity entity) {
                        entity.setCamouflage(CamouflageData.free(Blocks.STONE_BRICKS.defaultBlockState()));
                    } else {
                        canvas.problem("camouflage not applied at " + canvas.pos(r, 0, f).toShortString());
                    }
                }
                canvas.trim(material, r, f - 1);
                canvas.sign(r, f - 2, ShowcaseCanvas.label(material) + " trim", ShowcaseCanvas.label(door(j)) + " door",
                        open(j) ? "open" : "closed", camouflaged(j) ? "camouflaged" : "");
            }
        }
    }
}
