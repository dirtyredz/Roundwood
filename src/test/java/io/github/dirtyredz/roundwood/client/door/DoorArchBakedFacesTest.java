package io.github.dirtyredz.roundwood.client.door;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimGeometry;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceModels;
import io.github.dirtyredz.roundwood.client.render.SurfaceQuadBaker;
import io.github.dirtyredz.roundwood.geometry.*;
import io.github.dirtyredz.roundwood.testsupport.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static io.github.dirtyredz.roundwood.testsupport.BakedCoplanarity.*;
import static org.junit.jupiter.api.Assertions.*;

/** Y is yaw-invariant. Vertical normals can be parallel only at 0/90 degrees in this geometry. */
class DoorArchBakedFacesTest {
    static { MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("test","donor");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents){super(TEXTURE,contents,256,256,32,64);}
    }
    @Test void bakedFacesRemainSeparatedThroughoutTheContinuousSwing() throws Exception {
        int poses=0;
        try(var contents=new SpriteContents(TEXTURE,new FrameSize(16,16),new NativeImage(16,16,false),AnimationMetadataSection.EMPTY)) {
            var sprite=new Sprite(contents);
            for(var design:DoorDesign.values()) for(var hinge:DoorHingeSide.values()) {
                var leaf=leaf(design,hinge,sprite);
                for(var facing:Direction.Plane.HORIZONTAL) for(var depth:DoorDepth.values()) {
                    var casings=casings(design,depth,facing,sprite);
                    for(var thickness:ArchPieceThickness.values()) for(var frameDepth:ArchPieceDepth.values()) {
                        if(!DoorArchClearance.accepts(thickness,frameDepth,depth)) continue;
                        var frames=frames(design,thickness,frameDepth,facing,sprite,0);
                        prove(design,hinge,depth,facing,leaf,frames,casings);
                        poses++;
                    }
                    for(int adjacent:new int[]{-16,16}) {
                        var frames=frames(design,ArchPieceThickness.FULL,ArchPieceDepth.FRONT,facing,sprite,adjacent);
                        prove(design,hinge,depth,facing,leaf,frames,casings);
                        poses++;
                    }
                }
            }
        }
        assertEquals(480,poses,"pose matrix unexpectedly changed");
        System.out.println("Continuous baked separation: "+poses+" poses; both vertical roots and invariant Y planes");
    }
    private static void prove(DoorDesign design,DoorHingeSide hinge,DoorDepth depth,Direction facing,
                               List<BakedQuad> leaf,List<Rect> frames,List<Rect> casings) {
        disjoint(casings,frames,"static casings/frame");
        var stationary=new ArrayList<>(frames); stationary.addAll(casings);
        for(int angle:new int[]{0,90}) {
            var moving=leaf.stream().map(q->rectangle(q,p->{
                if(design.doubleLeaf()){p[0]+=16;p[1]=(p[1]+16)/DoorLeafModels.modelYScale(design);}
                var turned=DoorGeometry.turn(design,p[0],p[2],hinge==DoorHingeSide.LEFT?angle:-angle,hinge);
                return rotate(new double[]{turned[0],p[1],turned[1]+depth.offset()},facing);
            })).toList();
            if(angle==0) horizontalPlanesDistinct(moving,stationary);
            disjoint(moving,stationary,design+" "+hinge+" "+depth+" "+facing+" angle="+angle);
        }
    }
    private static List<BakedQuad> leaf(DoorDesign design,DoorHingeSide hinge,TextureAtlasSprite sprite) throws Exception {
        var model=BlockModel.fromString(Files.readString(Path.of("src/generated/resources/assets/roundwood/models/block/template_"
                +design.id()+"_leaf_"+hinge.getSerializedName()+".json")));
        var bakery=new FaceBakery(); var out=new ArrayList<BakedQuad>();
        for(var element:model.getElements()) for(var face:element.faces.entrySet())
            out.add(bakery.bakeQuad(element.from,element.to,face.getValue(),sprite,face.getKey(),BlockModelRotation.X0_Y0,
                    element.rotation,element.shade,TEXTURE));
        return out;
    }
    private static List<Rect> casings(DoorDesign design,DoorDepth depth,Direction facing,TextureAtlasSprite sprite) {
        var out=new ArrayList<Rect>();
        for(var part:DoorLayout.Part.of(design)) for(var face:AttachmentFace.values())
            for(var quad:io.github.dirtyredz.roundwood.client.render.PixelSlabModels.quads(
                    DoorTrimModels.surfaces(design,face,part.columnIndex(),part.row()),part.columnIndex(),part.row(),0,false,
                    io.github.dirtyredz.roundwood.client.material.TrimLooks.Look.tiled(TEXTURE),
                    new io.github.dirtyredz.roundwood.client.render.PixelSlabModels.Extent(design.width(),design.height(),Math.max(design.width(),design.height())), ignored->sprite)) {
                out.add(rectangle(quad,p->rotate(new double[]{p[0]+part.columnIndex()*16,p[1]+part.row()*16,p[2]+depth.offset()},facing)));
            }
        return out;
    }
    private static List<Rect> frames(DoorDesign design,ArchPieceThickness thickness,ArchPieceDepth depth,Direction facing,TextureAtlasSprite sprite,int adjacent) {
        var top=DoorArchFitPlan.topFor(design); var out=new ArrayList<Rect>();
        for(var cell:ArchPieceCell.of(top)) for(var layer:DoorArchGeometry.Layer.values()) {
            List<Surface> surfaces;
            if(adjacent==0) surfaces=DoorArchRenderer.surfaces(design,thickness,depth,layer,cell.column,cell.row);
            else if(layer==DoorArchGeometry.Layer.BODY) surfaces=ArchPieceModels.renderSurfaces(top,KeyholeShoulderProfile.Side.LEFT,cell,0,16,ArchPieceModels.Layer.BODY);
            else {
                var band=ArchPieceTrimGeometry.depth(layer==DoorArchGeometry.Layer.FRONT_CASING?AttachmentFace.FRONT:AttachmentFace.BACK);
                surfaces=ArchPieceModels.renderSurfaces(top,KeyholeShoulderProfile.Side.LEFT,cell,band[0],band[1],ArchPieceModels.Layer.CASING);
            }
            for(var quad:ArchPieceModels.bakeSurfaces(top,cell,surfaces,facing,sprite,false))
                out.add(rectangle(quad,p->{
                    // Undo the baker's per-cell facing, add the authored cell origin, then apply the world pose.
                    double x=p[0],z=p[2];
                    for(int i=0;i<(4-VoxelShapeFactory.quarterTurns(facing))%4;i++){double nx=16-z;z=x;x=nx;}
                    return rotate(new double[]{x+cell.column*16,p[1]+cell.row*16+DoorArchFitPlan.topOffset(design)*16,z+adjacent},facing);
                }));
        }
        return out;
    }
    private static double[] rotate(double[] p,Direction facing) {
        double x=p[0],z=p[2];
        for(int i=0;i<VoxelShapeFactory.quarterTurns(facing);i++){double nx=16-z;z=x;x=nx;}
        return new double[]{x,p[1],z};
    }
}
