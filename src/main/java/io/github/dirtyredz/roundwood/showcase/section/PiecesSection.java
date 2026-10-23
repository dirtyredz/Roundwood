package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowArchesSection;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.List;
import java.util.stream.IntStream;

/**
 * One of each placeable piece, oak, default state, in a single row with a sign beside each:
 * the thirteen windows (one per profile), the round door, the arched door (both closed, hinge left)
 * and the ordinary and keyhole archways. Windows stand every second column so none connects to the
 * next. Occupies {@code r 0..42, f 0..1}; the section's rectangle is checked to hold nothing else.
 */
public final class PiecesSection implements ShowcaseSection {
    public static final int F = 1;
    public static final int R_MAX = 42;
    public static final int ROUND_DOOR_R = 29;
    public static final int ARCHED_DOOR_R = 33;
    public static final int ARCHWAY_R = 37;
    public static final int KEYHOLE_R = 41;
    private static final int WINDOW_R0 = 1;
    private static final int WINDOW_PITCH = 2;

    @Override
    public String id() {
        return "pieces";
    }

    /** Frame {@code r} of window {@code index} (profile order). */
    public static int windowR(int index) {
        return WINDOW_R0 + WINDOW_PITCH * index;
    }

    /** The thirteen windows, one per profile, centred: the round one across the builder's right, the rest front towards the builder. */
    public static List<ShowcaseWindow> windows(ShowcaseFrame frame) {
        WindowProfile[] profiles = WindowProfile.values();
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
            canvas.sign(window.r(), F - 1, "Window", window.design().profile().name().toLowerCase().replace('_', ' '));
            canvas.window(window);
        }
        canvas.sign(ROUND_DOOR_R, F - 1, "Round door");
        canvas.door(oak, ROUND_DOOR_R, F, DoorHingeSide.LEFT, false);
        canvas.sign(ARCHED_DOOR_R, F - 1, "Arched door");
        canvas.door(ModBlocks.archedDoor(), oak, ARCHED_DOOR_R, F, DoorHingeSide.LEFT, false);
        canvas.sign(ARCHWAY_R, F - 1, "Archway");
        canvas.archway(ModBlocks.archway(ArchwayDesign.ORDINARY), oak, FaceTrims.NONE, ARCHWAY_R, F);
        canvas.sign(KEYHOLE_R, F - 1, "Keyhole archway");
        canvas.archway(ModBlocks.archway(ArchwayDesign.KEYHOLE), oak, FaceTrims.NONE, KEYHOLE_R, F);
    }
}
