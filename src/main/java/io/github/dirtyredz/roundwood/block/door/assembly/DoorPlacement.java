package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.RoundDoorItem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Places a complete assembly or nothing. The clicked (or replaceable) cell becomes the lower-left
 * part; every cell is checked before any is written. Writes go through ordinary {@code setBlock}
 * calls, so Forge's block-snapshot capture fires one multi-place event for the four cells and rolls
 * all of them (and the item count) back if it is cancelled. The stack's appearance is installed on
 * the new anchor inside the same transaction, so a rollback discards it with the cells.
 */
public final class DoorPlacement {
    private DoorPlacement() {
    }

    public static InteractionResult place(RoundDoorItem item, BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        RoundDoorBlock block = item.door();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection();
        BlockPos anchor = context.getClickedPos().immutable();
        DoorLayout.Assembly assembly = new DoorLayout.Assembly(anchor, facing,
                DoorHingePlacement.choose(block, context, anchor, facing));
        Optional<Map<DoorLayout.Part, BlockState>> plan = preflight(block, context, assembly);
        if (plan.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS; // the server writes the cells; nothing is predicted
        }
        ItemStack stack = context.getItemInHand();
        install(level, assembly, plan.get(), AppearanceStacks.read(stack));

        Player player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, anchor, stack);
        }
        BlockState placed = plan.get().get(DoorLayout.Part.LOWER_LEFT);
        SoundType sound = placed.getSoundType(level, anchor, player);
        level.playSound(player, anchor, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, anchor, GameEvent.Context.of(player, placed));
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /** Checks all four cells; returns the states to write, or empty if any check fails. */
    public static Optional<Map<DoorLayout.Part, BlockState>> preflight(RoundDoorBlock block, BlockPlaceContext context,
                                                                DoorLayout.Assembly assembly) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);

        boolean powered = false;
        for (BlockPos pos : assembly.positions()) {
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.isLoaded(pos)) {
                return Optional.empty();
            }
            if (player != null && (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) {
                return Optional.empty();
            }
            if (!level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, context.getClickedFace()))) {
                return Optional.empty();
            }
            if (!level.getFluidState(pos).isEmpty()) {
                return Optional.empty(); // doors are never waterlogged
            }
            powered |= level.hasNeighborSignal(pos);
        }

        Map<DoorLayout.Part, BlockState> states = new EnumMap<>(DoorLayout.Part.class);
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
            BlockPos pos = assembly.pos(part);
            BlockState state = assembly.stateFor(part, block, powered, powered);
            if (!level.isUnobstructed(state, pos, collision)) {
                return Optional.empty();
            }
            if (part.half == DoubleBlockHalf.LOWER
                    && !state.canSurvive(level, pos)) {
                return Optional.empty(); // each column needs a sturdy floor
            }
            states.put(part, state);
        }
        return Optional.of(states);
    }

    /**
     * Places a complete assembly without a player or item (the showcase builder, other commands):
     * the same bounds, loaded, replaceable, dry, unobstructed and floor checks, then the same single
     * write per cell. Returns false, writing nothing, if any check fails. The cells are left unpowered,
     * in the requested open state.
     */
    public static boolean placeAt(Level level, RoundDoorBlock block, DoorLayout.Assembly assembly, boolean open) {
        return placeAt(level, block, assembly, open, block.defaultAppearance());
    }

    /** As {@link #placeAt(Level, RoundDoorBlock, DoorLayout.Assembly, boolean)}, in {@code appearance}. */
    public static boolean placeAt(Level level, RoundDoorBlock block, DoorLayout.Assembly assembly, boolean open,
                                  Appearance appearance) {
        Map<DoorLayout.Part, BlockState> states = new EnumMap<>(DoorLayout.Part.class);
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
            BlockPos pos = assembly.pos(part);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.isLoaded(pos)
                    || !level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
                return false;
            }
            BlockState state = assembly.stateFor(part, block, open, false);
            if (!level.isUnobstructed(state, pos, CollisionContext.empty())
                    || part.half == DoubleBlockHalf.LOWER && !state.canSurvive(level, pos)) {
                return false;
            }
            states.put(part, state);
        }
        install(level, assembly, states, appearance);
        return true;
    }

    private static void install(Level level, DoorLayout.Assembly assembly, Map<DoorLayout.Part, BlockState> states,
                                Appearance appearance) {
        AssemblyMutationBatch.run(level, assembly.positions(), () -> {
            for (DoorLayout.Part part : DoorLayout.Part.values()) {
                level.setBlock(assembly.pos(part), states.get(part), Block.UPDATE_ALL);
            }
        });
        if (level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity) {
            entity.setAppearance(appearance);
        }
    }
}
