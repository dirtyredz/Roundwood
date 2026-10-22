package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
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
 * Places a complete assembly or nothing, exactly as its {@link DoorPlacementPlan} says (the plan the
 * client's preview draws). Writes go through ordinary {@code setBlock} calls, so Forge's
 * block-snapshot capture fires one multi-place event for all the cells and rolls all of them (and
 * the item count) back if it is cancelled; the item is charged once, after the writes. The stack's
 * appearance is installed on the new anchor inside the same transaction, so a rollback discards it
 * with the cells.
 */
public final class DoorPlacement {
    private DoorPlacement() {
    }

    public static InteractionResult place(RoundDoorItem item, BlockPlaceContext context) {
        Optional<DoorPlacementPlan> plan = DoorPlacementPlan.of(item.door(), context);
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
     * Places a complete assembly without a player or item (the showcase builder, other commands),
     * through {@link DoorPlacementPlan#at}: returns false, writing nothing, if any check fails. The
     * cells are left unpowered, in the requested open state.
     */
    public static boolean placeAt(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly, boolean open) {
        return placeAt(level, block, assembly, open, block.defaultAppearance());
    }

    /** As {@link #placeAt(Level, AssemblyDoorBlock, DoorLayout.Assembly, boolean)}, in {@code appearance}. */
    public static boolean placeAt(Level level, AssemblyDoorBlock block, DoorLayout.Assembly assembly, boolean open,
                                  Appearance appearance) {
        Optional<DoorPlacementPlan> plan = DoorPlacementPlan.at(level, block, assembly, open);
        plan.ifPresent(p -> install(level, p, appearance));
        return plan.isPresent();
    }

    private static void install(Level level, DoorPlacementPlan plan, Appearance appearance) {
        DoorLayout.Assembly assembly = plan.assembly();
        AssemblyMutationBatch.run(level, assembly.positions(), () -> {
            for (DoorLayout.Part part : assembly.parts()) {
                level.setBlock(assembly.pos(part), plan.states().get(part), Block.UPDATE_ALL);
            }
        });
        if (level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity) {
            entity.setAppearance(appearance);
        }
    }
}
