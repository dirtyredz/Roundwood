package io.github.dirtyredz.roundwood.geometry;

import net.minecraft.core.Direction;

/**
 * One visible rectangle of a model: the {@code face} side of {@code backing}, drawn with the model
 * texture variable {@code texture}. The backing box is a valid (positive-volume) box whose
 * {@code face} side is exactly the rectangle; its other faces are never drawn, so a model can show
 * part of a volume's side without overlaying coplanar faces.
 */
public record Surface(Box backing, Direction face, String texture) {
    /** Render-only normal separation: prevents attached skins sharing host/hardware planes. */
    public Surface separated() {
        double gap = 1.0 / 256;
        return new Surface(backing.translate(face.getStepX() * gap, face.getStepY() * gap, face.getStepZ() * gap), face, texture);
    }
}
