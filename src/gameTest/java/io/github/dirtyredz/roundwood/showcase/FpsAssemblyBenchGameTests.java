package io.github.dirtyredz.roundwood.showcase;

import com.google.gson.*;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Stage 2 only: replay saved assembly states; does not regenerate or recount the showcase. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FpsAssemblyBenchGameTests {
    static final Path OUT=Path.of("C:/Users/dirty/projects/artifacts/minecraft-mods/jobs/2026-10-26-showcase-fps");
    static volatile long sink;
    record Sample(BlockPos pos,BlockState state){}
    static <T extends Comparable<T>> BlockState set(BlockState state,Property<T> p,String value){return state.setValue(p,p.getValue(value).orElseThrow());}
    static BlockState state(String s){
        String id=s.substring(s.indexOf('{')+1,s.indexOf('}'));
        var block=BuiltInRegistries.BLOCK.get(new ResourceLocation(id));var state=block.defaultBlockState();
        for(String entry:s.substring(s.indexOf('[')+1,s.length()-1).split(",")){
            var kv=entry.split("=");state=set(state,block.getStateDefinition().getProperty(kv[0]),kv[1]);
        }
        return state;
    }
    static int run(Runnable task,long duration){long end=System.nanoTime()+duration;int n=0;do{for(int i=0;i<16;i++)task.run();n+=16;}while(System.nanoTime()<end);return n;}
    static Map<String,Object> bench(String name,int calls,Runnable task){
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
        for(int i=0;i<4;i++)run(task,150_000_000L);
        var times=new ArrayList<Double>();var bytes=new ArrayList<Double>();
        for(int i=0;i<7;i++){
            long b=bean.getThreadAllocatedBytes(Thread.currentThread().getId()),t=System.nanoTime();int n=run(task,200_000_000L);long dt=System.nanoTime()-t;
            times.add(dt/(double)n);bytes.add((bean.getThreadAllocatedBytes(Thread.currentThread().getId())-b)/(double)n);
        }
        var result=Map.<String,Object>of("name",name,"calls_per_corpus",calls,"ns_per_corpus",times,"bytes_per_corpus",bytes);
        System.out.println("FPS_ASSEMBLY "+new Gson().toJson(result));return result;
    }
    @GameTest(template="showcase",timeoutTicks=600)
    public static void measure(GameTestHelper helper)throws Exception{
        var level=helper.getLevel();var arches=new ArrayList<Sample>();var doors=new ArrayList<Sample>();
        for(var v:JsonParser.parseString(Files.readString(OUT.resolve("census.json"))).getAsJsonObject().getAsJsonArray("entities")){
            var e=v.getAsJsonObject();String type=e.get("type").getAsString();
            if(!type.equals("roundwood:arch_piece")&&!type.equals("roundwood:round_door"))continue;
            var p=e.getAsJsonArray("pos");var pos=helper.absolutePos(new BlockPos(p.get(0).getAsInt()-4,p.get(1).getAsInt()+59,p.get(2).getAsInt()-19));
            var state=state(e.get("state").getAsString());
            if(type.equals("roundwood:arch_piece")){
                var a=ArchPieceLayout.of(pos,state);for(var part:a.parts())level.setBlock(a.pos(part),a.stateFor(part,state.getBlock()),2);
                if(!a.complete(level,state.getBlock()))throw new AssertionError("arch setup incomplete");arches.add(new Sample(pos,state));
            }else{
                var a=DoorLayout.of(pos,state);for(var part:a.parts())level.setBlock(a.pos(part),a.stateFor(part,state.getBlock(),false,false),2);
                if(!a.complete(level,state.getBlock()))throw new AssertionError("door setup incomplete");doors.add(new Sample(pos,state));
            }
        }
        if(arches.size()!=25||doors.size()!=18)throw new AssertionError("corpus mismatch");
        var results=new ArrayList<Object>();
        results.add(bench("arch.of_and_complete_server",arches.size(),()->{long ok=0;for(var s:arches)if(ArchPieceLayout.of(s.pos(),s.state()).complete(level,s.state().getBlock()))ok++;if(ok!=25)throw new AssertionError();sink=ok;}));
        results.add(bench("door.of_and_complete_server",doors.size(),()->{long ok=0;for(var s:doors)if(DoorLayout.of(s.pos(),s.state()).complete(level,s.state().getBlock()))ok++;if(ok!=18)throw new AssertionError();sink=ok;}));
        results.add(bench("arch.render_bounds_server",arches.size(),()->{double sum=0;for(var s:arches)sum+=level.getBlockEntity(s.pos()).getRenderBoundingBox().maxX;sink=Double.doubleToRawLongBits(sum);}));
        results.add(bench("door.render_bounds_server",doors.size(),()->{double sum=0;for(var s:doors)sum+=level.getBlockEntity(s.pos()).getRenderBoundingBox().maxX;sink=Double.doubleToRawLongBits(sum);}));
        Files.writeString(OUT.resolve("assembly-bench.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));
        helper.succeed();
    }
}
