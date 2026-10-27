package io.github.dirtyredz.roundwood.client.render;

import com.electronwill.nightconfig.core.CommentedConfig;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceRenderer;
import io.github.dirtyredz.roundwood.client.door.DoorArchRenderer;
import net.minecraftforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaticSubmissionTest {
    @Test void migratedRenderersReturnBeforeReadingEntitiesOrSubmittingAnything() {
        StaticRenderingConfig.SPEC.setConfig(CommentedConfig.inMemory());
        StaticRenderingConfig.LEGACY.set(false);
        try {
            // A null entity, dispatcher and buffer are deliberate tripwires for any legacy work.
            new ArchPieceRenderer((AssemblyCellRenderer) null).render(null, 0, null, null, 0, 0);
            new DoorArchRenderer(null).render(null, null, null, null, null);
            new io.github.dirtyredz.roundwood.client.door.DoorTrimRenderer(null).render(null, null, null, null, null);
            io.github.dirtyredz.roundwood.client.camouflage.CamouflageRenderer.render(null, null, null, null, null);
            new io.github.dirtyredz.roundwood.client.trim.RoundTrimRenderer((AssemblyCellRenderer) null).render(null, 0, null, null, 0, 0);
            new io.github.dirtyredz.roundwood.client.archway.ArchwayRenderer((AssemblyCellRenderer) null).render(null, 0, null, null, 0, 0);
            new io.github.dirtyredz.roundwood.client.window.trim.WindowTrimRenderer((net.minecraft.client.renderer.block.ModelBlockRenderer) null).render(null, 0, null, null, 0, 0);
            StaticRenderingConfig.LEGACY.set(true);
            assertThrows(NullPointerException.class, () -> new ArchPieceRenderer((AssemblyCellRenderer) null).render(null, 0, null, null, 0, 0));
            assertTrue(new StaticWorldModel(null).getQuads(null, null, null, ModelData.EMPTY, null).isEmpty());
        } finally { StaticRenderingConfig.LEGACY.set(false); }
    }
}
