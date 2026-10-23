package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Where a round window can sit since 0.6.0: centred and flush against each of the six sides of its
 * cell, flush windows meeting vanilla panes, each other in a corner and a centred window (never any
 * glass bridge: frames simply abut, and since 0.13.1 no window grows a connecting pane at all),
 * beside stone and waterlogged, plus blocks to practise targeting on (a full block, a slab, stairs, a
 * two-high wall). One row of sixteen samples, each {@link ShowcaseSpacing#GAP} clear blocks from the
 * next with its sign two columns to its left: occupies {@code r 0..R_MAX, f 12..13}. Directions in the
 * constant names are the builder's: forward is away from them, back towards them.
 */
public final class WindowPositionsSection implements ShowcaseSection {
    /** The row of every sample here; the stone behind the {@link #SOLID_R} demo stands one row further. */
    public static final int ROW_F = ShowcaseSpacing.ROW_POSITIONS;
    private static final int ONE = ShowcaseSpacing.pitch(1);
    private static final int TWO = ShowcaseSpacing.pitch(2);
    /** The seven positions of one window. */
    public static final int CENTRE_R = ShowcaseSpacing.FIRST_R;
    public static final int FORWARD_R = CENTRE_R + ONE;
    public static final int BACK_R = FORWARD_R + ONE;
    public static final int RIGHT_R = BACK_R + ONE;
    public static final int LEFT_R = RIGHT_R + ONE;
    public static final int CEILING_R = LEFT_R + ONE;
    public static final int FLOOR_R = CEILING_R + ONE;
    /** Neighbour demos, two columns wide but the last. */
    public static final int PANE_R = FLOOR_R + ONE;
    public static final int CORNER_R = PANE_R + TWO;
    public static final int MIXED_R = CORNER_R + TWO;
    public static final int SOLID_R = MIXED_R + TWO;
    public static final int WATER_R = SOLID_R + TWO;
    /** Targeting practice. */
    public static final int STONE_R = WATER_R + ONE;
    public static final int SLAB_R = STONE_R + ONE;
    public static final int STAIRS_R = SLAB_R + ONE;
    public static final int WALL_R = STAIRS_R + ONE;
    /** The last column of the section: the practice wall is two wide. */
    public static final int R_MAX = WALL_R + 1;

    @Override
    public String id() {
        return "window positions";
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Appearance oak = Appearance.DEFAULT;
        Direction forward = canvas.frame().forward();
        Direction right = canvas.frame().right();
        int f = ROW_F;
        canvas.sign(CENTRE_R, f, "Centred", "aim at the", "middle zone");
        canvas.window(oak, CENTRE_R, 0, f, canvas.acrossAxis(), false);
        canvas.sign(FORWARD_R, f, "Flush far", "aim at the", "far edge");
        canvas.flushWindow(oak, FORWARD_R, 0, f, forward, false);
        canvas.sign(BACK_R, f, "Flush near", "aim at the", "near edge");
        canvas.flushWindow(oak, BACK_R, 0, f, forward.getOpposite(), false);
        canvas.sign(RIGHT_R, f, "Flush right");
        canvas.flushWindow(oak, RIGHT_R, 0, f, right, false);
        canvas.sign(LEFT_R, f, "Flush left");
        canvas.flushWindow(oak, LEFT_R, 0, f, right.getOpposite(), false);
        canvas.sign(CEILING_R, f, "Ceiling", "aim at a", "wall's top zone");
        canvas.flushWindow(oak, CEILING_R, 0, f, Direction.UP, false);
        canvas.sign(FLOOR_R, f, "Floor", "aim at a", "floor's edge");
        canvas.flushWindow(oak, FLOOR_R, 0, f, Direction.DOWN, false);

        // A far-flush window beside a glass pane: no glass grows back to the middle.
        canvas.sign(PANE_R, f, "Flush by", "a pane");
        canvas.flushWindow(oak, PANE_R, 0, f, forward, false);
        canvas.set(PANE_R + 1, 0, f, Blocks.GLASS_PANE.defaultBlockState());
        // Two flush windows meeting in an L: the frames abut at the corner, nothing else.
        canvas.sign(CORNER_R, f, "Flush corner", "frames meet");
        canvas.flushWindow(oak, CORNER_R, 0, f, forward, false);
        canvas.flushWindow(oak, CORNER_R + 1, 0, f, right.getOpposite(), false);
        // A far-flush window beside a centred one: different depths, no bridge between them.
        canvas.sign(MIXED_R, f, "Flush meets", "centred");
        canvas.window(oak, MIXED_R + 1, 0, f, canvas.acrossAxis(), false);
        canvas.flushWindow(oak, MIXED_R, 0, f, forward, false);
        // Stone beside and behind a flush window: solid blocks never make glass.
        canvas.sign(SOLID_R, f, "Stone beside", "no bridge");
        canvas.set(SOLID_R + 1, 0, f, Blocks.STONE.defaultBlockState());
        canvas.set(SOLID_R, 0, f + 1, Blocks.STONE.defaultBlockState());
        canvas.flushWindow(oak, SOLID_R, 0, f, forward, false);
        canvas.sign(WATER_R, f, "Waterlogged", "flush");
        canvas.flushWindow(oak, WATER_R, 0, f, forward, true);

        canvas.sign(STONE_R, f, "Practice:", "hold a window", "aim at faces");
        canvas.set(STONE_R, 0, f, Blocks.STONE.defaultBlockState());
        canvas.sign(SLAB_R, f, "Slab");
        canvas.set(SLAB_R, 0, f, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        canvas.sign(STAIRS_R, f, "Stairs");
        canvas.set(STAIRS_R, 0, f, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, forward));
        canvas.sign(WALL_R, f, "Wall", "two high");
        canvas.set(WALL_R, 0, f, Blocks.STONE_BRICKS.defaultBlockState());
        canvas.set(WALL_R, 1, f, Blocks.STONE_BRICKS.defaultBlockState());
        canvas.set(WALL_R + 1, 0, f, Blocks.STONE_BRICKS.defaultBlockState());
        canvas.set(WALL_R + 1, 1, f, Blocks.STONE_BRICKS.defaultBlockState());
    }
}
