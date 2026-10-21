package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Door Trim (the {@code roundwood:round_trim} item, 0.8.0): an attachment for one face of a round
 * door. Used on a closed door it trims the face that was clicked; on an open door it trims the face
 * whose static ring was aimed at, without moving the door; an occupied face is a handled no-op and
 * anything else is refused (no toggle, no camouflage, nothing placed). It keeps its block
 * association with the compatibility frame ({@code block/trim}) so saved frames still name it as
 * their item, but it never places one: ordinary placement is refused. The main hand wins; the offhand
 * fits only while the main hand is empty.
 */
public class DoorTrimItem extends AppearanceBlockItem {
    /** The registry path (kept from 0.4.0; removal looks the item up by it). */
    public static final String ID = "round_trim";

    /** The registered door trim item. */
    public static Item item() {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, ID));
    }

    public DoorTrimItem(Block frame, Properties properties) {
        this(frame, properties, Appearance.DEFAULT, null);
    }

    /** @param aliasId the 0.4.0 id of a hidden alias item ({@code fallback} its historical look), or null */
    public DoorTrimItem(Block frame, Properties properties, Appearance fallback, @Nullable String aliasId) {
        super(frame, properties, AppearanceFamily.TRIM, fallback, aliasId);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (player == null || !(state.getBlock() instanceof RoundDoorBlock)
                || context.getHand() == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return InteractionResult.FAIL;
        }
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(),
                context.isInside());
        Optional<AttachmentFace> face = DoorTrimTarget.fit(state, context.getClickedPos(), hit);
        if (face.isEmpty()) {
            return InteractionResult.FAIL;
        }
        return DoorTrimActions.fit(context.getLevel(), context.getClickedPos(), state, player, context.getItemInHand(), face.get());
    }

    /** Never places a freestanding frame (compatibility frames exist only from older saves and fixtures). */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.roundwood.door_trim").withStyle(ChatFormatting.DARK_GRAY));
    }
}
