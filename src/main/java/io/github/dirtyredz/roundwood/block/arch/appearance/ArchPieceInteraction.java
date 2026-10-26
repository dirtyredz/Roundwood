package io.github.dirtyredz.roundwood.block.arch.appearance;

import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimActions;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimTarget;
import io.github.dirtyredz.roundwood.block.attachment.interaction.HeldTrimTool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A piece's right-click router (0.14.0), following the window and trim convention: no action needs
 * sneaking and sneaking does the same (the sneak-use bridge asks {@link #claims}, the same decision);
 * the main hand wins, the offhand acts only while the main hand is empty; a handled or refused click
 * never falls through to the item (no plank is placed against the piece). Where the hit lands is
 * {@link ArchPieceTrimTarget}'s decision:
 * <table>
 *     <tr><th>Item</th><th>Where</th><th>Action</th></tr>
 *     <tr><td>Universal Trim (any id)</td><td>anywhere</td><td>fit the nearer face's casing (occupied: free no-op)</td></tr>
 *     <tr><td>Arch Sill / Cross Beams</td><td>anywhere</td><td>refused with a hint, nothing used</td></tr>
 *     <tr><td>shears</td><td>a face</td><td>remove that face's casing (none: free no-op)</td></tr>
 *     <tr><td>planks, masonry, dye</td><td>a face with a casing</td><td>restyle that casing</td></tr>
 *     <tr><td>planks, masonry, dye</td><td>elsewhere</td><td>restyle the piece</td></tr>
 *     <tr><td>anything else</td><td>anywhere</td><td>the item's ordinary use</td></tr>
 * </table>
 * Arch pieces have no glass, tint, swing, camouflage or redstone action.
 */
public final class ArchPieceInteraction {
    /** What a click does. */
    public enum Action {
        FIT_TRIM, REFUSE_SILL, REFUSE_BEAMS, REMOVE_TRIM, RESTYLE_TRIM, RESTYLE_BODY, PASS
    }

    private ArchPieceInteraction() {
    }

    public static InteractionResult use(ArchPieceBlock block, BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit) {
        var fitContext = new net.minecraft.world.item.context.UseOnContext(player, hand, hit);
        if (io.github.dirtyredz.roundwood.block.arch.fitting.ArchDoorFitting.claims(fitContext)) return io.github.dirtyredz.roundwood.block.arch.fitting.ArchDoorFitting.use(fitContext);
        ArchPieceTrimTarget.Hit target = ArchPieceTrimTarget.of(state, pos, hit);
        HeldTrimTool tool = HeldTrimTool.of(player, hand);
        return switch (decide(state, level, pos, tool, target)) {
            case FIT_TRIM -> ArchPieceTrimActions.fit(level, pos, state, player, tool.stack(), target.face());
            case REFUSE_BEAMS -> AppearanceActions.refuse(level, player, "message.roundwood.beams_window_only");
            case REFUSE_SILL -> AppearanceActions.refuse(level, player, "message.roundwood.sill_upper_arch_only");
            case REMOVE_TRIM -> ArchPieceTrimActions.remove(level, pos, state, player, hand, tool.stack(), target.face());
            case RESTYLE_TRIM -> AppearanceActions.apply(level, player, tool.donor().orElseThrow(),
                    () -> ArchPieceTrimActions.target(level, pos, state, target.face()));
            case RESTYLE_BODY -> AppearanceActions.apply(level, player, tool.donor().orElseThrow(),
                    () -> ArchPieceAppearance.target(block, state, level, pos));
            case PASS -> InteractionResult.PASS;
        };
    }

    /** True when this click is one of the piece's own actions: the sneak-use bridge lets exactly these reach {@link #use}. */
    public static boolean claims(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (io.github.dirtyredz.roundwood.block.arch.fitting.ArchDoorFitting.claims(new net.minecraft.world.item.context.UseOnContext(player, hand, hit))) return true;
        return decide(state, level, pos, HeldTrimTool.of(player, hand), ArchPieceTrimTarget.of(state, pos, hit)) != Action.PASS;
    }

    /** The action for this click: the held tool ({@link HeldTrimTool}, acting hand included) and where it lands. */
    public static Action decide(BlockState state, BlockGetter level, BlockPos pos, HeldTrimTool tool, ArchPieceTrimTarget.Hit target) {
        boolean onFace = target.region() == ArchPieceTrimTarget.Region.FACE;
        return switch (tool.kind()) {
            case ATTACHMENT -> switch (tool.slot().orElseThrow()) {
                case PERIMETER -> Action.FIT_TRIM;
                case SILL -> Action.REFUSE_SILL;
                case CROSS_BEAMS -> Action.REFUSE_BEAMS;
            };
            case SHEARS -> onFace ? Action.REMOVE_TRIM : Action.PASS;
            case DONOR -> onFace && ArchPieceAppearance.ANCHORS.anchor(level, pos, state).map(anchor -> anchor.trims().has(target.face())).orElse(false)
                    ? Action.RESTYLE_TRIM : Action.RESTYLE_BODY;
            case OTHER -> Action.PASS;
        };
    }
}
