package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.client.render.AssemblyCellRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Draws every door's static, separated casing surfaces through the same model path. */
public final class DoorTrimRenderer {
    private final AssemblyCellRenderer renderer;

    public DoorTrimRenderer(ModelBlockRenderer renderer) {
        this.renderer = new AssemblyCellRenderer(renderer, RandomSource.create());
    }

    public void render(RoundDoorBlockEntity anchor, DoorLayout.Assembly assembly, Level level, PoseStack pose,
                       MultiBufferSource buffers) {
        if (!anchor.trims().any()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        renderCasings(anchor, assembly, level, pose, consumer);
    }

    /** Each door cell's piece of each installed casing, at that cell with its light. */
    private void renderCasings(RoundDoorBlockEntity anchor, DoorLayout.Assembly assembly, Level level, PoseStack pose,
                               VertexConsumer consumer) {
        for (AttachmentFace face : AttachmentFace.values()) {
            Optional<Appearance> look = anchor.trims().get(face);
            if (look.isEmpty()) {
                continue;
            }
            TrimLooks.Look painted = DoorTrimModels.look(assembly.design(), look.get());
            for (DoorLayout.Part part : assembly.parts()) {
                BlockPos door = assembly.pos(part);
                BlockPos cell = face == AttachmentFace.FRONT ? door.relative(assembly.facing().getOpposite()) : door;
                BlockState state = level.getBlockState(door);
                Direction facing = assembly.facing();
                var shift = face == AttachmentFace.FRONT ? new net.minecraft.world.phys.Vec3(facing.getStepX(), 0, facing.getStepZ())
                        : net.minecraft.world.phys.Vec3.ZERO;
                renderer.draw(level, assembly.anchor(), cell,
                        DoorTrimModels.casing(assembly.design(), face, part, assembly.facing(), painted), state, pose, consumer, false, state.getSeed(door), shift);
            }
        }
    }

}
