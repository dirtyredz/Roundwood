package io.github.dirtyredz.roundwood.client.render.world;

import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Payload/load/removal are event driven. Only satellite arrival/replacement needs bounded polling. */
@EventBusSubscriber(modid = Roundwood.MOD_ID, value = Dist.CLIENT)
public final class AssemblyRenderLifecycle {
    private static final Set<AppearanceAnchorBlockEntity> ANCHORS = Collections.newSetFromMap(new IdentityHashMap<>());
    private AssemblyRenderLifecycle() {}
    public static void bind() {
        AssemblyRenderUpdates.bind((entity, loaded) -> { if (loaded) ANCHORS.add(entity); else ANCHORS.remove(entity); });
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        for (var entity : ANCHORS) {
            AssemblyRenderUpdates.refreshMembers(entity);
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        ANCHORS.removeIf(entity -> entity.getLevel() == event.getLevel());
    }
}
