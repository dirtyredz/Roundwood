package io.github.dirtyredz.roundwood.door;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Set;

/**
 * Marks the cells of an assembly while Roundwood itself rewrites them, so the parts' own callbacks
 * ({@code onRemove}, neighbour reactions) do not treat a half-written assembly as broken. Shared by
 * placement, state changes and removal. Nesting is bounded; a runaway chain is refused and logged.
 */
public final class DoorMutationBatch {
    static final int MAX_DEPTH = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Deque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private record Frame(LevelAccessor level, Set<BlockPos> positions) {
    }

    private DoorMutationBatch() {
    }

    /** True while a batch on this thread is rewriting {@code pos} in {@code level}. */
    public static boolean isMutating(LevelAccessor level, BlockPos pos) {
        for (Frame frame : FRAMES.get()) {
            if (frame.level == level && frame.positions.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Runs {@code mutation} with {@code positions} marked. Returns false if refused for depth. */
    public static boolean run(LevelAccessor level, Collection<BlockPos> positions, Runnable mutation) {
        Deque<Frame> frames = FRAMES.get();
        if (frames.size() >= MAX_DEPTH) {
            LOGGER.warn("Roundwood: refused a door update nested {} deep at {}", frames.size(), positions);
            return false;
        }
        Set<BlockPos> marked = Set.copyOf(positions.stream().map(BlockPos::immutable).toList());
        frames.push(new Frame(level, marked));
        try {
            mutation.run();
        } finally {
            frames.pop();
        }
        return true;
    }
}
