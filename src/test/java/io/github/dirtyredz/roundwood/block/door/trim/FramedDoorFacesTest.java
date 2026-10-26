package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry;

import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.geometry.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Positive-area coplanar rectangles fight even when their normals oppose. */
class FramedDoorFacesTest {
    @Test void casingsNeverSharePanelOrHardwarePlanes() {
        for (var design : DoorDesign.values()) for (var depth : DoorDepth.values())
            for (boolean open : new boolean[]{false,true}) for (var hinge : DoorHingeSide.values()) for (var face : AttachmentFace.values()) {
                var leaf = new ArrayList<Surface>();
                for (var piece : DoorGeometry.leaf(design, hinge)) for (var side : Direction.values()) {
                    var box = open ? DoorGeometry.swing(design, piece.box(), hinge) : piece.box();
                    var normal = !open || side.getAxis() == Direction.Axis.Y ? side
                            : hinge == DoorHingeSide.LEFT ? side.getCounterClockWise() : side.getClockWise();
                    leaf.add(new Surface(box.translate(0,0,depth.offset()), normal, piece.texture()));
                }
                for (var part : DoorLayout.Part.of(design)) {
                    for (var surface : DoorTrimGeometry.surfaces(design, face, part.columnIndex(), part.row())) {
                        var placed = new Surface(surface.backing().translate(part.columnIndex()*16, part.row()*16, depth.offset()), surface.face(), "trim");
                        for (var other : leaf) assertFalse(overlap(placed, other), () -> design+" "+face+" "+depth+" "+placed+" / "+other);
                    }
                }
            }
    }
    @Test void doorAndArchCasingsHaveNoCoplanarOverlapsInAdjacentLayers() {
        var side = KeyholeShoulderProfile.Side.LEFT;
        for (var design : DoorDesign.values())
            for (var depth : DoorDepth.values()) for (int layer : new int[]{-16,16}) {
                var arch = switch(design) {
                    case ROUND -> ArchPieceDesign.TOP_1X2;
                    case ARCHED -> ArchPieceDesign.TOP_1X1;
                    default -> ArchPieceDesign.TOP_2X3;
                };
                var archFaces = new ArrayList<Surface>();
                for (int col=0;col<arch.columns();col++) for (int row=0;row<arch.rows();row++) {
                    var surfaces = new ArrayList<>(ArchPieceGeometry.surfaces(arch,side,col,row,0,16,"body"));
                    for (var face : AttachmentFace.values()) {
                        var z = ArchPieceTrimGeometry.depth(face);
                        surfaces.addAll(ArchPieceTrimGeometry.surfaces(arch,side,col,row,z[0],z[1],"arch trim"));
                    }
                    for (var s : surfaces) archFaces.add(new Surface(s.backing().translate(col*16,row*16+design.height()-arch.height(),layer),s.face(),s.texture()));
                }
                var doorFaces = new ArrayList<Surface>();
                for (var part : DoorLayout.Part.of(design)) for (var face : AttachmentFace.values())
                    for (var s : DoorTrimGeometry.surfaces(design,face,part.columnIndex(),part.row()))
                        doorFaces.add(new Surface(s.backing().translate(part.columnIndex()*16,part.row()*16,depth.offset()),s.face(),s.texture()));
                for (var a : doorFaces) for (var b : archFaces)
                    assertFalse(overlap(a,b), () -> design+" "+depth+" layer "+layer+" "+a+" / "+b);
            }
    }

    @Test void originalDoubleDoorJambAndHingeStrapReproduceTheBleed() {
        var design = DoorDesign.DOUBLE_ARCHED;
        var raw = PixelSlab.cellFaces((u,v) -> DoorTrimGeometry.inCasing(design,u,v),0,0,16,17,"");
        var strap = new Surface(new Box(2,8,16,9,9,16.5),Direction.WEST,"brass");
        assertTrue(raw.stream().anyMatch(s -> overlap(s,strap)), "old jamb x=2 must reproduce the screenshot's strap overlap");
        assertFalse(DoorTrimGeometry.surfaces(design,AttachmentFace.FRONT,0,0).stream().anyMatch(s -> overlap(s,strap)));
    }

    @Test void guardRejectsOppositeNormalsAndIgnoresTouchingEdges() {
        var a = new Surface(new Box(0,0,0,1,1,1),Direction.SOUTH,"");
        assertTrue(overlap(a,new Surface(new Box(0,0,1,1,1,2),Direction.NORTH,"")));
        assertFalse(overlap(a,new Surface(new Box(1,0,1,2,1,2),Direction.NORTH,"")));
    }

    static boolean overlap(Surface a, Surface b) {
        int axis = a.face().getAxis().ordinal();
        if (a.face().getAxis() != b.face().getAxis()) return false;
        double[] amin = min(a.backing()), amax = max(a.backing()), bmin = min(b.backing()), bmax = max(b.backing());
        double ap = a.face().getAxisDirection() == Direction.AxisDirection.POSITIVE ? amax[axis] : amin[axis];
        double bp = b.face().getAxisDirection() == Direction.AxisDirection.POSITIVE ? bmax[axis] : bmin[axis];
        if (Math.abs(ap-bp)>1e-6) return false;
        for (int i=0;i<3;i++) if (i!=axis && Math.min(amax[i],bmax[i])-Math.max(amin[i],bmin[i])<=1e-6) return false;
        return true;
    }
    private static double[] min(Box b) { return new double[]{b.minX(),b.minY(),b.minZ()}; }
    private static double[] max(Box b) { return new double[]{b.maxX(),b.maxY(),b.maxZ()}; }
}
