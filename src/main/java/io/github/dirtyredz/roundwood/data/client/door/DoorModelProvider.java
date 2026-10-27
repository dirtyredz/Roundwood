package io.github.dirtyredz.roundwood.data.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
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
 * The doors' blockstates and models, per design (the round door, and the arched door since 0.11.0).
 * The leaf is drawn whole by the anchor's block-entity renderer, so every door state uses one empty
 * world model that only carries the break particle. Per design, two leaf templates (hinge left /
 * right) hold the geometry once; the oak leaves bind the oak board panel and the brass, and the
 * client retextures only the panel per appearance. The panel's front and back show the 32 x 32 board
 * image once across the whole leaf ({@link AssemblyFaceUv}; the 16-wide arched leaf its left half,
 * at the same board scale), so the vertical boards run unbroken top to bottom; its edges and the
 * brass keep the ordinary tiled mapping. The item models reuse the left-hinge templates
 * ({@link DoorItemModelProvider}).
 */
public class DoorModelProvider extends BlockStateProvider {
    /** Template names are {@code template_<design id>_leaf_<hinge>}. */
    public static final String TEMPLATE = "template_";
    /** The door's panel artwork bound into the oak models: the template the client retextures from. */
    public static final String OAK_PANEL = "block/door_panel_oak";

    public DoorModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood door blockstates and models";
    }

    /** The template a design's leaf for {@code hinge} is written into. */
    public static String template(DoorDesign design, DoorHingeSide hinge) {
        return TEMPLATE + design.id() + "_leaf_" + hinge.getSerializedName();
    }

    @Override
    protected void registerStatesAndModels() {
        for (DoorDesign design : DoorDesign.values()) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                BlockModelBuilder template = template(design, hinge, panelFaces(design));
                models().getBuilder(DoorLeafModels.path(design, hinge)).parent(template)
                        .texture(DoorGeometry.PANEL, modLoc(OAK_PANEL))
                        .texture("hinges", modLoc("block/brass"))
                    .texture("knobs", modLoc("block/brass"))
                        .texture("particle", modLoc(OAK_PANEL));
            }
        }
        // Every state (facing, half, hinge, column, open, powered, injected properties) draws nothing itself.
        for (AssemblyDoorBlock door : ModBlocks.doors()) {
            getVariantBuilder(door).partialState().setModels(new ConfiguredModel(worldModel(door.design())));
        }
    }

    /** The empty world model every state of {@code design}'s doors (canonical and alias) uses. */
    public ModelFile worldModel(DoorDesign design) {
        return models().getBuilder(design.id()).texture("particle", modLoc(OAK_PANEL));
    }

    /** Panel faces front and back: one board image across the whole leaf, at one texel per pixel. */
    private static GeometryModelWriter.FaceStyle panelFaces(DoorDesign design) {
        return new GeometryModelWriter.FaceStyle() {
            @Override public int tintIndex(Piece piece, Direction face) { return io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareSlot.marker(piece.texture()); }
            @Override
            public float[] uv(Piece piece, Box part, Direction face) {
                return DoorGeometry.PANEL.equals(piece.texture())
                        ? AssemblyFaceUv.of(DoorLeafModels.authoredBox(design, part), face, design.width(), design.height(), Math.max(32, Math.max(design.width(), design.height()))) : io.github.dirtyredz.roundwood.geometry.FaceUv.tiled(DoorLeafModels.authoredBox(design, part), face);
            }
        };
    }

    private BlockModelBuilder template(DoorDesign design, DoorHingeSide hinge, GeometryModelWriter.FaceStyle faces) {
        BlockModelBuilder builder = models().getBuilder(template(design, hinge));
        builder.texture("particle", "#" + DoorGeometry.PANEL);
        return GeometryModelWriter.addPieces(builder, DoorGeometry.leaf(design, hinge).stream().flatMap(p -> GeometryModelWriter.splitAtTiles(p.box()).stream().map(box -> new Piece(DoorLeafModels.modelBox(design, box), p.texture()))).toList(), false, faces);
    }
}
