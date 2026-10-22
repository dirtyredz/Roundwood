package io.github.dirtyredz.roundwood.showcase.section.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.pane.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.connects;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.windowLook;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.woods;

/**
 * The window sections of the showcase read back from the world: the one-per-wood window row, the
 * junction and stacking demos, the position and arch sections, and the sections listed as
 * {@code ShowcaseWindow}s (corners, cross beams, trims, circles, sills, edges). Called right after
 * building and again after the integrity ticks, for every facing the showcase test builds.
 */
public final class WindowSectionsChecks {
    private WindowSectionsChecks() {
    }

    /** One round window per wood, in a row beside the doors, connected to its neighbours. */
    public static void verifyWoodWindowRow(ServerLevel level, ShowcaseFrame frame) {
        List<Appearance> woods = woods();
        for (int i = 0; i < woods.size(); i++) {
            Appearance wood = woods.get(i);
            BlockPos windowPos = frame.pos(WindowsSection.ROW_R + i, 0, WindowsSection.ROW_F);
            BlockState state = level.getBlockState(windowPos);
            check(state.is(ModBlocks.roundWindow()), wood.key() + ": window missing from the row");
            check(wood.equals(windowLook(level, windowPos)), wood.key() + ": window shows " + windowLook(level, windowPos));
            check(state.getValue(RoundWindowBlock.AXIS) == frame.right().getAxis(), wood.key() + ": window axis");
            check(connects(state, frame.right()) == (i < woods.size() - 1), wood.key() + ": right-hand connection");
            check(connects(state, frame.right().getOpposite()) == (i > 0), wood.key() + ": left-hand connection");
        }
    }

    public static void verifyWindows(ServerLevel level, ShowcaseFrame frame) {
        Direction right = frame.right();
        RoundWindowBlock oak = ModBlocks.roundWindow();
        BlockState middle = level.getBlockState(frame.pos(WindowsSection.JUNCTION_R + 1, 0, WindowsSection.DEMO_F));
        check(middle.is(oak) && connects(middle, frame.forward()), "junction window does not connect to the glass pane");
        check(level.getBlockState(frame.pos(WindowsSection.JUNCTION_R + 1, 0, WindowsSection.DEMO_F + 1)).is(Blocks.GLASS_PANE),
                "glass pane missing from the junction");
        BlockState lower = level.getBlockState(frame.pos(WindowsSection.STACK_R, 0, WindowsSection.DEMO_F));
        BlockState upper = level.getBlockState(frame.pos(WindowsSection.STACK_R, 1, WindowsSection.DEMO_F));
        check(lower.is(oak) && upper.is(oak) && lower.getValue(RoundWindowBlock.AXIS) == right.getAxis()
                && upper.getValue(RoundWindowBlock.AXIS) != lower.getValue(RoundWindowBlock.AXIS), "stacked pair axes");
        BlockState wet = level.getBlockState(frame.pos(WindowsSection.WATER_R, 0, WindowsSection.DEMO_F));
        check(wet.is(oak) && wet.getValue(BlockStateProperties.WATERLOGGED), "waterlogged window missing");
        BlockState solid = level.getBlockState(frame.pos(WindowsSection.SOLID_R, 0, WindowsSection.DEMO_F));
        check(solid.is(oak) && level.getBlockState(frame.pos(WindowsSection.SOLID_R, 0, WindowsSection.DEMO_F + 1)).is(Blocks.STONE),
                "stone-behind window demo missing");
        check(!connects(solid, frame.forward()), "a window grew an arm towards the stone behind it");
    }

