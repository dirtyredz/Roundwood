package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimGeometry;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.client.trim.TrimAppearanceModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Optional;

/**
 * Draws a door's trim rings (0.8.0): static casing around the doorway, drawn before any facing or
 * swing transform so it never moves with the leaf. Each ring reuses the frame's turned quarter models
 * ({@link TrimAppearanceModels}, the same 32/28 ring and the same looks), whose ring sits at the back
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
    private final ModelBlockRenderer renderer;
    private final RandomSource random = RandomSource.create();

    public DoorTrimRenderer(ModelBlockRenderer renderer) {
        this.renderer = renderer;
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
            TrimLooks.Look painted = TrimAppearanceModels.look(look.get());
            // A quarter model's ring lies on the facing side of its cell (authored z 0..1). FRONT: the cell in
            // front of the door (z 16..17). BACK: the door's own cell, moved towards the front to z 12..13.
            double shift = face == AttachmentFace.FRONT ? 0 : (DoorGeometry.BACK - AttachmentFace.PROUD) / 16.0;
            for (TrimPart part : TrimPart.values()) {
                BlockPos door = assembly.pos(doorPart(part));
                BlockPos cell = face == AttachmentFace.FRONT ? door.relative(front) : door;
                BlockState state = level.getBlockState(door);
                pose.pushPose();
                pose.translate(cell.getX() - assembly.anchor().getX() + front.getStepX() * shift,
                        cell.getY() - assembly.anchor().getY(),
                        cell.getZ() - assembly.anchor().getZ() + front.getStepZ() * shift);
                renderer.tesselateBlock(level, TrimAppearanceModels.quarter(painted, assembly.facing(), part), state, cell, pose,
                        consumer, false, random, state.getSeed(door), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
                pose.popPose();
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
                pose.pushPose();
                pose.translate(cell.getX() - assembly.anchor().getX(), cell.getY() - assembly.anchor().getY(),
                        cell.getZ() - assembly.anchor().getZ());
                renderer.tesselateBlock(level, DoorTrimModels.casing(assembly.design(), face, part, assembly.facing(), painted),
                        state, cell, pose, consumer, false, random, state.getSeed(cell), OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY, RenderType.solid());
                pose.popPose();
            }
        }
    }

    private static DoorLayout.Part doorPart(TrimPart part) {
        return switch (part) {
            case LOWER_LEFT -> DoorLayout.Part.LOWER_LEFT;
            case LOWER_RIGHT -> DoorLayout.Part.LOWER_RIGHT;
            case UPPER_LEFT -> DoorLayout.Part.UPPER_LEFT;
            case UPPER_RIGHT -> DoorLayout.Part.UPPER_RIGHT;
        };
    }
}
