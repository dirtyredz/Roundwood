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
 * <p>Per canonical pose: the panel (core), and for a centred upright pose one arm per side across
 * its plane (positioned poses grow no glass, {@link WindowPaneGeometry}). Names are
 * {@code <profile>[<pose>]}, then {@code _arm_<side>}, sides named in the canonical frame. Faces
 * against other always-present geometry are omitted; faces on the cell boundary cull against their
 * side.
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
        for (Direction side : WindowPaneGeometry.neededSides(canonical)) {
            List<Piece> arm = WindowPaneGeometry.arm(profile, canonical, side);
            List<Box> solid = boxes(panel, arm);
            glass(models.getBuilder(stem + "_arm_" + side(side)), arm, glass,
                    (part, face) -> !WindowPaneGeometry.buried(part, face, solid));
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
        for (Direction side : WindowPaneGeometry.neededSides(pose)) {
            Direction authored = rotate(side, -canonical.turns());
            MultiPartBlockStateBuilder.PartBuilder arm = states.part().modelFile(model(stem + "_arm_" + side(authored)))
                    .rotationY(rotation).addModel();
            poseCondition.accept(arm);
            arm.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(side), true).end();
        }
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
