package io.github.dirtyredz.roundwood.client.window;

import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirtyredz.roundwood.client.render.PlacementHighlights;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.client.render.OutlineLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;

/**
 * While a window item is held, draws the Hammer-style targeting grid on the aimed face (centre
 * square and four edge trapezoids, from {@link WindowTargetZones}), the targeted zone picked out,
 * and the outline of the pane that would be placed, in its destination cell. Everything comes from
 * {@link WindowPlacementPreview}, the placement path itself; this class only draws. Lines are
 * camera-relative, depth-tested and lifted a hair off the face so they never flicker. The grid is
 * clipped to the hit block's outline (a slab's side shows only its lower half). Receives cancelled
 * highlight events (the door highlight cancels its own) and never changes their cancellation.
 * Client-only, on the Forge bus.
 */
@EventBusSubscriber(modid = Roundwood.MOD_ID, value = Dist.CLIENT)
public final class WindowPlacementOverlay {
    private static final double LIFT = 0.002;
    private static final float[] GRID = {0F, 0F, 0F, 0.45F};
    private static final float[] ZONE = {1F, 1F, 1F, 0.9F};
    private static final float[] PANE = {0.35F, 0.8F, 1F, 0.9F};

    private WindowPlacementOverlay() {
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        PlacementHighlights.draw(event, WindowPlacementPreview::of, (minecraft, preview) -> {
            Vec3 camera = event.getCamera().getPosition();
            PoseStack pose = event.getPoseStack();
            VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
            drawGrid(pose, lines, preview, minecraft, camera);
            preview.proposal().ifPresent(proposal -> {
                BlockPos pos = proposal.pos();
                VoxelShape shape = proposal.state().getShape(minecraft.level, pos);
                if (proposal.fittedShape() != null) {
                    var block = (WindowPaneBlock) proposal.state().getBlock();
                    var design = new WindowDesign(block.profile(), WindowPattern.CROSS);
                    shape = proposal.fittedShape();
                    WindowBeamPreview.draw(event, pos, design, block.pose(proposal.state()));
                }
                OutlineLines.edges(pose, lines, shape, pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z,
                        PANE[0], PANE[1], PANE[2], PANE[3]);
            });
        });
    }

    private static void drawGrid(PoseStack pose, VertexConsumer lines, WindowPlacementPreview.Preview preview,
                                 Minecraft minecraft, Vec3 camera) {
        BlockPos cell = preview.clicked();
        Direction face = preview.face();
        double[] clip = clip(minecraft.level.getBlockState(cell).getShape(minecraft.level, cell), face);
        // The surface the hit lies on (a slab's top is mid-cell), lifted towards the viewer.
        double plane = coordinate(preview.hit(), face.getAxis()) - coordinate(Vec3.atLowerCornerOf(cell), face.getAxis())
                + face.getAxisDirection().getStep() * LIFT;
        Frame frame = new Frame(cell, face, plane, camera, clip);
        // Every zone's outline once: shared edges are drawn a single time, so the grid is even.
        Set<List<Double>> edges = new LinkedHashSet<>();
        for (WindowTargetZones.Zone zone : WindowTargetZones.Zone.values()) {
            List<WindowTargetZones.FacePoint> points = WindowTargetZones.polygon(zone);
            for (int i = 0; i < points.size(); i++) {
                WindowTargetZones.FacePoint a = points.get(i);
                WindowTargetZones.FacePoint b = points.get((i + 1) % points.size());
                boolean ordered = a.u() < b.u() || (a.u() == b.u() && a.v() <= b.v());
                edges.add(ordered ? List.of(a.u(), a.v(), b.u(), b.v()) : List.of(b.u(), b.v(), a.u(), a.v()));
            }
        }
        for (List<Double> edge : edges) {
            segment(pose, lines, frame, edge.get(0), edge.get(1), edge.get(2), edge.get(3), GRID);
        }
        polygon(pose, lines, frame, WindowTargetZones.polygon(preview.zone()), ZONE);
    }

    private record Frame(BlockPos cell, Direction face, double plane, Vec3 camera, double[] clip) {
        Vec3 point(double u, double v) {
            double[] local = WindowTargetZones.toCell(face, u, v);
            int axis = face.getAxis().ordinal();
            local[axis] = plane;
            return new Vec3(cell.getX() + local[0] - camera.x, cell.getY() + local[1] - camera.y,
                    cell.getZ() + local[2] - camera.z);
        }
    }

    private static void polygon(PoseStack pose, VertexConsumer lines, Frame frame, List<WindowTargetZones.FacePoint> points,
                                float[] colour) {
        for (int i = 0; i < points.size(); i++) {
            WindowTargetZones.FacePoint a = points.get(i);
            WindowTargetZones.FacePoint b = points.get((i + 1) % points.size());
            segment(pose, lines, frame, a.u(), a.v(), b.u(), b.v(), colour);
        }
    }

    private static void segment(PoseStack pose, VertexConsumer lines, Frame frame, double u1, double v1, double u2, double v2,
                                float[] colour) {
        double[] segment = clipSegment(u1, v1, u2, v2, frame.clip());
        if (segment == null) {
            return;
        }
        Vec3 from = frame.point(segment[0], segment[1]);
        Vec3 to = frame.point(segment[2], segment[3]);
        OutlineLines.line(pose, lines, from.x, from.y, from.z, to.x, to.y, to.z, colour[0], colour[1], colour[2], colour[3]);
    }

    /** The hit block's outline extent on the face, in face coordinates {u0, v0, u1, v1} (the unit square when empty). */
    private static double[] clip(VoxelShape shape, Direction face) {
        if (shape.isEmpty()) {
            return new double[] {0, 0, 1, 1};
        }
        AABB b = shape.bounds();
        double[] range = switch (face.getAxis()) {
            case Y -> new double[] {b.minX, b.minZ, b.maxX, b.maxZ};
            case Z -> new double[] {b.minX, b.minY, b.maxX, b.maxY};
            case X -> new double[] {b.minZ, b.minY, b.maxZ, b.maxY};
        };
        for (int i = 0; i < 4; i++) {
            range[i] = Math.max(0, Math.min(1, range[i]));
        }
        return range;
    }

    /** Liang-Barsky: the part of segment (u1,v1)-(u2,v2) inside the clip rectangle, or null. */
    private static double[] clipSegment(double u1, double v1, double u2, double v2, double[] clip) {
        double t0 = 0;
        double t1 = 1;
        double du = u2 - u1;
        double dv = v2 - v1;
        double[] p = {-du, du, -dv, dv};
        double[] q = {u1 - clip[0], clip[2] - u1, v1 - clip[1], clip[3] - v1};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) {
                    return null;
                }
                continue;
            }
            double t = q[i] / p[i];
            if (p[i] < 0) {
                t0 = Math.max(t0, t);
            } else {
                t1 = Math.min(t1, t);
            }
            if (t0 > t1) {
                return null;
            }
        }
        return new double[] {u1 + t0 * du, v1 + t0 * dv, u1 + t1 * du, v1 + t1 * dv};
    }

    private static double coordinate(Vec3 point, Direction.Axis axis) {
        return point.get(axis);
    }
}
