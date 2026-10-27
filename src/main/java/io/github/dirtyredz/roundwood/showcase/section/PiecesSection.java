package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowArchesSection;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.List;
import java.util.stream.IntStream;

/**
 * One of each placeable piece, oak, default state, in a single row with a sign beside each:
 * the thirteen framed windows (the frameless pane is demonstrated in WindowEdgesSection), the round door, the arched door (both closed, hinge left)
 * with modular arches in their own section. Each stands {@link ShowcaseSpacing#GAP} clear blocks from the
 * next, its sign two columns to its left in the same row. Occupies {@code r 0..R_MAX, f 1}; the
 * section's rectangle is checked to hold nothing else.
 */
public final class PiecesSection implements ShowcaseSection {
    public static final int F = ShowcaseSpacing.ROW_PIECES;
    private static final int WINDOW_PITCH = ShowcaseSpacing.pitch(1);
    public static final int ROUND_DOOR_R = windowR(profiles().length);
    public static final int ARCHED_DOOR_R = ROUND_DOOR_R + ShowcaseSpacing.pitch(2);
    public static final int DOUBLE_DOOR_R = ARCHED_DOOR_R + ShowcaseSpacing.pitch(1);
    public static final int TALL_DOUBLE_DOOR_R = DOUBLE_DOOR_R + ShowcaseSpacing.pitch(3);
    public static final int R_MAX = TALL_DOUBLE_DOOR_R + 2;

    @Override
    public String id() {
        return "pieces";
    }

    private static WindowProfile[] profiles(){return java.util.Arrays.stream(WindowProfile.values()).filter(WindowProfile::takesTrim).toArray(WindowProfile[]::new);}

    /** Frame {@code r} of window {@code index} (profile order). */
    public static int windowR(int index) {
        return ShowcaseSpacing.FIRST_R + WINDOW_PITCH * index;
    }

    /** The framed windows, one per profile, centred: the round one across the builder's right, the rest front towards the builder. */
    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        WindowProfile[] profiles = profiles();
        WindowPose oriented = WindowArchesSection.centred(frame.forward());
        WindowPose round = WindowPosition.CENTER.pose(frame.right().getAxis());
        return IntStream.range(0, profiles.length)
                .mapToObj(i -> ShowcaseWindow.of(WindowDesign.plain(profiles[i]), profiles[i].oriented() ? oriented : round,
                        windowR(i), 0, F, Appearance.DEFAULT))
                .toList();
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        Appearance oak = Appearance.DEFAULT;
        for (ShowcaseWindow window : windows(canvas.frame())) {
            canvas.sign(window.r(), F, "Window", window.design().profile().name().toLowerCase().replace('_', ' '));
            canvas.window(window);
        }
        canvas.sign(ROUND_DOOR_R, F, "Round door");
        canvas.door(oak, ROUND_DOOR_R, F, DoorHingeSide.LEFT, false);
        canvas.sign(TALL_DOUBLE_DOOR_R, F, "Tall double door");
        canvas.door(ModBlocks.tallDoubleArchedDoor(), oak, TALL_DOUBLE_DOOR_R, F, DoorHingeSide.LEFT, false);
        canvas.sign(DOUBLE_DOOR_R, F, "Double arched door");
        canvas.door(ModBlocks.doubleArchedDoor(), oak, DOUBLE_DOOR_R, F, DoorHingeSide.LEFT, false);
        canvas.sign(ARCHED_DOOR_R, F, "Arched door");
        canvas.door(ModBlocks.archedDoor(), oak, ARCHED_DOOR_R, F, DoorHingeSide.LEFT, false);
    }
}
