package io.github.dirtyredz.roundwood.migration.block;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Supplier;

/** Retains the historical state encoding until the lossless cell transaction succeeds. */
public final class RetiredRoundWindowBlock extends RoundWindowBlock {
    public RetiredRoundWindowBlock(Properties properties, Supplier<BlockEntityType<RoundWindowBlockEntity>> type) {
        super(properties, WindowPattern.CROSS, type);
    }
    @Override public Item familyItem() { return ModItems.window(WindowDesign.plain(profile())); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this) && !level.restoringBlockSnapshots) level.scheduleTick(pos, this, 1);
    }
}
