package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceModels;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

/** Static fitted frame, independent of depth and swing, using the BER's exact cell baker. */
public final class DoorStaticModel {
    private DoorStaticModel() {}
    public static List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        var models = new ArrayList<BakedModel>();
        var door = (DoorLayout.Assembly) snapshot.assembly();
        var part = DoorLayout.partOf(state);
        double shift = door.depth().offset() / 16;
        for (var face : AttachmentFace.values()) snapshot.trims().get(face).ifPresent(look -> {
            var casing = DoorTrimModels.casing(door.design(), face, part, door.facing(), DoorTrimModels.look(door.design(), look));
            models.add(io.github.dirtyredz.roundwood.client.material.ModelRetexturing.map(casing, quad ->
                    io.github.dirtyredz.roundwood.client.render.StaticQuadTransform.translate(quad,
                            -door.facing().getStepX()*shift, 0, -door.facing().getStepZ()*shift, quad.getTintIndex()), side -> side));
        });
        snapshot.camouflage().ifPresent(donor -> {
            var corners = io.github.dirtyredz.roundwood.client.camouflage.CamouflageModels.quarter(donor, door.design(), door.facing(), part,
                    io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageCoverage.of(door.design(), snapshot.frame().isPresent()));
            models.add(io.github.dirtyredz.roundwood.client.material.ModelRetexturing.map(corners, quad ->
                    io.github.dirtyredz.roundwood.client.render.StaticQuadTransform.translate(quad,
                            -door.facing().getStepX()*shift, 0, -door.facing().getStepZ()*shift,
                            io.github.dirtyredz.roundwood.client.material.StaticBlockColors.bind(donor, quad.getTintIndex())), side -> side));
        });
        int row = part.row() - DoorArchFitPlan.topOffset(door.design());
        if (snapshot.frame().isEmpty() || row < 0) return models;
        var frame = snapshot.frame().get();
        if (frame.product() != DoorArchFitPlan.topFor(door.design())) return models;
        for (var cell : ArchPieceCell.of(frame.product())) {
            if (cell.row != row || cell.column != part.columnIndex()) continue;
            for (var layer : DoorArchGeometry.Layer.values()) {
                var appearance = layer == DoorArchGeometry.Layer.BODY ? java.util.Optional.of(frame.appearance())
                        : frame.casings().get(layer == DoorArchGeometry.Layer.FRONT_CASING ? AttachmentFace.FRONT : AttachmentFace.BACK);
                appearance.ifPresent(look -> models.add(DoorArchModels.cell(door.design(), door.facing(), frame.thickness(),
                        frame.depth(), layer, cell, ArchPieceModels.look(look))));
            }
        }
        return models;
    }
}
