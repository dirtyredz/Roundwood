package io.github.dirtyredz.roundwood.client.trim;

import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.client.render.world.StaticModelSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** The compatibility frame's {@link StaticModelSource}; keeps the ring quarter's original directional cull lists. */
public final class TrimWorldModel implements StaticModelSource {
    public static final TrimWorldModel SOURCE = new TrimWorldModel();
    private TrimWorldModel() {}
    @Override public List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        return List.of(RingModels.quarter(RingModels.look(snapshot.appearance()), snapshot.assembly().facing(), state.getValue(RoundTrimBlock.PART)));
    }
    @Override public boolean culledBySide() { return true; }
}
