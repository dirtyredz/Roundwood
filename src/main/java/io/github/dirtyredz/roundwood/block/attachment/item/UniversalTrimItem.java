package io.github.dirtyredz.roundwood.block.attachment.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Universal Trim (0.9.0): the perimeter attachment for one face of any host (a window, a door). The
 * host's router fits it (plain click or sneaking; main hand first, the offhand only while the main
 * hand is empty); the item's own use does nothing, so a click is never fitted twice. Carries a trim
 * appearance (wood, paint or one of the four masonry materials).
 *
 * <p>Registered as the canonical {@code roundwood:round_trim} (the advertised item; it keeps its
 * block association with the compatibility frame, {@code block/trim}, so saved frames still name it,
 * but never places one) and, hidden, as every older id of the same attachment: the 0.4.0 trim aliases
 * in their historical looks and the 0.8.0 {@code window_trim}. Those keep their ids, names and NBT in
 * old inventories and work everywhere; a removed or dropped attachment always comes back as
 * {@code round_trim} ({@link TrimSlot#PERIMETER}).
 */
public class UniversalTrimItem extends AppearanceBlockItem implements TrimAttachmentItem {
    /** The canonical registry path (kept from 0.4.0). */
    public static final String ID = TrimSlot.PERIMETER.itemId();

    public UniversalTrimItem(Block frame, Properties properties) {
        this(frame, properties, Appearance.DEFAULT, null);
    }

    /**
     * @param compatibilityId the registry path of a hidden compatibility item (a 0.4.0 alias with
     *                        {@code fallback} its historical look, or {@code window_trim}), or null
     */
    public UniversalTrimItem(Block frame, Properties properties, Appearance fallback, @Nullable String compatibilityId) {
        super(frame, properties, AppearanceFamily.TRIM, fallback, compatibilityId);
    }

    @Override
    public TrimSlot trimSlot() {
        return TrimSlot.PERIMETER;
    }

    /** Hosts fit in their own router; anywhere else the item does nothing. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.FAIL;
    }

    /** Never places a freestanding frame (compatibility frames exist only from older saves and fixtures). */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.roundwood.universal_trim").withStyle(ChatFormatting.DARK_GRAY));
    }
}