    /** The round window in each position, the junction demos and the practice blocks, in this facing's world directions. */
    public static void verifyWindowPositions(ServerLevel level, ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Direction right = frame.right();
        int f = WindowPositionsSection.ROW_F;
        expectPosition(level, frame, WindowPositionsSection.CENTRE_R, f, WindowPosition.CENTER);
        check(level.getBlockState(frame.pos(WindowPositionsSection.CENTRE_R, 0, f)).getValue(RoundWindowBlock.AXIS) == right.getAxis(),
                "centred window axis");
        expectPosition(level, frame, WindowPositionsSection.FORWARD_R, f, WindowPosition.flush(forward));
        expectPosition(level, frame, WindowPositionsSection.BACK_R, f, WindowPosition.flush(forward.getOpposite()));
        expectPosition(level, frame, WindowPositionsSection.RIGHT_R, f, WindowPosition.flush(right));
        expectPosition(level, frame, WindowPositionsSection.LEFT_R, f, WindowPosition.flush(right.getOpposite()));
        expectPosition(level, frame, WindowPositionsSection.CEILING_R, f, WindowPosition.UP);
        expectPosition(level, frame, WindowPositionsSection.CEILING_R, WindowPositionsSection.FLOOR_F, WindowPosition.DOWN);
        int d = WindowPositionsSection.DEMO_F;
        BlockState pane = expectPosition(level, frame, WindowPositionsSection.PANE_R, d, WindowPosition.flush(forward));
        check(WindowPaneBlock.connections(pane).isEmpty(), "flush window bridged to the glass pane on its right: " + pane);
        BlockState corner = expectPosition(level, frame, WindowPositionsSection.CORNER_R, d, WindowPosition.flush(forward));
        BlockState cornerSide = expectPosition(level, frame, WindowPositionsSection.CORNER_R + 1, d,
                WindowPosition.flush(right.getOpposite()));
        check(WindowPaneBlock.connections(corner).isEmpty() && WindowPaneBlock.connections(cornerSide).isEmpty(),
                "flush windows in an L kept connections: " + corner + " / " + cornerSide);
        BlockState mixed = expectPosition(level, frame, WindowPositionsSection.MIXED_R, d, WindowPosition.flush(forward));
        check(WindowPaneBlock.connections(mixed).isEmpty(), "flush window bridged to the centred one beside it");
        BlockState solid = expectPosition(level, frame, WindowPositionsSection.SOLID_R, d, WindowPosition.flush(forward));
        check(!connects(solid, right) && !connects(solid, forward), "stone made a flush window grow glass: " + solid);
        BlockState wet = expectPosition(level, frame, WindowPositionsSection.WATER_R, d, WindowPosition.flush(forward));
        check(wet.getValue(BlockStateProperties.WATERLOGGED), "flush window not waterlogged");
        int p = WindowPositionsSection.PRACTICE_F;
        check(level.getBlockState(frame.pos(WindowPositionsSection.SLAB_R, 0, p)).is(Blocks.OAK_SLAB)
                && level.getBlockState(frame.pos(WindowPositionsSection.STAIRS_R, 0, p)).is(Blocks.OAK_STAIRS)
                && level.getBlockState(frame.pos(WindowPositionsSection.WALL_R, 1, p)).is(Blocks.STONE_BRICKS), "practice blocks missing");
    }

    private static BlockState expectPosition(ServerLevel level, ShowcaseFrame frame, int r, int f, WindowPosition position) {
        BlockState state = level.getBlockState(frame.pos(r, 0, f));
        check(state.is(ModBlocks.roundWindow()) && state.getValue(RoundWindowBlock.POSITION) == position,
                "expected a " + position + " window at r " + r + " f " + f + ", found " + state);
        check(Appearance.DEFAULT.equals(windowLook(level, frame.pos(r, 0, f))), "window look at r " + r + " f " + f);
        return state;
    }

    /** Every arch pair: left on the builder's left, the pose placement would choose, the named look. */
    public static void verifyWindowArches(ServerLevel level, ShowcaseFrame frame) {
        Direction forward = frame.forward();
        int f = WindowArchesSection.ROW_F;
        expectPair(level, frame, WindowArchesSection.CENTRED_R, f, WindowArchesSection.centred(forward), Appearance.DEFAULT);
        expectPair(level, frame, WindowArchesSection.FLUSH_R, f, WindowArchesSection.flush(forward), Appearance.DEFAULT);
        expectPair(level, frame, WindowArchesSection.FLOOR_R, f, WindowArchesSection.floor(forward), Appearance.DEFAULT);
        expectPair(level, frame, WindowArchesSection.CEILING_R, f, WindowArchesSection.ceiling(forward), Appearance.DEFAULT);
        expectArch(level, frame, ModBlocks.archLeft(), WindowArchesSection.SINGLE_LEFT_R, WindowArchesSection.SINGLES_F,
                WindowArchesSection.centred(forward), Appearance.DEFAULT);
        expectArch(level, frame, ModBlocks.archRight(), WindowArchesSection.SINGLE_RIGHT_R, WindowArchesSection.SINGLES_F,
                WindowArchesSection.centred(forward), Appearance.DEFAULT);
        List<Appearance> looks = WindowArchesSection.looks();
        for (int i = 0; i < looks.size(); i++) {
            expectPair(level, frame, WindowArchesSection.LOOKS_R + WindowArchesSection.LOOKS_PITCH * i, WindowArchesSection.LOOKS_F,
                    WindowArchesSection.centred(forward), looks.get(i));
        }
        // Placement's own rule agrees with the showcase: a builder facing forward puts the left piece on their left.
        check(WindowArchesSection.centred(forward).right() == frame.right(), "centred pair's right is not the builder's right");
        check(WindowArchesSection.floor(forward).right() == frame.right(), "floor pair's right is not the builder's right");
        check(WindowArchesSection.ceiling(forward).right() == frame.right(), "ceiling pair's right is not the builder's right");
    }

