package io.github.dirtyredz.roundwood.block.door.assembly;

import net.minecraft.util.StringRepresentable;

/**
 * Which column of the 2-wide assembly a part is in, seen from the front (the side the placing
 * player stood on). Independent of the physical hinge: all four parts share one {@code hinge}.
 */
public enum DoorColumn implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    DoorColumn(String name) {
        this.name = name;
    }

    public DoorColumn opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    /** 0 for the left column, 1 for the right: the column's offset in the assembly plane. */
    public int index() {
        return ordinal();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
