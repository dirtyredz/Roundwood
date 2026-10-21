package io.github.dirtyredz.roundwood.block.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jetbrains.annotations.Nullable;

/** Places a whole 2x2 assembly in the stack's appearance; all checks and writes live in {@link DoorPlacement}. */
public class RoundDoorItem extends AppearanceBlockItem {
    public RoundDoorItem(RoundDoorBlock block, Properties properties) {
        this(block, properties, Appearance.DEFAULT, null);
    }

    /** @param aliasId the 0.4.0 id of a hidden alias placing {@code block} in {@code fallback}, or null */
    public RoundDoorItem(RoundDoorBlock block, Properties properties, Appearance fallback, @Nullable String aliasId) {
        super(block, properties, AppearanceFamily.DOOR, fallback, aliasId);
    }

    public RoundDoorBlock door() {
        return (RoundDoorBlock) getBlock();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return DoorPlacement.place(this, context);
    }
}
