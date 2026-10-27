package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.interaction.HeldTrimTool;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.appearance.DoorAppearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorStateController;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageInteraction;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitRegion;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimActions;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * Who handles a right-click on a door, in order:
 * <ol>
 *     <li><b>Trim attachment</b> (0.8.0, before anything else; 0.9.0: fitted here, not by the item):
 *     holding a Universal Trim (any of its ids), the click fits the face {@link DoorTrimTarget#fit}
 *     picks or is refused; the door never toggles, camouflages or places anything. On a trim ring
 *     ({@link DoorTrimTarget#ring}), no sneak needed: shears remove that face's trim (a free no-op on an
 *     untrimmed ring), and planks, masonry or dye restyle an installed ring.</li>
 *     <li><b>Restyling</b> ({@code AppearanceGestures}: sneak + planks, or dye): on the leaf it
 *     re-planks or paints the whole door; on a corner it is refused with a hint to aim at the leaf
 *     (never placing the plank, never toggling).</li>
 *     <li>Main hand empty while the offhand holds a restyling item or a trim attachment: the main hand
 *     passes, so the offhand acts as above (and the door does not toggle first).</li>
 *     <li><b>Camouflage</b> on the corners ({@link CamouflageInteraction}).</li>
 *     <li>Otherwise the ordinary toggle, once.</li>
 * </ol>
 * The hit region is decided once ({@link DoorHitRegion}) for restyling and camouflage.
 */
public final class DoorInteraction {
    private DoorInteraction() {
    }

    public static InteractionResult use(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit) {
        var fitContext = new net.minecraft.world.item.context.UseOnContext(player, hand, hit);
        if (io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.claims(fitContext)) return io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.use(fitContext);
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().getItem() instanceof io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem) return InteractionResult.PASS;
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareDonors.of(player,InteractionHand.OFF_HAND).isPresent()) return InteractionResult.PASS;
        var hardware = DoorHardwareInteraction.use(level,player,hand,hit);
        if (hardware.isPresent()) return hardware.get();
        var frameHit = io.github.dirtyredz.roundwood.block.door.frame.DoorArchInteraction.hit(level, pos, state, hit);
        if (frameHit.isPresent()) return io.github.dirtyredz.roundwood.block.door.frame.DoorArchInteraction.use(frameHit.get(), level, pos, player, hand);
        HeldTrimTool tool = HeldTrimTool.of(player, hand);
        if (tool.is(HeldTrimTool.Kind.ATTACHMENT)) {
            Optional<AttachmentFace> face = fitFace(state, pos, tool, hit);
            return face.isPresent()
                    ? DoorTrimActions.fit(level, pos, state, player, tool.stack(), face.get())
                    : InteractionResult.FAIL;
        }
        Optional<InteractionResult> ring = ring(block, state, level, pos, player, tool, hit);
        if (ring.isPresent()) {
            return ring.get();
        }
        if (io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareDonors.of(player,hand).isPresent()
                && anchor(level,pos,state,block).map(entity -> entity.swing().moving(level.getGameTime())).orElse(false)) return InteractionResult.CONSUME;
        DoorHitRegion.Region region = DoorHitRegion.classify(state, pos, hit,
                io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageCoverage.of(block.design(),
                        anchor(level, pos, state, block).flatMap(RoundDoorBlockEntity::arch).isPresent()));
        Optional<AppearanceGestures.Gesture> gesture = AppearanceGestures.of(AppearanceFamily.DOOR, player, hand);
        if (gesture.isPresent()) {
            if (region == DoorHitRegion.Region.CORNER) {
                return AppearanceActions.refuse(level, player, "message.roundwood.aim_at_leaf");
            }
            return AppearanceActions.apply(level, player, gesture.get(), () -> DoorAppearance.target(block, state, level, pos));
        }
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && (AppearanceGestures.of(AppearanceFamily.DOOR, player, InteractionHand.OFF_HAND).isPresent()
                || HeldTrimTool.of(player, InteractionHand.OFF_HAND).is(HeldTrimTool.Kind.ATTACHMENT))) {
            return InteractionResult.PASS; // the offhand's turn
        }
        InteractionResult camouflage = CamouflageInteraction.use(block, state, region, level, pos, player, hand);
        return camouflage != null ? camouflage : DoorStateController.use(block, state, level, pos, player);
    }

    /** The sneak-use bridge: a trim fit, a trim-ring action, or the door's ordinary restyling gesture. */
    public static boolean claims(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.claims(new net.minecraft.world.item.context.UseOnContext(player, hand, hit))) return true;
        if (io.github.dirtyredz.roundwood.block.door.frame.DoorArchInteraction.hit(level, pos, state, hit).isPresent()) return true;
        if (io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareDonors.of(player,hand).isPresent()) return true;
        HeldTrimTool tool = HeldTrimTool.of(player, hand);
        if (tool.is(HeldTrimTool.Kind.ATTACHMENT)) {
            return fitFace(state, pos, tool, hit).isPresent();
        }
        return ringAction(state, level, pos, tool, hit) != RingAction.NONE
                || AppearanceGestures.of(AppearanceFamily.DOOR, player, hand).isPresent();
    }

    private enum RingAction {
        NONE, REMOVE, RESTYLE
    }

    private static Optional<InteractionResult> ring(AssemblyDoorBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                                    HeldTrimTool tool, BlockHitResult hit) {
        RingAction action = ringAction(state, level, pos, tool, hit);
        if (action == RingAction.NONE) {
            return Optional.empty();
        }
        AttachmentFace face = DoorTrimTarget.ring(state, pos, hit).orElseThrow();
        if (action == RingAction.REMOVE) {
            return Optional.of(DoorTrimActions.remove(level, pos, state, player, tool.hand(), tool.stack(), face));
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        return Optional.of(AppearanceActions.apply(level, player, tool.donor().orElseThrow(), () -> anchor(level, pos, state, block)
                .map(anchor -> DoorTrimActions.target(anchor, assembly, level, face, pos))));
    }

    /** Shears on a ring remove it (installed or not: a free no-op); a trim donor restyles an installed one. */
    private static RingAction ringAction(BlockState state, BlockGetter level, BlockPos pos, HeldTrimTool tool, BlockHitResult hit) {
        if (!tool.is(HeldTrimTool.Kind.SHEARS) && !tool.is(HeldTrimTool.Kind.DONOR)) {
            return RingAction.NONE;
        }
        Optional<AttachmentFace> face = DoorTrimTarget.ring(state, pos, hit);
        if (face.isEmpty()) {
            return RingAction.NONE;
        }
        if (tool.is(HeldTrimTool.Kind.SHEARS)) {
            return RingAction.REMOVE;
        }
        boolean installed = DoorAppearance.ANCHORS.anchor(level, pos, state).map(anchor -> anchor.trims().has(face.get())).orElse(false);
        return installed ? RingAction.RESTYLE : RingAction.NONE;
    }

    /** The face a held trim fits for this hit; doors take only perimeter trims (an Arch Sill is refused as FAIL, unclaimed). */
    private static Optional<AttachmentFace> fitFace(BlockState state, BlockPos pos, HeldTrimTool tool, BlockHitResult hit) {
        if (!tool.attachment(TrimSlot.PERIMETER)) {
            return Optional.empty();
        }
        return DoorTrimTarget.fit(state, pos, hit);
    }

    private static Optional<RoundDoorBlockEntity> anchor(Level level, BlockPos pos, BlockState state, AssemblyDoorBlock block) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        if (!assembly.loaded(level) || !assembly.complete(level, block)) {
            return Optional.empty();
        }
        return DoorAppearance.ANCHORS.anchor(level, pos, state);
    }
}
