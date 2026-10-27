package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.door.DoorArchModels;
import io.github.dirtyredz.roundwood.geometry.*;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchWatertightTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("test","mesh");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents c){super(TEXTURE,c,256,256,32,64);}
    }
    record Point(double x,double y,double z) implements Comparable<Point> {
        public int compareTo(Point p){int c=Double.compare(x,p.x);if(c==0)c=Double.compare(y,p.y);if(c==0)c=Double.compare(z,p.z);return c;}
    }
    record Edge(Point a,Point b) {}
    @Test void fittedLayersShareEveryEdgeExactly() {
        try(var contents=new SpriteContents(TEXTURE,new FrameSize(16,16),new NativeImage(16,16,false),AnimationMetadataSection.EMPTY)) {
            var sprite=new Sprite(contents);
            for(var design:DoorDesign.values()) for(var thickness:ArchPieceThickness.values()) for(var depth:ArchPieceDepth.values())
                for(var layer:DoorArchGeometry.Layer.values()) for(boolean masonry:new boolean[]{false,true}) {
                    var edges=new HashMap<Edge,Integer>();var top=DoorArchFitPlan.topFor(design);
                    for(var cell:ArchPieceCell.of(top))add(edges,cell,Direction.NORTH,
                            DoorArchModels.quads(design,thickness,depth,layer,cell,Direction.NORTH,sprite,masonry));
                    check(edges,design+" "+thickness+" "+depth+" "+layer+" masonry="+masonry);
                }
        }
    }
    @Test void separationDoesNotOpenEvenASingleCuboid() {
        var edges=new HashMap<Edge,Integer>();
        for(var face:Direction.values()) {
            var b=io.github.dirtyredz.roundwood.client.render.SurfaceSeparation.ARCH_BODY.apply(new Surface(Box.cell(),face,"")).backing();
            assertTrue(b.minX()<=0 && b.minY()<=0 && b.maxX()>=16 && b.maxY()>=16,"outer joins must not retreat from adjacent blocks");
            double[] lo={b.minX(),b.minY(),b.minZ()},hi={b.maxX(),b.maxY(),b.maxZ()};
            int axis=face.getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;var points=new ArrayList<Point>();
            for(int[] corner:new int[][]{{0,0},{1,0},{1,1},{0,1}}) {
                double[] p=lo.clone();p[axis]=face.getAxisDirection()==Direction.AxisDirection.POSITIVE?hi[axis]:lo[axis];
                p[u]=corner[0]==0?lo[u]:hi[u];p[v]=corner[1]==0?lo[v]:hi[v];points.add(new Point(p[0],p[1],p[2]));
            }
            edges(edges,points);
        }
        check(edges,"separated cuboid without any T-junctions");
    }
    @Test void standaloneLayersShareEveryEdgeExactly() {
        try(var contents=new SpriteContents(TEXTURE,new FrameSize(16,16),new NativeImage(16,16,false),AnimationMetadataSection.EMPTY)) {
            var sprite=new Sprite(contents);
            for(var design:ArchPieceDesign.values()) for(var side:KeyholeShoulderProfile.Side.values())
                for(var thickness:ArchPieceThickness.values()) for(var depth:ArchPieceDepth.values())
                    for(var layer:ArchPieceModels.Layer.values()) for(var facing:Direction.Plane.HORIZONTAL) for(boolean masonry:new boolean[]{false,true}) {
                        var edges=new HashMap<Edge,Integer>();double back=thickness.back(depth),front=thickness.front(depth);
                        if(layer==ArchPieceModels.Layer.CASING){back=front;front++;}
                        for(var cell:ArchPieceCell.of(design))add(edges,cell,facing,ArchPieceModels.quads(design,side,cell,back,front,facing,sprite,masonry,layer));
                        check(edges,design+" "+side+" "+thickness+" "+depth+" "+layer+" "+facing+" masonry="+masonry);
                    }
        }
    }
    private static void add(Map<Edge,Integer> edges,ArchPieceCell cell,Direction facing,List<BakedQuad> quads) {
        for(var quad:quads) {
            var data=quad.getVertices();int stride=data.length/4;var points=new ArrayList<Point>();
            for(int i=0;i<data.length;i+=stride) {
                double x=Float.intBitsToFloat(data[i]),y=Float.intBitsToFloat(data[i+1]),z=Float.intBitsToFloat(data[i+2]);
                for(int turn=0;turn<(4-VoxelShapeFactory.quarterTurns(facing))%4;turn++){double nx=1-z;z=x;x=nx;}
                var point=new Point(Math.rint((x+cell.column)*1e5),Math.rint((y+cell.row)*1e5),Math.rint(z*1e5));
                if(!points.contains(point))points.add(point);
            }
            edges(edges,points);
        }
    }
    private static void edges(Map<Edge,Integer> edges,List<Point> points) {
        for(int i=0;i<points.size();i++){var a=points.get(i);var b=points.get((i+1)%points.size());if(!a.equals(b))edges.merge(a.compareTo(b)<0?new Edge(a,b):new Edge(b,a),1,Integer::sum);}
    }
    private static void check(Map<Edge,Integer> edges,String label) {
        for(var e:edges.entrySet())if(e.getValue()!=2)fail(label+" unmatched edge (gap or T-junction): "+e);
    }
}
