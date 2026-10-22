package io.github.dirtyredz.roundwood.data.client.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.item.UniversalTrimItem;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

import java.util.List;

/**
 * The Universal Trim's inventory model (0.9.0): a flat, face-on square frame, {@link #SIZE} pixels
 * across with a {@link #BAND}-pixel band and an open square centre, {@link #DEPTH} pixels deep, in
 * oak stripped log. Front and back spread one image over the whole square ({@link AssemblyFaceUv},
 * which for a tiled wood equals the default UVs), so the client can retexture it to any wood, paint
 * or masonry face ({@code client/item/UniversalTrimModels}). The hidden {@code window_trim} item
 * model inherits it; the 0.4.0 trim aliases already inherit {@code item/round_trim}.
 */
public class UniversalTrimItemModelProvider extends ItemModelProvider {
    public static final int SIZE = 16;
    public static final int BAND = 2;
    public static final int DEPTH = 2;
    static final String FACE = "face";
    static final String EDGE = "edge";
    private static final double Z1 = (16 - DEPTH) / 2.0;
    private static final Vector3f CENTRE = new Vector3f(8, 8, 8);

    public UniversalTrimItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood Universal Trim item models";
    }

    /** The four bands: bottom and top across the full width, the sides between them. */
    public static List<Box> frame() {
        int inner = SIZE - BAND;
        return List.of(
                new Box(0, 0, Z1, SIZE, BAND, Z1 + DEPTH),
                new Box(0, inner, Z1, SIZE, SIZE, Z1 + DEPTH),
                new Box(0, BAND, Z1, BAND, inner, Z1 + DEPTH),
                new Box(inner, BAND, Z1, SIZE, inner, Z1 + DEPTH));
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder item = getBuilder(UniversalTrimItem.ID);
        List<Piece> pieces = frame().stream().map(box -> new Piece(box, FACE)).toList();
        GeometryModelWriter.addPieces(item, pieces, false, new GeometryModelWriter.FaceStyle() {
            @Override
            public String texture(Piece piece, Direction face) {
                return face.getAxis() == Direction.Axis.Z ? FACE : EDGE;
            }

            @Override
            public float[] uv(Piece piece, Box part, Direction face) {
                return AssemblyFaceUv.of(part, face, SIZE);
            }
        });
        item.texture(FACE, TrimModelProvider.OAK_LOG).texture(EDGE, TrimModelProvider.OAK_LOG).texture("particle", TrimModelProvider.OAK_LOG);
        ItemModelTransforms.block(item, CENTRE);
        getBuilder(LegacyContent.WINDOW_TRIM_ID).parent(new ModelFile.UncheckedModelFile(modLoc("item/" + UniversalTrimItem.ID)));
    }
}
