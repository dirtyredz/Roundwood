package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.migration.block.LegacyDoorBlock;
import io.github.dirtyredz.roundwood.migration.block.LegacyTrimBlock;
import io.github.dirtyredz.roundwood.migration.block.LegacyWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Finds 0.4.0 blocks in loaded chunks and converts them, never during the load itself: the chunk-load
 * event only reads the chunk's own sections (skipping any whose palette cannot hold a legacy state)
 * and queues one cell per object; the conversions run on the server thread at the end of the level
 * tick, at most {@link #PER_TICK} per level per tick. Work for a chunk that has unloaded again is
 * dropped (its next load queues it anew). Aliases that appear later (commands, structures, placed
 * 0.4.0 items in old builds) convert through their own scheduled tick ({@link #convertOnTick}).
 */
public final class LegacyChunkMigration {
    static final int PER_TICK = 64;

    /** A queued object: one of its legacy cells, and its identity (the anchor, or the window cell). */
    private record Work(BlockPos pos, BlockPos key) {
    }

    private static final Map<LevelAccessor, Queue<Work>> QUEUES = new ConcurrentHashMap<>();
    private static final Map<LevelAccessor, Set<BlockPos>> QUEUED = new ConcurrentHashMap<>();

    private LegacyChunkMigration() {
    }

    /** True for any hidden 0.4.0 alias block. */
    public static boolean isLegacy(BlockState state) {
        Block block = state.getBlock();
        return block instanceof LegacyDoorBlock || block instanceof LegacyTrimBlock || block instanceof LegacyWindowBlock;
    }

    /** Chunk load: queue (never convert) the legacy objects of a full server chunk. */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        enqueueChunk(event.getLevel(), chunk);
    }

    /** Queues every legacy object whose cell lies in {@code chunk} (reads only the chunk's own sections). */
    public static void enqueueChunk(LevelAccessor level, LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(LegacyChunkMigration::isLegacy)) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (isLegacy(state)) {
                            enqueue(level, new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y,
                                    chunk.getPos().getMinBlockZ() + z), state);
                        }
                    }
                }
            }
        }
    }

    /** End of a server level tick: convert a bounded batch of queued objects. */
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        Queue<Work> queue = QUEUES.get(level);
        if (queue == null) {
            return;
        }
        Set<BlockPos> queued = QUEUED.get(level);
        for (int i = 0; i < PER_TICK; i++) {
            Work work = queue.poll();
            if (work == null) {
                return;
            }
            queued.remove(work.key());
            if (!level.isLoaded(work.pos())) {
                continue; // unloaded again: its next load queues it anew
            }
            BlockState state = level.getBlockState(work.pos());
            LegacyCells.Result result = convert(level, work.pos(), state);
            if (result == LegacyCells.Result.INCOMPLETE || result == LegacyCells.Result.DEFERRED) {
                // The block's own tick decides: integrity retries an unloaded partner or clears an incomplete object with its loot.
                level.scheduleTick(work.pos(), state.getBlock(), 1);
            }
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        QUEUES.remove(event.getLevel());
        QUEUED.remove(event.getLevel());
    }

    /**
     * From a legacy block's scheduled tick: converts its object if it can. True when converted; false
     * leaves the tick to the ordinary integrity check (which defers unloaded cells and clears an
     * incomplete object with its loot).
     */
    public static boolean convertOnTick(ServerLevel level, BlockPos pos, BlockState state) {
        return convert(level, pos, state) == LegacyCells.Result.CONVERTED;
    }

    static LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LegacyDoorBlock) {
            return LegacyDoorConversion.convert(level, pos, state);
        }
        if (block instanceof LegacyTrimBlock) {
            return LegacyTrimConversion.convert(level, pos, state);
        }
        if (block instanceof LegacyWindowBlock) {
            return LegacyWindowConversion.convert(level, pos, state);
        }
        return LegacyCells.Result.NOT_LEGACY;
    }

    private static void enqueue(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockPos key = key(pos, state);
        if (QUEUED.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet()).add(key)) {
            QUEUES.computeIfAbsent(level, l -> new ConcurrentLinkedQueue<>()).add(new Work(pos.immutable(), key));
        }
    }

    /** One entry per object: the anchor for doors and trims, the cell itself for windows. */
    private static BlockPos key(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LegacyDoorBlock) {
            return DoorLayout.of(pos, state).anchor();
        }
        if (state.getBlock() instanceof LegacyTrimBlock) {
            return TrimLayout.of(pos, state).anchor();
        }
        return pos.immutable();
    }
}
