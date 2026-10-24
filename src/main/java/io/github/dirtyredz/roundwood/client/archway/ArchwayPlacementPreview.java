package io.github.dirtyredz.roundwood.client.archway;

import io.github.dirtyredz.roundwood.client.render.PlacementHighlights;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPlacement;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayGeometry;
import io.github.dirtyredz.roundwood.client.render.OutlineLines;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

/**
 * While an archway item is held (0.12.0), draws where using it would put the archway: the frame as it
 * will stand (its outline shows the actual opening, ordinary or keyhole) and the four cells it will
 * own. The placement is the server's own decision ({@link ArchwayPlacement#of}, from a
 * {@link BlockPlaceContext} for the held stack and the aimed hit); when it would place nothing,
 * nothing is drawn. Sneaking changes nothing. Like the door and window previews, nothing here calls a
 * block's or item's use. Receives cancelled highlight events and never changes their cancellation.
 * Client-only, on the Forge bus.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArchwayPlacementPreview {
    private static final float[] FRAME = {0.35F, 0.8F, 1F, 0.9F};
    private static final float[] CELLS = {0F, 0F, 0F, 0.35F};

    private ArchwayPlacementPreview() {
    }

    /** The plan for {@code player} aiming at {@code hit}, or empty when no archway is held or nothing would be placed. */
    public static Optional<AssemblyPlacementPlan<ArchwayPart, ArchwayLayout.Assembly>> of(Player player, Level level, BlockHitResult hit) {
        InteractionHand hand = PlacementHighlights.held(player, stack -> stack.getItem() instanceof ArchwayItem);
        if (hand == null) {
            return Optional.empty();
        }
        ItemStack stack = player.getItemInHand(hand);
        return ArchwayPlacement.of(((ArchwayItem) stack.getItem()).archway(), new BlockPlaceContext(player, hand, stack, hit));
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        PlacementHighlights.draw(event, ArchwayPlacementPreview::of, (minecraft, plan) -> {
            ArchwayLayout.Assembly assembly = plan.assembly();
            Vec3 camera = event.getCamera().getPosition();
            BlockPos anchor = assembly.anchor();
            double x = anchor.getX() - camera.x;
            double y = anchor.getY() - camera.y;
            double z = anchor.getZ() - camera.z;
            PoseStack pose = event.getPoseStack();
            VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
            draw(pose, lines, List.of(ArchwayGeometry.footprint(assembly.facing())), x, y, z, CELLS);
            draw(pose, lines, ArchwayGeometry.placed(assembly.design(), assembly.facing()), x, y, z, FRAME);
        });
    }

    /** The outline of {@code boxes} (pixels relative to the anchor cell), merged into one shape so inner seams vanish. */
    private static void draw(PoseStack pose, VertexConsumer lines, List<Box> boxes, double x, double y, double z, float[] colour) {
        VoxelShape shape = VoxelShapeFactory.shape(boxes);
        OutlineLines.edges(pose, lines, shape, x, y, z, colour[0], colour[1], colour[2], colour[3]);
    }
}
