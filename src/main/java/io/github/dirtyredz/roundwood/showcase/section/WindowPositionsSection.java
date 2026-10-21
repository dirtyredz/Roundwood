package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Where a round window can sit since 0.6.0: centred and flush against each of the six sides of its
 * cell, junctions where flush panes meet vanilla panes and other windows (the glass bridges back to
 * the cell's middle), a flush window beside stone (no bridge) and a waterlogged one, plus blocks to
 * practise targeting on (a full block, a slab, stairs, a two-high wall). Occupies
 * {@code r 50..61, f 0..17}. Directions in the constant names are the builder's: forward is away
 * from them, back towards them.
 */
public final class WindowPositionsSection implements ShowcaseSection {
    /** Row of the seven positions; their signs stand one nearer. */
    public static final int ROW_F = 2;
    public static final int CENTRE_R = 50;
    public static final int FORWARD_R = 52;
    public static final int BACK_R = 54;
    public static final int RIGHT_R = 56;
    public static final int LEFT_R = 58;
    public static final int CEILING_R = 60;
    /** The floor window sits in the ceiling window's column, two nearer the far end (never touching it). */
    public static final int FLOOR_F = 4;
    /** Junction and neighbour demos. */
    public static final int DEMO_F = 6;
    public static final int PANE_R = 50;
    public static final int FIN_R = 53;
    public static final int MIXED_R = 56;
    public static final int SOLID_R = 59;
    public static final int WATER_R = 61;
    /** Targeting practice. */
    public static final int PRACTICE_F = 12;
    public static final int STONE_R = 50;
    public static final int SLAB_R = 53;
    public static final int STAIRS_R = 56;
    public static final int WALL_R = 59;

    @Override
    public String id() {
        return "window positions";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Appearance oak = Appearance.DEFAULT;
        Direction forward = canvas.frame().forward();
        Direction right = canvas.frame().right();
        canvas.sign(CENTRE_R, ROW_F - 1, "Centred", "aim at the", "middle zone");
        canvas.window(oak, CENTRE_R, 0, ROW_F, canvas.acrossAxis(), false);
        canvas.sign(FORWARD_R, ROW_F - 1, "Flush far", "aim at the", "far edge");
        canvas.flushWindow(oak, FORWARD_R, 0, ROW_F, forward, false);
        canvas.sign(BACK_R, ROW_F - 1, "Flush near", "aim at the", "near edge");
        canvas.flushWindow(oak, BACK_R, 0, ROW_F, forward.getOpposite(), false);
        canvas.sign(RIGHT_R, ROW_F - 1, "Flush right");
        canvas.flushWindow(oak, RIGHT_R, 0, ROW_F, right, false);
        canvas.sign(LEFT_R, ROW_F - 1, "Flush left");
        canvas.flushWindow(oak, LEFT_R, 0, ROW_F, right.getOpposite(), false);
        canvas.sign(CEILING_R, ROW_F - 1, "Ceiling", "aim at a", "wall's top zone");
        canvas.flushWindow(oak, CEILING_R, 0, ROW_F, Direction.UP, false);
        canvas.sign(CEILING_R, FLOOR_F - 1, "Floor", "aim at a", "floor's edge");
        canvas.flushWindow(oak, CEILING_R, 0, FLOOR_F, Direction.DOWN, false);

        // A far-flush window joined to a glass pane on its right: stem and arm back to the middle.
        canvas.sign(PANE_R, DEMO_F - 1, "Glass bridge", "to a pane");
        canvas.flushWindow(oak, PANE_R, 0, DEMO_F, forward, false);
        canvas.set(PANE_R + 1, 0, DEMO_F, Blocks.GLASS_PANE.defaultBlockState());
        // A ceiling window with a pane beyond it: a glass fin hangs down to the pane.
        canvas.sign(FIN_R, DEMO_F - 1, "Ceiling fin", "to a pane");
        canvas.set(FIN_R, 0, DEMO_F + 1, Blocks.GLASS_PANE.defaultBlockState());
        canvas.flushWindow(oak, FIN_R, 0, DEMO_F, Direction.UP, false);
        // A far-flush window beside a centred one: different depths meet through the bridge.
        canvas.sign(MIXED_R, DEMO_F - 1, "Flush meets", "centred");
        canvas.window(oak, MIXED_R + 1, 0, DEMO_F, canvas.acrossAxis(), false);
        canvas.flushWindow(oak, MIXED_R, 0, DEMO_F, forward, false);
        // Stone beside and behind a flush window: solid blocks never make glass.
        canvas.sign(SOLID_R, DEMO_F - 1, "Stone beside", "no bridge");
        canvas.set(SOLID_R + 1, 0, DEMO_F, Blocks.STONE.defaultBlockState());
        canvas.set(SOLID_R, 0, DEMO_F + 1, Blocks.STONE.defaultBlockState());
        canvas.flushWindow(oak, SOLID_R, 0, DEMO_F, forward, false);
        canvas.sign(WATER_R, DEMO_F - 1, "Waterlogged", "flush");
        canvas.flushWindow(oak, WATER_R, 0, DEMO_F, forward, true);

        canvas.sign(STONE_R, PRACTICE_F - 1, "Practice:", "hold a window", "aim at faces");
        canvas.set(STONE_R, 0, PRACTICE_F, Blocks.STONE.defaultBlockState());
        canvas.sign(SLAB_R, PRACTICE_F - 1, "Slab");
        canvas.set(SLAB_R, 0, PRACTICE_F, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        canvas.sign(STAIRS_R, PRACTICE_F - 1, "Stairs");
        canvas.set(STAIRS_R, 0, PRACTICE_F, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, forward));
        canvas.sign(WALL_R, PRACTICE_F - 1, "Wall", "two high");
        canvas.set(WALL_R, 0, PRACTICE_F, Blocks.STONE_BRICKS.defaultBlockState());
        canvas.set(WALL_R, 1, PRACTICE_F, Blocks.STONE_BRICKS.defaultBlockState());
        canvas.set(WALL_R + 1, 0, PRACTICE_F, Blocks.STONE_BRICKS.defaultBlockState());
        canvas.set(WALL_R + 1, 1, PRACTICE_F, Blocks.STONE_BRICKS.defaultBlockState());
    }
}
