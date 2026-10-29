package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.adapter.DoorRenderExtras;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageCoverage;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchGeometry;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceModels;
import io.github.dirtyredz.roundwood.client.door.arch.DoorArchModels;
import io.github.dirtyredz.roundwood.client.material.AppearanceLooks;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.material.tint.ModelRetexturing;
import io.github.dirtyredz.roundwood.client.material.tint.StaticBlockColors;
import io.github.dirtyredz.roundwood.client.render.world.CellLayers;
import io.github.dirtyredz.roundwood.client.render.world.StaticModelSource;
import io.github.dirtyredz.roundwood.client.render.world.StaticQuadTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * The door family's {@link StaticModelSource}: fitted casings, camouflage corners and the fitted
 * arch's cells, independent of leaf depth and swing (the leaf stays with the animated renderer).
 */
public final class DoorStaticModel implements StaticModelSource {
    public static final DoorStaticModel SOURCE = new DoorStaticModel();
    private DoorStaticModel() {}

    @Override public List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        var door = (DoorLayout.Assembly) snapshot.assembly();
        var extras = DoorRenderExtras.of(snapshot);
        var part = DoorLayout.partOf(state);
        var models = CellLayers.casings(snapshot.trims(), (face, look) -> depthShifted(
                DoorTrimModels.casing(door.design(), face, part, door.facing(), DoorTrimModels.look(door.design(), look)),
                door, tint -> tint));
        extras.camouflage().ifPresent(donor -> {
            var corners = CamouflageModels.quarter(donor, door.design(), door.facing(), part,
                    CamouflageCoverage.of(door.design(), extras.frame().isPresent()));
            models.add(depthShifted(corners, door, tint -> StaticBlockColors.bind(donor, tint)));
        });
        int row = part.row() - DoorArchFitPlan.topOffset(door.design());
        if (extras.frame().isEmpty() || row < 0) return models;
        var frame = extras.frame().get();
        if (frame.product() != DoorArchFitPlan.topFor(door.design())) return models;
        for (var cell : ArchPieceCell.of(frame.product())) {
            if (cell.row != row || cell.column != part.columnIndex()) continue;
            models.addAll(CellLayers.of(arch(door, frame, DoorArchGeometry.Layer.BODY, cell, frame.appearance()), frame.casings(),
                    (face, look) -> arch(door, frame, face == AttachmentFace.FRONT
                            ? DoorArchGeometry.Layer.FRONT_CASING : DoorArchGeometry.Layer.BACK_CASING, cell, look)));
        }
        return models;
    }

    private static BakedModel depthShifted(BakedModel model, DoorLayout.Assembly door, IntUnaryOperator tint) {
        double shift = door.depth().offset() / 16;
        return ModelRetexturing.map(model, quad -> StaticQuadTransform.translate(quad,
                -door.facing().getStepX() * shift, 0, -door.facing().getStepZ() * shift,
                tint.applyAsInt(quad.getTintIndex())), side -> side);
    }

    private static BakedModel arch(DoorLayout.Assembly door, DoorArchAttachment frame, DoorArchGeometry.Layer layer,
                                   ArchPieceCell cell, io.github.dirtyredz.roundwood.appearance.Appearance look) {
        return DoorArchModels.cell(door.design(), door.facing(), frame.thickness(), frame.depth(), layer, cell, ArchPieceModels.look(look));
    }

    /** The fitted arch's wood when present, else the door's: the door's own anchor may have no static model. */
    @Override public TextureAtlasSprite particle(AssemblyRenderSnapshot snapshot) {
        var appearance = DoorRenderExtras.of(snapshot).frame().map(DoorArchAttachment::appearance).orElse(snapshot.appearance());
        return DonorSprites.atlas(AppearanceLooks.planks(appearance));
    }
}
