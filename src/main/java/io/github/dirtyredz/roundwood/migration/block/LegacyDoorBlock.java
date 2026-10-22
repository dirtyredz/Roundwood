package io.github.dirtyredz.roundwood.migration.block;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.migration.LegacyAppearances;
import io.github.dirtyredz.roundwood.migration.LegacyChunkMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.function.Supplier;

/**
 * A 0.4.0 door id kept as a hidden alias (deprecated). It behaves as a round door in its historical
 * finish until it is converted, a tick after it appears (placement, structure, command) or soon after
 * its chunk loads ({@link LegacyChunkMigration}); its scheduled tick tries the conversion before the
 * ordinary integrity cleanup. Picks and drops are already the canonical door.
 */
@Deprecated
public class LegacyDoorBlock extends RoundDoorBlock {
    private final LegacyAppearances.Entry entry;
    private final Supplier<RoundDoorBlock> canonical;

    public LegacyDoorBlock(Properties properties, LegacyAppearances.Entry entry, Supplier<RoundDoorBlock> canonical,
                           Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType) {
        super(properties, BlockSetType.OAK, entityType);
        this.entry = entry;
        this.canonical = canonical;
    }

    public RoundDoorBlock canonical() {
        return canonical.get();
    }

    @Override
    public Appearance defaultAppearance() {
        return entry.appearance();
    }

    @Override
    public Item familyItem() {
        return canonical.get().asItem();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!LegacyChunkMigration.convertOnTick(level, pos, state)) {
            super.tick(state, level, pos, random);
        }
    }
}
