package io.github.dirtyredz.roundwood.client.material;

import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The array arithmetic behind retextured and turned quads, on raw block-format vertices (8 ints per
 * vertex: x y z as float bits, colour, u v as float bits, light, packed normal bytes nx | ny << 8 |
 * nz << 16). No BakedQuad or sprite is ever built.
 */
class ModelRetexturingTest {
    private static final int STRIDE = ModelRetexturing.STRIDE;
    private static final int COLOUR = 0xFFAABBCC;
    private static final int LIGHT = 0x00F000F0;

    /** A vertex array from positions {x, y, z} and uvs {u, v}, with a shared colour, light and normal. */
    private static int[] quad(float[][] positions, float[][] uvs, int normal) {
        int[] out = new int[positions.length * STRIDE];
        for (int i = 0; i < positions.length; i++) {
            out[i * STRIDE] = Float.floatToRawIntBits(positions[i][0]);
            out[i * STRIDE + 1] = Float.floatToRawIntBits(positions[i][1]);
            out[i * STRIDE + 2] = Float.floatToRawIntBits(positions[i][2]);
            out[i * STRIDE + 3] = COLOUR;
            out[i * STRIDE + 4] = Float.floatToRawIntBits(uvs[i][0]);
            out[i * STRIDE + 5] = Float.floatToRawIntBits(uvs[i][1]);
            out[i * STRIDE + 6] = LIGHT;
            out[i * STRIDE + 7] = normal;
        }
        return out;
    }

    private static int pack(int nx, int ny, int nz) {
        return (nx & 0xFF) | (ny & 0xFF) << 8 | (nz & 0xFF) << 16;
    }

    private static float f(int[] vertices, int index) {
        return Float.intBitsToFloat(vertices[index]);
    }

    private static int[] unpack(int normal) {
        return new int[] {(byte) normal, (byte) (normal >> 8), (byte) (normal >> 16)};
    }

    // ------------------------------------------------------------------ remapUv

