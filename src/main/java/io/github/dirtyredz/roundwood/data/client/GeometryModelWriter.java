package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.generators.ModelBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Writes geometry pieces as model elements with explicit UVs. A sprite only spans UV 0..16, and
 * Minecraft's default UVs come straight from element coordinates, so geometry beyond one block
 * (the 32-pixel door) would sample neighbouring atlas sprites. Every piece is therefore cut at the
 * 16-pixel texture-tile boundaries, and each part gets the default UVs of its position inside its
 * own tile ({@link FaceUv}): the texture repeats seamlessly across tiles, keeping grain direction and
 * continuity. A model may instead give some faces explicit UVs ({@link FaceStyle}), such as the door
 * panel and frame faces that spread one image across the whole assembly
 * ({@link io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv}).
 */
public final class GeometryModelWriter {
    private static final double TILE = 16;

    private GeometryModelWriter() {
    }

    /**
     * How a model writes the faces of its pieces. The default writes every face with the piece's own
     * texture variable and the tiled UVs; a model overrides only what it needs: hiding buried faces,
     * a different texture variable for some faces (a frame's sides), or explicit UVs (whole-assembly
     * mapping).
     */
    public interface FaceStyle {
        FaceStyle DEFAULT = new FaceStyle() {
        };

        /** Whether to write this face of a tile-split part (false for faces buried against other geometry). */
        default boolean draw(Box part, Direction face) {
            return true;
        }

        /** The texture variable (without {@code #}) for this face of the piece. */
        default String texture(Piece piece, Direction face) {
            return piece.texture();
        }

        /**
         * @param part one tile-split part of the piece, in model coordinates
         * @return {u1, v1, u2, v2} within 0..16, or null for the tiled default
         */
        default float[] uv(Piece piece, Box part, Direction face) {
            return null;
        }
    }

    /**
     * @param cullBlockFaces mark faces lying on the unit cell's boundary with a matching cullface
     *                       (only for single-block world models; never for the door leaf or items)
     */
    public static <T extends ModelBuilder<T>> T addPieces(T builder, List<Piece> pieces, boolean cullBlockFaces) {
        return addPieces(builder, pieces, cullBlockFaces, FaceStyle.DEFAULT);
    }

    /** As above, writing faces the way {@code style} says. */
    public static <T extends ModelBuilder<T>> T addPieces(T builder, List<Piece> pieces, boolean cullBlockFaces,
                                                          FaceStyle style) {
        for (Piece piece : pieces) {
            for (Box part : splitAtTiles(piece.box())) {
                if (Arrays.stream(Direction.values()).noneMatch(direction -> style.draw(part, direction))) {
                    continue; // an element needs at least one face; a wholly buried part is simply left out
                }
                Box local = part.translate(-tileOrigin(part.minX()), -tileOrigin(part.minY()), -tileOrigin(part.minZ()));
                ModelBuilder<T>.ElementBuilder element = builder.element()
                        .from((float) part.minX(), (float) part.minY(), (float) part.minZ())
                        .to((float) part.maxX(), (float) part.maxY(), (float) part.maxZ());
                for (Direction direction : Direction.values()) {
                    if (!style.draw(part, direction)) {
                        continue;
                    }
                    float[] explicit = style.uv(piece, part, direction);
                    float[] uv = explicit != null ? explicit : FaceUv.of(local, direction);
                    ModelBuilder<T>.ElementBuilder.FaceBuilder face = element.face(direction)
                            .texture("#" + style.texture(piece, direction)).uvs(uv[0], uv[1], uv[2], uv[3]);
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
