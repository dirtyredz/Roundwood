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
