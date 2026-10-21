package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jetbrains.annotations.Nullable;

/**
 * Places a whole 2x2 frame in the stack's appearance, fitted to a door or freestanding; all checks
 * and writes live in {@link TrimPlacement}.
 */
public class RoundTrimItem extends AppearanceBlockItem {
    public RoundTrimItem(RoundTrimBlock block, Properties properties) {
        this(block, properties, Appearance.DEFAULT, null);
    }

    /** @param aliasId the 0.4.0 id of a hidden alias placing {@code block} in {@code fallback}, or null */
    public RoundTrimItem(RoundTrimBlock block, Properties properties, Appearance fallback, @Nullable String aliasId) {
        super(block, properties, AppearanceFamily.TRIM, fallback, aliasId);
    }

    public RoundTrimBlock trim() {
        return (RoundTrimBlock) getBlock();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return TrimPlacement.place(this, context);
    }
}
