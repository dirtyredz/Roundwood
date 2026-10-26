package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.client.render.PlacementHighlights;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import io.github.dirtyredz.roundwood.block.door.selection.DoorPlacementOutline;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSwingOutline;
import io.github.dirtyredz.roundwood.client.render.OutlineLines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

/**
 * While a door item is held (0.11.0, both designs), draws where using it would put the door: the
 * leaf's silhouette as it will stand, the cells it will own and the hinge it will turn on. The
 * placement is the server's own decision ({@link DoorPlacement#of}, from a
 * {@link BlockPlaceContext} for the held stack and the door-resolved hit); when it would place
 * nothing, nothing is drawn. Sneaking changes nothing. Nothing here calls a block's or item's use, so
 * whatever an interactive block does with the click is not predicted (as for the window preview).
 * Receives cancelled highlight events (the door highlight cancels its own) and never changes their
 * cancellation. Client-only, on the Forge bus.
 */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DoorPlacementPreview {
    private static final float[] LEAF = {0.35F, 0.8F, 1F, 0.9F};
    private static final float[] CELLS = {0F, 0F, 0F, 0.35F};
    private static final float[] HINGE = {1F, 0.8F, 0.2F, 1F};

    private DoorPlacementPreview() {
    }

    /** The plan for {@code player} aiming at {@code hit}, or empty when no door is held, the hit is refused or nothing would be placed. */
    public static Optional<AssemblyPlacementPlan<DoorLayout.Part, DoorLayout.Assembly>> of(Player player, Level level, BlockHitResult hit) {
        InteractionHand hand = PlacementHighlights.held(player, stack -> stack.getItem() instanceof RoundDoorItem);
        if (hand == null) {
            return Optional.empty();
        }
        Optional<BlockHitResult> resolved = DoorHitResolver.resolve(level, hit);
        if (resolved.isEmpty()) {
            return Optional.empty(); // no loaded owner for the aimed point
        }
        if (io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.claims(new net.minecraft.world.item.context.UseOnContext(player, hand, hit))) return Optional.empty();
        ItemStack stack = player.getItemInHand(hand);
        return DoorPlacement.of(((RoundDoorItem) stack.getItem()).door(),
                new BlockPlaceContext(player, hand, stack, resolved.get()));
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        PlacementHighlights.draw(event, DoorPlacementPreview::of, (minecraft, plan) -> {
            Vec3 camera = event.getCamera().getPosition();
            BlockPos anchor = plan.assembly().anchor();
            Vec3 origin = new Vec3(anchor.getX() - camera.x, anchor.getY() - camera.y, anchor.getZ() - camera.z);
            PoseStack pose = event.getPoseStack();
            VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
            draw(pose, lines, origin, DoorPlacementOutline.footprint(plan.assembly()), CELLS);
            draw(pose, lines, origin, DoorPlacementOutline.silhouette(plan.assembly(), plan.anchorState().getValue(net.minecraft.world.level.block.DoorBlock.OPEN)), LEAF);
            draw(pose, lines, origin, DoorPlacementOutline.hingeAxes(plan.assembly()), HINGE);
            {
                var hit = event.getTarget();
                var face = hit.getDirection();
                var base = Vec3.atLowerCornerOf(hit.getBlockPos());
                double plane = hit.getLocation().subtract(base).get(face.getAxis()) + face.getAxisDirection().getStep() * .002;
                for (var depth : io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values()) {
                    var zone = depth.zone(plan.assembly().facing(), face);
                    Vec3[] points = new Vec3[4];
                    for (int i = 0; i < 4; i++) {
                        double u = i == 1 || i == 2 ? zone.maxU() : zone.minU(), v = i >= 2 ? zone.maxV() : zone.minV();
                        points[i] = (switch (face.getAxis()) {
                            case X -> new Vec3(plane, v, u);
                            case Y -> new Vec3(u, plane, v);
                            case Z -> new Vec3(u, v, plane);
                        }).add(base).subtract(camera);
                    }
                    float brightness = depth == plan.assembly().depth() ? 1 : .35F;
                    for (int i = 0; i < 4; i++) {
                        Vec3 a = points[i], b = points[(i + 1) % 4];
                        OutlineLines.line(pose, lines, a.x, a.y, a.z, b.x, b.y, b.z, brightness, brightness, brightness, .9F);
                    }
                }
            }
        });
    }

    /** Edges in pixels relative to the anchor cell, drawn from the camera-relative {@code origin}. */
    private static void draw(PoseStack pose, VertexConsumer lines, Vec3 origin, List<DoorSwingOutline.Edge> edges, float[] colour) {
        for (DoorSwingOutline.Edge e : edges) {
            OutlineLines.line(pose, lines, origin.x + e.x1() / 16, origin.y + e.y1() / 16, origin.z + e.z1() / 16,
                    origin.x + e.x2() / 16, origin.y + e.y2() / 16, origin.z + e.z2() / 16,
                    colour[0], colour[1], colour[2], colour[3]);
        }
    }
}
