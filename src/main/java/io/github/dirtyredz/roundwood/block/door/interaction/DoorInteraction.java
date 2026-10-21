package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.appearance.DoorAppearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorStateController;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageInteraction;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * Who handles a right-click on a door, in order:
 * <ol>
 *     <li><b>Restyling</b> ({@code AppearanceGestures}: sneak + planks, or dye): on the leaf it
 *     re-planks or paints the whole door; on a corner it is refused with a hint to aim at the leaf
 *     (never placing the plank, never toggling).</li>
 *     <li>Main hand empty while the offhand holds a restyling item: the main hand passes, so the
 *     offhand acts as above (and the door does not toggle first).</li>
 *     <li><b>Camouflage</b> on the corners ({@link CamouflageInteraction}).</li>
 *     <li>Otherwise the ordinary toggle, once.</li>
 * </ol>
 * The hit region is decided once ({@link DoorHitRegion}) for both systems.
 */
public final class DoorInteraction {
    private DoorInteraction() {
    }

    public static InteractionResult use(RoundDoorBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit) {
        DoorHitRegion.Region region = DoorHitRegion.classify(state, pos, hit);
        Optional<AppearanceGestures.Gesture> gesture = AppearanceGestures.of(AppearanceFamily.DOOR, player, hand);
        if (gesture.isPresent()) {
            if (region == DoorHitRegion.Region.CORNER) {
                return AppearanceActions.refuse(level, player, "message.roundwood.aim_at_leaf");
            }
            return AppearanceActions.apply(level, player, gesture.get(), () -> DoorAppearance.target(block, state, level, pos));
        }
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && AppearanceGestures.of(AppearanceFamily.DOOR, player, InteractionHand.OFF_HAND).isPresent()) {
            return InteractionResult.PASS; // the offhand's turn
        }
        InteractionResult camouflage = CamouflageInteraction.use(block, state, region, level, pos, player, hand);
        return camouflage != null ? camouflage : DoorStateController.use(block, state, level, pos, player);
    }
}
