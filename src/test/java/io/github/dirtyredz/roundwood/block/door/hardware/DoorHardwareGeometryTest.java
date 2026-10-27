package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorHardwareGeometryTest {
    @Test void everyHalfPixelOfHardwareIsSupportedAndSpearDimensionsAreFixed() {
        for (var design : DoorDesign.values()) for (var hinge : DoorHingeSide.values()) {
            for (var piece : DoorGeometry.leaf(design, hinge)) {
                if (piece.texture().equals(DoorGeometry.PANEL)) continue;
                var box = piece.box();
                for (double y=box.minY()+.25; y<box.maxY(); y+=.5)
                    for (double x=box.minX()+.25; x<box.maxX(); x+=.5)
                        assertTrue(design.inLeaf((int)x,(int)y), design+" unsupported hardware "+box);
                assertTrue(box.maxZ()-box.minZ()==.5 || box.maxZ()-box.minZ()==2);
            }
            int upper = design == DoorDesign.ROUND ? 22 : design == DoorDesign.TALL_DOUBLE_ARCHED ? 36 : 20;
            for (int root : new int[]{7, upper}) for (int subrow=0; subrow<6; subrow++) {
                double y=root+(subrow+.5)/2;
                int edge=0;
                while (!design.inLeaf(edge,(int)y)) edge++;
                int reach=subrow==0 || subrow==5 ? 2 : subrow==1 || subrow==4 ? 6 : 7;
                for (int dx=0;dx<16;dx++) {
                    double x=edge+(dx+.5)/2;
                    if (hinge==DoorHingeSide.RIGHT) x=design.width()-x;
                    final double px=x;
                    boolean filled=DoorGeometry.leaf(design,hinge).stream().filter(p->!p.texture().equals(DoorGeometry.PANEL))
                            .anyMatch(p->p.box().containsStrictly(px,y,16.25));
                    assertEquals(dx<reach*2,filled,design+" spear row "+subrow+" x "+dx);
                }
            }
        }
    }
    @Test void everyKnobIsCentredSixteenPixelsAboveTheFloor() {
        for (var design : DoorDesign.values()) for (var hinge : DoorHingeSide.values()) {
            var knobs = DoorGeometry.leaf(design, hinge).stream().filter(p -> !p.texture().equals(DoorGeometry.PANEL))
                    .map(p -> p.box()).filter(b -> b.maxZ() - b.minZ() == 2).toList();
            assertEquals(2, knobs.size());
            for (var knob : knobs) {
                assertEquals(15, knob.minY(), design + " " + hinge);
                assertEquals(17, knob.maxY());
                assertEquals(2, knob.maxX() - knob.minX());
            }
        }
    }

    @Test void threePixelRootsTouchActualLeafEdgeOnBothFaces() {
        for (var design : DoorDesign.values()) for (var hinge : DoorHingeSide.values()) {
            int upper = design == DoorDesign.ROUND ? 22 : design == DoorDesign.TALL_DOUBLE_ARCHED ? 36 : 20;
            var hardware = DoorGeometry.leaf(design, hinge).stream().filter(p -> !p.texture().equals(DoorGeometry.PANEL)).toList();
            for (int root : new int[]{7, upper}) for (int y = root; y < root + 3; y++) {
                int edge = 0;
                while (!design.inLeaf(edge, y)) edge++;
                double x = hinge == DoorHingeSide.LEFT ? edge + .25 : design.width() - edge - .25;
                double row = y + .25;
                for (double z : new double[]{12.75, 16.25}) {
                    assertTrue(hardware.stream().anyMatch(p -> p.box().containsStrictly(x, row, z)),
                            design + " " + hinge + " floating root at row " + y);
                }
            }
        }
    }

    @Test void pairedCrownsReachTheirAssemblyTops() {
        for (var design : new DoorDesign[]{DoorDesign.DOUBLE_ARCHED, DoorDesign.TALL_DOUBLE_ARCHED}) {
            assertTrue(design.inLeaf(23, design.height() - 1), design + " leaves an apex gap");
            assertTrue(design.inLeaf(24, design.height() - 1));
        }
    }
}
