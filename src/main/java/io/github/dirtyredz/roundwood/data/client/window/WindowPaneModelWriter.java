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
import net.minecraft.world.level.block.PipeBlock;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Model naming, emission and multipart parts for every window pane. Models are authored once per
 * <em>canonical</em> pose (upright facing south, or a floor/ceiling pane with its top north) and every
 * other pose is a blockstate quarter turn of one of them, so the shapes the block computes and the
 * models drawn are the same geometry ({@link WindowPaneGeometry}).
 *
 * <p>Each model is one complete state, drawn as its visible surfaces only
 * ({@link WindowSurfaceGeometry}, 0.7.0): no glass sheet inside glass, no buried face. So a centred
 * upright pose, whose arms reach on to the panel's glass, has one model per combination of its two
 * across-plane connections rather than a core plus overlapping arm parts; every other pose has one.
 * Since 0.8.0 the models are pane-only: face trims are attachments drawn by a block-entity renderer,
 * so there are no {@code _trimmed} models, and neither {@code trimmed} nor {@code face_reversed}
 * selects a model. Names are {@code <design>[<pose>][_arms_<north|south|both>]}, sides named in the
 * canonical frame. Faces on the cell boundary cull against their side.
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

    /** The full model name of a canonical state: pose and the canonical sides its arms reach. */
    public static String name(WindowDesign design, WindowPose canonical, Set<Direction> arms) {
        String out = stem(design, canonical);
        if (arms.isEmpty()) {
            return out;
        }
        return out + "_arms_" + (arms.size() == 2 ? "both" : side(arms.iterator().next()));
    }

    public static String side(Direction side) {
        return side.getSerializedName().toLowerCase(Locale.ROOT);
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

    /** Every subset of the sides a pose may grow arms on (just the empty set for a positioned pose). */
    public static List<Set<Direction>> armSets(WindowPose pose) {
        List<Direction> sides = WindowPaneGeometry.neededSides(pose);
        List<Set<Direction>> out = new ArrayList<>();
        for (int mask = 0; mask < 1 << sides.size(); mask++) {
            Set<Direction> set = EnumSet.noneOf(Direction.class);
            for (int i = 0; i < sides.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    set.add(sides.get(i));
                }
            }
            out.add(set);
        }
        return out;
    }

    /** The visible surfaces of one complete state (shared with the tests that check the models). */
    public static List<Surface> surfaces(WindowDesign design, WindowPose pose, Set<Direction> arms) {
        return WindowSurfaceGeometry.surfaces(WindowPaneGeometry.pieces(design, pose, arms), Set.of(WindowPaneGeometry.GLASS));
    }

    /** Writes every model of {@code design}'s canonical pose, one per arm combination. */
    public static void writeModels(BlockModelProvider models, WindowDesign design, WindowPose canonical) {
        ResourceLocation glass = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, GLASS);
        for (Set<Direction> arms : armSets(canonical)) {
            BlockModelBuilder builder = models.getBuilder(name(design, canonical, arms)).renderType(RENDER_TYPE);
            GeometryModelWriter.addSurfaces(builder, surfaces(design, canonical, arms), true, Surface::texture);
            builder.texture(WindowPaneGeometry.FRAME, OAK_PLANKS)
                    .texture(WindowPaneGeometry.GLASS, glass)
                    .texture("particle", OAK_PLANKS);
        }
    }

    /**
     * Adds the parts drawing {@code pose} to a multipart blockstate; {@code poseCondition} adds the
     * state values selecting the pose. One part per arm combination, each naming every connection it
     * depends on. Reads only names and geometry, so the alias blockstates can reuse the
     * canonical models written by another provider.
     */
    public static void addParts(MultiPartBlockStateBuilder states, WindowDesign design, WindowPose pose,
                                Consumer<MultiPartBlockStateBuilder.PartBuilder> poseCondition) {
        Canonical canonical = canonical(pose);
        int rotation = 90 * canonical.turns();
        List<Direction> sides = WindowPaneGeometry.neededSides(pose);
        for (Set<Direction> arms : armSets(pose)) {
            Set<Direction> authored = EnumSet.noneOf(Direction.class);
            arms.forEach(side -> authored.add(rotate(side, -canonical.turns())));
            MultiPartBlockStateBuilder.PartBuilder part = states.part()
                    .modelFile(model(name(design, canonical.pose(), authored))).rotationY(rotation).addModel();
            poseCondition.accept(part);
            for (Direction side : sides) {
                part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(side), arms.contains(side));
            }
            part.end();
        }
    }

    private static Direction rotate(Direction side, int turns) {
        Direction out = side;
        for (int i = 0; i < Math.floorMod(turns, 4); i++) {
            out = out.getClockWise();
        }
        return out;
    }

    private static ModelFile model(String name) {
        return new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/" + name));
    }
}
