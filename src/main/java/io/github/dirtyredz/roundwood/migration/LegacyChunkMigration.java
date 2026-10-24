package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.migration.conversion.DoorTrimConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyCells;
import io.github.dirtyredz.roundwood.migration.block.LegacyAlias;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 0.4.0 alias conversion policy: what is legacy, its object identity, and which conversion
 * rewrites it. Discovery and the bounded per-tick queue are {@link ChunkMigrations}' (0.8.0); aliases
 * that appear later (commands, structures) convert through their own scheduled tick
 * ({@link #convertOnTick}). After an alias frame or door becomes canonical, the 0.7.0 fitted-frame
 * conversion is tried for its pair ({@link DoorTrimConversion}), so the two migrations run in order.
 */
public final class LegacyChunkMigration {
    private LegacyChunkMigration() {
    }

    /** True for any hidden 0.4.0 alias block. */
    public static boolean isLegacy(BlockState state) {
        return state.getBlock() instanceof LegacyAlias;
    }

    /**
     * From a legacy block's scheduled tick: converts its object if it can. True when converted; false
     * leaves the tick to the ordinary integrity check (which defers unloaded cells and clears an
     * incomplete object with its loot).
     */
    public static boolean convertOnTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (convert(level, pos, state) != LegacyCells.Result.CONVERTED) {
            return false;
        }
        DoorTrimConversion.convert(level, pos);
        return true;
    }

    static LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof LegacyAlias alias
                ? alias.convert(level, pos, state) : LegacyCells.Result.NOT_LEGACY;
    }

    /** One entry per object: the anchor for doors and trims, the cell itself for windows. */
    static BlockPos key(BlockPos pos, BlockState state) {
        return state.getBlock() instanceof LegacyAlias alias ? alias.migrationKey(pos, state) : pos.immutable();
    }
}
