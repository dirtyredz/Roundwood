package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Which part of a trimmed window a click lands on: the trim band (the face pixels under the trim,
 * {@link WindowTrimGeometry#inBand}) or the pane. The hit point is read in the pane's own face
 * coordinates, so the rule is the same on both faces and in every pose. Also adapts a window's trim
 * to {@link AppearanceActions.Target} for restyling.
 */
public final class WindowTrimTarget {
    private static final double NUDGE = 1e-4;

    private WindowTrimTarget() {
    }

    /** True when {@code hit} (world coordinates) on the window at {@code pos} lands on its fitted trim. */
    public static boolean hitsBand(BlockState state, BlockPos pos, Vec3 hit) {
        if (!(state.getBlock() instanceof WindowPaneBlock block) || !WindowPaneBlock.trimmed(state)) {
            return false;
        }
        WindowPose pose = block.pose(state);
        double[] face = pose.facePoint(local(hit.x - pos.getX()), local(hit.y - pos.getY()), local(hit.z - pos.getZ()));
        int u = (int) Math.floor(face[0]);
        int v = (int) Math.floor(face[1]);
        return u >= 0 && u < WindowProfile.SIZE && v >= 0 && v < WindowProfile.SIZE
                && WindowTrimGeometry.inBand(block.profile(), u, v);
    }

    /** The window's trim as an appearance target (empty while it has none). */
    public static AppearanceActions.Target of(RoundWindowBlockEntity window) {
        return new AppearanceActions.Target() {
            @Override
            public Optional<Appearance> current() {
                return window.trim();
            }

            @Override
            public boolean mayEdit(Player player) {
                return window.mayEdit(player);
            }

            @Override
            public void set(Appearance appearance) {
                window.setTrim(appearance);
            }

            @Override
            public BlockPos origin() {
                return window.getBlockPos();
            }
        };
    }

    /** Block-local pixels, kept just inside the cell so a hit on its boundary stays in it. */
    private static double local(double blocks) {
        return Math.min(WindowProfile.SIZE - NUDGE, Math.max(NUDGE, blocks * WindowProfile.SIZE));
    }
}
