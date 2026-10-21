package io.github.dirtyredz.roundwood.data.client.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimGeometry;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimItem;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

import java.util.List;

/**
 * The window trim's inventory model: the round window's trim ring (diameters 16 and 12), two pixels
 * deep, face-on, in oak stripped log. Front and back spread one image over the 16-pixel ring
 * ({@link AssemblyFaceUv}, which for a tiled wood equals the default UVs), so the client can retexture
 * it to a masonry or painted face as well as a wood ({@code client/window/WindowTrimModels}).
 */
public class WindowTrimItemModelProvider extends ItemModelProvider {
    static final String OAK_LOG = "minecraft:block/stripped_oak_log";
    private static final Vector3f CENTRE = new Vector3f(8, 8, 8);

    public WindowTrimItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood window trim item model";
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder item = getBuilder(WindowTrimItem.ID);
        int width = (WindowProfile.SIZE - WindowProfile.ROUND_OPENING) / 2;
        List<Piece> ring = PixelCircle.ring(WindowProfile.SIZE, width).stream()
                .map(rect -> new Piece(rect.extrude(7, 9), WindowTrimGeometry.FACE)).toList();
        GeometryModelWriter.addPieces(item, ring, false, new GeometryModelWriter.FaceStyle() {
            @Override
            public String texture(Piece piece, Direction face) {
                return face.getAxis() == Direction.Axis.Z ? WindowTrimGeometry.FACE : WindowTrimGeometry.EDGE;
            }

            @Override
            public float[] uv(Piece piece, Box part, Direction face) {
                return AssemblyFaceUv.of(part, face, WindowProfile.SIZE);
            }
        });
        item.texture(WindowTrimGeometry.FACE, OAK_LOG).texture(WindowTrimGeometry.EDGE, OAK_LOG).texture("particle", OAK_LOG);
        ItemModelTransforms.block(item, CENTRE);
    }
}
