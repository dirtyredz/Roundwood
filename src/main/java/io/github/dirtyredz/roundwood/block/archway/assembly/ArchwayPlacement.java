package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

/**
 * Places a complete archway or nothing, exactly as its {@link ArchwayPlacementPlan} says (the plan the
 * client's preview draws), like the doors: ordinary {@code setBlock} writes, so Forge's block-snapshot
 * capture fires one multi-place event for all four cells and rolls all of them (and the item count)
 * back if it is cancelled; the item is charged once, after the writes; the stack's appearance goes on
 * the new anchor inside the same transaction.
 */
public final class ArchwayPlacement {
    private ArchwayPlacement() {
    }

    public static InteractionResult place(ArchwayItem item, BlockPlaceContext context) {
        Optional<ArchwayPlacementPlan> plan = ArchwayPlacementPlan.of(item.archway(), context);
        if (plan.isEmpty()) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS; // the server writes the cells; nothing is predicted
        }
        ItemStack stack = context.getItemInHand();
        install(level, plan.get(), AppearanceStacks.read(stack));

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

    /**
     * Places a complete archway without a player or item (the showcase builder, other commands),
     * through {@link ArchwayPlacementPlan#at}: returns false, writing nothing, if any check fails.
     */
    public static boolean placeAt(Level level, ArchwayBlock block, ArchwayLayout.Assembly assembly, Appearance appearance) {
        Optional<ArchwayPlacementPlan> plan = ArchwayPlacementPlan.at(level, block, assembly);
        plan.ifPresent(p -> install(level, p, appearance));
        return plan.isPresent();
    }

    private static void install(Level level, ArchwayPlacementPlan plan, Appearance appearance) {
        ArchwayLayout.Assembly assembly = plan.assembly();
        AssemblyMutationBatch.run(level, assembly.positions(), () -> {
            for (ArchwayPart part : ArchwayPart.values()) {
                level.setBlock(assembly.pos(part), plan.states().get(part), Block.UPDATE_ALL);
            }
        });
        if (level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity entity) {
            entity.setAppearance(appearance);
        }
    }
}
