package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowCirclesSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowCornersSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowEdgesSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowSillsSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowMuntinsSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowTrimsSection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * The window sections of the showcase listed as {@code ShowcaseWindow}s (0.7.0 onwards; circles, sills
 * and edges since 0.10.0), read back from the world: every listed window is the named block in the
 * listed pose, with the listed pane look, its {@code trimmed} summary set exactly when a trim or sill is
 * listed, and showing exactly the listed trims, sills and glass. Called right after building and again after the integrity ticks, for every
 * facing the showcase test builds.
 */
final class ShowcaseWindowChecks {
    private ShowcaseWindowChecks() {
    }

    static void verifyNewWindowSections(ServerLevel level, ShowcaseFrame frame) {
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
            check(entity.glass().equals(window.glass()), where + ": glass shows " + entity.glass());
        }
    }
}
