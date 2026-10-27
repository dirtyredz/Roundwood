package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.geometry.Surface;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DoorArchMergedSurfacesTest {
    @Test void fittedSurfacesHaveStandaloneCoverageAndTessellationBudget() {
        for (var door : DoorDesign.values()) for (var thickness : ArchPieceThickness.values())
            for (var depth : ArchPieceDepth.values()) for (var layer : DoorArchGeometry.Layer.values()) {
                var top = DoorArchFitPlan.topFor(door);
                double back = thickness.back(depth), front = thickness.front(depth);
                if (layer == DoorArchGeometry.Layer.FRONT_CASING) { back = front; front++; }
                if (layer == DoorArchGeometry.Layer.BACK_CASING) { front = back; back--; }
                for (int column = 0; column < top.columns(); column++) for (int row = 0; row < top.rows(); row++) {
                    var expected = ArchPieceGeometry.surfaces(top, Side.LEFT, column, row, back, front, "");
                    var actual = DoorArchGeometry.surfaces(door, thickness, depth, layer, column, row);
                    assertEquals(coverage(expected), coverage(actual), "oriented half-pixel coverage");
                    assertEquals(expected.size(), actual.size(), "fitted surfaces must use merged rectangles and strips");
                }
            }
    }

    private static Map<String, Integer> coverage(List<Surface> surfaces) {
        var result = new HashMap<String, Integer>();
        for (var surface : surfaces) {
            var b = surface.backing(); var face = surface.face();
            double plane, u0, u1, v0, v1;
            switch (face.getAxis()) {
                case X -> { plane = face.getStepX() < 0 ? b.minX() : b.maxX(); u0=b.minY(); u1=b.maxY(); v0=b.minZ(); v1=b.maxZ(); }
                case Y -> { plane = face.getStepY() < 0 ? b.minY() : b.maxY(); u0=b.minX(); u1=b.maxX(); v0=b.minZ(); v1=b.maxZ(); }
                default -> { plane = face.getStepZ() < 0 ? b.minZ() : b.maxZ(); u0=b.minX(); u1=b.maxX(); v0=b.minY(); v1=b.maxY(); }
            }
            for (int u=(int)(u0*2); u<u1*2; u++) for (int v=(int)(v0*2); v<v1*2; v++)
                result.merge(face+":"+plane+":"+u+":"+v, 1, Integer::sum);
        }
        return result;
    }
}
