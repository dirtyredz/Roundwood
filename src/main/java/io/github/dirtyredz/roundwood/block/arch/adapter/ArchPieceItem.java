package io.github.dirtyredz.roundwood.block.arch.adapter;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Styled modular product; item use recomputes and installs one complete placement proposal. */
public final class ArchPieceItem extends AppearanceBlockItem {
    public ArchPieceItem(ArchPieceBlock block, Properties properties) {
        super(block, properties, AppearanceFamily.TRIM, Appearance.DEFAULT, null);
    }
    public ArchPieceBlock piece() { return (ArchPieceBlock) getBlock(); }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return io.github.dirtyredz.roundwood.block.arch.fitting.ArchDoorFitting.claims(context) ? io.github.dirtyredz.roundwood.block.arch.fitting.ArchDoorFitting.use(context) : super.useOn(context);
    }
    @Override public InteractionResult place(BlockPlaceContext context) { return ArchPiecePlacement.place(this, context); }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.roundwood.arch_piece").withStyle(ChatFormatting.GRAY));
        if (piece().design().shoulder()) {
            tooltip.add(Component.translatable("tooltip.roundwood.keyhole_shoulder").withStyle(ChatFormatting.GRAY));
        }
    }
}
