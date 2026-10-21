package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The whole-assembly front/back UV mapping that spreads one image across a 32-pixel assembly. */
class AssemblyFaceUvTest {
    private static final float TOL = 1e-5F;
    private static final double SIZE = 32;
    private static final Direction[] FRONT_AND_BACK = {Direction.SOUTH, Direction.NORTH};

    @Test
    void southMapsXLinearlyAndFlipsY() {
        float[] uv = AssemblyFaceUv.of(new Box(4, 6, 0, 10, 20, 1), Direction.SOUTH, SIZE);
        assertArrayEquals(new float[] {2F, (32 - 20) / 2F, 5F, (32 - 6) / 2F}, uv, TOL, "south uv");
    }

    @Test
    void northMirrorsU() {
        float[] uv = AssemblyFaceUv.of(new Box(4, 6, 0, 10, 20, 1), Direction.NORTH, SIZE);
        assertArrayEquals(new float[] {(32 - 10) / 2F, (32 - 20) / 2F, (32 - 4) / 2F, (32 - 6) / 2F}, uv, TOL, "north uv");
    }

    @Test
    void otherDirectionsHaveNoAssemblyMapping() {
        Box box = new Box(0, 0, 0, 4, 4, 4);
        for (Direction face : Direction.values()) {
            if (face != Direction.SOUTH && face != Direction.NORTH) {
                assertNull(AssemblyFaceUv.of(box, face, SIZE), face + " must keep the tiled mapping");
            }
        }
    }

    @Test
    void theWholeFaceMapsToExactlyTheWholeSprite() {
        Box whole = new Box(0, 0, 0, 32, 32, 1);
        for (Direction face : FRONT_AND_BACK) {
            assertArrayEquals(new float[] {0, 0, 16, 16}, AssemblyFaceUv.of(whole, face, SIZE), 0F, face + " whole face");
        }
    }

    @Test
    void adjacentBoxesShareTheirUvEdgeExactly() {
        for (Direction face : FRONT_AND_BACK) {
            float[] left = AssemblyFaceUv.of(new Box(0, 0, 0, 16, 16, 1), face, SIZE);
            float[] right = AssemblyFaceUv.of(new Box(16, 0, 0, 32, 16, 1), face, SIZE);
            if (face == Direction.SOUTH) {
                assertEquals(left[2], right[0], 0F, "south: u2 of left == u1 of right");
            } else {
                assertEquals(right[2], left[0], 0F, "north: u2 of right == u1 of left (mirrored)");
            }
            float[] below = AssemblyFaceUv.of(new Box(0, 0, 0, 16, 16, 1), face, SIZE);
            float[] above = AssemblyFaceUv.of(new Box(0, 16, 0, 16, 32, 1), face, SIZE);
            assertEquals(below[1], above[3], 0F, face + ": v1 of the box below == v2 of the box above");
        }
    }

    @Test
    void cellLocalBoxesLandInTheirQuarterOfTheSprite() {
        Box local = new Box(2, 3, 0, 13, 14, 1);
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                Box placed = local.translate(16 * column, 16 * row, 0);
                assertInQuarter(AssemblyFaceUv.of(placed, Direction.SOUTH, SIZE), 8 * column, 8 * (1 - row),
                        "south " + column + "," + row);
                assertInQuarter(AssemblyFaceUv.of(placed, Direction.NORTH, SIZE), 8 * (1 - column), 8 * (1 - row),
                        "north " + column + "," + row);
            }
        }
    }

    private static void assertInQuarter(float[] uv, double u0, double v0, String label) {
        assertTrue(uv[0] >= u0 - TOL && uv[2] <= u0 + 8 + TOL, label + " u out of quadrant: " + Arrays.toString(uv));
        assertTrue(uv[1] >= v0 - TOL && uv[3] <= v0 + 8 + TOL, label + " v out of quadrant: " + Arrays.toString(uv));
    }

    @Test
    void randomBoxesInsideTheAssemblyStayInsideTheSprite() {
        Random random = new Random(42);
        for (int i = 0; i < 500; i++) {
            double x1 = random.nextDouble() * 31;
            double y1 = random.nextDouble() * 31;
            double x2 = x1 + 0.1 + random.nextDouble() * (31.9 - x1);
            double y2 = y1 + 0.1 + random.nextDouble() * (31.9 - y1);
            Box box = new Box(x1, y1, 0, x2, y2, 1);
            for (Direction face : FRONT_AND_BACK) {
                float[] uv = AssemblyFaceUv.of(box, face, SIZE);
                for (float value : uv) {
                    assertTrue(value >= -TOL && value <= 16 + TOL, face + " uv out of 0..16: " + value + " for " + box);
                }
                assertTrue(uv[0] < uv[2] && uv[1] < uv[3], face + " uv inverted for " + box);
            }
        }
    }
}
