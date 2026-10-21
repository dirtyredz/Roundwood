package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.appearance.DoorAppearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorBlockEntities;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorStateController;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageInteraction;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitRegion;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimActions;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimItem;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;

import java.util.Optional;

/**
 * Who handles a right-click on a door, in order:
 * <ol>
 *     <li><b>Door trim</b> (0.8.0, before anything else): holding the door trim item, the click is
 *     the item's (it attaches or is refused; the door never toggles). On a trim ring
 *     ({@link DoorTrimTarget#ring}), no sneak needed: shears remove that face's trim (a free no-op on an
 *     untrimmed ring), and planks, masonry or dye restyle an installed ring.</li>
 *     <li><b>Restyling</b> ({@code AppearanceGestures}: sneak + planks, or dye): on the leaf it
 *     re-planks or paints the whole door; on a corner it is refused with a hint to aim at the leaf
 *     (never placing the plank, never toggling).</li>
 *     <li>Main hand empty while the offhand holds a restyling item or the door trim: the main hand
 *     passes, so the offhand acts as above (and the door does not toggle first).</li>
 *     <li><b>Camouflage</b> on the corners ({@link CamouflageInteraction}).</li>
 *     <li>Otherwise the ordinary toggle, once.</li>
 * </ol>
 * The hit region is decided once ({@link DoorHitRegion}) for restyling and camouflage.
 */
public final class DoorInteraction {
    private DoorInteraction() {
    }

    public static InteractionResult use(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit) {
        if (heldTrim(player, hand)) {
            return InteractionResult.PASS; // the door trim item's own use attaches it
        }
        Optional<InteractionResult> ring = ring(block, state, level, pos, player, hand, hit);
        if (ring.isPresent()) {
            return ring.get();
        }
        DoorHitRegion.Region region = DoorHitRegion.classify(state, pos, hit);
        Optional<AppearanceGestures.Gesture> gesture = AppearanceGestures.of(AppearanceFamily.DOOR, player, hand);
        if (gesture.isPresent()) {
            if (region == DoorHitRegion.Region.CORNER) {
                return AppearanceActions.refuse(level, player, "message.roundwood.aim_at_leaf");
            }
            return AppearanceActions.apply(level, player, gesture.get(), () -> DoorAppearance.target(block, state, level, pos));
        }
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && (AppearanceGestures.of(AppearanceFamily.DOOR, player, InteractionHand.OFF_HAND).isPresent()
                || player.getOffhandItem().getItem() instanceof DoorTrimItem)) {
            return InteractionResult.PASS; // the offhand's turn
        }
        InteractionResult camouflage = CamouflageInteraction.use(block, state, region, level, pos, player, hand);
        return camouflage != null ? camouflage : DoorStateController.use(block, state, level, pos, player);
    }

    /** The sneak-use bridge: a trim-ring action, or the door's ordinary restyling gesture. */
    public static boolean claims(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (heldTrim(player, hand)) {
            return false;
        }
        return ringAction(state, level, pos, player, hand, hit) != RingAction.NONE
                || AppearanceGestures.of(AppearanceFamily.DOOR, player, hand).isPresent();
    }

    private enum RingAction {
        NONE, REMOVE, RESTYLE
    }

    private static Optional<InteractionResult> ring(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                                    InteractionHand hand, BlockHitResult hit) {
        RingAction action = ringAction(state, level, pos, player, hand, hit);
        if (action == RingAction.NONE) {
            return Optional.empty();
        }
        AttachmentFace face = DoorTrimTarget.ring(state, pos, hit).orElseThrow();
        ItemStack stack = player.getItemInHand(hand);
        if (action == RingAction.REMOVE) {
            return Optional.of(DoorTrimActions.remove(level, pos, state, player, hand, stack, face, DoorTrimItem.item()));
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        return AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand).map(gesture -> AppearanceActions.apply(level, player,
                gesture, () -> anchor(level, pos, state, block)
                        .map(anchor -> DoorTrimTarget.of(anchor, assembly, level, face, pos))));
    }

    private static RingAction ringAction(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                         BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return RingAction.NONE;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return RingAction.NONE;
        }
        Optional<AttachmentFace> face = DoorTrimTarget.ring(state, pos, hit);
        if (face.isEmpty()) {
            return RingAction.NONE;
        }
        if (stack.getItem() instanceof ShearsItem || stack.is(Tags.Items.SHEARS)) {
            return RingAction.REMOVE;
        }
        boolean installed = DoorBlockEntities.anchor(level, pos, state).map(anchor -> anchor.trims().has(face.get())).orElse(false);
        return installed && AppearanceGestures.donor(AppearanceFamily.TRIM, player, hand).isPresent()
                ? RingAction.RESTYLE : RingAction.NONE;
    }

    private static boolean heldTrim(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) {
            return false;
        }
        return player.getItemInHand(hand).getItem() instanceof DoorTrimItem;
    }

    private static Optional<RoundDoorBlockEntity> anchor(Level level, BlockPos pos, BlockState state, RoundDoorBlock block) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)) {
            return Optional.empty();
        }
        return DoorBlockEntities.anchor(level, pos, state);
    }
}
