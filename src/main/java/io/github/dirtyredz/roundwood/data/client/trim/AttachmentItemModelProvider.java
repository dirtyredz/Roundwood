package io.github.dirtyredz.roundwood.data.client.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.item.ArchSillTrimItem;
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
 * The attachment items' inventory models, flat and face-on, {@link #DEPTH} pixels deep, in oak
 * stripped log. Front and back spread one image over the whole 16-pixel square ({@link AssemblyFaceUv},
 * which for a tiled wood equals the default UVs), so the client can retexture them to any wood, paint
 * or masonry face ({@code client/item/TrimItemModels}).
 * <ul>
 *     <li>Universal Trim (0.9.0): a square frame {@link #SIZE} pixels across with a {@link #BAND}-pixel
 *     band and an open square centre ({@link #frame()}). The hidden {@code window_trim} item model
 *     inherits it; the 0.4.0 trim aliases already inherit {@code item/round_trim}.</li>
 *     <li>Arch Sill (0.10.0): one bar across the full width, {@link #SILL_HEIGHT} pixels high, centred
 *     ({@link #sill()}).</li>
 * </ul>
 * Was {@code UniversalTrimItemModelProvider} (0.9.0).
 */
public class AttachmentItemModelProvider extends ItemModelProvider {
    public static final int SIZE = 16;
    public static final int BAND = 2;
    public static final int DEPTH = 2;
    public static final int SILL_HEIGHT = 4;
    static final String FACE = "face";
    static final String EDGE = "edge";
    private static final double Z1 = (16 - DEPTH) / 2.0;
    private static final Vector3f CENTRE = new Vector3f(8, 8, 8);

    public AttachmentItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood attachment item models";
    }

    /** The Universal Trim's four bands: bottom and top across the full width, the sides between them. */
    public static List<Box> frame() {
        int inner = SIZE - BAND;
        return List.of(
                new Box(0, 0, Z1, SIZE, BAND, Z1 + DEPTH),
                new Box(0, inner, Z1, SIZE, SIZE, Z1 + DEPTH),
                new Box(0, BAND, Z1, BAND, inner, Z1 + DEPTH),
                new Box(inner, BAND, Z1, SIZE, inner, Z1 + DEPTH));
    }

    /** The Arch Sill's bar. */
    public static List<Box> sill() {
        double y1 = (SIZE - SILL_HEIGHT) / 2.0;
        return List.of(new Box(0, y1, Z1, SIZE, y1 + SILL_HEIGHT, Z1 + DEPTH));
    }

    @Override
    protected void registerModels() {
        write(UniversalTrimItem.ID, frame());
        write(ArchSillTrimItem.ID, sill());
        write("cross_beams", List.of(new Box(7.5, 0, Z1, 8.5, 16, Z1 + DEPTH),
                new Box(0, 7.5, Z1, 7.5, 8.5, Z1 + DEPTH), new Box(8.5, 7.5, Z1, 16, 8.5, Z1 + DEPTH)));
        getBuilder(LegacyContent.WINDOW_TRIM_ID).parent(new ModelFile.UncheckedModelFile(modLoc("item/" + UniversalTrimItem.ID)));
    }

    private void write(String id, List<Box> boxes) {
        ItemModelBuilder item = getBuilder(id);
        List<Piece> pieces = boxes.stream().map(box -> new Piece(box, FACE)).toList();
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
        String texture = id.equals("cross_beams") ? "minecraft:block/oak_planks" : TrimModelProvider.OAK_LOG;
        item.texture(FACE, texture).texture(EDGE, texture).texture("particle", texture);
        ItemModelTransforms.block(item, CENTRE);
    }
}
