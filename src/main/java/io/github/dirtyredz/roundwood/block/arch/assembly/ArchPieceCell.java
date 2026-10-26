package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import net.minecraft.util.StringRepresentable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Column-major owned cells, including geometrically empty cells; anchor first. */
public enum ArchPieceCell implements StringRepresentable {
    LOWER_LEFT(0, 0), UPPER_LEFT(0, 1), LOWER_MIDDLE(1, 0), UPPER_MIDDLE(1, 1), LOWER_RIGHT(2, 0), UPPER_RIGHT(2, 1);

    public final int column;
    public final int row;
    ArchPieceCell(int column, int row) { this.column = column; this.row = row; }
    public boolean in(ArchPieceDesign design) { return column < design.columns() && row < design.rows(); }
    public static List<ArchPieceCell> of(ArchPieceDesign design) {
        return Arrays.stream(values()).filter(cell -> cell.in(design)).toList();
    }
    public ArchPieceCell mirrored(ArchPieceDesign design) {
        return of(design).stream().filter(cell -> cell.column == design.columns() - 1 - column && cell.row == row)
                .findFirst().orElseThrow();
    }
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
