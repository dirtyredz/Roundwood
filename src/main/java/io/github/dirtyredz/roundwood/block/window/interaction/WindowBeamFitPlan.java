package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.interaction.HeldTrimTool;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/** The same local fit decision for the server and the preview; contains no mutation or payment. */
public record WindowBeamFitPlan(BlockPos pos, WindowDesign design, WindowPose pose, WindowHitTarget target) {
    public static Optional<WindowBeamFitPlan> of(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isLoaded(pos) || !player.mayBuild() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), player.getItemInHand(hand))
                || !HeldTrimTool.of(player, hand).attachment(TrimSlot.CROSS_BEAMS)) return Optional.empty();
        var state = level.getBlockState(pos);
        var window = WindowPaneState.entity(level, pos);
        if (!(state.getBlock() instanceof WindowPaneBlock block) || window == null
                || block.design().pattern() != WindowPattern.PLAIN || !window.trimsEditable(TrimSlot.CROSS_BEAMS)
                || window.trims(TrimSlot.CROSS_BEAMS).any()) return Optional.empty();
        return WindowHitTarget.of(state, pos, hit.getLocation(), hit.getDirection(), player.getEyePosition(),
                window.trims(), window.trims(TrimSlot.SILL)).filter(target -> target.region() != WindowHitRegions.Region.NONE)
                .map(target -> new WindowBeamFitPlan(pos.immutable(), new WindowDesign(block.profile(), WindowPattern.CROSS),
                        block.pose(state), target));
    }
}
