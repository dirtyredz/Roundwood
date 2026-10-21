package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * The round door's blockstate and models. The leaf is drawn whole by the anchor's block-entity
 * renderer, so every door state uses one empty world model that only carries the break particle.
 * Two leaf templates (hinge left / right) hold the geometry once; the oak leaves bind the oak board
 * panel and the brass, and the client retextures only the panel per appearance. The panel's front
 * and back show its 32 x 32 board image once across the whole leaf ({@link AssemblyFaceUv}), so the
 * vertical boards run unbroken top to bottom; its edges and the brass keep the ordinary tiled
 * mapping. The item model reuses the left-hinge template ({@link DoorItemModelProvider}).
 */
public class DoorModelProvider extends BlockStateProvider {
    public static final String LEAF_TEMPLATE = "template_round_door_leaf_";
    /** The door's panel artwork bound into the oak models: the template the client retextures from. */
    public static final String OAK_PANEL = "block/door_panel_oak";

    /** Panel faces front and back: one image across the whole door. */
    static final GeometryModelWriter.FaceStyle PANEL_FACES = new GeometryModelWriter.FaceStyle() {
        @Override
        public float[] uv(Piece piece, Box part, Direction face) {
            return DoorGeometry.PANEL.equals(piece.texture()) ? AssemblyFaceUv.of(part, face, DoorGeometry.DIAMETER) : null;
        }
    };

    public DoorModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood door blockstates and models";
    }

    @Override
    protected void registerStatesAndModels() {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            BlockModelBuilder template = template(hinge);
            models().getBuilder(DoorLeafModels.path(hinge)).parent(template)
                    .texture(DoorGeometry.PANEL, modLoc(OAK_PANEL))
                    .texture(DoorGeometry.BRASS, modLoc("block/brass"))
                    .texture("particle", modLoc(OAK_PANEL));
        }
        // Every state (facing, half, hinge, column, open, powered, injected properties) draws nothing itself.
        getVariantBuilder(ModBlocks.roundDoor()).partialState().setModels(new ConfiguredModel(worldModel()));
    }

    /** The empty world model every door state (canonical and alias) uses. */
    ModelFile worldModel() {
        return models().getBuilder(DoorLeafModels.DOOR_ID).texture("particle", modLoc(OAK_PANEL));
    }

    private BlockModelBuilder template(DoorHingeSide hinge) {
        BlockModelBuilder builder = models().getBuilder(LEAF_TEMPLATE + hinge.getSerializedName());
        builder.texture("particle", "#" + DoorGeometry.PANEL);
        return GeometryModelWriter.addPieces(builder, DoorGeometry.leaf(hinge), false, PANEL_FACES);
    }
}
