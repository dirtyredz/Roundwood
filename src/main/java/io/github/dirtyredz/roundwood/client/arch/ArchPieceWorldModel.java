package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

/** Shares the BER's body/casing bakers and keeps their owning-cell coordinates. */
public final class ArchPieceWorldModel {
    private ArchPieceWorldModel() {}
    public static List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        var assembly = (ArchPieceLayout.Assembly) snapshot.assembly();
        var part = state.getValue(ArchPieceBlock.CELL);
        var models = new ArrayList<BakedModel>();
        models.add(ArchPieceModels.body(assembly.design(), assembly.side(), part, assembly.facing(),
                assembly.back(), assembly.front(), ArchPieceModels.look(snapshot.appearance())));
        for (var face : AttachmentFace.values()) snapshot.trims().get(face).ifPresent(look ->
                models.add(ArchPieceModels.casing(assembly.design(), assembly.side(), face, part, assembly.facing(),
                        assembly.back(), assembly.front(), ArchPieceModels.look(look))));
        return models;
    }
}
