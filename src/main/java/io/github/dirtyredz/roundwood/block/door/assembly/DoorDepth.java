package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.assembly.placement.FaceDepthBands;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Saved depth; FRONT preserves the historical slab when older states omit the property. */
public enum DoorDepth implements StringRepresentable {
    FRONT(0), MIDDLE(-6.5), BACK(-13);
    public static final EnumProperty<DoorDepth> PROPERTY = EnumProperty.create("depth", DoorDepth.class);
    private final double offset;
    DoorDepth(double offset) { this.offset = offset; }
    public double offset() { return offset; }
    @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }

    public record FaceZone(double minU, double minV, double maxU, double maxV) {}
    public FaceZone zone(Direction facing, Direction face) {
        var zone = FaceDepthBands.zone(2 - ordinal(), facing, face);
        return new FaceZone(zone.minU(), zone.minV(), zone.maxU(), zone.maxV());
    }

    /** Floor/side hits use depth along facing. Front/back walls use vertical thirds. */
    public static DoorDepth target(Direction facing, Direction face, double x, double y, double z) {
        return values()[2 - FaceDepthBands.target(facing, face, x, y, z)];
    }
}
