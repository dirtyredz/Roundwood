package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.geometry.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorArchSurfaceSeparationTest {
    @Test void frameBodyMustNotShareAnOpposedLeafStepPlane() {
        var design=DoorDesign.ROUND;
        var top=DoorArchFitPlan.topFor(design);
        for(int column=0;column<top.columns();column++) for(int row=0;row<top.rows();row++)
            for(var s:io.github.dirtyredz.roundwood.client.door.arch.DoorArchModels.surfaces(design,ArchPieceThickness.FULL,ArchPieceDepth.FRONT,DoorArchGeometry.Layer.BODY,column,row)) {
                var frame=new Surface(s.backing().translate(column*16,row*16+16,0),s.face(),"");
                for(var piece:DoorGeometry.leaf(design,DoorHingeSide.LEFT)) for(var face:Direction.values())
                    assertFalse(overlap(frame,new Surface(piece.box(),face,"")),"opposed leaf/frame step is coplanar: "+frame);
            }
    }

    private static boolean overlap(Surface a,Surface b) {
        if(a.face().getAxis()!=b.face().getAxis()) return false;
        int axis=a.face().getAxis().ordinal();
        double[] loA=min(a.backing()),hiA=max(a.backing()),loB=min(b.backing()),hiB=max(b.backing());
        double pa=a.face().getAxisDirection()==Direction.AxisDirection.POSITIVE?hiA[axis]:loA[axis];
        double pb=b.face().getAxisDirection()==Direction.AxisDirection.POSITIVE?hiB[axis]:loB[axis];
        if(Math.abs(pa-pb)>1e-7) return false;
        for(int i=0;i<3;i++) if(i!=axis && Math.min(hiA[i],hiB[i])-Math.max(loA[i],loB[i])<=1e-7) return false;
        return true;
    }
    private static double[] min(Box b){return new double[]{b.minX(),b.minY(),b.minZ()};}
    private static double[] max(Box b){return new double[]{b.maxX(),b.maxY(),b.maxZ()};}
}
