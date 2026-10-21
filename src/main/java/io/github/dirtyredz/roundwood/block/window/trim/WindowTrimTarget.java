package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * One face's window trim as an {@link AppearanceActions.Target}, so restyling a trim follows the same
 * permission, payment and refusal rules as every other restyle (0.8.0: per face; empty while that
 * face has no trim, or while the window holds trims it does not understand).
 */
public final class WindowTrimTarget {
    private WindowTrimTarget() {
    }

    public static AppearanceActions.Target of(RoundWindowBlockEntity window, AttachmentFace face) {
        return new AppearanceActions.Target() {
            @Override
            public Optional<Appearance> current() {
                return window.trimsEditable() ? window.trims().get(face) : Optional.empty();
            }

            @Override
            public boolean mayEdit(Player player) {
                return window.mayEdit(player);
            }

            @Override
            public void set(Appearance appearance) {
                if (window.getLevel() != null) {
                    WindowTrimActions.restyle(window.getLevel(), window.getBlockPos(), window, face, appearance);
                }
            }

            @Override
            public BlockPos origin() {
                return window.getBlockPos();
            }
        };
    }
}
