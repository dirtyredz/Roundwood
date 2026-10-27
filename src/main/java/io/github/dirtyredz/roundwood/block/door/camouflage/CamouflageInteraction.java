package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Right-clicks on a door's corners (classified by {@code selection/DoorHitRegion}). One material covers all four corners of the assembly.
 * <ul>
 *     <li>Corner, not sneaking, block item in the main hand: apply it (or replace the current one);
 *     an unacceptable block item is refused without toggling, consuming or placing it.</li>
 *     <li>Corner, main hand empty, block item in the offhand: the main hand passes, so the offhand
 *     applies it.</li>
 *     <li>Filled corner, sneaking, both hands empty: clear it and give the paid item back. (Forge
 *     skips block use for a sneaking player holding anything; only the restyling gestures are let
 *     through, and {@code interaction/DoorInteraction} routes those before camouflage.)</li>
 *     <li>Anything else, including every click on the leaf: the ordinary toggle ({@code null} here).</li>
 * </ul>
 * Survival consumes one item for the whole assembly and keeps it as the refund; creative consumes
 * nothing and stores no refund. Permission and a complete, loaded assembly are checked before
 * anything is consumed. The client mirrors the decision and only predicts the arm swing.
 */
public final class CamouflageInteraction {
    private CamouflageInteraction() {
    }

    /**
     * Handles a click if it is a camouflage gesture. Returns {@code null} when the caller should
     * toggle the door instead (never for the offhand: the main hand already chose).
     */
    @Nullable
    public static InteractionResult use(AssemblyDoorBlock block, BlockState state, DoorHitRegion.Region region, Level level,
                                        BlockPos pos, Player player, InteractionHand hand) {
        boolean corner = region == DoorHitRegion.Region.CORNER;
        boolean sneaking = player.isSecondaryUseActive();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (hand == InteractionHand.OFF_HAND) {
            return corner && !sneaking && off.getItem() instanceof BlockItem
                    ? apply(block, state, level, pos, player, off) : InteractionResult.PASS;
        }
        if (!corner) {
            return null;
        }
        if (main.getItem() instanceof BlockItem) {
            return sneaking ? null : apply(block, state, level, pos, player, main);
        }
        if (main.isEmpty() && off.getItem() instanceof BlockItem && !sneaking) {
            return InteractionResult.PASS; // the offhand's turn
        }
        if (main.isEmpty() && off.isEmpty() && sneaking && anchorEntity(block, state, level, pos)
                .flatMap(RoundDoorBlockEntity::camouflage).isPresent()) {
            return clear(block, state, level, pos, player);
        }
        return null;
    }

    private static InteractionResult apply(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                           ItemStack stack) {
        if (anchorEntity(block, state, level, pos).map(e -> e.arch().isPresent() || !e.archEditable()).orElse(false)) return InteractionResult.CONSUME;
        Optional<BlockState> material = CamouflageMaterial.fromStack(stack);
        if (material.isEmpty()) {
            return InteractionResult.CONSUME; // refused: no toggle, nothing consumed or placed
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Optional<RoundDoorBlockEntity> entity = editable(block, state, level, pos, player);
        if (entity.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        Optional<CamouflageData> previous = entity.get().camouflage();
        if (previous.isPresent() && previous.get().material() == material.get()) {
            return InteractionResult.CONSUME; // same material: nothing to do
        }
        boolean creative = player.getAbilities().instabuild;
        CamouflageData next = creative ? CamouflageData.free(material.get()) : CamouflageData.paid(material.get(), stack);
        entity.get().setCamouflage(next);
        if (!creative) {
            stack.shrink(1);
        }
        previous.ifPresent(old -> CamouflageDrops.giveBack(player, old));
        SoundType sound = material.get().getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult clear(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Optional<RoundDoorBlockEntity> entity = editable(block, state, level, pos, player);
        if (entity.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        Optional<CamouflageData> previous = entity.get().camouflage();
        entity.get().setCamouflage(null);
        previous.ifPresent(old -> {
            CamouflageDrops.giveBack(player, old);
            SoundType sound = old.material().getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        });
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.CONSUME;
    }

    /** The anchor entity, if the player may edit and the assembly is whole and loaded. */
    private static Optional<RoundDoorBlockEntity> editable(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos,
                                                           Player player) {
        if (!player.mayBuild() || !level.mayInteract(player, pos)) {
            return Optional.empty();
        }
        return anchorEntity(block, state, level, pos);
    }

    private static Optional<RoundDoorBlockEntity> anchorEntity(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)) {
            return Optional.empty();
        }
        return level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity
                ? Optional.of(entity) : Optional.empty();
    }
}
