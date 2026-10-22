package io.github.dirtyredz.roundwood.block.archway.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.host.TrimFitting;
import io.github.dirtyredz.roundwood.block.attachment.host.TrimHost;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * The archway as a face-trim host (0.12.0, like the door): the trims live on the anchor entity of a
 * complete, loaded archway, and the player must be able to edit every cell. Hands the shared
 * transaction ({@link TrimFitting}) a {@link TrimHost} for that anchor, so fitting, payment, removal
 * with its refund and wear, and restyling are the doors' and windows' exactly; a refusal changes and
 * charges nothing.
 */
public final class ArchwayTrimActions {
    private ArchwayTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack,
                                        AttachmentFace face) {
        return TrimFitting.fit(level, player, stack, face, () -> editable(level, pos, state, player));
    }

    public static InteractionResult remove(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand,
                                           ItemStack shears, AttachmentFace face) {
        return TrimFitting.remove(level, player, hand, shears, face, () -> editable(level, pos, state, player));
    }

    /** Fits {@code look} on {@code face} of the complete archway at {@code pos} without a player (showcase, fixtures). */
    public static boolean install(Level level, BlockPos pos, AttachmentFace face, Appearance look) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ArchwayBlock block)) {
            return false;
        }
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
        return assembly.complete(level, block) && level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity anchor
                && TrimFitting.install(host(anchor, pos), face, look);
    }

    /** One face's casing as a restyling target, if the archway is complete and loaded. */
    public static Optional<AppearanceActions.Target> target(Level level, BlockPos pos, BlockState state, AttachmentFace face) {
        if (!(state.getBlock() instanceof ArchwayBlock block)) {
            return Optional.empty();
        }
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !(level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity anchor)) {
            return Optional.empty();
        }
        return Optional.of(TrimFitting.target(host(anchor, pos), face,
                player -> assembly.positions().stream().allMatch(cell -> level.mayInteract(player, cell))));
    }

    /** The anchor of the complete, loaded archway at {@code pos}, if {@code player} may edit all of it. */
    private static Optional<TrimHost> editable(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(state.getBlock() instanceof ArchwayBlock block) || !player.mayBuild()) {
            return Optional.empty();
        }
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)
                || !assembly.positions().stream().allMatch(cell -> level.mayInteract(player, cell))
                || !(level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity anchor)) {
            return Optional.empty();
        }
        return Optional.of(host(anchor, pos));
    }

    /** The anchor's perimeter trims as a host, announced at {@code origin} (the cell that was used). */
    private static TrimHost host(ArchwayBlockEntity anchor, BlockPos origin) {
        return new TrimHost() {
            @Override
            public TrimSlot slot() {
                return TrimSlot.PERIMETER;
            }

            @Override
            public FaceTrims trims() {
                return anchor.trims();
            }

            @Override
            public boolean editable() {
                return anchor.trimsEditable();
            }

            @Override
            public boolean store(FaceTrims next) {
                return anchor.setTrims(next);
            }

            @Override
            public BlockPos origin() {
                return origin;
            }
        };
    }
}
