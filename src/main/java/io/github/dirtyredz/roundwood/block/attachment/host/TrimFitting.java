package io.github.dirtyredz.roundwood.block.attachment.host;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The one face-trim transaction shared by every host (0.9.0, extracted from the door and window trim
 * actions). Each call is one checked change on the server; the client only predicts the arm swing.
 * The host is looked up only on the server, and a host that cannot be edited (incomplete, unloaded,
 * protected, or holding an unknown payload) changes and charges nothing.
 * <ul>
 *     <li>{@link #fit}: an empty face takes the stack's look and one item is used (none in creative);
 *     an occupied face is a handled, free no-op.</li>
 *     <li>{@link #remove} (shears): only that face comes off; in survival the slot's canonical item in
 *     its look is given back (or dropped) and the shears take one point of wear. An empty face is a
 *     free no-op.</li>
 *     <li>{@link #target}: one face as an {@link AppearanceActions.Target}, so restyling follows the
 *     ordinary permission, payment and refusal rules.</li>
 * </ul>
 */
public final class TrimFitting {
    private TrimFitting() {
    }

    public static InteractionResult fit(Level level, Player player, ItemStack stack, AttachmentFace face,
                                         Supplier<Optional<TrimHost>> host) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Optional<TrimHost> found = host.get();
        if (found.isEmpty() || !found.get().editable() || found.get().trims().has(face)) {
            return InteractionResult.CONSUME;
        }
        Appearance look = AppearanceStacks.read(stack);
        if (!found.get().store(found.get().trims().with(face, look))) {
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        announce(level, player, found.get().origin(), look, true);
        return InteractionResult.CONSUME;
    }

    public static InteractionResult remove(Level level, Player player, InteractionHand hand, ItemStack shears, AttachmentFace face,
                                           Supplier<Optional<TrimHost>> host) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Optional<TrimHost> found = host.get();
        if (found.isEmpty() || !found.get().editable()) {
            return InteractionResult.CONSUME;
        }
        Optional<Appearance> look = found.get().trims().get(face);
        if (look.isEmpty() || !found.get().store(found.get().trims().without(face))) {
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            ItemStack returned = AppearanceStacks.create(found.get().slot().item(), look.get());
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
            shears.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        announce(level, player, found.get().origin(), look.get(), false);
        return InteractionResult.CONSUME;
    }

    /** Fits {@code look} on an empty {@code face} without a player (showcase, fixtures, migration). */
    public static boolean install(TrimHost host, AttachmentFace face, Appearance look) {
        return host.editable() && !host.trims().has(face) && host.store(host.trims().with(face, look));
    }

    /** Replaces an installed face's look, keeping the other face; false when that face has none. */
    public static boolean restyle(TrimHost host, AttachmentFace face, Appearance look) {
        return host.editable() && host.trims().has(face) && host.store(host.trims().with(face, look));
    }

    /** One face as a restyling target: empty while that face has no trim or the host is not editable. */
    public static AppearanceActions.Target target(TrimHost host, AttachmentFace face, Predicate<Player> mayEdit) {
        return new AppearanceActions.Target() {
            @Override
            public Optional<Appearance> current() {
                return host.editable() ? host.trims().get(face) : Optional.empty();
            }

            @Override
            public boolean mayEdit(Player player) {
                return mayEdit.test(player);
            }

            @Override
            public void set(Appearance appearance) {
                restyle(host, face, appearance);
            }

            @Override
            public BlockPos origin() {
                return host.origin();
            }
        };
    }

    private static void announce(Level level, Player player, BlockPos pos, Appearance look, boolean fitted) {
        SoundType sound = BuiltInRegistries.BLOCK.get(look.material()).defaultBlockState().getSoundType();
        level.playSound(null, pos, fitted ? sound.getPlaceSound() : sound.getBreakSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }
}
