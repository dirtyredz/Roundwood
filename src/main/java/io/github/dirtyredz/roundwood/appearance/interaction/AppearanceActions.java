package io.github.dirtyredz.roundwood.appearance.interaction;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Applies one recognised gesture to one object, on the server: at most one change and one payment.
 * Editing permission across the whole loaded object and its completeness are checked before
 * anything is paid. A change costs one plank, masonry block or dye (nothing in creative) and refunds
 * nothing; an unchanged result is handled but free; a refusal (painting masonry) explains itself and
 * costs nothing. The client only predicts the arm swing.
 */
public final class AppearanceActions {
    /** The object a gesture edits: a whole door or trim (through its anchor), or one window. */
    public interface Target {
        /** The current appearance, or empty when the object cannot be edited now (incomplete, unloaded). */
        Optional<Appearance> current();

        /** Whether {@code player} may change every cell of the object. */
        boolean mayEdit(Player player);

        void set(Appearance appearance);

        /** Where the sound and game event come from. */
        BlockPos origin();
    }

    private AppearanceActions() {
    }

    public static InteractionResult apply(Level level, Player player, AppearanceGestures.Gesture gesture,
                                          Supplier<Optional<Target>> target) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Optional<Target> resolved = target.get();
        if (resolved.isEmpty() || !player.mayBuild() || !resolved.get().mayEdit(player)) {
            return InteractionResult.CONSUME;
        }
        Optional<Appearance> current = resolved.get().current();
        if (current.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        AppearanceChanges.Change change = gesture.apply(current.get());
        switch (change.outcome()) {
            case REFUSED -> refuse(player, "message.roundwood.masonry_unpaintable");
            case UNCHANGED -> {
            }
            case CHANGED -> {
                resolved.get().set(change.next());
                if (!player.getAbilities().instabuild) {
                    gesture.stack().shrink(1);
                }
                announce(level, player, gesture, resolved.get().origin());
            }
        }
        return InteractionResult.CONSUME;
    }

    /** A handled refusal: an action-bar hint for the player, nothing changed or consumed. */
    public static InteractionResult refuse(Level level, Player player, String message) {
        if (!level.isClientSide) {
            refuse(player, message);
        }
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static void refuse(Player player, String message) {
        player.displayClientMessage(Component.translatable(message), true);
    }

    private static void announce(Level level, Player player, AppearanceGestures.Gesture gesture, BlockPos origin) {
        if (gesture.type() == AppearanceGestures.Type.PAINT) {
            level.playSound(null, origin, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            SoundType sound = BuiltInRegistries.BLOCK.get(gesture.material()).defaultBlockState().getSoundType();
            level.playSound(null, origin, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
        }
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, origin);
    }
}
