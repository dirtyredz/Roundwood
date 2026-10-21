package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.generators.ModelBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes geometry pieces as model elements with explicit UVs. A sprite only spans UV 0..16, and
 * Minecraft's default UVs come straight from element coordinates, so geometry beyond one block
 * (the 32-pixel door) would sample neighbouring atlas sprites. Every piece is therefore cut at the
 * 16-pixel texture-tile boundaries, and each part gets the default UVs of its position inside its
 * own tile ({@link FaceUv}): the texture repeats seamlessly across tiles, keeping grain direction and
 * continuity.
 */
public final class GeometryModelWriter {
    private static final double TILE = 16;

    private GeometryModelWriter() {
    }

    /** Which faces of a (tile-split) part to write; every face unless a model hides internal ones. */
    @FunctionalInterface
    public interface FaceFilter {
        FaceFilter ALL = (part, face) -> true;

        boolean draw(Box part, Direction face);
    }

    /**
     * @param cullBlockFaces mark faces lying on the unit cell's boundary with a matching cullface
     *                       (only for single-block world models; never for the door leaf or items)
     */
    public static <T extends ModelBuilder<T>> T addPieces(T builder, List<Piece> pieces, boolean cullBlockFaces) {
        return addPieces(builder, pieces, cullBlockFaces, FaceFilter.ALL);
    }

    /** As above, writing only the faces {@code faces} accepts (for faces buried against other geometry). */
    public static <T extends ModelBuilder<T>> T addPieces(T builder, List<Piece> pieces, boolean cullBlockFaces,
                                                          FaceFilter faces) {
        for (Piece piece : pieces) {
            String texture = "#" + piece.texture();
            for (Box part : splitAtTiles(piece.box())) {
                Box local = part.translate(-tileOrigin(part.minX()), -tileOrigin(part.minY()), -tileOrigin(part.minZ()));
                ModelBuilder<T>.ElementBuilder element = builder.element()
                        .from((float) part.minX(), (float) part.minY(), (float) part.minZ())
                        .to((float) part.maxX(), (float) part.maxY(), (float) part.maxZ());
                for (Direction direction : Direction.values()) {
                    if (!faces.draw(part, direction)) {
                        continue;
                    }
                    float[] uv = FaceUv.of(local, direction);
                    ModelBuilder<T>.ElementBuilder.FaceBuilder face = element.face(direction)
                            .texture(texture).uvs(uv[0], uv[1], uv[2], uv[3]);
                    if (cullBlockFaces && onCellBoundary(part, direction)) {
                        face.cullface(direction);
                    }
                    face.end();
                }
                element.end();
            }
        }
        return builder;
    }

    /** Cuts a box at every multiple of 16 strictly inside it, on all three axes. */
    static List<Box> splitAtTiles(Box box) {
        List<Box> out = new ArrayList<>();
        for (double[] x : cuts(box.minX(), box.maxX())) {
            for (double[] y : cuts(box.minY(), box.maxY())) {
                for (double[] z : cuts(box.minZ(), box.maxZ())) {
                    out.add(new Box(x[0], y[0], z[0], x[1], y[1], z[1]));
                }
            }
        }
        return out;
    }

    private static List<double[]> cuts(double min, double max) {
        List<double[]> spans = new ArrayList<>();
        double start = min;
        double next = (Math.floor(min / TILE) + 1) * TILE;
        while (next < max) {
            spans.add(new double[] {start, next});
            start = next;
            next += TILE;
        }
        spans.add(new double[] {start, max});
        return spans;
    }

    private static double tileOrigin(double min) {
        return Math.floor(min / TILE) * TILE;
    }

    private static boolean onCellBoundary(Box box, Direction face) {
        return switch (face) {
            case DOWN -> box.minY() == 0;
            case UP -> box.maxY() == TILE;
            case NORTH -> box.minZ() == 0;
            case SOUTH -> box.maxZ() == TILE;
            case WEST -> box.minX() == 0;
            case EAST -> box.maxX() == TILE;
        };
    }
}
