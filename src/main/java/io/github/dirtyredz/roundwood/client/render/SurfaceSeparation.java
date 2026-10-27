package io.github.dirtyredz.roundwood.client.render;

import io.github.dirtyredz.roundwood.geometry.Surface;
import java.util.List;

/** Render-only normal offsets. Apply once to authored surfaces, never to logical volumes. */
public enum SurfaceSeparation {
    ARCH_BODY(1), ARCH_CASING(2), DOOR_CASING(3);
    private final double pixels;
    SurfaceSeparation(int units) { pixels=units/256.0; }
    public Surface apply(Surface surface) {
        var face=surface.face();
        return new Surface(surface.backing().translate(face.getStepX()*pixels,face.getStepY()*pixels,face.getStepZ()*pixels),face,surface.texture());
    }
    public List<Surface> apply(List<Surface> surfaces) { return surfaces.stream().map(this::apply).toList(); }
}
