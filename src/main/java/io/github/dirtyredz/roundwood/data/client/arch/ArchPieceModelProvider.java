package io.github.dirtyredz.roundwood.data.client.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

/** Empty world models for all modular cell states; the anchor BER draws their body and casings. */
public class ArchPieceModelProvider extends BlockStateProvider {
    /** The oak artwork of the piece's world particle and its oak item model. */
    public static final String OAK_PLANKS = "minecraft:block/oak_planks";

    public ArchPieceModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood modular arch blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        for (ArchPieceBlock piece : ModBlocks.archPieces()) {
            getVariantBuilder(piece).partialState().setModels(new ConfiguredModel(
                    models().getBuilder(piece.id()).texture("particle", OAK_PLANKS)));
        }
    }
}
