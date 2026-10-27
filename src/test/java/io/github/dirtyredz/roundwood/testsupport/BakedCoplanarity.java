package io.github.dirtyredz.roundwood.testsupport;

import net.minecraft.client.renderer.block.model.BakedQuad;
import java.util.*;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

/** Rectangle proof over baked float positions, independent of reported quad normals. */
public final class BakedCoplanarity {
    private BakedCoplanarity() {}
    public record Rect(int axis, double plane, double[] min, double[] max) {}
    public static Rect rectangle(BakedQuad quad, UnaryOperator<double[]> transform) {
        var data=quad.getVertices(); int stride=data.length/4;
        double[][] points=new double[4][];
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(int i=0;i<4;i++) {
            points[i]=transform.apply(new double[]{Float.intBitsToFloat(data[i*stride])*16,
                    Float.intBitsToFloat(data[i*stride+1])*16,Float.intBitsToFloat(data[i*stride+2])*16});
            for(int a=0;a<3;a++){min[a]=Math.min(min[a],points[i][a]);max[a]=Math.max(max[a],points[i][a]);}
        }
        double[] u=new double[3],v=new double[3];
        for(int a=0;a<3;a++){u[a]=points[1][a]-points[0][a];v[a]=points[2][a]-points[0][a];}
        double[] normal={u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]};
        int axis=-1;
        for(int a=0;a<3;a++) if(Math.abs(normal[a])>1e-7){assertEquals(-1,axis,"non-axis-aligned geometry invalidates endpoint proof");axis=a;}
        assertTrue(axis>=0,"degenerate baked face");
        assertEquals(min[axis],max[axis],1e-6,"nonplanar face");
        for(int a=0;a<3;a++) if(a!=axis) assertTrue(max[a]>min[a]);
        // A triangle's AABB is a conservative superset: proving these bounds disjoint also
        // proves the conforming triangles disjoint. Retain the independent plane/winding checks.
        return new Rect(axis,min[axis],min,max);
    }
    public static void disjoint(List<Rect> moving,List<Rect> stationary,String context) {
        Map<String,List<Rect>> planes=new HashMap<>();
        for(var a:stationary) planes.computeIfAbsent(key(a),k->new ArrayList<>()).add(a);
        for(var a:moving) for(var b:planes.getOrDefault(key(a),List.of())) {
            boolean area=true;
            for(int i=0;i<3;i++) if(i!=a.axis && Math.min(a.max[i],b.max[i])-Math.max(a.min[i],b.min[i])<=1e-6) area=false;
            assertFalse(area,context+" coplanar area: "+a+" / "+b);
        }
    }
    public static void horizontalPlanesDistinct(List<Rect> moving,List<Rect> stationary) {
        Set<Long> planes=new HashSet<>();
        for(var r:stationary) if(r.axis==1) planes.add(Math.round(r.plane*65536));
        for(var r:moving) if(r.axis==1) assertFalse(planes.contains(Math.round(r.plane*65536)),"Y is invariant throughout yaw; horizontal planes must differ");
    }
    private static String key(Rect r){return r.axis+":"+Math.round(r.plane*65536);}
}
