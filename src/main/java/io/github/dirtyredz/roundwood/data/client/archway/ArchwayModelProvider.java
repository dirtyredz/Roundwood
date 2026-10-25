package io.github.dirtyredz.roundwood.data.client.archway;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * The archways' blockstates (0.12.0). An archway is drawn whole by its anchor's block-entity renderer
 * ({@code client/archway/ArchwayRenderer}, from the frame mask in the archway's look), so every state of
 * a design (facing, part) uses one empty world model that only carries the oak plank break particle.
 */
public class ArchwayModelProvider extends BlockStateProvider {
    /** The oak artwork of the archway's world particle and its oak item model. */
    public static final String OAK_PLANKS = "minecraft:block/oak_planks";

    public ArchwayModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood archway blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        for (ArchwayBlock archway : ModBlocks.archways()) {
            getVariantBuilder(archway).partialState().setModels(new ConfiguredModel(
                    models().getBuilder(archway.design().id()).texture("particle", OAK_PLANKS)));
        }
    }
}
