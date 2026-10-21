package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Places a single-cell window (the round window or an arch piece) through vanilla single-block
 * placement, so the targeted pose ({@code WindowPaneBlock.getStateForPlacement}), pane connections,
 * waterlogging and Forge's placement capture all apply, and puts the stack's appearance on its
 * block entity inside the same placement, before any rollback could happen.
 */
public class RoundWindowItem extends AppearanceBlockItem {
    public RoundWindowItem(WindowPaneBlock block, Properties properties) {
        this(block, properties, Appearance.DEFAULT, null);
    }

    /** @param aliasId the 0.4.0 id of a hidden alias placing {@code block} in {@code fallback}, or null */
    public RoundWindowItem(WindowPaneBlock block, Properties properties, Appearance fallback, @Nullable String aliasId) {
        super(block, properties, AppearanceFamily.WINDOW, fallback, aliasId);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack,
                                                 BlockState state) {
        boolean updated = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity) {
            entity.setAppearance(AppearanceStacks.read(stack));
        }
        return updated;
    }
}
