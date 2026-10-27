package io.github.dirtyredz.roundwood.client.trim;

import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.client.render.RingModels;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps the compatibility frame's original directional cull lists. */
public final class TrimWorldModel {
    private TrimWorldModel() {}
    public static BakedModel model(BlockState state, AssemblyRenderSnapshot snapshot) {
        return RingModels.quarter(RingModels.look(snapshot.appearance()), snapshot.assembly().facing(), state.getValue(RoundTrimBlock.PART));
    }
}
