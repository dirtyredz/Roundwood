package io.github.dirtyredz.roundwood.data.client.archway;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayGeometry;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

import java.util.List;

/**
 * The archways' inventory models (0.12.0), one per design: the whole frame face-on, full depth,
 * from the same boxes as its shapes ({@link ArchwayGeometry#frame}), in oak stripped log tiled like a
 * block (front and back {@code #face}, sides {@code #edge}), with the two-block assembly's display
 * transforms centred on the frame. A styled stack is baked from the same mask on the client
 * ({@code client/archway/ArchwayModels#item}), which copies these transforms.
 */
public class ArchwayItemModelProvider extends ItemModelProvider {
    static final String FACE = "face";
    static final String EDGE = "edge";
    private static final Vector3f CENTRE = new Vector3f(ArchwayDesign.SIZE / 2F, ArchwayDesign.SIZE / 2F,
            (float) (ArchwayDesign.BACK + ArchwayDesign.FRONT) / 2F);

    public ArchwayItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood archway item models";
    }

    @Override
    protected void registerModels() {
        for (ArchwayDesign design : ArchwayDesign.values()) {
            ItemModelBuilder item = getBuilder(design.id());
            List<Piece> pieces = ArchwayGeometry.frame(design).stream().map(box -> new Piece(box, FACE)).toList();
            GeometryModelWriter.addPieces(item, pieces, false, new GeometryModelWriter.FaceStyle() {
                @Override
                public String texture(Piece piece, Direction face) {
                    return face.getAxis() == Direction.Axis.Z ? FACE : EDGE;
                }

                @Override
                public boolean draw(Box part, Direction face) {
                    return exposed(design, part, face);
                }
            });
            item.texture(FACE, ArchwayModelProvider.OAK_LOG).texture(EDGE, ArchwayModelProvider.OAK_LOG)
                    .texture("particle", ArchwayModelProvider.OAK_LOG);
            ItemModelTransforms.assembly(item, CENTRE);
        }
    }

    /**
     * Whether a side of one tile-split part shows: front and back always; any other side only where the
     * neighbouring pixel row or column beyond it is not frame (no faces buried between merged parts).
     */
    static boolean exposed(ArchwayDesign design, Box part, Direction face) {
        if (face.getAxis() == Direction.Axis.Z) {
            return true;
        }
        int x1 = (int) part.minX();
        int x2 = (int) part.maxX();
        int y1 = (int) part.minY();
        int y2 = (int) part.maxY();
        return switch (face) {
            case UP -> anyOpen(design, x1, x2, y2, y2 + 1);
            case DOWN -> anyOpen(design, x1, x2, y1 - 1, y1);
            case EAST -> anyOpen(design, x2, x2 + 1, y1, y2);
            case WEST -> anyOpen(design, x1 - 1, x1, y1, y2);
            default -> true;
        };
    }

    private static boolean anyOpen(ArchwayDesign design, int u1, int u2, int v1, int v2) {
        for (int u = u1; u < u2; u++) {
            for (int v = v1; v < v2; v++) {
                if (!design.inFrame(u, v)) {
                    return true;
                }
            }
        }
        return false;
    }
}
