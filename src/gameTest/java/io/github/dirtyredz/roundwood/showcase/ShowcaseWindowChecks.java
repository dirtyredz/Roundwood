package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.section.WindowCornersSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowMuntinsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowTrimsSection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * The 0.7.0 window sections of the showcase, read back from the world: every listed window is the
 * named block in the listed pose, with the listed pane look, trimmed exactly when a trim look is
 * listed and showing it. Called right after building and again after the integrity ticks, for every
 * facing the showcase test builds.
 */
final class ShowcaseWindowChecks {
    private ShowcaseWindowChecks() {
    }

    static void verifyNewWindowSections(ServerLevel level, ShowcaseFrame frame) {
        verify(level, frame, "corners", WindowCornersSection.windows(frame));
        verify(level, frame, "cross beams", WindowMuntinsSection.windows(frame));
        verify(level, frame, "trims", WindowTrimsSection.windows(frame));
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
            check(WindowPaneBlock.trimmed(state) == window.trims().any(), where + ": trimmed " + WindowPaneBlock.trimmed(state));
            if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity)) {
                check(false, where + ": no window entity");
                return;
            }
            check(entity.appearance().equals(window.pane()), where + ": pane shows " + entity.appearance().key());
            check(entity.trims().equals(window.trims()), where + ": trims show " + entity.trims());
            check(entity.glass().equals(window.glass()), where + ": glass shows " + entity.glass());
        }
    }
}
