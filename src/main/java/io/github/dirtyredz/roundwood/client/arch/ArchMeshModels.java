package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.render.SurfaceSeparation;
import io.github.dirtyredz.roundwood.client.render.SurfaceQuadBaker;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import java.util.*;

/** Bake a complete layer before cutting it back into owning-cell models; topology crosses cells. */
final class ArchMeshModels {
    private record Key(ArchPieceDesign design,KeyholeShoulderProfile.Side side,double back,double front,
                       Direction facing,TextureAtlasSprite sprite,boolean masonry) {}
    private static final AppearanceModelCache<Key,Map<ArchPieceCell,List<BakedQuad>>> MESHES=new AppearanceModelCache<>(128);
    private ArchMeshModels() {}
    static List<BakedQuad> quads(ArchPieceDesign design,KeyholeShoulderProfile.Side side,ArchPieceCell cell,
                                 double back,double front,Direction facing,TextureAtlasSprite sprite,boolean masonry,
                                 ArchPieceModels.Layer layer) {
        var key=new Key(design,side,back,front,facing,sprite,masonry);
        var mesh=MESHES.get(key,ArchMeshModels::build).get(cell);
        int turns=VoxelShapeFactory.quarterTurns(facing);
        var separation=layer==ArchPieceModels.Layer.BODY?SurfaceSeparation.ARCH_BODY:SurfaceSeparation.ARCH_CASING;
        return mesh.stream().map(q->transform(q,turns,separation.pixels(),back,front,cell)).toList();
    }
    private static Map<ArchPieceCell,List<BakedQuad>> build(Key key) {
        int turns=VoxelShapeFactory.quarterTurns(key.facing);
        var all=new ArrayList<ArchMeshTopology.CellQuad>();
        var joints=key.masonry?ArchMasonryModels.joints(key.design):List.<List<PolygonPartition.Point>>of();
        for(var cell:ArchPieceCell.of(key.design)) {
            for(var surface:ArchPieceGeometry.surfaces(key.design,key.side,cell.column,cell.row,key.back,key.front,"")) {
                var quad=SurfaceQuadBaker.bake(surface.backing().rotateY(turns),VoxelShapeFactory.turn(surface.face(),turns),key.sprite);
                var quads=key.masonry && surface.face().getAxis()==Direction.Axis.Z
                        ?ArchMasonryModels.bake(surface,quad,cell.column*16,cell.row*16,turns,joints):List.of(quad);
                for(var q:quads)all.add(new ArchMeshTopology.CellQuad(cell,transform(q,(4-turns)%4,0,key.back,key.front,cell)));
            }
        }
        var result=new EnumMap<ArchPieceCell,List<BakedQuad>>(ArchPieceCell.class);
        for(var cell:ArchPieceCell.of(key.design))result.put(cell,new ArrayList<>());
        for(var entry:ArchMeshTopology.conform(all))result.get(entry.cell()).add(entry.quad());
        result.replaceAll((cell,quads)->List.copyOf(quads));return Map.copyOf(result);
    }
    private static BakedQuad transform(BakedQuad q,int turns,double separation,double back,double front,ArchPieceCell cell) {
        var data=q.getVertices().clone();int stride=data.length/4;
        for(int i=0;i<data.length;i+=stride) {
            double x=Float.intBitsToFloat(data[i])*16,y=Float.intBitsToFloat(data[i+1])*16,z=Float.intBitsToFloat(data[i+2])*16;
            x+=x+cell.column*16==0?-separation:separation;
            y+=y+cell.row*16==0?-separation:separation;
            z+=separation*(2*(z-back)/(front-back)-1);
            for(int turn=0;turn<turns;turn++){double nx=16-z;z=x;x=nx;}
            data[i]=Float.floatToRawIntBits((float)(x/16));data[i+1]=Float.floatToRawIntBits((float)(y/16));data[i+2]=Float.floatToRawIntBits((float)(z/16));
        }
        return new BakedQuad(data,q.getTintIndex(),q.getDirection(),q.getSprite(),q.isShade(),q.hasAmbientOcclusion());
    }
}
