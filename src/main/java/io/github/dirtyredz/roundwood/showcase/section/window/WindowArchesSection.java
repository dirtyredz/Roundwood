package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.List;

/**
 * The arch pieces: pairs forming the upper half of a two-block round window (centred upright, flush
 * against the far side of the cell, on the floor, on the ceiling), each piece alone, and pairs in
 * other woods and paints. Every pair is a left piece on the builder's left and a right piece beside
 * it, front towards the builder (floor: top away from them; ceiling: top towards them, as placement
 * would choose). Occupies {@code r 63..73, f 0..17}.
 */
public final class WindowArchesSection implements ShowcaseSection {
    public static final int ROW_F = 2;
    public static final int CENTRED_R = 63;
    public static final int FLUSH_R = 66;
    public static final int FLOOR_R = 69;
    public static final int CEILING_R = 72;
    public static final int SINGLES_F = 6;
    public static final int SINGLE_LEFT_R = 63;
    public static final int SINGLE_RIGHT_R = 66;
    public static final int LOOKS_F = 10;
    /** Left piece columns of the look pairs (three pairs, three apart). */
    public static final int LOOKS_R = 63;
    public static final int LOOKS_PITCH = 3;

    @Override
    public String id() {
        return "window arches";
    }

    /** The looks of the three sample pairs: spruce, red paint, blue paint. */
    public static List<Appearance> looks() {
        return List.of(Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks")),
                Appearance.DEFAULT.painted(DyeColor.RED), Appearance.DEFAULT.painted(DyeColor.BLUE));
    }

    /** The pose of the pair at a row: front towards the builder. */
    public static WindowPose centred(Direction forward) {
        return WindowPose.upright(forward.getOpposite(), WindowDepth.CENTER);
    }

    public static WindowPose flush(Direction forward) {
        return WindowPose.upright(forward.getOpposite(), WindowDepth.flushAgainst(forward));
    }

    public static WindowPose floor(Direction forward) {
        return new WindowPose(Direction.UP, forward, WindowDepth.NEGATIVE);
    }

    public static WindowPose ceiling(Direction forward) {
        return new WindowPose(Direction.DOWN, forward.getOpposite(), WindowDepth.POSITIVE);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Direction forward = canvas.frame().forward();
        Appearance oak = Appearance.DEFAULT;
        pair(canvas, oak, CENTRED_R, ROW_F, centred(forward), "Arch pair", "centred");
        pair(canvas, oak, FLUSH_R, ROW_F, flush(forward), "Arch pair", "flush far");
        pair(canvas, oak, FLOOR_R, ROW_F, floor(forward), "Arch pair", "on the floor");
        pair(canvas, oak, CEILING_R, ROW_F, ceiling(forward), "Arch pair", "on the ceiling");
        canvas.sign(SINGLE_LEFT_R, SINGLES_F - 1, "Left piece", "alone");
        canvas.arch(ModBlocks.archLeft(), oak, SINGLE_LEFT_R, 0, SINGLES_F, centred(forward));
        canvas.sign(SINGLE_RIGHT_R, SINGLES_F - 1, "Right piece", "alone");
        canvas.arch(ModBlocks.archRight(), oak, SINGLE_RIGHT_R, 0, SINGLES_F, centred(forward));
        List<Appearance> looks = looks();
        for (int i = 0; i < looks.size(); i++) {
            pair(canvas, looks.get(i), LOOKS_R + LOOKS_PITCH * i, LOOKS_F, centred(forward),
                    ShowcaseCanvas.label(looks.get(i)), "arch pair");
        }
    }

    private static void pair(ShowcaseCanvas canvas, Appearance look, int r, int f, WindowPose pose, String... label) {
        canvas.sign(r, f - 1, label);
        canvas.arch(ModBlocks.archLeft(), look, r, 0, f, pose);
        canvas.arch(ModBlocks.archRight(), look, r + 1, 0, f, pose);
    }
}
