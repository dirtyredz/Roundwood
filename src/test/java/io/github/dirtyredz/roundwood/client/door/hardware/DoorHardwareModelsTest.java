package io.github.dirtyredz.roundwood.client.door.hardware;

import io.github.dirtyredz.roundwood.client.door.leaf.DoorLeafModels;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.hardware.*;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class DoorHardwareModelsTest {
    static { MinecraftBootstrap.ensure(); }
    private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("test","hardware");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents,int x){super(ID,contents,256,256,x,64);}
    }
    @Test void bakedMarkersDistinguishSlotsSharingBrassAndKeepAuthoredUvScale() throws Exception {
        try(var contents=new SpriteContents(ID,new FrameSize(16,16),new NativeImage(16,16,false),net.minecraft.server.packs.resources.ResourceMetadata.EMPTY)) {
            var brass=new Sprite(contents,0);var iron=new Sprite(contents,32);var gold=new Sprite(contents,64);
            var a=Appearance.surface(ResourceLocation.fromNamespaceAndPath("minecraft","iron_block"));
            var b=Appearance.surface(ResourceLocation.fromNamespaceAndPath("minecraft","gold_block"));
            var looks=new DoorHardware(Optional.of(a),Optional.of(b));
            for(var design:DoorDesign.values()) for(var hinge:DoorHingeSide.values()) {
                var model=BlockModel.fromString(Files.readString(io.github.dirtyredz.roundwood.testsupport.ProjectFiles.path("src/generated/resources/assets/roundwood/models/block/template_"+design.id()+"_leaf_"+hinge.getSerializedName()+".json")));
                int hinges=0,knobs=0;
                for(var element:model.getElements()) for(var face:element.faces.entrySet()) {
                    var q=new FaceBakery().bakeQuad(element.from,element.to,face.getValue(),brass,face.getKey(),BlockModelRotation.X0_Y0,element.rotation,element.shade);
                    var mapped=DoorHardwareModels.retexture(q,looks,look->look.equals(a)?iron:gold);
                    if(face.getValue().texture().equals("#panel")){assertSame(q,mapped);continue;}
                    boolean isHinge=face.getValue().texture().equals("#hinges");
                    assertEquals(isHinge?1:2,q.getTintIndex());
                    assertSame(isHinge?iron:gold,mapped.getSprite());assertFalse(mapped.isTinted());
                    assertSame(brass,DoorHardwareModels.retexture(q,DoorHardware.DEFAULT,look->iron).getSprite());
                    assertFalse(DoorHardwareModels.retexture(q,DoorHardware.DEFAULT,look->iron).isTinted());
                    if(isHinge)hinges++;else knobs++;
                    var data=q.getVertices();int stride=data.length/4;
                    double[] first=null,uv0=null;
                    for(int i=0;i<data.length;i+=stride){
                        double x=Float.intBitsToFloat(data[i])*16,y=Float.intBitsToFloat(data[i+1])*16,z=Float.intBitsToFloat(data[i+2])*16;
                        if(design.doubleLeaf()){x+=16;y=(y+16)/DoorLeafModels.modelYScale(design);}
                        double[] p=switch(q.getDirection()){
                            case SOUTH -> new double[]{x,-y};case NORTH -> new double[]{-x,-y};
                            case UP -> new double[]{x,z};case DOWN -> new double[]{x,-z};
                            case EAST -> new double[]{-z,-y};case WEST -> new double[]{z,-y};};
                        double[] uv={16 * brass.getUOffset(Float.intBitsToFloat(data[i+4])),16 * brass.getVOffset(Float.intBitsToFloat(data[i+5]))};
                        if(first==null){first=p;uv0=uv;}
                        for(int axis=0;axis<2;axis++)assertEquals(p[axis]-first[axis],(uv[axis]-uv0[axis])/(1-brass.uvShrinkRatio()),.0003,"hardware tile scale");
                    }
                }
                assertTrue(hinges>0 && knobs>0);
            }
        }
    }
}
