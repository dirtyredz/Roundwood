package io.github.dirtyredz.roundwood.client.archway;

import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public final class ArchwayWorldModel {
    private ArchwayWorldModel() {}
    public static List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot) {
        var assembly = (ArchwayLayout.Assembly) snapshot.assembly();
        var part = state.getValue(ArchwayBlock.PART);
        var models = new ArrayList<BakedModel>();
        models.add(ArchwayModels.body(assembly.design(), part, assembly.facing(), ArchwayModels.look(snapshot.appearance())));
        for (var face : AttachmentFace.values()) snapshot.trims().get(face).ifPresent(look ->
                models.add(ArchwayModels.casing(assembly.design(), face, part, assembly.facing(), ArchwayModels.look(look))));
        return models;
    }
}
