package io.github.dirtyredz.roundwood.block.door.assembly;

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
        int band = this == BACK ? 0 : this == MIDDLE ? 1 : 2;
        boolean wallFront = face.getAxis() == facing.getAxis();
        boolean splitU = !wallFront && (face.getAxis() != Direction.Axis.Y || facing.getAxis() == Direction.Axis.X);
        int step = facing.getAxis() == Direction.Axis.X ? facing.getStepX() : facing.getStepZ();
        if (!wallFront && step > 0) band = 2 - band;
        double low = band / 3.0, high = (band + 1) / 3.0;
        return splitU ? new FaceZone(low, 0, high, 1) : new FaceZone(0, low, 1, high);
    }

    /** Floor/side hits use depth along facing. Front/back walls use vertical thirds. */
    public static DoorDepth target(Direction facing, Direction face, double x, double y, double z) {
        double depth = face.getAxis() == facing.getAxis() ? y
                : .5 - (x - .5) * facing.getStepX() - (z - .5) * facing.getStepZ();
        if (!Double.isFinite(depth)) throw new IllegalArgumentException("Finite hit required");
        return depth < 1.0 / 3 ? BACK : depth > 2.0 / 3 ? FRONT : MIDDLE;
    }
}
