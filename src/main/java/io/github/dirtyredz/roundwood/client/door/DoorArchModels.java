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
                .apply(DoorArchGeometry.surfaces(design, thickness, depth, layer, column, row));
    }

    public static BakedModel cell(DoorDesign design, Direction facing, ArchPieceThickness thickness,
                                   ArchPieceDepth depth, DoorArchGeometry.Layer layer, ArchPieceCell cell,
                                   ArchPieceModels.Look material) {
        return MODELS.get(new Key(design, facing, thickness, depth, layer, cell, material), key -> {
            var sprite = DonorSprites.atlas(key.material.texture());
            return GeneralQuadModels.world(ArchPieceModels.bakeSurfaces(DoorArchFitPlan.topFor(key.design), key.cell,
                    surfaces(key.design, key.thickness, key.depth, key.layer, key.cell.column, key.cell.row),
                    key.facing, sprite, key.material.masonry()), sprite);
        });
    }
}
