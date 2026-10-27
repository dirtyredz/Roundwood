package io.github.dirtyredz.roundwood.block.door.leaf;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorPlacementOutline;
import io.github.dirtyredz.roundwood.geometry.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoubleDoorGeometryTest {
    private static final DoorDesign DESIGN = DoorDesign.DOUBLE_ARCHED;
    @Test void tallDoorAddsOneStraightRowAndKeepsTheExactArchCrown() {
        var tall = DoorDesign.TALL_DOUBLE_ARCHED;
        assertEquals(12, DoorLayout.Part.of(tall).size());
        for (int x=0;x<48;x++) for (int y=0;y<64;y++) {
            assertEquals(y<16 || DESIGN.inLeaf(x,y-16), tall.inLeaf(x,y), x+"/"+y);
        }
        for (var hinge : DoorHingeSide.values()) for (var piece : DoorGeometry.leaf(tall, hinge)) {
            var model = io.github.dirtyredz.roundwood.client.door.DoorLeafModels.modelBox(tall, piece.box());
            assertTrue(model.within(new Box(-16,-16,-16,32,32,32)));
            assertEquals(piece.box(), io.github.dirtyredz.roundwood.client.door.DoorLeafModels.authoredBox(tall,model));
        }
        var open = DoorGeometry.placedEnvelope(tall, DoorHingeSide.LEFT, true);
        assertTrue(open.stream().allMatch(b -> b.maxX() <= 3 || b.minX() >= 45));
    }
    @Test void squareBottomAndRoundTopExactlyFillTheThreeWideArchOpening() {
        assertEquals(9, DoorLayout.Part.of(DESIGN).size());
        for (int x = 0; x < 48; x++) for (int y = 0; y < 48; y++) {
            double dx=x+.5-24, dy=y+.5-24;
            boolean expected = y < 24 || dx*dx+dy*dy<=24*24;
            assertEquals(expected, y < 16 || !ArchPieceDesign.TOP_2X3.top().contains(x, y-16));
            assertEquals(expected, DESIGN.inLeaf(x, y), x + "/" + y);
            double px = x + .5, py = y + .5;
            long leaves = java.util.Arrays.stream(DoorHingeSide.values()).flatMap(side -> DoorGeometry.leaf(DESIGN, side).stream())
                    .filter(p -> p.texture().equals(DoorGeometry.PANEL) && inside(p.box(), px, py, 14.5)).count();
            assertEquals(expected ? 1 : 0, leaves, "overlap or hole " + x + "/" + y);
        }
    }
    @Test void bothLeavesHaveHardwareOnBothFacesAndTurnOnTheirOuterHinges() {
        for (var side : DoorHingeSide.values()) {
            var pieces = DoorGeometry.leaf(DESIGN, side);
            assertEquals(2, pieces.stream().filter(p -> !p.texture().equals(DoorGeometry.PANEL) && p.box().maxZ()-p.box().minZ()==2).count());
            for (var p : pieces) {
                Box b = p.box().translate(-16, -16, 0);
                assertTrue(b.within(new Box(-16, -16, -16, 32, 32, 32)), "illegal model bounds");
            }
        }
        var open = DoorGeometry.placedEnvelope(DESIGN, DoorHingeSide.LEFT, true);
        assertTrue(open.stream().allMatch(b -> b.maxX() <= 3 || b.minX() >= 45), "leaf crossed the open passage");
        assertTrue(open.stream().anyMatch(b -> b.minZ() < 0), "missing projected free edges");
    }
    @Test void everyDepthKeepsCollisionInsideCellsAndPreviewAtTheSavedPlane() {
        for (var design : DoorDesign.values()) for (var depth : DoorDepth.values()) for (var hinge : DoorHingeSide.values()) {
            var assembly = new DoorLayout.Assembly(BlockPos.ZERO, Direction.NORTH, hinge, design, depth);
            var outline = DoorPlacementOutline.silhouette(assembly, false);
            assertEquals(13 + depth.offset(), outline.stream().mapToDouble(e -> Math.min(e.z1(), e.z2())).min().orElseThrow(), 1e-9);
            assertEquals(16 + depth.offset(), outline.stream().mapToDouble(e -> Math.max(e.z1(), e.z2())).max().orElseThrow(), 1e-9);
            for (boolean open : new boolean[]{false, true}) for (var part : assembly.parts()) {
                for (var b : DoorGeometry.cellEnvelope(design, hinge, part.columnIndex(), part.row(), open, depth.offset())) {
                    assertTrue(b.within(new Box(0, 0, 0, 16, 16, 16)), "projection must not collide");
                }
            }
        }
    }
    private static boolean inside(Box b, double x, double y, double z) {
        return x > b.minX() && x < b.maxX() && y > b.minY() && y < b.maxY() && z > b.minZ() && z < b.maxZ();
    }
}
