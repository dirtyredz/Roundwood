package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaticWorldGeometryTest {
    @Test void allDepthsAndFacingsMatchTheBerPoseAndPreserveQuadAttributes() {
        for (var facing : Direction.Plane.HORIZONTAL) for (var depth : DoorDepth.values())
            for (boolean shade : new boolean[]{false, true}) for (boolean ao : new boolean[]{false, true}) {
                int[] data = new int[32];
                for (int v = 0; v < 4; v++) {
                    data[v*8] = Float.floatToIntBits(v*.25f);
                    data[v*8+1] = Float.floatToIntBits(1.0625f); // Proud band must not be clipped.
                    data[v*8+2] = Float.floatToIntBits(-.0625f);
                    for (int a = 3; a < 8; a++) data[v*8+a] = v*100+a;
                }
                var original = new BakedQuad(data, 7, Direction.UP, null, shade, ao);
                double x = -facing.getStepX()*depth.offset()/16, z = -facing.getStepZ()*depth.offset()/16;
                var chunk = StaticQuadTransform.translate(original, x, 0, z, 42);
                assertEquals(42, chunk.getTintIndex()); assertEquals(shade, chunk.isShade());
                assertEquals(ao, chunk.hasAmbientOcclusion()); assertSame(original.getSprite(), chunk.getSprite());
                assertEquals(original.getDirection(), chunk.getDirection());
                for (int v = 0; v < 4; v++) {
                    assertEquals((float)(v*.25+x), Float.intBitsToFloat(chunk.getVertices()[v*8]));
                    assertEquals(1.0625f, Float.intBitsToFloat(chunk.getVertices()[v*8+1]));
                    assertEquals((float)(-.0625+z), Float.intBitsToFloat(chunk.getVertices()[v*8+2]));
                    for (int a = 3; a < 8; a++) assertEquals(data[v*8+a], chunk.getVertices()[v*8+a]);
                }
                assertArrayEquals(data, original.getVertices());
            }
    }
}
