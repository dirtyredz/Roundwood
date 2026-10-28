package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jetbrains.annotations.Nullable;

/**
 * Places a whole door assembly (the round door's 2 x 2, the arched door's 1 x 2) in the stack's
 * appearance; all checks and writes live in {@link DoorPlacement}, from one placement plan.
 */
public class RoundDoorItem extends AppearanceBlockItem {
    public RoundDoorItem(AssemblyDoorBlock block, Properties properties) {
        this(block, properties, Appearance.DEFAULT, null);
    }

    /** @param aliasId the 0.4.0 id of a hidden alias placing {@code block} in {@code fallback}, or null */
    public RoundDoorItem(AssemblyDoorBlock block, Properties properties, Appearance fallback, @Nullable String aliasId) {
        super(block, properties, AppearanceFamily.DOOR, fallback, aliasId);
    }

    public AssemblyDoorBlock door() {
        return (AssemblyDoorBlock) getBlock();
    }

    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (context.getItemInHand().hasTag() && context.getItemInHand().getTag().contains(io.github.dirtyredz.roundwood.block.door.frame.DoorArchStore.TAG)) {
            if (!context.getLevel().isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.roundwood.unknown_arch_item"), true);
            return InteractionResult.FAIL;
        }
        return io.github.dirtyredz.roundwood.block.door.frame.fitting.DoorArchFitting.claims(context) ? io.github.dirtyredz.roundwood.block.door.frame.fitting.DoorArchFitting.use(context) : super.useOn(context);
    }
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return DoorPlacement.place(this, context);
    }
}
