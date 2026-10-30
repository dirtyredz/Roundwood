package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowSurfaceGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Model naming, emission and multipart parts for every window pane. Models are authored once per
 * <em>canonical</em> pose (upright facing south, or a floor/ceiling pane with its top north) and every
 * other pose is a blockstate quarter turn of one of them, so the shapes the block computes and the
 * models drawn are the same geometry ({@link WindowPaneGeometry}).
 *
 * <p>Each model is one complete state, drawn as its visible surfaces only
 * ({@link WindowSurfaceGeometry}, 0.7.0): no glass sheet inside glass, no buried face. Every pose has
 * exactly one model: since 0.13.1 a window grows no connecting glass, so no connection selects a model
 * (the 0.5.0 {@code _arms_*} models are gone). Since 0.8.0 the models are pane-only: face trims are
 * attachments drawn by a block-entity renderer, so there are no {@code _trimmed} models, and neither
 * {@code trimmed} nor {@code face_reversed} selects a model. Names are {@code <design>[<pose>]}. Faces
 * on the cell boundary cull against their side.
 */
public final class WindowPaneModelWriter {
    /** The wood the panel and beams bind: the template the client retextures from. */
    public static final String OAK_PLANKS = "minecraft:block/oak_planks";
    static final String RENDER_TYPE = "translucent";
    /** Roundwood's own glazing, the clearer 0.7.0 artwork (the diagonal accents unchanged). */
    static final String GLASS = "block/window_glass_clear";

    private WindowPaneModelWriter() {
    }

    /** A pose as a quarter turn of its canonical pose. */
    public record Canonical(WindowPose pose, int turns) {
    }

    public static Canonical canonical(WindowPose pose) {
        Direction reference = pose.upright() ? pose.front() : pose.top();
        Direction start = pose.upright() ? Direction.SOUTH : Direction.NORTH;
        int turns = 0;
        while (start != reference) {
            start = start.getClockWise();
            turns++;
        }
        return new Canonical(pose.rotateY(-turns), turns);
    }

    /** The model-name stem of a canonical pose. */
    public static String stem(WindowDesign design, WindowPose canonical) {
        if (canonical.upright()) {
            return design.id() + switch (canonical.depth()) {
                case CENTER -> "";
                case NEGATIVE -> "_back";
                case POSITIVE -> "_front";
            };
        }
        String level = switch (canonical.depth()) {
            case NEGATIVE -> "floor";
            case CENTER -> "level";
            case POSITIVE -> "ceiling";
        };
        return design.id() + "_" + (canonical.front() == Direction.UP ? "up" : "down") + "_" + level;
    }

    /** The canonical poses a profile is drawn in. */
    public static List<WindowPose> canonicalPoses(WindowProfile profile) {
        List<WindowPose> poses = new ArrayList<>();
        if (!profile.oriented()) {
            poses.add(WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER));
            poses.add(WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE));
            poses.add(new WindowPose(Direction.DOWN, Direction.NORTH, WindowDepth.POSITIVE));
            poses.add(new WindowPose(Direction.UP, Direction.NORTH, WindowDepth.NEGATIVE));
            return poses;
        }
        for (WindowDepth depth : WindowDepth.values()) {
            poses.add(WindowPose.upright(Direction.SOUTH, depth));
        }
        for (Direction front : List.of(Direction.UP, Direction.DOWN)) {
            for (WindowDepth depth : WindowDepth.values()) {
                poses.add(new WindowPose(front, Direction.NORTH, depth));
            }
        }
        return poses;
    }

    /** The visible surfaces of one complete state (shared with the tests that check the models). */
    public static List<Surface> surfaces(WindowDesign design, WindowPose pose) {
        return WindowSurfaceGeometry.surfaces(WindowPaneGeometry.panel(design, pose), Set.of(WindowPaneGeometry.GLASS));
    }

    /** Writes the model of {@code design}'s canonical pose. */
    public static void writeModels(BlockModelProvider models, WindowDesign design, WindowPose canonical) {
        ResourceLocation glass = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, GLASS);
        BlockModelBuilder builder = models.getBuilder(stem(design, canonical)).renderType(RENDER_TYPE);
        GeometryModelWriter.addSurfaces(builder, surfaces(design, canonical), true, Surface::texture);
        builder.texture(WindowPaneGeometry.FRAME, OAK_PLANKS)
                .texture(WindowPaneGeometry.GLASS, glass)
                .texture("particle", OAK_PLANKS);
    }

    /**
     * Adds the part drawing {@code pose} to a multipart blockstate; {@code poseCondition} adds the
     * state values selecting the pose. Reads only names and geometry, so the alias blockstates can
     * reuse the canonical models written by another provider.
     */
    public static void addParts(MultiPartBlockStateBuilder states, WindowDesign design, WindowPose pose,
                                Consumer<MultiPartBlockStateBuilder.PartBuilder> poseCondition) {
        Canonical canonical = canonical(pose);
        MultiPartBlockStateBuilder.PartBuilder part = states.part()
                .modelFile(model(stem(design, canonical.pose()))).rotationY(90 * canonical.turns()).addModel();
        poseCondition.accept(part);
        part.end();
    }

    private static ModelFile model(String name) {
        return new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/" + name));
    }
}