    @Test
    void remapUvMovesTheSourceRectangleCornersToTheTargetCorners() {
        float[] from = {2f, 4f, 10f, 12f};
        float[] to = {100f, 200f, 116f, 216f};
        float[][] uvs = {{2f, 4f}, {10f, 4f}, {10f, 12f}, {2f, 12f}};
        int[] vertices = quad(new float[][] {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}}, uvs, pack(0, 0, -1));
        int[] mapped = ModelRetexturing.remapUv(vertices, from, to);
        float[][] expected = {{100f, 200f}, {116f, 200f}, {116f, 216f}, {100f, 216f}};
        for (int i = 0; i < 4; i++) {
            assertEquals(expected[i][0], f(mapped, i * STRIDE + 4), 1e-4, "u of corner " + i);
            assertEquals(expected[i][1], f(mapped, i * STRIDE + 5), 1e-4, "v of corner " + i);
        }
    }

    @Test
    void remapUvMapsAMidPointProportionally() {
        float[] from = {2f, 4f, 10f, 12f};
        float[] to = {100f, 200f, 116f, 216f};
        int[] vertices = quad(new float[][] {{0, 0, 0}}, new float[][] {{6f, 8f}}, 0);
        int[] mapped = ModelRetexturing.remapUv(vertices, from, to);
        assertEquals(108f, f(mapped, 4), 1e-4);
        assertEquals(208f, f(mapped, 5), 1e-4);
        // A quarter of the way along each axis.
        int[] quarter = ModelRetexturing.remapUv(quad(new float[][] {{0, 0, 0}}, new float[][] {{4f, 7f}}, 0), from, to);
        assertEquals(104f, f(quarter, 4), 1e-4);
        assertEquals(206f, f(quarter, 5), 1e-4);
    }

    @Test
    void remapUvLeavesEverythingButTheUvsAloneAndNeverMutatesItsInput() {
        float[][] positions = {{0.1f, 0.2f, 0.3f}, {0.4f, 0.5f, 0.6f}, {0.7f, 0.8f, 0.9f}, {0f, 1f, 0.5f}};
        float[][] uvs = {{2f, 4f}, {10f, 4f}, {10f, 12f}, {2f, 12f}};
        int normal = pack(0, 1, 0);
        int[] vertices = quad(positions, uvs, normal);
        int[] before = vertices.clone();
        int[] mapped = ModelRetexturing.remapUv(vertices, new float[] {2f, 4f, 10f, 12f}, new float[] {0f, 0f, 1f, 1f});
        assertArrayEquals(before, vertices, "the input array must not be mutated");
        for (int i = 0; i < 4; i++) {
            for (int slot : new int[] {0, 1, 2, 3, 6, 7}) {
                assertEquals(before[i * STRIDE + slot], mapped[i * STRIDE + slot], "vertex " + i + " slot " + slot);
            }
        }
        assertEquals(4, mapped.length / STRIDE);
    }

    // ------------------------------------------------------------------ rotateVerticesY

    @Test
    void oneTurnMapsXZToOneMinusZX() {
        int[] vertices = quad(new float[][] {{0.25f, 0.5f, 0.75f}, {0f, 1f, 0f}, {1f, 0f, 0.5f}},
                new float[][] {{0, 0}, {1, 0}, {1, 1}}, pack(0, 0, -1));
        int[] turned = ModelRetexturing.rotateVerticesY(vertices, 1);
        float[][] expected = {{0.25f, 0.5f, 0.25f}, {1f, 1f, 0f}, {0.5f, 0f, 1f}};
        for (int i = 0; i < 3; i++) {
            assertEquals(expected[i][0], f(turned, i * STRIDE), 1e-6, "x of vertex " + i);
            assertEquals(expected[i][1], f(turned, i * STRIDE + 1), 1e-6, "y of vertex " + i);
            assertEquals(expected[i][2], f(turned, i * STRIDE + 2), 1e-6, "z of vertex " + i);
        }
    }

    @Test
    void fourTurnsAreTheIdentityAndTurnsWrap() {
        int[] vertices = quad(new float[][] {{0.125f, 0.5f, 0.75f}, {0.9f, 0.1f, 0.3f}},
                new float[][] {{0, 0}, {1, 1}}, pack(1, 0, 0));
        int[] four = ModelRetexturing.rotateVerticesY(vertices, 4);
        for (int i = 0; i < 2; i++) {
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(f(vertices, i * STRIDE + axis), f(four, i * STRIDE + axis), 1e-6);
            }
            assertEquals(vertices[i * STRIDE + 7], four[i * STRIDE + 7], "normal after four turns");
        }
        assertArrayEquals(ModelRetexturing.rotateVerticesY(vertices, 1), ModelRetexturing.rotateVerticesY(vertices, 5));
        assertArrayEquals(ModelRetexturing.rotateVerticesY(vertices, 3), ModelRetexturing.rotateVerticesY(vertices, -1));
        assertArrayEquals(vertices, ModelRetexturing.rotateVerticesY(vertices, 0));
        // Two quarter turns are a half turn about the centre: (x, z) -> (1 - x, 1 - z).
        int[] half = ModelRetexturing.rotateVerticesY(vertices, 2);
        assertEquals(0.875f, f(half, 0), 1e-6);
        assertEquals(0.25f, f(half, 2), 1e-6);
    }

    @Test
    void normalsRotateWithThePositionsAndOtherFieldsSurvive() {
        int[] vertices = quad(new float[][] {{0.5f, 0.5f, 0.5f}}, new float[][] {{0.25f, 0.75f}}, pack(0, 1, -1));
        int[] turned = ModelRetexturing.rotateVerticesY(vertices, 1);
        // North (0, *, -1) becomes east (1, *, 0); the vertical component stays.
        assertArrayEquals(new int[] {1, 1, 0}, unpack(turned[7]));
        assertEquals(COLOUR, turned[3]);
        assertEquals(LIGHT, turned[6]);
        assertEquals(0.25f, f(turned, 4), 0f);
        assertEquals(0.75f, f(turned, 5), 0f);
        // North -> east -> south -> west in successive turns.
        int[][] expected = {{1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}};
        int[] north = quad(new float[][] {{0.5f, 0.5f, 0.5f}}, new float[][] {{0, 0}}, pack(0, 0, -1));
        for (int turns = 1; turns <= 4; turns++) {
            assertArrayEquals(expected[turns - 1], unpack(ModelRetexturing.rotateVerticesY(north, turns)[7]), "turns " + turns);
        }
    }

    @Test
    void rotateVerticesYNeverMutatesItsInput() {
        int[] vertices = quad(new float[][] {{0.2f, 0.3f, 0.4f}, {0.6f, 0.7f, 0.8f}}, new float[][] {{0, 0}, {1, 1}}, pack(0, 0, 1));
        int[] before = vertices.clone();
        ModelRetexturing.rotateVerticesY(vertices, 3);
        assertArrayEquals(before, vertices);
    }

    // ------------------------------------------------------------------ turn

    @Test
    void horizontalDirectionsTurnClockwiseAndVerticalOnesStay() {
        assertEquals(Direction.EAST, VoxelShapeFactory.turn(Direction.NORTH, 1));
        assertEquals(Direction.SOUTH, VoxelShapeFactory.turn(Direction.EAST, 1));
        assertEquals(Direction.WEST, VoxelShapeFactory.turn(Direction.SOUTH, 1));
        assertEquals(Direction.NORTH, VoxelShapeFactory.turn(Direction.WEST, 1));
        assertEquals(Direction.SOUTH, VoxelShapeFactory.turn(Direction.NORTH, 2));
        assertEquals(Direction.NORTH, VoxelShapeFactory.turn(Direction.NORTH, 4));
        for (int turns = -5; turns <= 9; turns++) {
            assertEquals(Direction.UP, VoxelShapeFactory.turn(Direction.UP, turns));
            assertEquals(Direction.DOWN, VoxelShapeFactory.turn(Direction.DOWN, turns));
        }
    }

    @Test
    void negativeAndLargeTurnsUseFloorMod() {
        assertEquals(Direction.WEST, VoxelShapeFactory.turn(Direction.NORTH, -1));
        assertEquals(Direction.SOUTH, VoxelShapeFactory.turn(Direction.NORTH, -2));
        assertEquals(Direction.EAST, VoxelShapeFactory.turn(Direction.NORTH, -3));
        assertEquals(Direction.EAST, VoxelShapeFactory.turn(Direction.NORTH, 5));
        assertEquals(Direction.EAST, VoxelShapeFactory.turn(Direction.NORTH, 401));
        assertEquals(Direction.NORTH, VoxelShapeFactory.turn(Direction.NORTH, -400));
    }

    @Test
    void turnedDirectionsAgreeWithRotatedNormals() {
        for (Direction side : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            for (int turns = -4; turns <= 8; turns++) {
                int[] vertex = quad(new float[][] {{0.5f, 0.5f, 0.5f}}, new float[][] {{0, 0}},
                        pack(side.getStepX(), 0, side.getStepZ()));
                Direction turned = VoxelShapeFactory.turn(side, turns);
                int[] normal = unpack(ModelRetexturing.rotateVerticesY(vertex, turns)[7]);
                assertArrayEquals(new int[] {turned.getStepX(), 0, turned.getStepZ()}, normal, side + " x" + turns);
            }
        }
    }

    // ------------------------------------------------------------------ rewind

    /** The four corners of a face with the given bounds, in vanilla's baked order for {@code direction}. */
    private static float[][] corners(Direction direction, float[] bounds) {
        FaceInfo face = FaceInfo.fromFacing(direction);
        float[][] out = new float[4][];
        for (int i = 0; i < 4; i++) {
            FaceInfo.VertexInfo corner = face.getVertexInfo(i);
            out[i] = new float[] {bounds[corner.xFace], bounds[corner.yFace], bounds[corner.zFace]};
        }
        return out;
    }

    private static float[] boundsFor(Direction direction) {
        float[] bounds = new float[Direction.values().length];
        bounds[FaceInfo.Constants.MIN_X] = 0.25f;
        bounds[FaceInfo.Constants.MAX_X] = 0.75f;
        bounds[FaceInfo.Constants.MIN_Y] = 0.125f;
        bounds[FaceInfo.Constants.MAX_Y] = 0.5f;
        bounds[FaceInfo.Constants.MIN_Z] = 0.0625f;
        bounds[FaceInfo.Constants.MAX_Z] = 0.875f;
        // A flat face has no extent along its own axis.
        switch (direction.getAxis()) {
            case X -> bounds[FaceInfo.Constants.MIN_X] = bounds[FaceInfo.Constants.MAX_X] = 0.5f;
            case Y -> bounds[FaceInfo.Constants.MIN_Y] = bounds[FaceInfo.Constants.MAX_Y] = 0.5f;
            case Z -> bounds[FaceInfo.Constants.MIN_Z] = bounds[FaceInfo.Constants.MAX_Z] = 0.5f;
        }
        return bounds;
    }

    @Test
    void rewindPutsEveryCornerWhereVanillaExpectsItAndKeepsItsUv() {
        int normal = pack(0, 0, 1);
        for (Direction direction : Direction.values()) {
            float[][] canonical = corners(direction, boundsFor(direction));
            // The UV that belongs to each physical corner, distinct per corner.
            float[][] uvAt = {{0.05f, 0.9f}, {0.15f, 0.8f}, {0.25f, 0.7f}, {0.35f, 0.6f}};
            for (int shift = 0; shift < 4; shift++) {
                for (boolean reversed : new boolean[] {false, true}) {
                    float[][] positions = new float[4][];
                    float[][] uvs = new float[4][];
                    for (int j = 0; j < 4; j++) {
                        int source = reversed ? (7 - j - shift) % 4 : (j + shift) % 4;
                        positions[j] = canonical[source];
                        uvs[j] = uvAt[source];
                    }
                    int[] scrambled = quad(positions, uvs, normal);
                    int[] before = scrambled.clone();
                    int[] fixed = ModelRetexturing.rewind(scrambled, direction);
                    String what = direction + " shift " + shift + (reversed ? " reversed" : "");
                    assertArrayEquals(before, scrambled, what + ": input must not be mutated");
                    for (int i = 0; i < 4; i++) {
                        for (int axis = 0; axis < 3; axis++) {
                            assertEquals(canonical[i][axis], f(fixed, i * STRIDE + axis), 1e-6, what + " corner " + i + " axis " + axis);
                        }
                        assertEquals(uvAt[i][0], f(fixed, i * STRIDE + 4), 1e-6, what + " u of corner " + i);
                        assertEquals(uvAt[i][1], f(fixed, i * STRIDE + 5), 1e-6, what + " v of corner " + i);
                        assertEquals(COLOUR, fixed[i * STRIDE + 3], what);
                        assertEquals(LIGHT, fixed[i * STRIDE + 6], what);
                    }
                }
            }
        }
    }

    @Test
    void rewindingAnAlreadyOrderedQuadChangesNothing() {
        for (Direction direction : Direction.values()) {
            float[][] canonical = corners(direction, boundsFor(direction));
            float[][] uvs = {{0f, 0f}, {1f, 0f}, {1f, 1f}, {0f, 1f}};
            int[] ordered = quad(canonical, uvs, pack(0, 1, 0));
            assertArrayEquals(ordered, ModelRetexturing.rewind(ordered, direction), direction.toString());
        }
    }
}
