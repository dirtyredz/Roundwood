package io.github.dirtyredz.roundwood.data.client.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.data.client.ItemModelTransforms;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.joml.Vector3f;

import io.github.dirtyredz.roundwood.client.door.leaf.DoorLeafModels;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import java.util.ArrayList;
import net.minecraft.core.Direction;

/**
 * The doors' inventory models, one per design: the whole closed leaf (left hinge) face-on, from one
 * template per design carrying the display transforms (two blocks tall, so both draw at half a
 * block's scale, centred on the leaf), bound to oak; the client retextures its panel per stack
 * appearance, keyed by design.
 */
public class DoorItemModelProvider extends ItemModelProvider {
    public DoorItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood door item models";
    }

    /** The item template of {@code design} ({@code template_round_door}, {@code template_arched_door}). */
    static String template(DoorDesign design) {
        return DoorModelProvider.TEMPLATE + design.id();
    }

    @Override
    protected void registerModels() {
        for (DoorDesign design : DoorDesign.values()) {
            ItemModelBuilder template = getBuilder(template(design))
                    .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + DoorModelProvider.template(design, DoorHingeSide.LEFT))));
            if (design.doubleLeaf()) {
                var pieces = new ArrayList<Piece>();
                for (DoorHingeSide side : DoorHingeSide.values()) for (var p : DoorGeometry.leaf(design, side))
                    for (var box : GeometryModelWriter.splitAtTiles(p.box())) pieces.add(new Piece(DoorLeafModels.modelBox(design, box), p.texture()));
                template = getBuilder(template(design));
                GeometryModelWriter.addPieces(template, pieces, false,
                        new GeometryModelWriter.FaceStyle() {
            @Override public int tintIndex(Piece piece, Direction face) { return io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareSlot.marker(piece.texture()); }
                            @Override public float[] uv(Piece piece,
                                    Box part, Direction face) {
                                return DoorGeometry.PANEL.equals(piece.texture()) ? AssemblyFaceUv.of(
                                        DoorLeafModels.authoredBox(design, part), face, design.width(), design.height(), Math.max(design.width(), design.height())) : io.github.dirtyredz.roundwood.geometry.FaceUv.tiled(DoorLeafModels.authoredBox(design, part), face);
                            }
                        });
            }
            // The leaf's middle, in model pixels.
            ItemModelTransforms.assembly(template, new Vector3f(design.width() / 2F - (design.doubleLeaf() ? 16 : 0), (float)(design.height() * DoorLeafModels.modelYScale(design) / 2) - (design.doubleLeaf() ? 16 : 0),
                    (float) DoorGeometry.PIVOT_Z), Math.max(32, design.width()), design.height(), (float)(1 / DoorLeafModels.modelYScale(design)));
            getBuilder(design.id()).parent(template)
                    .texture(DoorGeometry.PANEL, modLoc(DoorModelProvider.OAK_PANEL))
                    .texture("hinges", modLoc("block/brass"))
                    .texture("knobs", modLoc("block/brass"))
                    .texture("particle", modLoc(DoorModelProvider.OAK_PANEL));
        }
    }
}
