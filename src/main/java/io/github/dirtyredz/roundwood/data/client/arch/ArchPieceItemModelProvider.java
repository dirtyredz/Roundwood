package io.github.dirtyredz.roundwood.data.client.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceItemGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

/** Full-shape oak icons, with the same legal-bounds translation as the styled runtime model. */
public final class ArchPieceItemModelProvider extends ItemModelProvider {
    public ArchPieceItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }
    @Override public String getName() { return "Roundwood modular arch item models"; }
    @Override protected void registerModels() {
        for (var piece : io.github.dirtyredz.roundwood.registry.ModBlocks.archPieces()) {
            var design = piece.design();
            var depth = io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.MIDDLE;
            var item = getBuilder(piece.id());
            int offset = ArchPieceItemGeometry.offsetX(design);
            for (int column = 0; column < design.columns(); column++) for (int row = 0; row < design.rows(); row++)
                GeometryModelWriter.addSurfaces(item, ArchPieceGeometry.surfaces(design, Side.LEFT, column, row, piece.thickness().back(depth), piece.thickness().front(depth), ""),
                        false, surface -> "donor", column * 16 + offset, row * 16);
            item.texture("donor", ArchPieceModelProvider.OAK_PLANKS).texture("particle", ArchPieceModelProvider.OAK_PLANKS);
            ItemModelTransforms.assembly(item, new Vector3f(design.width() / 2F + offset, design.height() / 2F, 8),
                    design.width(), design.height());
        }
    }
}
