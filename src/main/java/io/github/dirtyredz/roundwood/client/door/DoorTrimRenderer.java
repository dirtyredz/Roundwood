package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.client.render.AssemblyCellRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.client.render.RingModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Draws a door's trim rings (0.8.0): static casing around the doorway, drawn before any facing or
 * swing transform so it never moves with the leaf. Each ring reuses the frame's turned quarter models
 * ({@link RingModels}, the same 32/28 ring and the same looks), whose ring sits at the back
 * of its cells: the FRONT ring is drawn in the cells in front of the door (authored {@code z 16..17},
 * one pixel past the door's cells, render-only), the BACK ring moved into the door's own cells
 * ({@code z 12..13}). Both keep their rear surfaces, which the opening leaf exposes. Tessellated with
 * world light, no cull sides, in the solid layer.
 *
 * <p>The arched door's casings (0.11.0) are built from its own outline instead ({@link DoorTrimModels},
 * jambs and crown, already at their depth), one piece per door cell. The round door keeps the frame
 * quarters, so its rings look exactly as before.
 */
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
        if (assembly.design() != DoorDesign.ROUND) {
            renderCasings(anchor, assembly, level, pose, consumer);
            return;
        }
        Direction front = assembly.facing().getOpposite();
        for (AttachmentFace face : AttachmentFace.values()) {
            Optional<Appearance> look = anchor.trims().get(face);
            if (look.isEmpty()) {
                continue;
            }
            TrimLooks.Look painted = RingModels.look(look.get());
            // A quarter model's ring lies on the facing side of its cell (authored z 0..1). FRONT: the cell in
            // front of the door (z 16..17). BACK: the door's own cell, moved towards the front to z 12..13.
            double shift = face == AttachmentFace.FRONT ? 0 : (DoorGeometry.BACK - AttachmentFace.PROUD) / 16.0;
            for (Quarter part : Quarter.values()) {
                BlockPos door = assembly.pos(doorPart(part));
                BlockPos cell = face == AttachmentFace.FRONT ? door.relative(front) : door;
                BlockState state = level.getBlockState(door);
                renderer.draw(level, assembly.anchor(), cell, RingModels.quarter(painted, assembly.facing(), part),
                        state, pose, consumer, false, state.getSeed(door),
                        new net.minecraft.world.phys.Vec3(front.getStepX() * shift, 0, front.getStepZ() * shift));
            }
        }
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
                BlockPos cell = assembly.pos(part);
                BlockState state = level.getBlockState(cell);
                renderer.draw(level, assembly.anchor(), cell,
                        DoorTrimModels.casing(assembly.design(), face, part, assembly.facing(), painted), state, pose, consumer, false);
            }
        }
    }

    private static DoorLayout.Part doorPart(Quarter part) {
        return switch (part) {
            case LOWER_LEFT -> DoorLayout.Part.LOWER_LEFT;
            case LOWER_RIGHT -> DoorLayout.Part.LOWER_RIGHT;
            case UPPER_LEFT -> DoorLayout.Part.UPPER_LEFT;
            case UPPER_RIGHT -> DoorLayout.Part.UPPER_RIGHT;
        };
    }
}