    private static void expectPair(ServerLevel level, ShowcaseFrame frame, int r, int f, WindowPose pose, Appearance look) {
        expectArch(level, frame, ModBlocks.archLeft(), r, f, pose, look);
        expectArch(level, frame, ModBlocks.archRight(), r + 1, f, pose, look);
        check(!connects(level.getBlockState(frame.pos(r, 0, f)), frame.right()) || pose.depth() == WindowDepth.CENTER,
                "a flush or floor pair bridged between its own pieces at r " + r);
    }

    private static void expectArch(ServerLevel level, ShowcaseFrame frame, OrientedWindowBlock piece, int r, int f, WindowPose pose,
                                   Appearance look) {
        BlockPos pos = frame.pos(r, 0, f);
        BlockState state = level.getBlockState(pos);
        check(state.is(piece) && piece.pose(state).equals(pose), "expected " + piece + " in " + pose + " at r " + r + " f " + f + ", found " + state);
        check(look.equals(windowLook(level, pos)), piece + " at r " + r + " shows " + windowLook(level, pos));
    }

    /**
     * The sections listed as {@code ShowcaseWindow}s (0.7.0 onwards; circles, sills and edges since 0.10.0):
     * every listed window is the named block in the listed pose, with the listed pane look, its
     * {@code trimmed} summary set exactly when a trim or sill is listed, and showing exactly the listed
     * trims, sills and glass.
     */
    public static void verifyNewWindowSections(ServerLevel level, ShowcaseFrame frame) {
        verify(level, frame, "corners", WindowCornersSection.windows(frame));
        verify(level, frame, "cross beams", WindowMuntinsSection.windows(frame));
        verify(level, frame, "trims", WindowTrimsSection.windows(frame));
        verify(level, frame, "circles", WindowCirclesSection.windows(frame));
        verify(level, frame, "sills", WindowSillsSection.windows(frame));
        verify(level, frame, "edges", WindowEdgesSection.windows(frame));
    }

    private static void verify(ServerLevel level, ShowcaseFrame frame, String section, List<ShowcaseWindow> windows) {
        check(!windows.isEmpty(), section + ": no windows listed");
        for (ShowcaseWindow window : windows) {
            BlockPos pos = frame.pos(window.r(), window.y(), window.f());
            BlockState state = level.getBlockState(pos);
            String where = section + " r " + window.r() + " y " + window.y() + " f " + window.f();
            check(state.is(ModBlocks.window(window.design())), where + ": expected " + window.design().id() + ", found " + state);
            WindowPaneBlock block = (WindowPaneBlock) state.getBlock();
            check(block.pose(state).equals(window.pose()), where + ": pose " + block.pose(state) + " instead of " + window.pose());
            check(WindowPaneBlock.trimmed(state) == window.attached(), where + ": trimmed " + WindowPaneBlock.trimmed(state));
            if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity)) {
                check(false, where + ": no window entity");
                return;
            }
            check(entity.appearance().equals(window.pane()), where + ": pane shows " + entity.appearance().key());
            check(entity.trims().equals(window.trims()), where + ": trims show " + entity.trims());
            check(entity.trims(TrimSlot.SILL).equals(window.sills()), where + ": sills show " + entity.trims(TrimSlot.SILL));
            check(entity.trims(TrimSlot.CROSS_BEAMS).get(io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.FRONT).equals(window.beams()), where + ": independent beams");
            check(block.design().pattern() == io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern.PLAIN, where + ": retired block in showcase");
            check(entity.glass().equals(window.glass()), where + ": glass shows " + entity.glass());
        }
    }
}
