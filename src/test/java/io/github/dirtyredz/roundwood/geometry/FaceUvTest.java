package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tile-normalised face UVs (0.8.0): a box one pixel past the cell samples the same spot of the sprite
 * a block there would, never outside the sprite; in-tile boxes keep vanilla's default UVs.
 */
class FaceUvTest {
    @Test void normalSeparationDoesNotChangeUvTiles() {
        var box = new Box(0,0,16,16,16,17);
        for (var face : Direction.values()) {
            var separated = io.github.dirtyredz.roundwood.client.render.SurfaceSeparation.DOOR_CASING.apply(new Surface(box, face, ""));
            assertArrayEquals(FaceUv.tiled(box,face),FaceUv.tiled(separated.backing(),face));
        }
    }

    @Test
    void inTileBoxesAreUnchanged() {
        Box box = new Box(2, 3, 4, 6, 9, 5);
        for (Direction face : Direction.values()) {
            assertArrayEquals(FaceUv.of(box, face), FaceUv.tiled(box, face), face.toString());
        }
    }

    @Test
    void projectingBoxesFoldBackIntoTheSprite() {
        // z -1..0 is z 15..16 of the tile behind; z 16..17 is z 0..1 of the tile in front.
        assertArrayEquals(FaceUv.of(new Box(0, 0, 15, 16, 16, 16), Direction.EAST),
                FaceUv.tiled(new Box(0, 0, -1, 16, 16, 0), Direction.EAST));
        assertArrayEquals(new float[] {15, 0, 16, 16}, FaceUv.tiled(new Box(0, 0, -1, 16, 16, 0), Direction.WEST));
        assertArrayEquals(new float[] {0, 0, 1, 16}, FaceUv.tiled(new Box(0, 0, 16, 16, 16, 17), Direction.WEST));
        assertArrayEquals(new float[] {3, 0, 4, 1}, FaceUv.tiled(new Box(3, 16, 0, 4, 17, 1), Direction.UP));
    }

    @Test
    void aBoxAcrossATileBoundaryIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> FaceUv.tiled(new Box(0, 0, -1, 16, 16, 1), Direction.EAST));
    }
}
