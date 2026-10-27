package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceModels;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.render.AssemblyCellRenderer;
import io.github.dirtyredz.roundwood.client.render.GeneralQuadModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import java.util.List;

/** Static fitted frame, drawn before the door's depth and swing transforms. */
public final class DoorArchRenderer {
    private static final AppearanceModelCache<List<Object>, BakedModel> MODELS=new AppearanceModelCache<>(AppearanceModelCache.MODELS);
    private final AssemblyCellRenderer renderer;
    public DoorArchRenderer(ModelBlockRenderer models) { renderer=new AssemblyCellRenderer(models,RandomSource.create()); }
    public static java.util.List<io.github.dirtyredz.roundwood.geometry.Surface> surfaces(
            io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign design,
            io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness thickness,
            io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth depth,
            DoorArchGeometry.Layer layer, int column, int row) {
        return (layer == DoorArchGeometry.Layer.BODY ? io.github.dirtyredz.roundwood.client.render.SurfaceSeparation.ARCH_BODY
                : io.github.dirtyredz.roundwood.client.render.SurfaceSeparation.ARCH_CASING)
                .apply(DoorArchGeometry.surfaces(design,thickness,depth,layer,column,row));
    }
    public void render(RoundDoorBlockEntity entity, DoorLayout.Assembly door, Level level, PoseStack pose, MultiBufferSource buffers) {
        var attachment=entity.arch(); if(attachment.isEmpty()) return; var frame=attachment.get();
        if(frame.product()!=DoorArchFitPlan.topFor(door.design())) return;
        for(var layer:DoorArchGeometry.Layer.values()) {
            var look=layer==DoorArchGeometry.Layer.BODY?java.util.Optional.of(frame.appearance()):frame.casings().get(
                    layer==DoorArchGeometry.Layer.FRONT_CASING?AttachmentFace.FRONT:AttachmentFace.BACK);
            if(look.isEmpty()) continue;
            var material=ArchPieceModels.look(look.get());
            for(var cell:ArchPieceCell.of(frame.product())) {
                var key=List.<Object>of(door.design(),door.facing(),frame.thickness(),frame.depth(),layer,cell,material);
                var model=MODELS.get(key,k->{
                    var sprite=DonorSprites.atlas(material.texture());
                    return GeneralQuadModels.world(ArchPieceModels.bakeSurfaces(frame.product(),cell,
                            surfaces(door.design(),frame.thickness(),frame.depth(),layer,cell.column,cell.row),
                            door.facing(),sprite,material.masonry()),sprite);
                });
                var pos=door.anchor().above(DoorArchFitPlan.topOffset(door.design())+cell.row).relative(door.facing().getClockWise(),cell.column);
                renderer.draw(level,door.anchor(),pos,model,level.getBlockState(pos),pose,buffers.getBuffer(RenderType.solid()),false);
            }
        }
    }
}
