package io.github.dirtyredz.roundwood.migration.block;

import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyCells;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A frozen alias supplies its canonical destination, object identity and family conversion. */
public interface LegacyAlias extends AppearanceDefaults {
    Block canonical();
    BlockPos migrationKey(BlockPos pos, BlockState state);
    LegacyCells.Result convert(ServerLevel level, BlockPos pos, BlockState state);
}
