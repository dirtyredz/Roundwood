package io.github.dirtyredz.roundwood.client.arch.archway;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.client.render.world.CellLayers;
import io.github.dirtyredz.roundwood.client.render.world.StaticModelSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** The compatibility archway's {@link StaticModelSource}: one cell's frame and casings. */
public final class ArchwayWorldModel implements StaticModelSource {
    public static final ArchwayWorldModel SOURCE = new ArchwayWorldModel();
    private ArchwayWorldModel() {}
    @Override public List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        var assembly = (ArchwayLayout.Assembly) snapshot.assembly();
        var part = state.getValue(ArchwayBlock.PART);
        return CellLayers.of(ArchwayModels.body(assembly.design(), part, assembly.facing(), ArchwayModels.look(snapshot.appearance())),
                snapshot.trims(), (face, look) -> ArchwayModels.casing(assembly.design(), face, part, assembly.facing(), ArchwayModels.look(look)));
    }
}
