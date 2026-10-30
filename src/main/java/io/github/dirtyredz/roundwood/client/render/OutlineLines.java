package io.github.dirtyredz.roundwood.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Line drawing in vanilla's block-outline style ({@code RenderType.lines()}: position, colour and
 * the line's direction as its normal), camera-relative. Shared by the door highlight, the door
 * placement preview and the window placement overlay.
 */
public final class OutlineLines {
    private OutlineLines() {
    }

    /** Every edge of {@code shape} (one shape: no seams between its boxes), offset by (x, y, z). */
    public static void edges(PoseStack pose, VertexConsumer lines, VoxelShape shape, double x, double y, double z,
                             float r, float g, float b, float a) {
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
                line(pose, lines, x1 + x, y1 + y, z1 + z, x2 + x, y2 + y, z2 + z, r, g, b, a));
    }

    /** One segment between two camera-relative points. */
    public static void line(PoseStack pose, VertexConsumer lines, double x1, double y1, double z1,
                            double x2, double y2, double z2, float r, float g, float b, float a) {
        PoseStack.Pose last = pose.last();
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1e-6F) {
            return;
        }
        dx /= length;
        dy /= length;
        dz /= length;
        lines.addVertex(last, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a)
                .setNormal(last, dx, dy, dz);
        lines.addVertex(last, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a)
                .setNormal(last, dx, dy, dz);
    }
}
