package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import io.github.dirtyredz.roundwood.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Round door blockstates and models. The leaf is drawn whole by the anchor's block-entity renderer,
 * so every door state uses one empty world model that only carries the break particle. Two leaf
 * templates (hinge left / right) hold the geometry once; each finish binds textures to them. The
 * item models reuse the left-hinge template (see {@link RoundwoodItemModelProvider}).
 */
public class DoorModelProvider extends BlockStateProvider {
    public static final String LEAF_TEMPLATE = "template_round_door_leaf_";

    public DoorModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood door blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        BlockModelBuilder leftTemplate = template(DoorHingeSide.LEFT);
        BlockModelBuilder rightTemplate = template(DoorHingeSide.RIGHT);
        for (Finish finish : FinishCatalog.ALL) {
            bind(models().getBuilder(DoorLeafModels.path(finish.doorId(), DoorHingeSide.LEFT)), leftTemplate, finish);
            bind(models().getBuilder(DoorLeafModels.path(finish.doorId(), DoorHingeSide.RIGHT)), rightTemplate, finish);
            ModelFile world = models().getBuilder(finish.doorId()).texture("particle", finish.panelTexture());
            // Every state (facing, half, hinge, column, open, powered, injected properties) draws nothing itself.
            getVariantBuilder(ModBlocks.door(finish)).partialState().setModels(new ConfiguredModel(world));
        }
    }

    private BlockModelBuilder template(DoorHingeSide hinge) {
        BlockModelBuilder builder = models().getBuilder(LEAF_TEMPLATE + hinge.getSerializedName());
        builder.texture("particle", "#" + DoorGeometry.PANEL);
        return GeometryModelWriter.addPieces(builder, DoorGeometry.leaf(hinge), false);
    }

    private BlockModelBuilder bind(BlockModelBuilder builder, ModelFile template, Finish finish) {
        return builder.parent(template)
                .texture(DoorGeometry.PANEL, finish.panelTexture())
                .texture(DoorGeometry.TRIM, finish.trimTexture())
                .texture(DoorGeometry.BRASS, modLoc("block/brass"))
                .texture("particle", finish.panelTexture());
    }
}
