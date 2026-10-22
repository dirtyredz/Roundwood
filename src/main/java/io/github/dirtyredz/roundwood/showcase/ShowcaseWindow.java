package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.pane.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import net.minecraft.world.item.DyeColor;

/**
 * One window a showcase section places: which block in which pose at frame coordinates ({@code r},
 * {@code y}, {@code f}), the frame's look, the face trims (0.8.0: each face its own, FRONT being the
 * pose's front for a freshly placed window) and the glass colour. Sections build from these and the
 * showcase checks read the same list back from the world.
 */
public record ShowcaseWindow(WindowDesign design, WindowPose pose, int r, int y, int f, Appearance pane,
                             FaceTrims trims, Optional<DyeColor> glass) {
    public static ShowcaseWindow of(WindowDesign design, WindowPose pose, int r, int y, int f, Appearance pane) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, FaceTrims.NONE, Optional.empty());
    }

    /** The same look on both faces. */
    public ShowcaseWindow trimmed(Appearance look) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, FaceTrims.both(look), glass);
    }

    /** {@code look} added on one face. */
    public ShowcaseWindow trimmed(AttachmentFace face, Appearance look) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, trims.with(face, look), glass);
    }

    public ShowcaseWindow glass(DyeColor color) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, trims, Optional.of(color));
    }

    /** The block state drawing {@link #pose} (connections are left to the neighbours). */
    public BlockState state() {
        BlockState base = ModBlocks.window(design).defaultBlockState();
        if (design.profile().oriented()) {
            return OrientedWindowBlock.withPose(base, pose);
        }
        if (pose.flushSide().isPresent()) {
            return base.setValue(RoundWindowBlock.POSITION, WindowPosition.flush(pose.flushSide().get()));
        }
        return base.setValue(RoundWindowBlock.POSITION, WindowPosition.CENTER)
                .setValue(RoundWindowBlock.AXIS, pose.normal() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z);
    }

    /**
     * A 2 x 2 arched window of {@code pattern}'s pieces with its lower-left corner at ({@code r},
     * {@code y}, {@code f}): corners in the lower row, arches in the upper row, both along the pose's
     * right; for a floor or ceiling pose the upper row is one step along the pose's top instead of up.
     */
    public static List<ShowcaseWindow> arched(WindowPattern pattern, WindowPose pose, ShowcaseFrame frame, int r, int y, int f,
                                              Appearance pane) {
        int[] up = step(pose.top(), frame);
        int[] right = step(pose.right(), frame);
        return List.of(
                of(new WindowDesign(WindowProfile.BOTTOM_LEFT, pattern), pose, r, y, f, pane),
                of(new WindowDesign(WindowProfile.BOTTOM_RIGHT, pattern), pose, r + right[0], y + right[1], f + right[2], pane),
                of(new WindowDesign(WindowProfile.ARCH_LEFT, pattern), pose, r + up[0], y + up[1], f + up[2], pane),
                of(new WindowDesign(WindowProfile.ARCH_RIGHT, pattern), pose,
                        r + up[0] + right[0], y + up[1] + right[1], f + up[2] + right[2], pane));
    }

    /** A world direction as a step in frame coordinates {r, y, f}. */
    private static int[] step(Direction direction, ShowcaseFrame frame) {
        if (direction == Direction.UP) {
            return new int[] {0, 1, 0};
        }
        if (direction == Direction.DOWN) {
            return new int[] {0, -1, 0};
        }
        if (direction == frame.right()) {
            return new int[] {1, 0, 0};
        }
        if (direction == frame.right().getOpposite()) {
            return new int[] {-1, 0, 0};
        }
        return direction == frame.forward() ? new int[] {0, 0, 1} : new int[] {0, 0, -1};
    }
}
