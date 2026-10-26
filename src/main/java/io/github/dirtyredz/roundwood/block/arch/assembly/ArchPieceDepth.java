package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.assembly.placement.FaceDepthBands;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Thin-only saved depth. Full pieces have no depth property. */
public enum ArchPieceDepth implements StringRepresentable {
    FRONT(13), MIDDLE(6.5), BACK(0);
    public static final EnumProperty<ArchPieceDepth> PROPERTY = EnumProperty.create("depth", ArchPieceDepth.class);
    private final double back;
    ArchPieceDepth(double back) { this.back = back; }
    public double back() { return back; }
    @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    public FaceDepthBands.Zone zone(Direction facing, Direction face) {
        return FaceDepthBands.zone(2 - ordinal(), facing, face);
    }
    public static ArchPieceDepth target(Direction facing, Direction face, double x, double y, double z) {
        return values()[2 - FaceDepthBands.target(facing, face, x, y, z)];
    }
}
