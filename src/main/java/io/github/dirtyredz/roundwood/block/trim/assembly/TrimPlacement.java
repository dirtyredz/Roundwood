package io.github.dirtyredz.roundwood.block.trim.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimItem;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Places a whole frame or nothing, through the ordinary item-use path (so Forge's block-snapshot
 * capture fires one multi-place event for the four cells and rolls them all back if it is
 * cancelled). Two ways in:
 * <ul>
 *     <li><b>Fitted</b>: using the item on the front face of a closed round door (sneaking, since an
 *     ordinary click opens the door) puts the frame in the four cells just in front of it, aligned
 *     with the door, whichever quarter was clicked. Any other face, or an open door, is refused
 *     without consuming anything.</li>
 *     <li><b>Freestanding</b>: anywhere else the target cell becomes the lower-left quarter and the
 *     frame extends right and up, facing away from the player.</li>
 * </ul>
 * Every cell is checked first: loaded, inside the world, editable, replaceable and dry, and both
 * lower quarters need a sturdy floor. Entities do not block it (the ring never collides). The
 * stack's appearance goes on the new anchor inside the same transaction.
 */
public final class TrimPlacement {
    private TrimPlacement() {
    }

    public static InteractionResult place(RoundTrimItem item, BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.replacingClickedOnBlock()
                ? context.getClickedPos() : context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState clickedState = level.getBlockState(clicked);
        TrimLayout.Assembly assembly;
        if (clickedState.getBlock() instanceof RoundDoorBlock door) {
            Optional<TrimLayout.Assembly> fitted = fitted(level, door, clickedState, clicked, context.getClickedFace());
            if (fitted.isEmpty()) {
                return InteractionResult.FAIL;
            }
            assembly = fitted.get();
        } else {
            if (!context.canPlace()) {
                return InteractionResult.FAIL;
            }
            assembly = new TrimLayout.Assembly(context.getClickedPos().immutable(), context.getHorizontalDirection());
        }
        RoundTrimBlock block = item.trim();
        Optional<Map<TrimPart, BlockState>> plan = preflight(level, block, assembly, context);
        if (plan.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS; // the server writes the cells; nothing is predicted
        }
        ItemStack stack = context.getItemInHand();
        install(level, assembly, plan.get(), AppearanceStacks.read(stack));

        Player player = context.getPlayer();
        BlockPos anchor = assembly.anchor();
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, anchor, stack);
        }
        BlockState placed = plan.get().get(TrimPart.LOWER_LEFT);
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
     * The frame that fits the door containing {@code clicked}: same facing, in the cells one step
     * towards the door's front. Empty unless the click was on the front face of a complete, loaded,
     * closed door.
     */
    public static Optional<TrimLayout.Assembly> fitted(Level level, RoundDoorBlock door, BlockState state, BlockPos clicked,
                                                     Direction face) {
        DoorLayout.Assembly doorAssembly = DoorLayout.of(clicked, state);
        Direction front = doorAssembly.facing().getOpposite();
        if (face != front || state.getValue(DoorBlock.OPEN) || !doorAssembly.complete(level, door)) {
            return Optional.empty();
        }
        return Optional.of(new TrimLayout.Assembly(doorAssembly.anchor().relative(front), doorAssembly.facing()));
    }

    /** Checks all four cells against a player's placement; returns the states to write, or empty. */
    public static Optional<Map<TrimPart, BlockState>> preflight(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly,
                                                                BlockPlaceContext context) {
        Player player = context.getPlayer();
        for (BlockPos pos : assembly.positions()) {
            if (!inWorld(level, pos)) {
                return Optional.empty();
            }
            if (player != null && (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) {
                return Optional.empty();
            }
            if (!level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, context.getClickedFace()))) {
                return Optional.empty();
            }
        }
        return states(level, block, assembly);
    }

    /**
     * Places a complete frame without a player or item (the showcase builder, other commands): the
     * same bounds, loaded, replaceable, dry and floor checks, then the same single write per cell.
     * Returns false, writing nothing, if any check fails.
     */
    public static boolean placeAt(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly) {
        return placeAt(level, block, assembly, block.defaultAppearance());
    }

    /** As {@link #placeAt(Level, RoundTrimBlock, TrimLayout.Assembly)}, in {@code appearance}. */
    public static boolean placeAt(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly, Appearance appearance) {
        for (BlockPos pos : assembly.positions()) {
            if (!inWorld(level, pos) || !level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }
        Optional<Map<TrimPart, BlockState>> states = states(level, block, assembly);
        states.ifPresent(plan -> install(level, assembly, plan, appearance));
        return states.isPresent();
    }

    private static boolean inWorld(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.isLoaded(pos);
    }

    /** The states to write, if every cell is dry and both lower quarters stand on a sturdy floor. */
    private static Optional<Map<TrimPart, BlockState>> states(Level level, RoundTrimBlock block, TrimLayout.Assembly assembly) {
        Map<TrimPart, BlockState> states = new EnumMap<>(TrimPart.class);
        for (TrimPart part : TrimPart.values()) {
            BlockPos pos = assembly.pos(part);
            BlockState state = assembly.stateFor(part, block);
            if (!level.getFluidState(pos).isEmpty() || !state.canSurvive(level, pos)) {
                return Optional.empty(); // frames are never waterlogged; the lower row needs a floor
            }
            states.put(part, state);
        }
        return Optional.of(states);
    }

    private static void install(Level level, TrimLayout.Assembly assembly, Map<TrimPart, BlockState> states,
                                Appearance appearance) {
        AssemblyMutationBatch.run(level, assembly.positions(), () -> {
            for (TrimPart part : TrimPart.values()) {
                level.setBlock(assembly.pos(part), states.get(part), Block.UPDATE_ALL);
            }
        });
        if (level.getBlockEntity(assembly.anchor()) instanceof RoundTrimBlockEntity entity) {
            entity.setAppearance(appearance);
        }
    }
}
