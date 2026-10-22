package io.github.dirtyredz.roundwood.fixture.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;

/**
 * Exactly one loaded chunk with every neighbour unloaded: a region ticket of distance 0 keeps the
 * chunk FULL (ticket level 33) while its neighbours only reach the partial statuses the generation
 * of the centre needs (level 34), so {@code Level.isLoaded} is true inside and false in all eight
 * surrounding chunks. A part written at the chunk's edge therefore has a partner in an unloaded
 * chunk, and any read of that partner's cell that is not guarded by {@code isLoaded} would load the
 * neighbour, which {@link #assertIsolated} catches. Close it (try-with-resources) to drop the ticket.
 */
public final class SingleChunk implements AutoCloseable {
    private static final TicketType<ChunkPos> TICKET =
            TicketType.create("roundwood_single_chunk", Comparator.comparingLong(ChunkPos::toLong));

    private final ServerLevel level;
    private final ChunkPos chunk;

    private SingleChunk(ServerLevel level, ChunkPos chunk) {
        this.level = level;
        this.chunk = chunk;
    }

    /** Loads chunk ({@code chunkX}, {@code chunkZ}) as FULL, alone; fails the test if a neighbour is already loaded. */
    public static SingleChunk open(ServerLevel level, int chunkX, int chunkZ) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        ServerChunkCache cache = level.getChunkSource();
        SingleChunk single = new SingleChunk(level, pos);
        single.assertIsolated("before loading");
        cache.addRegionTicket(TICKET, pos, 0, pos);
        level.getChunk(chunkX, chunkZ); // blocks until FULL
        if (!level.hasChunk(chunkX, chunkZ)) {
            throw new GameTestAssertException("chunk " + pos + " did not load");
        }
        single.assertIsolated("after loading the chunk");
        return single;
    }

    /** First block x of the chunk. */
    public int minX() {
        return chunk.getMinBlockX();
    }

    /** Last block x of the chunk. */
    public int maxX() {
        return chunk.getMaxBlockX();
    }

    /** A block z in the middle of the chunk. */
    public int midZ() {
        return chunk.getMinBlockZ() + 8;
    }

    /** True when {@code pos} is in this chunk. */
    public boolean contains(BlockPos pos) {
        return (pos.getX() >> 4) == chunk.x && (pos.getZ() >> 4) == chunk.z;
    }

    /** Fails the test if any chunk around this one is loaded (something loaded it, or it was already there). */
    public void assertIsolated(String when) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && level.hasChunk(chunk.x + dx, chunk.z + dz)) {
                    throw new GameTestAssertException("neighbour chunk " + new ChunkPos(chunk.x + dx, chunk.z + dz)
                            + " of " + chunk + " is loaded " + when);
                }
            }
        }
    }

    @Override
    public void close() {
        level.getChunkSource().removeRegionTicket(TICKET, chunk, 0, chunk);
    }
}
