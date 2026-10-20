package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraftforge.client.model.generators.ModelBuilder;

import java.util.List;

/**
 * Writes geometry pieces as model elements. Every face uses the piece's texture variable; UVs are
 * left to Minecraft's default (taken from the element's position), so tiling follows the block grid.
 */
public final class GeometryModelWriter {
    private GeometryModelWriter() {
    }

    public static <T extends ModelBuilder<T>> T addPieces(T builder, List<Piece> pieces) {
        for (Piece piece : pieces) {
            Box box = piece.box();
            String texture = "#" + piece.texture();
            builder.element()
                    .from((float) box.minX(), (float) box.minY(), (float) box.minZ())
                    .to((float) box.maxX(), (float) box.maxY(), (float) box.maxZ())
                    .allFaces((direction, face) -> face.texture(texture))
                    .end();
        }
        return builder;
    }
}
