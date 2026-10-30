package io.github.dirtyredz.roundwood.fixture.assembly;

import net.neoforged.fml.common.EventBusSubscriber;
import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Test-only recorder of a placement's observable success effects at one watched cell: the place sound
 * and the {@code BLOCK_PLACE} game event, each logged with a probe the test supplies (for instance the
 * stack's count and the anchor's look at that moment), so a test can tell what had already happened
 * when each effect fired. Keyed by absolute position, so tests running in parallel never mix.
 * Records only; never changes an event.
 */
@EventBusSubscriber(modid = Roundwood.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PlacementEffects {
    private record Watch(Supplier<String> probe, List<String> log) {
    }

    private static final Map<BlockPos, Watch> WATCHED = new ConcurrentHashMap<>();

    private PlacementEffects() {
    }

    /** Starts recording the effects at {@code pos} (absolute), each tagged with {@code probe}'s answer at that moment. */
    public static void watch(BlockPos pos, Supplier<String> probe) {
        WATCHED.put(pos.immutable(), new Watch(probe, new ArrayList<>()));
    }

    /** Stops recording at {@code pos} and returns what was recorded, in order. */
    public static List<String> stop(BlockPos pos) {
        Watch watch = WATCHED.remove(pos.immutable());
        return watch == null ? List.of() : List.copyOf(watch.log());
    }

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getVanillaEvent() == GameEvent.BLOCK_PLACE) {
            record(BlockPos.containing(event.getEventPosition()), "place");
        }
    }

    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtPosition event) {
        record(BlockPos.containing(event.getPosition()), "sound");
    }

    private static void record(BlockPos pos, String what) {
        Watch watch = WATCHED.get(pos);
        if (watch != null) {
            watch.log().add(what + " " + watch.probe().get());
        }
    }
}
