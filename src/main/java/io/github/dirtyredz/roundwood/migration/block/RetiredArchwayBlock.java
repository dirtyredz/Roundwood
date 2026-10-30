package io.github.dirtyredz.roundwood.migration.block;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;
import io.github.dirtyredz.roundwood.migration.conversion.ArchwayConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyCells;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Frozen source identity: convert before integrity cleanup. */
public final class RetiredArchwayBlock extends ArchwayBlock {
    public RetiredArchwayBlock(Properties properties, ArchwayDesign design,
                               Supplier<BlockEntityType<ArchwayBlockEntity>> entityType) {
        super(properties, design, entityType);
    }
    @Override public void tick(BlockState state, ServerLevel level,
                               BlockPos pos, RandomSource random) {
        var result = ArchwayConversion.convert(level, pos);
        if (result == LegacyCells.Result.INCOMPLETE) super.tick(state, level, pos, random);
        else if (result == LegacyCells.Result.DEFERRED) level.scheduleTick(pos, this, 20);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held, BlockState state,
            Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return io.github.dirtyredz.roundwood.block.BlockUseResults.itemUse(InteractionResult.FAIL);
    }
    @Override public boolean claimsUse(BlockState state, BlockGetter level,
            BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) { return false; }
}
