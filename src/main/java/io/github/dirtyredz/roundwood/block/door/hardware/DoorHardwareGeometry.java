package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.PixelCircle;
import java.util.ArrayList;
import java.util.List;

/** Authored left-hand hardware. Roots follow the actual staircase edge, on a half-pixel grid. */
public final class DoorHardwareGeometry {
    public static final double KNOB_SIZE = 2, KNOB_PROUD = 2, STRAP_PROUD = .5;
    private DoorHardwareGeometry() {}

    public record Part(DoorHardwareSlot slot, Box box) {}

    public static List<Part> placed(DoorDesign design, net.minecraft.world.level.block.state.properties.DoorHingeSide hinge, boolean open) {
        var out=new ArrayList<Part>();
        for(var side:design.doubleLeaf()?net.minecraft.world.level.block.state.properties.DoorHingeSide.values()
                :new net.minecraft.world.level.block.state.properties.DoorHingeSide[]{hinge})
            for(var piece:io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry.leaf(design,side)) {
                if(piece.texture().equals(io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry.PANEL))continue;
                var slot=piece.texture().equals(DoorHardwareSlot.HINGES.mesh())?DoorHardwareSlot.HINGES:DoorHardwareSlot.KNOBS;
                out.add(new Part(slot,open?io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry.swing(design,piece.box(),side):piece.box()));
            }
        return List.copyOf(out);
    }

    public static List<Part> left(DoorDesign design) {
        List<Part> boxes = new ArrayList<>();
        int upper = switch (design) { case ROUND -> 22; case TALL_DOUBLE_ARCHED -> 36; default -> 20; };
        for (int bottom : new int[]{7, upper}) {
            var rects = PixelCircle.rasterize(design.width() * 2, 6, (x, y) -> {
                int row = bottom + y / 2;
                int edge = 0;
                while (edge < design.width() && !design.inLeaf(edge, row)) edge++;
                int reach = y == 0 || y == 5 ? 2 : y == 1 || y == 4 ? 6 : 7;
                return x >= edge * 2 && x < (edge + reach) * 2 && design.inLeaf(x / 2, row);
            });
            for (var rect : rects) {
                Box b = rect.extrude(0, 1);
                boxes.add(new Part(DoorHardwareSlot.HINGES, new Box(b.minX()/2, bottom+b.minY()/2, 16, b.maxX()/2, bottom+b.maxY()/2, 16.5)));
                boxes.add(new Part(DoorHardwareSlot.HINGES, new Box(b.minX()/2, bottom+b.minY()/2, 12.5, b.maxX()/2, bottom+b.maxY()/2, 13)));
            }
        }
        int x = switch (design) { case ROUND -> 15; case ARCHED -> 11; default -> 20; };
        boxes.add(new Part(DoorHardwareSlot.KNOBS, new Box(x, 15, 16, x+2, 17, 18)));
        boxes.add(new Part(DoorHardwareSlot.KNOBS, new Box(x, 15, 11, x+2, 17, 13)));
        return List.copyOf(boxes);
    }
}
