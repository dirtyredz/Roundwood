package io.github.dirtyredz.roundwood.block.assembly.placement;

import io.github.dirtyredz.roundwood.block.assembly.Assembly;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.AppearanceAnchor;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

/**
 * A player placing a whole assembly with its item, for every family: the plan the family decided for
 * this click (the same plan its preview draws) is installed through {@link AssemblyInstallation} with
 * the stack's look, then, only on success: the placed-block criterion, the place sound, the
 * {@code BLOCK_PLACE} game event, one item charged (none in creative) and {@code CONSUME}. No plan, or a
 * refused installation, is {@code FAIL} with none of those. The client only predicts success and
 * writes nothing. Forge's capture around the item's use still fires one multi-place event for all the
 * cells and rolls them back with the item count if it is cancelled.
 */
public final class AssemblyPlacement {
    private AssemblyPlacement() {
    }

    public static <P, A extends Assembly<P>> InteractionResult place(BlockPlaceContext context, Optional<AssemblyPlacementPlan<P, A>> plan,
                                              Class<? extends AppearanceAnchor> anchorType) {
        if (plan.isEmpty()) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS; // the server writes the cells; nothing is predicted
        }
        ItemStack stack = context.getItemInHand();
        if (!AssemblyInstallation.install(level, plan.get(), AppearanceStacks.read(stack), anchorType)) {
            return InteractionResult.FAIL;
        }
        BlockPos anchor = plan.get().assembly().anchor();
        Player player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, anchor, stack);
        }
        BlockState placed = plan.get().anchorState();
        SoundType sound = placed.getSoundType(level, anchor, player);
        level.playSound(player, anchor, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, anchor, GameEvent.Context.of(player, placed));
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
