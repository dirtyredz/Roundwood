package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.migration.block.RetiredArchwayBlock;
import io.github.dirtyredz.roundwood.migration.conversion.ArchwayConversion;
import io.github.dirtyredz.roundwood.migration.conversion.DoorTrimConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyCells;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import io.github.dirtyredz.roundwood.migration.conversion.WindowCrossConversion;

/**
 * Finds content to migrate in loaded chunks and migrates it, never during the load itself (0.8.0,
 * scheduling extracted from {@code LegacyChunkMigration}). The chunk-load event only reads the chunk's
 * own sections (skipping any whose palette cannot hold a candidate) and queues one entry per object
 * ({@link ChunkMigrationQueue}); the work runs at the end of the level tick, at most 64 entries per
 * level per tick. Shared discovery for frozen aliases, retired crossed windows/archways and fitted frames:
 * <ol>
 *     <li>0.4.0 aliases become canonical ({@link LegacyChunkMigration}, unchanged policy);</li>
 *     <li>then a 0.7.0 fitted frame becomes its door's FRONT trim ({@link DoorTrimConversion}). Frames
 *     and doors are both discovered, so whichever of the two loads last retries the pair; a door is
 *     queued only when the cell in front of it may hold a frame.</li>
 * </ol>
 * Retired archways use their four-cell anchor as the queue key and convert before integrity.
 * Work for a chunk that has unloaded again is dropped (its next load queues it anew).
 */
public final class ChunkMigrations {
    private static final ChunkMigrationQueue QUEUE = new ChunkMigrationQueue();

    private ChunkMigrations() {
    }

    /** Chunk load: queue (never convert) the candidates of a full server chunk. */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        enqueueChunk(event.getLevel(), chunk);
    }

    /** Queues every candidate whose cell lies in {@code chunk} (reads only the chunk's own sections). */
    public static void enqueueChunk(LevelAccessor level, LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(ChunkMigrations::candidate)) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (candidate(state)) {
                            BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                            enqueue(level, chunk, pos, state);
                        }
                    }
                }
            }
        }
    }

    /** End of a server level tick: migrate a bounded batch of queued objects. */
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        QUEUE.drain(level, work -> migrate(level, work.pos()));
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        QUEUE.forget(event.getLevel());
    }

    private static void migrate(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return; // unloaded again: its next load queues it anew
        }
        BlockState state = level.getBlockState(pos);
        if (LegacyChunkMigration.isLegacy(state)) {
            LegacyCells.Result result = LegacyChunkMigration.convert(level, pos, state);
            if (result == LegacyCells.Result.INCOMPLETE || result == LegacyCells.Result.DEFERRED) {
                // The block's own tick decides: integrity retries an unloaded partner or clears an incomplete object with its loot.
                level.scheduleTick(pos, state.getBlock(), 1);
                return;
            }
            if (result != LegacyCells.Result.CONVERTED) {
                return;
            }
        }
        if (state.getBlock() instanceof RetiredArchwayBlock) {
            var result = ArchwayConversion.convert(level, pos);
            if (result == LegacyCells.Result.DEFERRED || result == LegacyCells.Result.INCOMPLETE) level.scheduleTick(pos, state.getBlock(), 1);
            return;
        }
        WindowCrossConversion.convert(level, pos);
        DoorTrimConversion.convert(level, pos);
    }

    private static boolean candidate(BlockState state) {
        return state.getBlock() instanceof RetiredArchwayBlock || CrossWindowCompatibility.isRetired(state) || LegacyChunkMigration.isLegacy(state) || state.is(ModBlocks.roundTrim()) || state.is(ModBlocks.roundDoor());
    }

    private static void enqueue(LevelAccessor level, LevelChunk chunk, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof RetiredArchwayBlock) {
            QUEUE.add(level, pos, ArchwayLayout.of(pos, state).anchor());
            return;
        }
        if (CrossWindowCompatibility.isRetired(state)) {
            QUEUE.add(level, pos, pos);
            return;
        }
        if (LegacyChunkMigration.isLegacy(state)) {
            QUEUE.add(level, pos, LegacyChunkMigration.key(pos, state));
            return;
        }
        if (state.is(ModBlocks.roundTrim())) {
            QUEUE.add(level, pos, TrimLayout.of(pos, state).anchor());
            return;
        }
        if (state.getBlock() instanceof RoundDoorBlock && DoorLayout.partOf(state) == DoorLayout.Part.LOWER_LEFT) {
            DoorLayout.Assembly door = DoorLayout.of(pos, state);
            BlockPos front = door.anchor().relative(door.facing().getOpposite());
            boolean sameChunk = SectionPos.blockToSectionCoord(front.getX()) == chunk.getPos().x
                    && SectionPos.blockToSectionCoord(front.getZ()) == chunk.getPos().z;
            if (!sameChunk || chunk.getBlockState(front).is(ModBlocks.roundTrim())) {
                QUEUE.add(level, pos, door.anchor().relative(door.facing().getOpposite()));
            }
        }
    }
}
