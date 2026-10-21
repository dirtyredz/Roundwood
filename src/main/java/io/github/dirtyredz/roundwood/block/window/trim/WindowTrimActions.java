package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

/**
 * Fitting, removing and restyling one face's window trim, on the server, as one checked change each
 * (0.8.0). The entity's face trims and the blockstate's {@link WindowPaneBlock#TRIMMED} summary
 * always change together ({@link #store}).
 * <ul>
 *     <li>Fit: an empty face takes the stack's look; one item is used up (none in creative). An
 *     occupied face is a handled no-op: nothing changes, nothing is used.</li>
 *     <li>Remove (shears on the band): only that face's trim comes off; in survival one trim item in
 *     its look is given back (or dropped) and the shears take one point of wear.</li>
 * </ul>
 * Build permission over the cell is checked first; a refusal changes and charges nothing. Trims of an
 * unknown future version are never edited. The client only predicts the arm swing.
 */
public final class WindowTrimActions {
    private WindowTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, Player player, ItemStack stack, AttachmentFace face) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window) || !allowed(level, pos, player)
                || !window.trimsEditable() || window.trims().has(face)) {
            return InteractionResult.CONSUME;
        }
        Appearance look = AppearanceStacks.read(stack);
        store(level, pos, window, window.trims().with(face, look));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        announce(level, player, pos, look, true);
        return InteractionResult.CONSUME;
    }

    /**
     * Fits a trim in {@code look} on {@code face} of the window at {@code pos} without a player (the
     * fit's own write; the showcase and tests build through it). False when there is no window there
     * or that face is taken.
     */
    public static boolean install(Level level, BlockPos pos, AttachmentFace face, Appearance look) {
        if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window) || !window.trimsEditable()
                || window.trims().has(face)) {
            return false;
        }
        store(level, pos, window, window.trims().with(face, look));
        return true;
    }

    public static InteractionResult remove(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack shears,
                                           AttachmentFace face) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window) || !window.trimsEditable()
                || !allowed(level, pos, player)) {
            return InteractionResult.CONSUME;
        }
        Optional<Appearance> look = window.trims().get(face);
        if (look.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        store(level, pos, window, window.trims().without(face));
        if (!player.getAbilities().instabuild) {
            ItemStack returned = AppearanceStacks.create(WindowTrimItem.item(), look.get());
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
            shears.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        announce(level, player, pos, look.get(), false);
        return InteractionResult.CONSUME;
    }

    /** Replaces one face's look (restyling), keeping the other face; false when that face has no trim. */
    public static boolean restyle(Level level, BlockPos pos, RoundWindowBlockEntity window, AttachmentFace face, Appearance look) {
        if (!window.trims().has(face)) {
            return false;
        }
        store(level, pos, window, window.trims().with(face, look));
        return true;
    }

    /** Writes the faces and brings the {@code trimmed} summary into step with them. */
    public static void store(Level level, BlockPos pos, RoundWindowBlockEntity window, FaceTrims next) {
        if (!window.setTrims(next)) {
            return;
        }
        reconcile(level, pos);
    }

    /** Sets {@code trimmed} to "either face present" when it disagrees (clients only: no shape or neighbour change). */
    public static void reconcile(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WindowPaneBlock && level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window
                && window.trimsEditable() && WindowPaneBlock.trimmed(state) != window.trims().any()) {
            level.setBlock(pos, state.setValue(WindowPaneBlock.TRIMMED, window.trims().any()), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean allowed(Level level, BlockPos pos, Player player) {
        return player.mayBuild() && level.mayInteract(player, pos);
    }

    private static void announce(Level level, Player player, BlockPos pos, Appearance look, boolean fitted) {
        SoundType sound = BuiltInRegistries.BLOCK.get(look.material()).defaultBlockState().getSoundType();
        level.playSound(null, pos, fitted ? sound.getPlaceSound() : sound.getBreakSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }
}
