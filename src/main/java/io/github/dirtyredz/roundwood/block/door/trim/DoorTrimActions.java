package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

/**
 * Fitting and removing one face's door trim (0.8.0), on the server, as one checked change each, on
 * the anchor entity of a complete, loaded door. Permission over all four cells first; a refusal
 * changes and charges nothing.
 * <ul>
 *     <li>Fit: an empty face takes the stack's look, one item used (none in creative); an occupied face
 *     is a handled no-op. The door does not move.</li>
 *     <li>Remove (shears on the ring): only that face; in survival one door trim in its look comes
 *     back (or drops) and the shears wear once; an untrimmed ring target is a free no-op.</li>
 * </ul>
 */
public final class DoorTrimActions {
    private DoorTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack,
                                        AttachmentFace face) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Optional<RoundDoorBlockEntity> anchor = editable(level, pos, state, player);
        if (anchor.isEmpty() || anchor.get().trims().has(face)) {
            return InteractionResult.CONSUME;
        }
        Appearance look = AppearanceStacks.read(stack);
        anchor.get().setTrims(anchor.get().trims().with(face, look));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        announce(level, player, pos, look, true);
        return InteractionResult.CONSUME;
    }

    public static InteractionResult remove(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand,
                                           ItemStack shears, AttachmentFace face, Item trimItem) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Optional<RoundDoorBlockEntity> anchor = editable(level, pos, state, player);
        if (anchor.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        Optional<Appearance> look = anchor.get().trims().get(face);
        if (look.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        anchor.get().setTrims(anchor.get().trims().without(face));
        if (!player.getAbilities().instabuild) {
            ItemStack returned = AppearanceStacks.create(trimItem, look.get());
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
            shears.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        announce(level, player, pos, look.get(), false);
        return InteractionResult.CONSUME;
    }

    /** Fits {@code look} on {@code face} of the complete door at {@code pos} without a player (showcase, fixtures). */
    public static boolean install(Level level, BlockPos pos, AttachmentFace face, Appearance look) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RoundDoorBlock door)) {
            return false;
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.complete(level, door) || !(level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity anchor)
                || !anchor.trimsEditable() || anchor.trims().has(face)) {
            return false;
        }
        return anchor.setTrims(anchor.trims().with(face, look));
    }

    /** The anchor of the complete, loaded door at {@code pos}, if {@code player} may edit all of it. */
    private static Optional<RoundDoorBlockEntity> editable(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(state.getBlock() instanceof RoundDoorBlock door) || !player.mayBuild()) {
            return Optional.empty();
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, door)
                || !assembly.positions().stream().allMatch(cell -> level.mayInteract(player, cell))
                || !(level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity anchor) || !anchor.trimsEditable()) {
            return Optional.empty();
        }
        return Optional.of(anchor);
    }

    private static void announce(Level level, Player player, BlockPos pos, Appearance look, boolean fitted) {
        SoundType sound = BuiltInRegistries.BLOCK.get(look.material()).defaultBlockState().getSoundType();
        level.playSound(null, pos, fitted ? sound.getPlaceSound() : sound.getBreakSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }
}
