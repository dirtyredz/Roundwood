package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchGeometry;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceModels;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.DonorSprites;
import io.github.dirtyredz.roundwood.client.render.GeneralQuadModels;
import io.github.dirtyredz.roundwood.client.render.SurfaceSeparation;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

import java.util.List;

/** Fitted cell geometry and reload-aware models, independent of the submission path. */
public final class DoorArchModels {
    private record Key(DoorDesign design, Direction facing, ArchPieceThickness thickness,
                       ArchPieceDepth depth, DoorArchGeometry.Layer layer, ArchPieceCell cell,
                       ArchPieceModels.Look material) {}

    private static final AppearanceModelCache<Key, BakedModel> MODELS =
            new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private DoorArchModels() {}

    public static List<Surface> surfaces(DoorDesign design, ArchPieceThickness thickness,
                                         ArchPieceDepth depth, DoorArchGeometry.Layer layer,
                                         int column, int row) {
        return (layer == DoorArchGeometry.Layer.BODY ? SurfaceSeparation.ARCH_BODY : SurfaceSeparation.ARCH_CASING)
                .apply(DoorArchGeometry.surfaces(design, thickness, depth, layer, column, row),column*16,row*16);
    }

    public static BakedModel cell(DoorDesign design, Direction facing, ArchPieceThickness thickness,
                                   ArchPieceDepth depth, DoorArchGeometry.Layer layer, ArchPieceCell cell,
                                   ArchPieceModels.Look material) {
        return MODELS.get(new Key(design, facing, thickness, depth, layer, cell, material), key -> {
            var sprite = DonorSprites.atlas(key.material.texture());
            return GeneralQuadModels.world(quads(key.design,key.thickness,key.depth,key.layer,key.cell,
                    key.facing,sprite,key.material.masonry()),sprite);
        });
    }

    /** Explicit complete-layer inputs: neighboring cells participate in edge repair before baking. */
    public static List<net.minecraft.client.renderer.block.model.BakedQuad> quads(DoorDesign design,
            ArchPieceThickness thickness,ArchPieceDepth depth,DoorArchGeometry.Layer layer,ArchPieceCell cell,
            Direction facing,net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,boolean masonry) {
        double back=thickness.back(depth),front=thickness.front(depth);
        if(layer==DoorArchGeometry.Layer.FRONT_CASING){back=front;front++;}
        if(layer==DoorArchGeometry.Layer.BACK_CASING){front=back;back--;}
        return ArchPieceModels.quads(DoorArchFitPlan.topFor(design),
                io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side.LEFT,cell,back,front,facing,sprite,masonry,
                layer==DoorArchGeometry.Layer.BODY?ArchPieceModels.Layer.BODY:ArchPieceModels.Layer.CASING);
    }
}
