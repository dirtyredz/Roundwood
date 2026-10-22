package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.pane.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One window a showcase section places: which block in which pose at frame coordinates ({@code r},
 * {@code y}, {@code f}), the frame's look, the face trims (0.8.0: each face its own, FRONT being the
 * pose's front for a freshly placed window), the arch sills (0.10.0, the same two faces) and the glass
 * colour. Sections build from these and the showcase checks read the same list back from the world.
 */
public record ShowcaseWindow(WindowDesign design, WindowPose pose, int r, int y, int f, Appearance pane,
                             FaceTrims trims, FaceTrims sills, Optional<DyeColor> glass) {
    public static ShowcaseWindow of(WindowDesign design, WindowPose pose, int r, int y, int f, Appearance pane) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, FaceTrims.NONE, FaceTrims.NONE, Optional.empty());
    }

    /** The same look on both faces. */
    public ShowcaseWindow trimmed(Appearance look) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, FaceTrims.both(look), sills, glass);
    }

    /** {@code look} added on one face. */
    public ShowcaseWindow trimmed(AttachmentFace face, Appearance look) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, trims.with(face, look), sills, glass);
    }

    /** An arch sill in {@code look} added on one face (upper arches only). */
    public ShowcaseWindow silled(AttachmentFace face, Appearance look) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, trims, sills.with(face, look), glass);
    }

    public ShowcaseWindow glass(DyeColor color) {
        return new ShowcaseWindow(design, pose, r, y, f, pane, trims, sills, Optional.of(color));
    }

    /** True when anything is fitted (the window's {@code trimmed} summary). */
    public boolean attached() {
        return trims.any() || sills.any();
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
        return grid(pattern, pose, frame, r, y, f, pane, new WindowProfile[][] {
                {WindowProfile.BOTTOM_LEFT, WindowProfile.BOTTOM_RIGHT}, {WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT}});
    }

    /** A 2 x 2 round window (0.10.0): lower arches under upper arches, like {@link #arched}. */
    public static List<ShowcaseWindow> circle(WindowPattern pattern, WindowPose pose, ShowcaseFrame frame, int r, int y, int f,
                                              Appearance pane) {
        return grid(pattern, pose, frame, r, y, f, pane, new WindowProfile[][] {
                {WindowProfile.ARCH_BOTTOM_LEFT, WindowProfile.ARCH_BOTTOM_RIGHT}, {WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT}});
    }

    /**
     * Pieces of {@code pattern} laid out as {@code rows} (the first row lowest, each row left to right
     * along the pose's right, each next row one step along the pose's top), the first piece at
     * ({@code r}, {@code y}, {@code f}). A null entry leaves that cell empty.
     */
    public static List<ShowcaseWindow> grid(WindowPattern pattern, WindowPose pose, ShowcaseFrame frame, int r, int y, int f,
                                            Appearance pane, WindowProfile[][] rows) {
        int[] up = step(pose.top(), frame);
        int[] right = step(pose.right(), frame);
        List<ShowcaseWindow> out = new ArrayList<>();
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length; column++) {
                if (rows[row][column] != null) {
                    out.add(of(new WindowDesign(rows[row][column], pattern), pose, r + up[0] * row + right[0] * column,
                            y + up[1] * row + right[1] * column, f + up[2] * row + right[2] * column, pane));
                }
            }
        }
        return List.copyOf(out);
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
