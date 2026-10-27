package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareGeometry;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.world.phys.shapes.*;
import java.util.*;
import java.util.function.Supplier;

/** Bounded cache of expensive selection Boolean operations; keys contain geometry, never a world. */
final class DoorTargetShapes {
    private static final Map<Object,VoxelShape> SHAPES=new LinkedHashMap<>(64,.75F,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Object,VoxelShape> entry){return size()>2048;}
    };
    private record Union(VoxelShape a,VoxelShape b) {}
    private record Difference(VoxelShape a,VoxelShape b) {}
    private record Move(VoxelShape shape,int x,int y,int z) {}
    private record Hardware(Object design,Object hinge,boolean open,Object depth,Object facing,Object part) {}
    private record Clipped(VoxelShape shape) {}
    private DoorTargetShapes() {}
    private static synchronized VoxelShape get(Object key,Supplier<VoxelShape> build) {
        var result=SHAPES.get(key);
        if(result==null){result=build.get();SHAPES.put(key,result);}
        return result;
    }
    static VoxelShape union(VoxelShape a,VoxelShape b) {
        if(a.isEmpty())return b;if(b.isEmpty())return a;
        return get(new Union(a,b),()->Shapes.or(a,b));
    }
    static VoxelShape exposed(VoxelShape hardware,VoxelShape blockers) {
        if(blockers.isEmpty())return hardware;
        return get(new Difference(hardware,blockers),()->Shapes.join(hardware,blockers,BooleanOp.ONLY_FIRST));
    }
    static VoxelShape cell(VoxelShape shape) {
        return get(new Clipped(shape),()->Shapes.join(shape,Shapes.block(),BooleanOp.AND));
    }
    static VoxelShape move(VoxelShape shape,int x,int y,int z) {
        if(x==0 && y==0 && z==0)return shape;
        return get(new Move(shape,x,y,z),()->shape.move(x,y,z));
    }
    static VoxelShape hardware(DoorLayout.Assembly door,DoorLayout.Part part,boolean open) {
        return get(new Hardware(door.design(),door.hinge(),open,door.depth(),door.facing(),part),()->{
            var boxes=DoorHardwareGeometry.placed(door.design(),door.hinge(),open).stream()
                    .map(p->p.box().translate(0,0,door.depth().offset())).toList();
            return VoxelShapeFactory.shape(VoxelShapeFactory.rotate(DoorOutlineGeometry.relativeTo(boxes,part.columnIndex(),part.row()),VoxelShapeFactory.quarterTurns(door.facing())));
        });
    }
}
