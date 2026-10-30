package io.github.dirtyredz.roundwood.client.door.leaf;

import io.github.dirtyredz.roundwood.client.door.DoorAppearanceModels;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorSurfaceModelsTest {
    static { MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("test","metal");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents){super(TEXTURE,contents,256,256,32,64);}
    }
    @Test void donorPanelsTileAtAuthoredScaleIncludingCompressedTallModels() {
        try(var contents=new SpriteContents(TEXTURE,new FrameSize(16,16),new NativeImage(16,16,false),net.minecraft.server.packs.resources.ResourceMetadata.EMPTY)) {
            var sprite=new Sprite(contents);
            for(var design:DoorDesign.values()) for(var hinge:DoorHingeSide.values()) {
                double frontArea=0;
                for(var q:DoorSurfaceModels.panels(design,hinge,sprite)) {
                    assertSame(sprite,q.getSprite());assertFalse(q.isTinted());
                    var data=q.getVertices();int stride=data.length/4;
                    double[] first=null,firstUv=null;
                    double minX=100,maxX=-100,minY=100,maxY=-100;
                    for(int i=0;i<data.length;i+=stride) {
                        double x=Float.intBitsToFloat(data[i])*16,y=Float.intBitsToFloat(data[i+1])*16,z=Float.intBitsToFloat(data[i+2])*16;
                        if(design.doubleLeaf()){x+=16;y=(y+16)/DoorLeafModels.modelYScale(design);}
                        minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                        double[] p=switch(q.getDirection()) {
                            case SOUTH -> new double[]{x,-y};case NORTH -> new double[]{-x,-y};
                            case UP -> new double[]{x,z};case DOWN -> new double[]{x,-z};
                            case EAST -> new double[]{-z,-y};case WEST -> new double[]{z,-y};
                        };
                        double[] uv={16 * sprite.getUOffset(Float.intBitsToFloat(data[i+4])),16 * sprite.getVOffset(Float.intBitsToFloat(data[i+5]))};
                        if(first==null){first=p;firstUv=uv;}
                        for(int a=0;a<2;a++) {
                            assertTrue(uv[a]>=0 && uv[a]<=16,"donor atlas bleed");
                            assertEquals(p[a]-first[a],(uv[a]-firstUv[a])/(1-sprite.uvShrinkRatio()),.0002);
                        }
                    }
                    if(q.getDirection()==Direction.SOUTH) frontArea+=(maxX-minX)*(maxY-minY);
                }
                int pixels=0;
                for(int x=0;x<design.width();x++) for(int y=0;y<design.height();y++)
                    if(design.inLeaf(x,y) && (!design.doubleLeaf() || (hinge==DoorHingeSide.LEFT?x<24:x>=24))) pixels++;
                assertEquals(pixels,frontArea,.0001,"donor panel coverage");
            }
        }
    }
    @Test void surfaceAndBoardLooksCannotShareACacheEntryAndReloadClearsThem() {
        var tiled=new DoorAppearanceModels.Look(TEXTURE,-1,true);
        var board=new DoorAppearanceModels.Look(TEXTURE,-1,false);
        var cache=new AppearanceModelCache<DoorAppearanceModels.Look,Object>(4);
        assertNotSame(cache.get(tiled,k->new Object()),cache.get(board,k->new Object()));
        AppearanceModelCache.clearAll();assertEquals(0,cache.size());
    }
}
