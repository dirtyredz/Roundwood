package io.github.dirtyredz.roundwood.migration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * The bounded, deduplicated work queue every chunk migration shares (extracted from
 * {@code LegacyChunkMigration}, 0.8.0): one entry per object (its key), per level, drained at most
 * {@link #PER_TICK} entries per level per tick on the server thread. Policy lives with the callers:
 * what to queue ({@link ChunkMigrations}) and what to do with an entry.
 */
final class ChunkMigrationQueue {
    static final int PER_TICK = 64;

    /** A queued object: one of its cells, and its identity (the anchor, or the cell itself). */
    record Work(BlockPos pos, BlockPos key) {
    }

    private final Map<LevelAccessor, Queue<Work>> queues = new ConcurrentHashMap<>();
    private final Map<LevelAccessor, Set<BlockPos>> queued = new ConcurrentHashMap<>();

    /** Queues {@code pos} under {@code key}, once while it is waiting. */
    void add(LevelAccessor level, BlockPos pos, BlockPos key) {
        if (queued.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet()).add(key.immutable())) {
            queues.computeIfAbsent(level, l -> new ConcurrentLinkedQueue<>()).add(new Work(pos.immutable(), key.immutable()));
        }
    }

    /** Hands at most {@link #PER_TICK} waiting entries of {@code level} to {@code handler}. */
    void drain(LevelAccessor level, Consumer<Work> handler) {
        Queue<Work> queue = queues.get(level);
        if (queue == null) {
            return;
        }
        Set<BlockPos> keys = queued.get(level);
        for (int i = 0; i < PER_TICK; i++) {
            Work work = queue.poll();
            if (work == null) {
                return;
            }
            keys.remove(work.key());
            handler.accept(work);
        }
    }

    void forget(LevelAccessor level) {
        queues.remove(level);
        queued.remove(level);
    }
}
