package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceTargeting;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyPlacementPlan;
import io.github.dirtyredz.roundwood.client.render.OutlineLines;
import io.github.dirtyredz.roundwood.client.render.PlacementHighlights;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/** Read-only placement proposal, landing face and shoulder split. Never invokes a use callback. */
@Mod.EventBusSubscriber(modid = Roundwood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArchPiecePlacementPreview {
    private ArchPiecePlacementPreview() {}

    // Discover by types, not a mapped method name: works in development and the reobfuscated jar.
    private static final Method USE = Arrays.stream(BlockBehaviour.class.getDeclaredMethods())
            .filter(method -> method.getReturnType() == InteractionResult.class
                    && Arrays.equals(method.getParameterTypes(), new Class<?>[]{BlockState.class, Level.class,
                    net.minecraft.core.BlockPos.class, Player.class, net.minecraft.world.InteractionHand.class, BlockHitResult.class}))
            .findFirst().orElseThrow();
    private static final ClassValue<Boolean> PASSIVE = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try { return type.getMethod(USE.getName(), USE.getParameterTypes()).getDeclaringClass() == BlockBehaviour.class; }
            catch (ReflectiveOperationException exception) { return false; }
        }
    };

    /** An overridden callback may consume the click. Suppress success conservatively, without running it. */
    static boolean passive(BlockState state) { return PASSIVE.get(state.getBlock().getClass()); }

    public static Optional<AssemblyPlacementPlan<ArchPieceCell, ArchPieceLayout.Assembly>> of(Player player, Level level, BlockHitResult hit) {
        var hand = PlacementHighlights.held(player, stack -> stack.getItem() instanceof ArchPieceItem);
        if (hand == null || player.isSpectator() || !level.isLoaded(hit.getBlockPos())) return Optional.empty();
        if (!player.isSecondaryUseActive() && !passive(level.getBlockState(hit.getBlockPos()))) return Optional.empty();
        var stack = player.getItemInHand(hand);
        return ArchPiecePlacement.of(((ArchPieceItem) stack.getItem()).piece(), new BlockPlaceContext(player, hand, stack, hit));
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        PlacementHighlights.draw(event, ArchPiecePlacementPreview::of, (client, plan) -> {
            var assembly = plan.assembly();
            var camera = event.getCamera().getPosition();
            var pose = event.getPoseStack();
            var lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
            for (var pos : assembly.positions()) {
                OutlineLines.edges(pose, lines, Shapes.block(), pos.getX() - camera.x, pos.getY() - camera.y,
                        pos.getZ() - camera.z, 0, 0, 0, .4F);
            }
            var anchor = assembly.anchor();
            OutlineLines.edges(pose, lines, VoxelShapeFactory.shape(ArchPieceGeometry.placed(assembly.design(), assembly.side(), assembly.facing())),
                    anchor.getX() - camera.x, anchor.getY() - camera.y, anchor.getZ() - camera.z, .35F, .8F, 1, .9F);
            var hit = event.getTarget();
            var face = hit.getDirection();
            Vec3 origin = Vec3.atLowerCornerOf(hit.getBlockPos());
            double plane = hit.getLocation().subtract(origin).get(face.getAxis()) + face.getAxisDirection().getStep() * .002;
            Vec3[] corners = new Vec3[4];
            for (int i = 0; i < 4; i++) corners[i] = facePoint(face, plane, i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0).add(origin).subtract(camera);
            for (int i = 0; i < 4; i++) line(event, corners[i], corners[(i + 1) % 4]);
            if (assembly.design().shoulder() && face != assembly.facing().getClockWise() && face != assembly.facing().getCounterClockWise()) {
                boolean splitU = face.getAxis() != Direction.Axis.Y || assembly.facing().getClockWise().getAxis() == Direction.Axis.X;
                double mid = ArchPieceTargeting.MIDPOINT;
                Vec3 a = facePoint(face, plane, splitU ? mid : 0, splitU ? 0 : mid).add(origin).subtract(camera);
                Vec3 b = facePoint(face, plane, splitU ? mid : 1, splitU ? 1 : mid).add(origin).subtract(camera);
                line(event, a, b);
            }
        });
    }

    static Vec3 facePoint(Direction face, double plane, double u, double v) {
        return switch (face.getAxis()) {
            case X -> new Vec3(plane, v, u);
            case Y -> new Vec3(u, plane, v);
            case Z -> new Vec3(u, v, plane);
        };
    }

    private static void line(RenderHighlightEvent.Block event, Vec3 a, Vec3 b) {
        OutlineLines.line(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
                a.x, a.y, a.z, b.x, b.y, b.z, 1, 1, 1, .9F);
    }
}
