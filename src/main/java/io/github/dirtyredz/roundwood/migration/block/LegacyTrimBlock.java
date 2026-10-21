package io.github.dirtyredz.roundwood.migration.block;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.migration.LegacyAppearances;
import io.github.dirtyredz.roundwood.migration.LegacyChunkMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * A 0.4.0 trim id kept as a hidden alias (deprecated): a round trim in its historical finish until
 * converted. Its scheduled tick tries the conversion before the ordinary integrity cleanup.
 */
@Deprecated
public class LegacyTrimBlock extends RoundTrimBlock {
    private final LegacyAppearances.Entry entry;
    private final Supplier<RoundTrimBlock> canonical;

    public LegacyTrimBlock(Properties properties, LegacyAppearances.Entry entry, Supplier<RoundTrimBlock> canonical,
                           Supplier<BlockEntityType<RoundTrimBlockEntity>> entityType) {
        super(properties, entityType);
        this.entry = entry;
        this.canonical = canonical;
    }

    public RoundTrimBlock canonical() {
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
