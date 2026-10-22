package io.github.dirtyredz.roundwood.block.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * A whole assembly's look as a restyling target, for every family: only a loaded, complete assembly
 * whose anchor cell holds the family's anchor entity; editing needs permission over every cell; the
 * sound and game event come from the clicked cell ({@code origin}), never the anchor.
 */
public final class AssemblyAppearanceTarget {
    private AssemblyAppearanceTarget() {
    }

    /**
     * The look of {@code assembly} (of {@code block}, its anchor entity of {@code type}) as a target, or
     * empty when it is unloaded, incomplete or its anchor entity is missing.
     */
    public static Optional<AppearanceActions.Target> of(Level level, BlockPos origin, Block block, Assembly<?> assembly,
                                                        Class<? extends AppearanceAnchorBlockEntity> type) {
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !(level.getBlockEntity(assembly.anchor()) instanceof AppearanceAnchorBlockEntity entity) || !type.isInstance(entity)) {
            return Optional.empty();
        }
        return Optional.of(new AppearanceActions.Target() {
            @Override
            public Optional<Appearance> current() {
                return Optional.of(entity.appearance());
            }

            @Override
            public boolean mayEdit(Player player) {
                return assembly.mayEditAll(level, player);
            }

            @Override
            public void set(Appearance appearance) {
                entity.setAppearance(appearance);
            }

            @Override
            public BlockPos origin() {
                return origin;
            }
        });
    }
}
