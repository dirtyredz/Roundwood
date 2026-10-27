package io.github.dirtyredz.roundwood.client.render.assembly;

import io.github.dirtyredz.roundwood.block.assembly.render.*;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.*;

/** Worker-safe: use only the supplied region and a single immutable published revision. */
public final class AssemblyModelData {
    public static final ModelProperty<AssemblyRenderSnapshot> PROPERTY = new ModelProperty<>();
    private AssemblyModelData() {}
    public static ModelData resolve(BlockAndTintGetter region, BlockPos pos, BlockState state, ModelData original) {
        var assembly = AssemblyRenderUpdates.layout(pos, state);
        if (assembly == null) return original;
        if (!(region.getBlockEntity(assembly.anchor()) instanceof AppearanceAnchorBlockEntity entity)) return ModelData.EMPTY;
        var snapshot = entity.staticRenderPublication().current();
        if (snapshot == null || !snapshot.available() || !snapshot.assembly().equals(assembly)) return ModelData.EMPTY;
        for (var member : snapshot.members().entrySet()) {
            if (region.getBlockState(member.getKey()) != member.getValue()) return ModelData.EMPTY;
        }
        return original.derive().with(PROPERTY, snapshot).build();
    }
}
