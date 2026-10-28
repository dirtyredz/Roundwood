package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Port a host family implements so {@link StaticWorldModel} can draw it from a published snapshot
 * without naming the family. Each family's client package owns one source; the client composition
 * root binds it to the family's block class ({@link StaticModelSources}). Called on chunk-render
 * workers: implementations read only the snapshot and the state they are given.
 */
public interface StaticModelSource {
    /** The owning-cell models {@code state}'s cell draws for {@code snapshot}. */
    List<BakedModel> models(BlockState state, AssemblyRenderSnapshot snapshot);

    /**
     * True when the models carry directional cull lists the chunk builder must honour per side (the
     * compatibility frame's ring quarters). Otherwise every quad is emitted unculled with the
     * null side and directional requests return nothing.
     */
    default boolean culledBySide() { return false; }

    /** The breaking/landing particle sprite, or null for the wrapped model's own. Default: the anchor cell's first model. */
    default @Nullable TextureAtlasSprite particle(AssemblyRenderSnapshot snapshot) {
        var state = snapshot.members().get(snapshot.assembly().anchor());
        if (state == null) return null;
        var models = models(state, snapshot);
        return models.isEmpty() ? null : models.get(0).getParticleIcon(ModelData.EMPTY);
    }
}
