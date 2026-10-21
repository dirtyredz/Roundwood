package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowProfile;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Model naming, emission and multipart parts for posed window panes (every pose except the round
 * window's centred one, whose 0.5.0 models are kept as they were). Models are authored once per
 * <em>canonical</em> pose (upright facing south, or a floor/ceiling pane with its top north) from
 * {@link WindowPaneGeometry}, and every other pose is a blockstate quarter turn of one of them, so
 * the shapes the block computes and the models drawn are the same geometry.
 *
 * <p>Per canonical pose: the panel (core), the hub (post and stem, when the pose has one), one arm
 * per side that needs a bridge, and one cap per such side: the hub faces an arm hides, drawn only
 * while that side is not connected, so no glass face is ever buried behind other glass. Names are
 * {@code <profile>[<pose>]}, then {@code _hub}, {@code _arm_<side>}, {@code _cap_<side>}, sides
 * named in the canonical frame. Faces against other always-present geometry are omitted; faces on
 * the cell boundary cull against their side.
 */
public final class WindowPaneModelWriter {
    /** The wood the core binds: the template the client retextures from. */
    public static final String OAK_PLANKS = "minecraft:block/oak_planks";
    static final String RENDER_TYPE = "translucent";
    /** Roundwood's own glazing (see {@link WindowModelProvider}). */
    static final String GLASS = "block/window_glass";

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
    public static String stem(WindowProfile profile, WindowPose canonical) {
        if (canonical.upright()) {
            return profile.id() + switch (canonical.depth()) {
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
        return profile.id() + "_" + (canonical.front() == Direction.UP ? "up" : "down") + "_" + level;
    }

    public static String side(Direction side) {
        return side.getSerializedName().toLowerCase(Locale.ROOT);
    }

    /** The canonical poses a profile draws through this writer (the round window's centre excluded). */
    public static List<WindowPose> canonicalPoses(WindowProfile profile) {
        List<WindowPose> poses = new ArrayList<>();
        if (profile == WindowProfile.ROUND) {
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

    /** Writes every model of {@code profile}'s canonical pose. */
    public static void writeModels(BlockModelProvider models, WindowProfile profile, WindowPose canonical) {
        String stem = stem(profile, canonical);
        ResourceLocation glass = ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, GLASS);
        List<Piece> panel = WindowPaneGeometry.panel(profile, canonical);
        GeometryModelWriter.addPieces(models.getBuilder(stem).renderType(RENDER_TYPE), panel, true)
                .texture(WindowPaneGeometry.FRAME, OAK_PLANKS)
                .texture(WindowPaneGeometry.GLASS, glass)
                .texture("particle", OAK_PLANKS);
        List<Piece> hub = WindowPaneGeometry.hub(profile, canonical);
        List<Box> fixed = boxes(panel, hub);
        if (!hub.isEmpty()) {
            glass(models.getBuilder(stem + "_hub"), hub, glass, (part, face) ->
                    !WindowPaneGeometry.buried(part, face, fixed) && capSide(profile, canonical, hub, part, face) == null);
        }
        for (Direction side : WindowPaneGeometry.neededSides(canonical)) {
            List<Piece> arm = WindowPaneGeometry.arm(profile, canonical, side);
            List<Box> solid = boxes(panel, hub, arm);
            glass(models.getBuilder(stem + "_arm_" + side(side)), arm, glass,
                    (part, face) -> !WindowPaneGeometry.buried(part, face, solid));
            if (hasCap(profile, canonical, side)) {
                glass(models.getBuilder(stem + "_cap_" + side(side)), hub, glass,
                        (part, face) -> capSide(profile, canonical, hub, part, face) == side);
            }
        }
    }

    /**
     * Adds the parts drawing {@code pose} to a multipart blockstate; {@code poseCondition} adds the
     * state values selecting the pose. Reads only names and geometry, so the alias blockstates can
     * reuse the canonical models written by another provider.
     */
    public static void addParts(MultiPartBlockStateBuilder states, WindowProfile profile, WindowPose pose,
                                Consumer<MultiPartBlockStateBuilder.PartBuilder> poseCondition) {
        Canonical canonical = canonical(pose);
        String stem = stem(profile, canonical.pose());
        int rotation = 90 * canonical.turns();
        MultiPartBlockStateBuilder.PartBuilder core = states.part().modelFile(model(stem)).rotationY(rotation).addModel();
        poseCondition.accept(core);
        core.end();
        List<Direction> needed = WindowPaneGeometry.neededSides(pose);
        if (!WindowPaneGeometry.hub(profile, canonical.pose()).isEmpty()) {
            // "any needed side connected" as disjoint terms: side i true, every earlier side false.
            for (int i = 0; i < needed.size(); i++) {
                MultiPartBlockStateBuilder.PartBuilder hub = states.part().modelFile(model(stem + "_hub")).rotationY(rotation).addModel();
                poseCondition.accept(hub);
                for (int j = 0; j < i; j++) {
                    hub.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(needed.get(j)), false);
                }
                hub.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(needed.get(i)), true).end();
            }
        }
        for (Direction side : needed) {
            Direction authored = rotate(side, -canonical.turns());
            MultiPartBlockStateBuilder.PartBuilder arm = states.part().modelFile(model(stem + "_arm_" + side(authored)))
                    .rotationY(rotation).addModel();
            poseCondition.accept(arm);
            arm.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(side), true).end();
            if (!hasCap(profile, canonical.pose(), authored)) {
                continue;
            }
            // The cap shows while this side is open and the hub is there (another needed side connected).
            List<Direction> others = needed.stream().filter(other -> other != side).toList();
            for (int i = 0; i < others.size(); i++) {
                MultiPartBlockStateBuilder.PartBuilder cap = states.part().modelFile(model(stem + "_cap_" + side(authored)))
                        .rotationY(rotation).addModel();
                poseCondition.accept(cap);
                cap.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
                for (int j = 0; j < i; j++) {
                    cap.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(others.get(j)), false);
                }
                cap.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(others.get(i)), true).end();
            }
        }
    }

    /** Whether the canonical pose has a cap towards {@code side} (some hub face an arm there hides). */
    public static boolean hasCap(WindowProfile profile, WindowPose canonical, Direction side) {
        List<Piece> hub = WindowPaneGeometry.hub(profile, canonical);
        for (Piece piece : hub) {
            if (capSide(profile, canonical, hub, piece.box(), side) == side) {
                return true;
            }
        }
        return false;
    }

    /**
     * The side whose arm buries this hub face (it is exposed without that arm), or null for a face
     * that is always drawn or always buried.
     */
    private static Direction capSide(WindowProfile profile, WindowPose canonical, List<Piece> hub, Box part, Direction face) {
        if (!face.getAxis().isHorizontal() || !WindowPaneGeometry.neededSides(canonical).contains(face)) {
            return null;
        }
        List<Piece> panel = WindowPaneGeometry.panel(profile, canonical);
        if (WindowPaneGeometry.buried(part, face, boxes(panel, hub))) {
            return null;
        }
        List<Box> withArm = boxes(panel, hub, WindowPaneGeometry.arm(profile, canonical, face));
        return WindowPaneGeometry.buried(part, face, withArm) ? face : null;
    }

    private interface Faces {
        boolean draw(Box part, Direction face);
    }

    private static void glass(BlockModelBuilder builder, List<Piece> pieces, ResourceLocation glass, Faces faces) {
        GeometryModelWriter.addPieces(builder.renderType(RENDER_TYPE), pieces, true, new GeometryModelWriter.FaceStyle() {
            @Override
            public boolean draw(Box part, Direction face) {
                return faces.draw(part, face);
            }
        }).texture(WindowPaneGeometry.GLASS, glass).texture("particle", glass);
    }

    @SafeVarargs
    private static List<Box> boxes(List<Piece>... groups) {
        List<Box> out = new ArrayList<>();
        for (List<Piece> group : groups) {
            group.forEach(piece -> out.add(piece.box()));
        }
        return out;
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
