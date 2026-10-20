package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.DoorGeometry;
import io.github.dirtyredz.roundwood.door.DoorLayout;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Round door blockstates and models. Eight geometry templates (four cells, closed and open) carry
 * the elements once; each finish only binds textures. Blockstates turn the north-facing models by
 * facing and ignore {@code powered}.
 */
public class DoorModelProvider extends BlockStateProvider {
    public DoorModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood door blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        Map<DoorLayout.Part, ModelFile> closedTemplates = new EnumMap<>(DoorLayout.Part.class);
        Map<DoorLayout.Part, ModelFile> openTemplates = new EnumMap<>(DoorLayout.Part.class);
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
            closedTemplates.put(part, template(part, false));
            openTemplates.put(part, template(part, true));
        }
        ItemModelBuilder itemTemplate = itemTemplate();

        for (Finish finish : FinishCatalog.ALL) {
            Map<DoorLayout.Part, ModelFile> closed = new EnumMap<>(DoorLayout.Part.class);
            Map<DoorLayout.Part, ModelFile> open = new EnumMap<>(DoorLayout.Part.class);
            for (DoorLayout.Part part : DoorLayout.Part.values()) {
                closed.put(part, bind(models().getBuilder(finish.doorId() + "_" + partName(part)), closedTemplates.get(part), finish));
                open.put(part, bind(models().getBuilder(finish.doorId() + "_" + partName(part) + "_open"), openTemplates.get(part), finish));
            }
            getVariantBuilder(ModBlocks.door(finish)).forAllStatesExcept(state -> {
                DoorLayout.Part part = DoorLayout.partOf(state);
                ModelFile model = (state.getValue(DoorBlock.OPEN) ? open : closed).get(part);
                return ConfiguredModel.builder()
                        .modelFile(model)
                        .rotationY(90 * VoxelShapeFactory.quarterTurns(state.getValue(DoorBlock.FACING)))
                        .build();
            }, DoorBlock.POWERED);

            ItemModelBuilder item = itemModels().getBuilder(finish.doorId()).parent(itemTemplate);
            item.texture("panel", finish.panelTexture())
                    .texture("trim", finish.trimTexture())
                    .texture("brass", modLoc("block/brass"))
                    .texture("particle", finish.panelTexture());
        }
    }

    private BlockModelBuilder template(DoorLayout.Part part, boolean open) {
        BlockModelBuilder builder = models().getBuilder("template_round_door_" + partName(part) + (open ? "_open" : ""));
        builder.texture("particle", "#" + DoorGeometry.PANEL);
        return GeometryModelWriter.addPieces(builder,
                DoorGeometry.pieces(DoorGeometry.column(part.hinge), DoorGeometry.row(part.half), open));
    }

    private BlockModelBuilder bind(BlockModelBuilder builder, ModelFile template, Finish finish) {
        return builder.parent(template)
                .texture(DoorGeometry.PANEL, finish.panelTexture())
                .texture(DoorGeometry.TRIM, finish.trimTexture())
                .texture(DoorGeometry.BRASS, modLoc("block/brass"))
                .texture("particle", finish.panelTexture());
    }

    /** The whole closed disc (32 pixels) shrunk to item size; the model spans 0..32 in x and y. */
    private ItemModelBuilder itemTemplate() {
        ItemModelBuilder builder = itemModels().getBuilder("template_round_door");
        GeometryModelWriter.addPieces(builder, DoorGeometry.assembly());
        builder.texture("particle", "#" + DoorGeometry.PANEL);
        // With scale s, the disc centre (1, 1) block lands at 0.5 * s; translate it back to the slot centre.
        builder.transforms()
                .transform(ItemDisplayContext.GUI).rotation(0, 180, 0).translation(4, -4, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, 0).translation(4, -4, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.GROUND).translation(-2, 1, -2).scale(0.25F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, 180, 0).translation(2, 1, -2).scale(0.25F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 180, 0).translation(2, 1, -2).scale(0.25F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 135, 0).translation(0, 2, 0).scale(0.3F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 135, 0).translation(0, 2, 0).scale(0.3F).end()
                .end();
        return builder;
    }

    private static String partName(DoorLayout.Part part) {
        return part.name().toLowerCase(Locale.ROOT);
    }

}
