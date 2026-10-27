package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Insert only existing edge vertices, including mortar vertices and vertices in adjacent cells. */
final class ArchMeshTopology {
    record CellQuad(ArchPieceCell cell,BakedQuad quad) {}
    private record Point(long x,long y,long z) {
        static Point of(double x,double y,double z){return new Point(Math.round(x*1e6),Math.round(y*1e6),Math.round(z*1e6));}
        Vec3 vector(){return new Vec3(x/1e6,y/1e6,z/1e6);}
    }
    private record Vertex(Point point,int[] data) {}
    private ArchMeshTopology() {}
    static List<CellQuad> conform(List<CellQuad> input) {
        var points=new LinkedHashSet<Point>();
        var polygons=new ArrayList<List<Vertex>>();
        for(var entry:input) {
            var polygon=new ArrayList<Vertex>();var data=entry.quad.getVertices();int stride=data.length/4;
            for(int v=0;v<4;v++) {
                int i=v*stride;
                var point=Point.of(Float.intBitsToFloat(data[i])+entry.cell.column,Float.intBitsToFloat(data[i+1])+entry.cell.row,Float.intBitsToFloat(data[i+2]));
                if(polygon.stream().noneMatch(p->p.point.equals(point)))polygon.add(new Vertex(point,Arrays.copyOfRange(data,i,i+stride)));
                points.add(point);
            }
            polygons.add(polygon);
        }
        var result=new ArrayList<CellQuad>();
        for(int index=0;index<input.size();index++) {
            var entry=input.get(index);var original=polygons.get(index);var boundary=new ArrayList<Vertex>();
            for(int edge=0;edge<original.size();edge++) {
                var a=original.get(edge);var b=original.get((edge+1)%original.size());boundary.add(a);
                var interior=new TreeMap<Double,Point>();
                for(var p:points){double t=along(a.point,b.point,p);if(t>1e-7 && t<1-1e-7)interior.put(t,p);}
                interior.forEach((t,p)->boundary.add(interpolate(a,b,t,p)));
            }
            if(boundary.size()==original.size()){result.add(new CellQuad(entry.cell,quad(entry,boundary)));continue;}
            var triangles=new ArrayList<List<Vertex>>();
            // Convex ear clipping retains collinear boundary vertices; a fan would skip the
            // subdivisions on the two edges touching its root and recreate the T-junction.
            while(boundary.size()>3) {
                boolean cut=false;
                for(int i=0;i<boundary.size();i++) {
                    var a=boundary.get((i+boundary.size()-1)%boundary.size());var b=boundary.get(i);var c=boundary.get((i+1)%boundary.size());
                    if(b.point.vector().subtract(a.point.vector()).cross(c.point.vector().subtract(a.point.vector())).lengthSqr()<1e-18)continue;
                    boolean blocked=false;
                    for(var p:boundary)if(p!=a && p!=b && p!=c){double t=along(a.point,c.point,p.point);if(t>1e-7 && t<1-1e-7){blocked=true;break;}}
                    if(blocked)continue;
                    triangles.add(List.of(a,b,c));boundary.remove(i);cut=true;break;
                }
                if(!cut)throw new IllegalStateException("Degenerate arch mesh boundary");
            }
            triangles.add(List.copyOf(boundary));
            for(int i=0;i<triangles.size();i++) {
                var polygon=triangles.get(i);if(polygon==null)continue;
                for(int j=i+1;j<triangles.size();j++) {
                    var next=triangles.get(j);if(next==null)continue;
                    var merged=merge(polygon,next);
                    if(merged!=null){polygon=merged;triangles.set(j,null);break;}
                }
                result.add(new CellQuad(entry.cell,quad(entry,polygon)));
            }
        }
        return List.copyOf(result);
    }
    private static List<Vertex> merge(List<Vertex> a,List<Vertex> b) {
        for(int i=0;i<3;i++)for(int j=0;j<3;j++) {
            if(!a.get(i).point.equals(b.get((j+1)%3).point) || !a.get((i+1)%3).point.equals(b.get(j).point))continue;
            var polygon=List.of(a.get((i+1)%3),a.get((i+2)%3),a.get(i),b.get((j+2)%3));
            Vec3 normal=null;
            for(int k=0;k<4;k++) {
                var p=polygon.get(k).point.vector();
                var cross=polygon.get((k+1)%4).point.vector().subtract(p).cross(polygon.get((k+2)%4).point.vector().subtract(p));
                if(cross.lengthSqr()<1e-18 || normal!=null && normal.dot(cross)<=0)return null;
                normal=cross;
            }
            return polygon;
        }
        return null;
    }
    private static double along(Point a,Point b,Point p) {
        double dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z,px=p.x-a.x,py=p.y-a.y,pz=p.z-a.z;
        double length=dx*dx+dy*dy+dz*dz;if(length==0)return -1;
        double t=(px*dx+py*dy+pz*dz)/length;
        if(t<=1e-7 || t>=1-1e-7)return -1;
        px-=t*dx;py-=t*dy;pz-=t*dz;
        return px*px+py*py+pz*pz<=2?t:-1;
    }
    private static Vertex interpolate(Vertex a,Vertex b,double t,Point point) {
        var data=a.data.clone();
        for(int axis=4;axis<=5;axis++)data[axis]=Float.floatToRawIntBits((float)(Float.intBitsToFloat(a.data[axis])*(1-t)+Float.intBitsToFloat(b.data[axis])*t));
        return new Vertex(point,data);
    }
    private static BakedQuad quad(CellQuad source,List<Vertex> vertices) {
        int stride=vertices.get(0).data.length;var data=new int[stride*4];
        for(int i=0;i<4;i++) {
            var v=vertices.get(Math.min(i,vertices.size()-1));System.arraycopy(v.data,0,data,i*stride,stride);
            var p=v.point.vector();data[i*stride]=Float.floatToRawIntBits((float)(p.x-source.cell.column));
            data[i*stride+1]=Float.floatToRawIntBits((float)(p.y-source.cell.row));data[i*stride+2]=Float.floatToRawIntBits((float)p.z);
        }
        var q=source.quad;return new BakedQuad(data,q.getTintIndex(),q.getDirection(),q.getSprite(),q.isShade(),q.hasAmbientOcclusion());
    }
}
