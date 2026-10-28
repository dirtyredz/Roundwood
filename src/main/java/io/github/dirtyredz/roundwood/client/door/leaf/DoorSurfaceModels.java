package io.github.dirtyredz.roundwood.client.door.leaf;

import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.client.render.SurfaceQuadBaker;
import io.github.dirtyredz.roundwood.geometry.PixelSlab;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import java.util.*;
import java.util.function.Predicate;

/** Block-scale donor panels, tiled before legal model-coordinate compression. Hardware stays separate. */
public final class DoorSurfaceModels {
    private DoorSurfaceModels() {}
    public static BakedModel replacePanel(BakedModel template,DoorDesign design,DoorHingeSide hinge,boolean item,
                                          TextureAtlasSprite sprite,Predicate<BakedQuad> panel) {
        var quads=new ArrayList<>(template.getQuads(null,null,RandomSource.create(42)).stream().filter(panel.negate()).toList());
        for(var side:item && design.doubleLeaf()?DoorHingeSide.values():new DoorHingeSide[]{hinge})
            quads.addAll(panels(design,side,sprite));
        var culled=new EnumMap<Direction,List<BakedQuad>>(Direction.class);
        for(var direction:Direction.values()) culled.put(direction,List.of());
        return new SimpleBakedModel(quads,culled,template.useAmbientOcclusion(),template.usesBlockLight(),template.isGui3d(),
                sprite,template.getTransforms(),ItemOverrides.EMPTY);
    }
    static List<BakedQuad> panels(DoorDesign design,DoorHingeSide hinge,TextureAtlasSprite sprite) {
        var out=new ArrayList<BakedQuad>();
        for(int col=0;col<design.columns();col++) for(int row=0;row<design.height()/16;row++) {
            var surfaces=PixelSlab.cellFaces((x,y)->design.inLeaf(x,y) && (!design.doubleLeaf()
                    || (hinge==DoorHingeSide.LEFT?x<design.width()/2:x>=design.width()/2)),col*16,row*16,13,16,"");
            for(var surface:surfaces) {
                var quad=SurfaceQuadBaker.bake(surface.backing(),surface.face(),sprite);
                int[] vertices=quad.getVertices().clone(); int stride=vertices.length/4;
                for(int i=0;i<vertices.length;i+=stride) {
                    double x=Float.intBitsToFloat(vertices[i])*16+col*16;
                    double y=Float.intBitsToFloat(vertices[i+1])*16+row*16;
                    if(design.doubleLeaf()){x-=16;y=y*DoorLeafModels.modelYScale(design)-16;}
                    vertices[i]=Float.floatToRawIntBits((float)(x/16));
                    vertices[i+1]=Float.floatToRawIntBits((float)(y/16));
                }
                out.add(new BakedQuad(vertices,-1,quad.getDirection(),sprite,quad.isShade(),quad.hasAmbientOcclusion()));
            }
        }
        return List.copyOf(out);
    }
}
