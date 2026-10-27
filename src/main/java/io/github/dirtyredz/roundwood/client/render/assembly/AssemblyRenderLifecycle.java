package io.github.dirtyredz.roundwood.client.render.assembly;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Payload/load/removal are event driven. Only satellite arrival/replacement needs bounded polling. */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, value = Dist.CLIENT)
public final class AssemblyRenderLifecycle {
    private static final Set<AppearanceAnchorBlockEntity> ANCHORS = Collections.newSetFromMap(new IdentityHashMap<>());
    private AssemblyRenderLifecycle() {}
    public static void bind() {
        AssemblyRenderUpdates.bind((entity, loaded) -> { if (loaded) ANCHORS.add(entity); else ANCHORS.remove(entity); });
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (var entity : ANCHORS) {
            AssemblyRenderUpdates.refreshMembers(entity);
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        ANCHORS.removeIf(entity -> entity.getLevel() == event.getLevel());
    }
}
