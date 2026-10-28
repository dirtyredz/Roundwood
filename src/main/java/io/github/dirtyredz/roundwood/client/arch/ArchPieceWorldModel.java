package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.client.render.world.CellLayers;
import io.github.dirtyredz.roundwood.client.render.world.StaticModelSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** The arch-piece family's {@link StaticModelSource}: one cell's body and casings in owning-cell coordinates. */
public final class ArchPieceWorldModel implements StaticModelSource {
    public static final ArchPieceWorldModel SOURCE = new ArchPieceWorldModel();
    private ArchPieceWorldModel() {}
    @Override public List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        var assembly = (ArchPieceLayout.Assembly) snapshot.assembly();
        var part = state.getValue(ArchPieceBlock.CELL);
        return CellLayers.of(ArchPieceModels.body(assembly.design(), assembly.side(), part, assembly.facing(),
                        assembly.back(), assembly.front(), ArchPieceModels.look(snapshot.appearance())), snapshot.trims(),
                (face, look) -> ArchPieceModels.casing(assembly.design(), assembly.side(), face, part, assembly.facing(),
                        assembly.back(), assembly.front(), ArchPieceModels.look(look)));
    }
}
