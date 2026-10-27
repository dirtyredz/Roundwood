package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import java.util.Objects;

/** The top's independent custody; facing and anchor are relative to its owning door. */
public record DoorArchAttachment(ArchPieceDesign product, ArchPieceThickness thickness, ArchPieceDepth depth,
                                 Appearance appearance, FaceTrims casings) {
    public DoorArchAttachment {
        Objects.requireNonNull(product); Objects.requireNonNull(thickness); Objects.requireNonNull(depth);
        Objects.requireNonNull(appearance); Objects.requireNonNull(casings);
        if (product.shoulder() || (thickness == ArchPieceThickness.FULL && depth != ArchPieceDepth.FRONT)
                || !AppearanceFamily.TRIM.accepts(appearance)
                || !casings.front().map(AppearanceFamily.TRIM::accepts).orElse(true)
                || !casings.back().map(AppearanceFamily.TRIM::accepts).orElse(true))
            throw new IllegalArgumentException("Unsupported fitted arch");
    }
    public DoorArchAttachment withAppearance(Appearance look) { return new DoorArchAttachment(product, thickness, depth, look, casings); }
    public DoorArchAttachment withCasings(FaceTrims looks) { return new DoorArchAttachment(product, thickness, depth, appearance, looks); }
}
