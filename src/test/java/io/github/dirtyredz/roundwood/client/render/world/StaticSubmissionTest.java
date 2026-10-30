package io.github.dirtyredz.roundwood.client.render.world;

import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaticSubmissionTest {
    @Test void migratedPiecesHaveNoStaticBlockEntityRenderers() {
        for (String renderer : new String[]{
                "io.github.dirtyredz.roundwood.client.arch.ArchPieceRenderer",
                "io.github.dirtyredz.roundwood.client.archway.ArchwayRenderer",
                "io.github.dirtyredz.roundwood.client.trim.RoundTrimRenderer",
                "io.github.dirtyredz.roundwood.client.window.trim.WindowTrimRenderer",
                "io.github.dirtyredz.roundwood.client.door.DoorArchRenderer",
                "io.github.dirtyredz.roundwood.client.door.DoorTrimRenderer",
                "io.github.dirtyredz.roundwood.client.camouflage.CamouflageRenderer"
        }) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(renderer), renderer);
        }
        assertTrue(new StaticWorldModel(null, null).getQuads(null, null, null, ModelData.EMPTY, null).isEmpty());
    }
}
