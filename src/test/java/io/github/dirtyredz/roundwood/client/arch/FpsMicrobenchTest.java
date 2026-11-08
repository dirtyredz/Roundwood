package io.github.dirtyredz.roundwood.client.arch;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.block.window.geometry.*;
import io.github.dirtyredz.roundwood.block.window.geometry.face.*;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.*;
import io.github.dirtyredz.roundwood.client.door.*;
import io.github.dirtyredz.roundwood.client.material.*;
import io.github.dirtyredz.roundwood.client.render.*;
import io.github.dirtyredz.roundwood.client.window.trim.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Temporary Stage 2 harness. Synthetic sprite, no client, no GL, no world lighting. */
class FpsMicrobenchTest {
    public static void main(String[] args)throws Exception { new FpsMicrobenchTest().measure(); }
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    static final Path OUT=Path.of("C:/Users/dirty/projects/artifacts/minecraft-mods/jobs/2026-10-26-showcase-fps");
    static final ResourceLocation TEX=ResourceLocation.fromNamespaceAndPath("test","donor");
    static volatile Object sink;
    static volatile long checksum;
    static class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents c){super(TEX,c,256,256,32,64);}
    }
    record Draw(String group,String detail,BakedModel model,Supplier<BakedModel> lookup){}
    final List<Draw> draws=new ArrayList<>();
    final List<Draw> candidates=new ArrayList<>();
    final List<ArchPieceLayout.Assembly> arches=new ArrayList<>();
    final List<DoorLayout.Assembly> doors=new ArrayList<>();
    final List<Map<String,Object>> results=new ArrayList<>();
    final Counting consumer=new Counting();
    final PoseStack pose=new PoseStack();
    final ModelBlockRenderer renderer=new ModelBlockRenderer(null);
    final RandomSource random=RandomSource.create(42);
    final com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    static final java.lang.invoke.MethodHandle PUT_QUAD=putQuadHandle();
    static java.lang.invoke.MethodHandle putQuadHandle(){
        try{return java.lang.invoke.MethodHandles.privateLookupIn(ModelBlockRenderer.class,java.lang.invoke.MethodHandles.lookup()).findVirtual(ModelBlockRenderer.class,"putQuadData",
            java.lang.invoke.MethodType.methodType(void.class,net.minecraft.world.level.BlockAndTintGetter.class,net.minecraft.world.level.block.state.BlockState.class,BlockPos.class,VertexConsumer.class,PoseStack.Pose.class,BakedQuad.class,
            float.class,float.class,float.class,float.class,int.class,int.class,int.class,int.class,int.class));}catch(Exception e){throw new RuntimeException(e);}
    }
    Sprite sprite;
    int signs,characters;
    // SimpleBakedModel initializes RenderType -> ItemRenderer -> Items outside a Forge launch.
    // Equivalent general/side quad storage, without that unrelated static initialization.
    static BakedModel headlessModel(List<BakedQuad> input,TextureAtlasSprite sprite){
        var quads=List.copyOf(input);
        return new BakedModel(){
            public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState s,Direction d,RandomSource r){return d==null?quads:List.of();}
            public boolean useAmbientOcclusion(){return true;}
            public boolean usesBlockLight(){return true;}
            public boolean isGui3d(){return false;}
            public boolean isCustomRenderer(){return false;}
            public TextureAtlasSprite getParticleIcon(){return sprite;}
            public ItemOverrides getOverrides(){return null;}
        };
    }

    @SuppressWarnings("unchecked")
    static AppearanceModelCache<Object,BakedModel> cache(Class<?> owner,String field)throws Exception{
        var f=owner.getDeclaredField(field);f.setAccessible(true);return (AppearanceModelCache<Object,BakedModel>)f.get(null);
    }
    static Object key(Class<?> owner,Object... args)throws Exception{
        var c=Arrays.stream(owner.getDeclaredClasses()).filter(x->x.getSimpleName().equals("Key")).findFirst().orElseThrow();
        var ctor=c.getDeclaredConstructors()[0];ctor.setAccessible(true);return ctor.newInstance(args);
    }
    static String property(String s,String name,String fallback){
        var m=java.util.regex.Pattern.compile("(?:\\[|,)"+name+"=([^,\\]]+)").matcher(s);return m.find()?m.group(1):fallback;
    }
    static String id(String s){return s.substring(s.indexOf(':')+1,s.indexOf('}'));}
    static ResourceLocation texture(CompoundTag n){
        String m=n.getString("Material"),p=n.getString("Paint");
        return ResourceLocation.fromNamespaceAndPath("test",(m.isEmpty()?"oak":m.replace(':','/'))+(p.isEmpty()?"":"/"+p));
    }
    static ArchPieceModels.Look look(CompoundTag n){return new ArchPieceModels.Look(texture(n),n.getString("Kind").equals("masonry"));}
    void add(String group,String detail,List<BakedQuad> quads,Supplier<BakedModel> lookup){
        draws.add(new Draw(group,detail,headlessModel(quads,sprite),lookup));
    }
    void arch(JsonObject e,CompoundTag n)throws Exception{
        String state=e.get("state").getAsString(),id=id(state);
        var design=Arrays.stream(ArchPieceDesign.values()).filter(d->id.startsWith(d.id())).findFirst().orElseThrow();
        var side=Side.valueOf(property(state,"side","left").toUpperCase());
        var thickness=id.endsWith("_thin")?ArchPieceThickness.THIN:ArchPieceThickness.FULL;
        var depth=ArchPieceDepth.valueOf(property(state,"depth","front").toUpperCase());
        var a=new ArchPieceLayout.Assembly(pos(e),Direction.SOUTH,design,side,thickness,depth);arches.add(a);
        for(int layer=0;layer<3;layer++){
            var face=layer==1?AttachmentFace.FRONT:AttachmentFace.BACK;
            var tag=layer==0?n.getCompound("RoundwoodAppearance"):n.getCompound("ArchPieceTrim").getCompound(layer==1?"Front":"Back");
            if(layer>0 && tag.isEmpty())continue;
            var look=look(layer==0?tag:tag.getCompound("RoundwoodAppearance"));
            double back=a.back(),front=a.front();
            var kind=layer==0?ArchPieceModels.Layer.BODY:ArchPieceModels.Layer.CASING;
            if(layer>0){var d=io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry.depth(face,back,front);back=d[0];front=d[1];}
            for(var cell:a.parts()){
                var q=ArchPieceModels.quads(design,side,cell,back,front,a.facing(),sprite,look.masonry(),kind);
                var model=headlessModel(q,sprite);
                cache(ArchPieceModels.class,"MODELS").get(key(ArchPieceModels.class,design,side,cell,a.facing(),back,front,kind,look),k->model);
                Supplier<BakedModel> lookup=kind==ArchPieceModels.Layer.BODY?
                    ()->ArchPieceModels.body(design,side,cell,a.facing(),a.back(),a.front(),look):
                    ()->ArchPieceModels.casing(design,side,face,cell,a.facing(),a.back(),a.front(),look);
                add(layer==0?"arch_body":"arch_casing",id+":"+cell,q,lookup);
            }
        }
    }
    record LeafKey(DoorAppearanceModels.Look look,DoorDesign design,String variant){}
    final AppearanceModelCache<LeafKey,BakedModel> leafCache=new AppearanceModelCache<>(2048);
    final Map<ResourceLocation,BakedModel> modelManager=new HashMap<>();
    void door(JsonObject e,CompoundTag n)throws Exception{
        String state=e.get("state").getAsString(),id=id(state);
        var design=Arrays.stream(DoorDesign.values()).filter(d->id.equals(d.id())).findFirst().orElseThrow();
        var hinge=DoorHingeSide.valueOf(property(state,"hinge","left").toUpperCase());
        doors.add(new DoorLayout.Assembly(pos(e),Direction.SOUTH,hinge,design,DoorDepth.valueOf(property(state,"depth","front").toUpperCase())));
        for(var h:design.doubleLeaf()?DoorHingeSide.values():new DoorHingeSide[]{hinge}){
            var q=leafQuads(design,h);var m=headlessModel(q,sprite);
            boolean oak=n.getCompound("RoundwoodAppearance").isEmpty();
            var look=new DoorAppearanceModels.Look(oak?BuiltinWoodStyles.OAK_DOOR_PANEL:texture(n.getCompound("RoundwoodAppearance")),-1);
            modelManager.put(DoorLeafModels.location(design,h),m);
            leafCache.get(new LeafKey(look,design,h.getSerializedName()),k->m);
            add("door_leaf",design+":"+h,q,()->{
                var l=new DoorAppearanceModels.Look(look.panel(),-1);
                var template=modelManager.get(DoorLeafModels.location(design,h));
                return l.isOakTemplate()?template:leafCache.get(new LeafKey(l,design,h.getSerializedName()),k->{throw new AssertionError("miss");});
            });
        }
        var frame=n.getCompound("DoorArch");
        if(!frame.isEmpty()){
            var product=ArchPieceDesign.valueOf(frame.getString("Product"));
            var thick=ArchPieceThickness.valueOf(frame.getString("Thickness"));var depth=ArchPieceDepth.valueOf(frame.getString("Depth"));
            var cache=cache(DoorArchRenderer.class,"MODELS");
            for(var layer:DoorArchGeometry.Layer.values()){
                var mat=layer==DoorArchGeometry.Layer.BODY?frame.getCompound("Appearance"):frame.getCompound("Casings").getCompound(layer==DoorArchGeometry.Layer.FRONT_CASING?"Front":"Back").getCompound("RoundwoodAppearance");
                if(mat.isEmpty())continue;
                var look=look(mat);
                for(var cell:ArchPieceCell.of(product)){
                    var originalSurfaces=DoorArchGeometry.surfaces(design,thick,depth,layer,cell.column,cell.row);
                    var q=ArchPieceModels.bakeSurfaces(product,cell,originalSurfaces,Direction.SOUTH,sprite,look.masonry());
                    double back=thick.back(depth),front=thick.front(depth);
                    boolean body=layer==DoorArchGeometry.Layer.BODY;
                    if(!body){var band=io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry.depth(layer==DoorArchGeometry.Layer.FRONT_CASING?AttachmentFace.FRONT:AttachmentFace.BACK,back,front);back=band[0];front=band[1];}
                    var merged=body?ArchPieceGeometry.surfaces(product,Side.LEFT,cell.column,cell.row,back,front,""):
                        io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry.surfaces(product,Side.LEFT,cell.column,cell.row,back,front,"");
                    assertEquals(coverage(originalSurfaces),coverage(merged),"oriented surface coverage: "+design+layer+cell);
                    var cq=ArchPieceModels.bakeSurfaces(product,cell,merged,Direction.SOUTH,sprite,look.masonry());
                    var originalArea=areas(q);var candidateArea=areas(cq);
                    assertEquals(originalArea.keySet(),candidateArea.keySet());
                    for(var k:originalArea.keySet())assertEquals(originalArea.get(k),candidateArea.get(k),.0001,"area/normal/color "+k);
                    candidates.add(new Draw(body?"fitted_body_merged_candidate":"fitted_casing_merged_candidate",design+":"+thick+":"+depth+":"+cell,headlessModel(cq,sprite),()->null));
                    var m=headlessModel(q,sprite);
                    cache.get(List.of(design,Direction.SOUTH,thick,depth,layer,cell,look),k->m);
                    add(layer==DoorArchGeometry.Layer.BODY?"fitted_body":"fitted_casing",design+":"+thick+":"+depth+":"+cell,q,
                        ()->cache.get(List.of(design,Direction.SOUTH,thick,depth,layer,cell,look),k->{throw new AssertionError("miss");}));
                }
            }
        }
        if(n.contains("Camouflage")) for(var part:DoorLayout.Part.of(design)){
            var q=new ArrayList<BakedQuad>();
            for(var f:io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageGeometry.exposedFaces(design,part.columnIndex(),part.row()))
                q.add(SurfaceQuadBaker.bake(f.box(),Direction.valueOf(f.side().name()),sprite));
            add("camouflage",part.toString(),q,()->null);
        }
    }
    List<BakedQuad> leafQuads(DoorDesign d,DoorHingeSide h)throws Exception{
        var path=Path.of("src/generated/resources/assets/roundwood/models/block/template_"+d.id()+"_leaf_"+h.getSerializedName()+".json");
        var model=BlockModel.fromString(Files.readString(path));var out=new ArrayList<BakedQuad>();var bakery=new FaceBakery();
        for(var element:model.getElements())for(var entry:element.faces.entrySet())
            out.add(bakery.bakeQuad(element.from,element.to,entry.getValue(),sprite,entry.getKey(),BlockModelRotation.X0_Y0,element.rotation,element.shade,TEX));
        assertFalse(out.isEmpty());return out;
    }
    void window(JsonObject e,CompoundTag n)throws Exception{
        String id=id(e.get("state").getAsString());
        var profile=Arrays.stream(WindowProfile.values()).filter(p->p.id().equals(id)).findFirst().orElseThrow();
        var p=WindowPose.upright(Direction.NORTH,WindowDepth.CENTER);
        for(var slot:List.of(TrimSlot.PERIMETER,TrimSlot.SILL))for(var face:AttachmentFace.values()){
            var tag=n.getCompound(slot==TrimSlot.PERIMETER?"WindowTrim":"WindowSillTrim").getCompound(face==AttachmentFace.FRONT?"Front":"Back");
            if(tag.isEmpty())continue;
            var mat=tag.getCompound("RoundwoodAppearance");var tex=texture(mat);
            var look=new TrimLooks.Look(mat.getString("Kind").equals("masonry"),tex,tex);
            var side=face==AttachmentFace.FRONT?p.front():p.front().getOpposite();
            var q=new ArrayList<BakedQuad>();
            for(var s:WindowTrimSurfaces.surfaces(slot,profile,p,side))q.add(SurfaceQuadBaker.bake(s.backing(),s.face(),sprite));
            var m=headlessModel(q,sprite);
            cache(WindowTrimWorldModels.class,"MODELS").get(key(WindowTrimWorldModels.class,slot,profile,p,side,look),k->m);
            add(slot==TrimSlot.PERIMETER?"window_perimeter":"window_sill",profile+":"+face,q,
                ()->WindowTrimWorldModels.model(slot,profile,new WindowPose(p.front(),p.top(),p.depth()),side,new TrimLooks.Look(look.whole(),look.face(),look.edge())));
        }
    }
    static BlockPos pos(JsonObject e){var p=e.getAsJsonArray("pos");return new BlockPos(p.get(0).getAsInt(),p.get(1).getAsInt(),p.get(2).getAsInt());}
    static Map<String,Integer> coverage(List<io.github.dirtyredz.roundwood.geometry.Surface> surfaces){
        var out=new HashMap<String,Integer>();
        for(var s:surfaces){var b=s.backing();var d=s.face();double plane,u0,u1,v0,v1;
            switch(d.getAxis()){
                case X->{plane=d==Direction.EAST?b.maxX():b.minX();u0=b.minY();u1=b.maxY();v0=b.minZ();v1=b.maxZ();}
                case Y->{plane=d==Direction.UP?b.maxY():b.minY();u0=b.minX();u1=b.maxX();v0=b.minZ();v1=b.maxZ();}
                default->{plane=d==Direction.SOUTH?b.maxZ():b.minZ();u0=b.minX();u1=b.maxX();v0=b.minY();v1=b.maxY();}
            }
            for(double u=u0;u<u1;u++)for(double v=v0;v<v1;v++)out.merge(d+":"+Math.round(plane*256)+":"+u+":"+v,1,Integer::sum);
        }return out;
    }
    static Map<String,Double> areas(List<BakedQuad> quads){
        var out=new HashMap<String,Double>();for(var q:quads){var data=q.getVertices();int stride=data.length/4;var p=new org.joml.Vector3d[4];
            for(int i=0;i<4;i++)p[i]=new org.joml.Vector3d(Float.intBitsToFloat(data[i*stride]),Float.intBitsToFloat(data[i*stride+1]),Float.intBitsToFloat(data[i*stride+2]));
            double area=new org.joml.Vector3d(p[1]).sub(p[0]).cross(new org.joml.Vector3d(p[2]).sub(p[0])).length()/2+
                new org.joml.Vector3d(p[2]).sub(p[0]).cross(new org.joml.Vector3d(p[3]).sub(p[0])).length()/2;
            out.merge(q.getDirection()+":"+data[3],area,Double::sum);
        }return out;
    }
    void bench(String name,int calls,Runnable task){
        for(int i=0;i<4;i++)runFor(task,150_000_000L);
        var ns=new ArrayList<Double>();var bytes=new ArrayList<Double>();
        for(int i=0;i<7;i++){
            long b=bean.getThreadAllocatedBytes(Thread.currentThread().getId()),t=System.nanoTime();
            int n=runFor(task,200_000_000L);long elapsed=System.nanoTime()-t;
            bytes.add((bean.getThreadAllocatedBytes(Thread.currentThread().getId())-b)/(double)n);ns.add(elapsed/(double)n);
        }
        var row=new LinkedHashMap<String,Object>();row.put("name",name);row.put("calls_per_corpus",calls);row.put("ns_per_corpus",ns);row.put("bytes_per_corpus",bytes);
        results.add(row);System.out.println("FPS_BENCH "+new Gson().toJson(row));
        try{Files.writeString(OUT.resolve(System.getProperty("fps.output","microbench")+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}catch(Exception e){throw new RuntimeException(e);}
    }
    static int runFor(Runnable r,long duration){long stop=System.nanoTime()+duration;int n=0;do{for(int i=0;i<16;i++)r.run();n+=16;}while(System.nanoTime()<stop);return n;}
    void emit(Draw d){
        if(d.group().equals("door_leaf")){renderer.renderModel(pose.last(),consumer,null,d.model(),1,1,1,0xf000f0,655360,ModelData.EMPTY,null);return;}
        for(var q:d.model().getQuads(null,null,random,ModelData.EMPTY,null))try{
            PUT_QUAD.invokeExact(renderer,(net.minecraft.world.level.BlockAndTintGetter)null,(net.minecraft.world.level.block.state.BlockState)null,BlockPos.ZERO,(VertexConsumer)consumer,pose.last(),q,
                1F,1F,1F,1F,0xf000f0,0xf000f0,0xf000f0,0xf000f0,655360);
        }catch(Throwable t){throw new RuntimeException(t);}
    }
    <P> boolean partialComplete(io.github.dirtyredz.roundwood.block.assembly.Assembly<P> a,Map<BlockPos,P> map){
        var parts=a.parts();var present=new ArrayList<P>(parts.size());
        for(var part:parts){var p=a.pos(part);if(map.get(p)==part)present.add(part);}
        return present.size()==a.parts().size();
    }
    @Test void measure()throws Exception{
        bean.setThreadAllocatedMemoryEnabled(true);
        try(var contents=new SpriteContents(TEX,new FrameSize(16,16),new NativeImage(16,16,false),AnimationMetadataSection.EMPTY)){
            sprite=new Sprite(contents);
            for(var v:JsonParser.parseString(Files.readString(OUT.resolve("census.json"))).getAsJsonObject().getAsJsonArray("entities")){
                var e=v.getAsJsonObject();var n=TagParser.parseTag(e.get("nbt").getAsString());
                switch(e.get("type").getAsString()){
                    case "roundwood:arch_piece"->arch(e,n);
                    case "roundwood:round_door"->door(e,n);
                    case "roundwood:round_window"->window(e,n);
                    case "minecraft:sign"->{signs++;for(var side:List.of("front_text","back_text"))for(var line:n.getCompound(side).getList("messages",8)){
                        var j=JsonParser.parseString(line.getAsString());if(j.isJsonObject()&&j.getAsJsonObject().has("text"))characters+=j.getAsJsonObject().get("text").getAsString().replace(" ","").length();
                    }}
                }
            }
            assertEquals(241,draws.size());assertEquals(25,arches.size());assertEquals(18,doors.size());assertEquals(97,signs);
            var geometry=new ArrayList<Object>();
            for(var d:draws){consumer.vertices=0;emit(d);int q=d.model().getQuads(null,null,random).size();assertEquals(q*4L,consumer.vertices);geometry.add(Map.of("group",d.group(),"detail",d.detail(),"quads",q));}
            Files.writeString(OUT.resolve("geometry.json"),new GsonBuilder().setPrettyPrinting().create().toJson(geometry));
            var candidateGeometry=new ArrayList<Object>();for(var d:candidates){consumer.vertices=0;emit(d);int q=d.model().getQuads(null,null,random).size();assertEquals(q*4L,consumer.vertices);candidateGeometry.add(Map.of("group",d.group(),"detail",d.detail(),"quads",q));}
            Files.writeString(OUT.resolve("candidate-geometry.json"),new GsonBuilder().setPrettyPrinting().create().toJson(candidateGeometry));
            bench("empty",1,()->checksum++);
            for(var group:draws.stream().map(Draw::group).distinct().toList()){
                var list=draws.stream().filter(d->d.group().equals(group)).toList();
                if(!group.equals("camouflage"))bench(group+".warm_lookup",list.size(),()->{for(var d:list)sink=d.lookup().get();});
                bench(group+(group.equals("door_leaf")?".renderModel_partial":".putQuadData_partial"),list.size(),()->{for(var d:list)emit(d);checksum=Double.doubleToRawLongBits(consumer.sum)+consumer.vertices;});
            }
            for(var group:candidates.stream().map(Draw::group).distinct().toList()){
                var list=candidates.stream().filter(d->d.group().equals(group)).toList();
                bench(group+".putQuadData_partial",list.size(),()->{for(var d:list)emit(d);checksum=Double.doubleToRawLongBits(consumer.sum)+consumer.vertices;});
            }
            var am=new HashMap<BlockPos,ArchPieceCell>();for(var a:arches)for(var p:a.parts())am.put(a.pos(p),p);
            var dm=new HashMap<BlockPos,DoorLayout.Part>();for(var a:doors)for(var p:a.parts())dm.put(a.pos(p),p);
            bench("arch.assembly_partial_complete",arches.size(),()->{for(var a:arches){var copy=new ArchPieceLayout.Assembly(a.anchor(),a.facing(),a.design(),a.side(),a.thickness(),a.depth());if(!partialComplete(copy,am))throw new AssertionError();}});
            bench("door.assembly_partial_complete",doors.size(),()->{for(var a:doors){var copy=new DoorLayout.Assembly(a.anchor(),a.facing(),a.hinge(),a.design(),a.depth());if(!partialComplete(copy,dm))throw new AssertionError();}});
            var sign=net.minecraft.client.renderer.blockentity.SignRenderer.createSignLayer().bakeRoot();
            bench("sign97.board_model_only",97,()->{for(int i=0;i<97;i++)sign.render(pose,consumer,0xf000f0,655360);checksum=consumer.vertices;});
            var glyph=new net.minecraft.client.gui.font.glyphs.BakedGlyph(null,0,.1f,0,.1f,0,5,0,8);
            bench("sign97.glyph_emission_proxy",characters,()->{for(int i=0;i<characters;i++)glyph.render(false,i%20,0,pose.last().pose(),consumer,0,0,0,1,0xf000f0);checksum=Double.doubleToRawLongBits(consumer.sum);});
            System.out.println("SIGN_CHARACTERS "+characters);
            Files.writeString(OUT.resolve(System.getProperty("fps.output","microbench")+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));
        }
    }
    static final class Counting implements VertexConsumer{
        long vertices;double sum;
        public VertexConsumer vertex(double x,double y,double z){sum+=x+y+z;return this;}
        public VertexConsumer color(int r,int g,int b,int a){sum+=r+g+b+a;return this;}
        public VertexConsumer uv(float u,float v){sum+=u+v;return this;}
        public VertexConsumer overlayCoords(int u,int v){sum+=u+v;return this;}
        public VertexConsumer uv2(int u,int v){sum+=u+v;return this;}
        public VertexConsumer normal(float x,float y,float z){sum+=x+y+z;return this;}
        public void endVertex(){vertices++;}
        public void defaultColor(int r,int g,int b,int a){}
        public void unsetDefaultColor(){}
    }
}
