package io.github.dirtyredz.roundwood.block.archway.assembly;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Which cell of a 2 x 2 archway a block holds (0.12.0), seen from the front: column 0 is the left (the
 * anchor's column), row 0 the lower. The archway's own state identity (like the compatibility frame's
 * quarters, never shared with it: the families do not import each other).
 */
public enum ArchwayPart implements StringRepresentable {
    LOWER_LEFT(0, 0),
    UPPER_LEFT(0, 1),
    LOWER_RIGHT(1, 0),
    UPPER_RIGHT(1, 1);

    public final int column;
    public final int row;

    ArchwayPart(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public static ArchwayPart of(int column, int row) {
        for (ArchwayPart part : values()) {
            if (part.column == column && part.row == row) {
                return part;
            }
        }
        throw new IllegalArgumentException("no archway part at " + column + "," + row);
    }

    public boolean lower() {
        return row == 0;
    }

    /** The same cell in the other column (a mirrored archway). */
    public ArchwayPart mirrored() {
        return of(1 - column, row);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
