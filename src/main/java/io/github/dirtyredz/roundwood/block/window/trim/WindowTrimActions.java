package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
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

/**
 * Fitting and removing a window trim, on the server, as one checked change each: the blockstate's
 * {@link WindowPaneBlock#TRIMMED} and the entity's trim look always change together.
 * <ul>
 *     <li>Fit: an untrimmed window takes the stack's look; one item is used up (none in creative). A
 *     window that already has a trim is left alone and nothing is used.</li>
 *     <li>Remove (shears on the band): the trim comes off; in survival one trim item in its look is
 *     given back (or dropped) and the shears take one point of wear.</li>
 * </ul>
 * Build permission over the cell is checked first; a refusal changes and charges nothing. The client
 * only predicts the arm swing.
 */
public final class WindowTrimActions {
    private WindowTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WindowPaneBlock) || WindowPaneBlock.trimmed(state)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window) || !allowed(level, pos, player)) {
            return InteractionResult.FAIL;
        }
        Appearance look = AppearanceStacks.read(stack);
        level.setBlock(pos, state.setValue(WindowPaneBlock.TRIMMED, true), Block.UPDATE_ALL);
        window.setTrim(look);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        announce(level, player, pos, look, true);
        return InteractionResult.CONSUME;
    }

    public static InteractionResult remove(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack shears) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        if (!WindowPaneBlock.trimmed(state) || !(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window)
                || !allowed(level, pos, player)) {
            return InteractionResult.CONSUME;
        }
        Appearance look = window.trim().orElse(Appearance.DEFAULT);
        level.setBlock(pos, state.setValue(WindowPaneBlock.TRIMMED, false), Block.UPDATE_ALL);
        window.clearTrim();
        if (!player.getAbilities().instabuild) {
            ItemStack returned = AppearanceStacks.create(WindowTrimItem.item(), look);
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
            shears.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        announce(level, player, pos, look, false);
        return InteractionResult.CONSUME;
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
