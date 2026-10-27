package io.github.dirtyredz.roundwood.client.window.trim;

import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.block.window.pane.*;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

/** Independent opaque perimeter and sill passes over the existing window snapshot. */
public final class WindowStaticModels {
    private WindowStaticModels() {}
    public static List<BakedModel> models(BlockState state, WindowAppearanceData data) {
        if (!(state.getBlock() instanceof WindowPaneBlock block) || data == null) return List.of();
        var models = new ArrayList<BakedModel>();
        var pose = block.pose(state);
        for (var slot : List.of(TrimSlot.PERIMETER, TrimSlot.SILL)) {
            var trims = slot == TrimSlot.PERIMETER ? data.trims() : data.sills();
            for (var face : AttachmentFace.values()) trims.get(face).ifPresent(look -> {
                var side = WindowFaceIdentity.side(pose, WindowPaneBlock.faceReversed(state), face);
                models.add(WindowTrimWorldModels.model(slot, block.profile(), pose, side, WindowTrimModels.look(look, slot, block.profile())));
            });
        }
        return models;
    }
}
