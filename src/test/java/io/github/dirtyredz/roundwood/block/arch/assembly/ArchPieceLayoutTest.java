package io.github.dirtyredz.roundwood.block.arch.assembly;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceLayoutTest {
    @Test
    void everyCellResolvesItsOwnAnchorAcrossRotations() {
        BlockPos anchor = new BlockPos(-27, 51, 33);
        for (var design : ArchPieceDesign.values()) for (Direction facing : Direction.Plane.HORIZONTAL)
            for (Side side : Side.values()) {
                var assembly = new ArchPieceLayout.Assembly(anchor, facing, design, side);
                assertEquals(design.columns() * design.rows(), assembly.parts().size());
                assertEquals(assembly.parts().size(), new HashSet<>(assembly.positions()).size());
                assertEquals(anchor, assembly.pos(assembly.parts().get(0)));
                for (var cell : assembly.parts()) {
                    assertEquals(anchor, ArchPieceLayout.anchorFor(assembly.pos(cell), facing, cell));
                    assertEquals(cell, cell.mirrored(design).mirrored(design));
                    assertEquals(design.columns() - 1 - cell.column, cell.mirrored(design).column);
                    Vec3 centre = assembly.toAuthored(Vec3.atCenterOf(assembly.pos(cell)));
                    assertEquals(cell.column * 16 + 8, centre.x);
                    assertEquals(cell.row * 16 + 8, centre.y);
                    assertEquals(8, centre.z);
                }
            }
    }

    @Test
    void includesEmptyCentreAndRejectsVerticalOrOutOfProductCells() {
        var assembly = new ArchPieceLayout.Assembly(BlockPos.ZERO, Direction.NORTH, ArchPieceDesign.TOP_2X3, Side.LEFT);
        assertTrue(assembly.parts().contains(ArchPieceCell.LOWER_MIDDLE));
        assertEquals(6, assembly.parts().size());
        assertThrows(IllegalArgumentException.class, () -> new ArchPieceLayout.Assembly(BlockPos.ZERO, Direction.UP, ArchPieceDesign.TOP_1X1, Side.LEFT));
        var small = new ArchPieceLayout.Assembly(BlockPos.ZERO, Direction.NORTH, ArchPieceDesign.TOP_1X1, Side.LEFT);
        assertThrows(IllegalArgumentException.class, () -> small.pos(ArchPieceCell.LOWER_RIGHT));
    }
}
