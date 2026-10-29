package io.github.dirtyredz.roundwood.client.render;

import io.github.dirtyredz.roundwood.geometry.Surface;
import java.util.List;

/** Render-only separation. Arch layers stay connected; logical volumes never move. */
public enum SurfaceSeparation {
    ARCH_BODY(1), ARCH_CASING(2), DOOR_CASING(3);
    private final double pixels;
    SurfaceSeparation(int units) { pixels=units/256.0; }
    public double pixels() { return pixels; }
    public Surface apply(Surface surface) {
        return apply(surface,0,0);
    }
    private Surface apply(Surface surface,int originX,int originY) {
        var face=surface.face();
        if (this != DOOR_CASING) {
            // Translate the complete XY silhouette, not each face independently. Expand the
            // shared depth interval so front/back and reveal edges still meet exactly.
            var b=surface.backing();
            return new Surface(new io.github.dirtyredz.roundwood.geometry.Box(
                    shift(b.minX(),originX),shift(b.minY(),originY),b.minZ()-pixels,
                    shift(b.maxX(),originX),shift(b.maxY(),originY),b.maxZ()+pixels),face,surface.texture());
        }
        return new Surface(surface.backing().translate(face.getStepX()*pixels,face.getStepY()*pixels,face.getStepZ()*pixels),face,surface.texture());
    }
    private double shift(double coordinate,int origin){return coordinate+(coordinate+origin==0?-pixels:pixels);}
    public List<Surface> apply(List<Surface> surfaces) { return surfaces.stream().map(this::apply).toList(); }
    public List<Surface> apply(List<Surface> surfaces,int originX,int originY) {
        return surfaces.stream().map(surface->apply(surface,originX,originY)).toList();
    }
}
