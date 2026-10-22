package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimFitting;
import io.github.dirtyredz.roundwood.block.attachment.TrimHost;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * The door as a face-trim host (0.9.0): the trims live on the anchor entity of a complete, loaded
 * door, and every cell must be editable by the player (permission over all four cells first). Hands
 * the shared transaction ({@link TrimFitting}) a {@link TrimHost} for that anchor; the door never
 * moves, and a refusal changes and charges nothing.
 */
public final class DoorTrimActions {
    private DoorTrimActions() {
    }

    public static InteractionResult fit(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack,
                                        AttachmentFace face) {
        return TrimFitting.fit(level, player, stack, face, () -> editable(level, pos, state, player));
    }

    public static InteractionResult remove(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand,
                                           ItemStack shears, AttachmentFace face) {
        return TrimFitting.remove(level, player, hand, shears, face, () -> editable(level, pos, state, player));
    }

    /** Fits {@code look} on {@code face} of the complete door at {@code pos} without a player (showcase, fixtures). */
    public static boolean install(Level level, BlockPos pos, AttachmentFace face, Appearance look) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AssemblyDoorBlock door)) {
            return false;
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        return assembly.complete(level, door) && level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity anchor
                && TrimFitting.install(host(anchor, pos), face, look);
    }

    /** One face's ring as a restyling target (permission over every cell of {@code assembly}). */
    public static AppearanceActions.Target target(RoundDoorBlockEntity anchor, DoorLayout.Assembly assembly, Level level,
                                                  AttachmentFace face, BlockPos origin) {
        return TrimFitting.target(host(anchor, origin), face,
                player -> assembly.positions().stream().allMatch(cell -> level.mayInteract(player, cell)));
    }

    /** The anchor of the complete, loaded door at {@code pos}, if {@code player} may edit all of it. */
    private static Optional<TrimHost> editable(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(state.getBlock() instanceof AssemblyDoorBlock door) || !player.mayBuild()) {
            return Optional.empty();
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, door)
                || !assembly.positions().stream().allMatch(cell -> level.mayInteract(player, cell))
                || !(level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity anchor)) {
            return Optional.empty();
        }
        return Optional.of(host(anchor, pos));
    }

    /** The anchor's perimeter trims as a host, announced at {@code origin} (the cell that was used). */
    private static TrimHost host(RoundDoorBlockEntity anchor, BlockPos origin) {
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
