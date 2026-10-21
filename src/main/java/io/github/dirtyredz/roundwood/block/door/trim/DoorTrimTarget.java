package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Which door trim face a click addresses (0.8.0), from the hit point in the door's authored frame
 * (nudged into the surface hit). The ring is static, so the rule ignores the swing animation:
 * <ul>
 *     <li>{@link #ring}: a hit on a ring's footprint, at the depth of that face's ring or of its
 *     in-cell target (closed: anywhere on the leaf's face over the footprint) - restyling, shears;</li>
 *     <li>{@link #fit}: where the door trim item attaches. Closed door: any hit inside the ring's
 *     outer edge, the face by depth; open door: only a ring target.</li>
 * </ul>
 * Also adapts one face's ring to {@link AppearanceActions.Target} for restyling.
 */
public final class DoorTrimTarget {
    private DoorTrimTarget() {
    }

    /** The face whose ring (installed or not) the hit is on, if any. */
    public static Optional<AttachmentFace> ring(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3 local = authored(state, pos, hit);
        if (!DoorTrimGeometry.onRing(local.x, local.y)) {
            return Optional.empty();
        }
        AttachmentFace face = DoorTrimGeometry.face(local.z);
        if (!state.getValue(DoorBlock.OPEN) || DoorTrimGeometry.inTargetDepth(face, local.z)) {
            return Optional.of(face);
        }
        return Optional.empty();
    }

    /** The face the door trim item would attach to for this hit, if any. */
    public static Optional<AttachmentFace> fit(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (state.getValue(DoorBlock.OPEN)) {
            return ring(state, pos, hit);
        }
        Vec3 local = authored(state, pos, hit);
        return DoorTrimGeometry.withinRing(local.x, local.y) ? Optional.of(DoorTrimGeometry.face(local.z)) : Optional.empty();
    }

    private static Vec3 authored(BlockState state, BlockPos pos, BlockHitResult hit) {
        return DoorLayout.of(pos, state).toAuthored(DoorHitResolver.insidePoint(hit));
    }

    /** One face's ring as an appearance target (empty while that face has none). */
    public static AppearanceActions.Target of(RoundDoorBlockEntity anchor, DoorLayout.Assembly assembly, Level level,
                                              AttachmentFace face, BlockPos origin) {
        return new AppearanceActions.Target() {
            @Override
            public Optional<Appearance> current() {
                return anchor.trimsEditable() ? anchor.trims().get(face) : Optional.empty();
            }

            @Override
            public boolean mayEdit(Player player) {
                return assembly.positions().stream().allMatch(cell -> level.mayInteract(player, cell));
            }

            @Override
            public void set(Appearance appearance) {
                if (anchor.trims().has(face)) {
                    anchor.setTrims(anchor.trims().with(face, appearance));
                }
            }

            @Override
            public BlockPos origin() {
                return origin;
            }
        };
    }
}
