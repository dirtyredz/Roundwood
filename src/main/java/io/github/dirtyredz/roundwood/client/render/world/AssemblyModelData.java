package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.block.assembly.render.*;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.*;

/** Worker-safe: use only the supplied region and a single immutable published revision. */
public final class AssemblyModelData {
    public static final ModelProperty<AssemblyRenderSnapshot> PROPERTY = new ModelProperty<>();
    private AssemblyModelData() {}
    public static ModelData resolve(BlockAndTintGetter region, BlockPos pos, BlockState state, ModelData original) {
        var assembly = AssemblyRenderUpdates.layout(pos, state);
        if (assembly == null) return original;
        if (!(region.getBlockEntity(assembly.anchor()) instanceof AppearanceAnchorBlockEntity entity)) return ModelData.EMPTY;
        var snapshot = entity.staticRenderPublication().current();
        if (!matches(assembly, snapshot, region::getBlockState)) return ModelData.EMPTY;
        return original.derive().with(PROPERTY, snapshot).build();
    }

    static boolean matches(io.github.dirtyredz.roundwood.block.assembly.Assembly<?> assembly,
                           AssemblyRenderSnapshot snapshot, java.util.function.Function<BlockPos, BlockState> states) {
        if (snapshot == null || !snapshot.available() || !snapshot.assembly().equals(assembly)) return false;
        for (var member : snapshot.members().entrySet()) {
            if (states.apply(member.getKey()) != member.getValue()) return false;
        }
        return true;
    }
}
