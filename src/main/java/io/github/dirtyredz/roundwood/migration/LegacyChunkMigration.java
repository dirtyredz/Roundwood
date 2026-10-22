package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.migration.block.LegacyDoorBlock;
import io.github.dirtyredz.roundwood.migration.block.LegacyTrimBlock;
import io.github.dirtyredz.roundwood.migration.block.LegacyWindowBlock;
import io.github.dirtyredz.roundwood.migration.conversion.DoorTrimConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyCells;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyDoorConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyTrimConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyWindowConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
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
        Block block = state.getBlock();
        return block instanceof LegacyDoorBlock || block instanceof LegacyTrimBlock || block instanceof LegacyWindowBlock;
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

    /** One entry per object: the anchor for doors and trims, the cell itself for windows. */
    static BlockPos key(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LegacyDoorBlock) {
            return DoorLayout.of(pos, state).anchor();
        }
        if (state.getBlock() instanceof LegacyTrimBlock) {
            return TrimLayout.of(pos, state).anchor();
        }
        return pos.immutable();
    }
}
