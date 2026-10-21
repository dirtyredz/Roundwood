package io.github.dirtyredz.roundwood.block.trim.assembly;

import net.minecraft.util.StringRepresentable;

/**
 * Which quarter of a frame a cell holds, seen from the front: column 0 is the left (the anchor's
 * column), row 0 the lower. Unlike the door there is no hinge or open state, so one property says it.
 */
public enum TrimPart implements StringRepresentable {
    LOWER_LEFT(0, 0),
    UPPER_LEFT(0, 1),
    LOWER_RIGHT(1, 0),
    UPPER_RIGHT(1, 1);

    public final int column;
    public final int row;

    TrimPart(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public static TrimPart of(int column, int row) {
        for (TrimPart part : values()) {
            if (part.column == column && part.row == row) {
                return part;
            }
        }
        throw new IllegalArgumentException("no frame part at " + column + "," + row);
    }

    public boolean lower() {
        return row == 0;
    }

    /** The same quarter in the other column (a mirrored frame). */
    public TrimPart mirrored() {
        return of(1 - column, row);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
