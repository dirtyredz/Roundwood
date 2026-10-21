package io.github.dirtyredz.roundwood.migration.block;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.migration.LegacyAppearances;
import io.github.dirtyredz.roundwood.migration.LegacyChunkMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * A 0.4.0 window id kept as a hidden alias (deprecated): a round window in its historical finish
 * until converted, one tick after it appears or soon after its chunk loads.
 */
@Deprecated
public class LegacyWindowBlock extends RoundWindowBlock {
    private static final int CONVERT_DELAY = 1;

    private final LegacyAppearances.Entry entry;
    private final Supplier<RoundWindowBlock> canonical;

    public LegacyWindowBlock(Properties properties, LegacyAppearances.Entry entry, Supplier<RoundWindowBlock> canonical,
                             Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType) {
        super(properties, WindowPattern.PLAIN, entityType);
        this.entry = entry;
        this.canonical = canonical;
    }

    public RoundWindowBlock canonical() {
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && !oldState.is(this) && !level.restoringBlockSnapshots) {
            level.scheduleTick(pos, this, CONVERT_DELAY);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        LegacyChunkMigration.convertOnTick(level, pos, state);
    }
}
