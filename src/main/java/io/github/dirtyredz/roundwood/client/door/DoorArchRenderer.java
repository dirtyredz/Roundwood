package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceModels;
import io.github.dirtyredz.roundwood.client.render.AssemblyCellRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Static fitted frame, drawn before the door's depth and swing transforms. */
public final class DoorArchRenderer {
    private final AssemblyCellRenderer renderer;
    public DoorArchRenderer(ModelBlockRenderer models) { renderer=new AssemblyCellRenderer(models,RandomSource.create()); }
    public void render(RoundDoorBlockEntity entity, DoorLayout.Assembly door, Level level, PoseStack pose, MultiBufferSource buffers) {
        if (!io.github.dirtyredz.roundwood.client.render.StaticRenderingConfig.legacy()) return;
        var attachment=entity.arch(); if(attachment.isEmpty()) return; var frame=attachment.get();
        if(frame.product()!=DoorArchFitPlan.topFor(door.design())) return;
        for(var layer:DoorArchGeometry.Layer.values()) {
            var look=layer==DoorArchGeometry.Layer.BODY?java.util.Optional.of(frame.appearance()):frame.casings().get(
                    layer==DoorArchGeometry.Layer.FRONT_CASING?AttachmentFace.FRONT:AttachmentFace.BACK);
            if(look.isEmpty()) continue;
            var material=ArchPieceModels.look(look.get());
            for(var cell:ArchPieceCell.of(frame.product())) {
                var model=DoorArchModels.cell(door.design(),door.facing(),frame.thickness(),frame.depth(),layer,cell,material);
                var pos=door.anchor().above(DoorArchFitPlan.topOffset(door.design())+cell.row).relative(door.facing().getClockWise(),cell.column);
                renderer.draw(level,door.anchor(),pos,model,level.getBlockState(pos),pose,buffers.getBuffer(RenderType.solid()),false);
            }
        }
    }
}
