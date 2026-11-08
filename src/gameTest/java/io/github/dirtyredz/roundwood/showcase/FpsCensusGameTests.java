package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.*;

/** Temporary Stage 1 instrumentation, not a production test. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FpsCensusGameTests {
    static void add(Map<String,Integer> out,String key,int n){out.merge(key,n,Integer::sum);}
    @GameTest(template="showcase",timeoutTicks=600)
    public static void census(GameTestHelper helper) throws Exception {
        var level=helper.getLevel();
        var feet=helper.absolutePos(new BlockPos(46,2,46).relative(Direction.NORTH,(ShowcaseFrame.DEPTH+5)/2));
        var frame=ShowcaseFrame.at(feet,Direction.SOUTH);
        var bounds=frame.bounds();
        for(var pos:BlockPos.betweenClosed(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()))
            level.setBlock(pos,pos.getY()==frame.floorY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        var canvas=new ShowcaseCanvas(level,frame);
        var rows=new ArrayList<Object>();
        var previous=new TreeMap<String,Integer>();
        for(var section:ShowcaseSections.ALL){
            section.build(canvas);
            var current=count(helper,frame,null);
            var delta=new TreeMap<String,Integer>();
            current.forEach((k,v)->{int n=v-previous.getOrDefault(k,0);if(n!=0)delta.put(k,n);});
            rows.add(Map.of("section",section.id(),"counts",delta));
            previous.clear();previous.putAll(current);
        }
        canvas.verifyLabels();
        if(!canvas.problems().isEmpty())throw new AssertionError(canvas.problems().toString());
        helper.runAfterDelay(5,()->{
            try {
                var entities=new ArrayList<Object>();
                var totals=count(helper,frame,entities);
                var result=new LinkedHashMap<String,Object>();
                result.put("sections",rows);result.put("totals",totals);result.put("entities",entities);
                result.put("bounds",List.of(bounds.getXSpan(),bounds.getYSpan(),bounds.getZSpan()));
                result.put("problems",canvas.problems());
                var output=java.nio.file.Path.of("C:/Users/dirty/projects/artifacts/minecraft-mods/jobs/2026-10-26-showcase-fps/census.json");
                java.nio.file.Files.writeString(output,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
                System.out.println("FPS_CENSUS "+totals);
                helper.succeed();
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    static TreeMap<String,Integer> count(GameTestHelper helper,ShowcaseFrame frame,List<Object> entities){
        var out=new TreeMap<String,Integer>();var b=frame.bounds();var level=helper.getLevel();
        for(var pos:BlockPos.betweenClosed(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ())){
            var entity=level.getBlockEntity(pos);if(entity==null)continue;
            var id=BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString();add(out,"BE:"+id,1);
            var row=new LinkedHashMap<String,Object>();row.put("type",id);row.put("pos",List.of(pos.getX(),pos.getY(),pos.getZ()));
            row.put("state",entity.getBlockState().toString());row.put("nbt",entity.saveWithoutMetadata().toString());
            if(entity instanceof ArchPieceBlockEntity a){
                var assembly=ArchPieceLayout.of(pos,a.getBlockState());int cells=assembly.parts().size();
                add(out,"arch:"+assembly.design(),1);add(out,"tessellate:arch_body",cells);
                for(var face:AttachmentFace.values())if(a.trims().get(face).isPresent())add(out,"tessellate:arch_casing",cells);
            }
            if(entity instanceof RoundDoorBlockEntity d){
                var assembly=DoorLayout.of(pos,d.getBlockState());int cells=assembly.parts().size();
                add(out,"door:"+assembly.design(),1);add(out,"renderModel:leaf",assembly.design().doubleLeaf()?2:1);
                for(var face:AttachmentFace.values())if(d.trims().get(face).isPresent())add(out,"tessellate:door_trim",cells);
                if(d.camouflage().isPresent())add(out,"tessellate:camouflage",cells);
                if(d.arch().isPresent()){
                    var arch=d.arch().get();add(out,"fitted_arch",1);int n=ArchPieceCell.of(arch.product()).size();
                    add(out,"tessellate:fitted_body",n);
                    for(var face:AttachmentFace.values())if(arch.casings().get(face).isPresent())add(out,"tessellate:fitted_casing",n);
                }
            }
            if(entity instanceof RoundWindowBlockEntity w){
                if(w.anyAttachment())add(out,"window:any_attachment",1);else add(out,"window:bare",1);
                for(var slot:TrimSlot.values())for(var face:slot.addresses())if(w.trims(slot).get(face).isPresent())
                    add(out,"window_attachment:"+slot,1);
                boolean staticBer=w.trims(TrimSlot.PERIMETER).any()||w.trims(TrimSlot.SILL).any();
                if(staticBer)add(out,"window:BER_geometry",1);
                else if(w.anyAttachment())add(out,"window:beam_only",1);
            }
            if(entities!=null)entities.add(row);
        }
        return out;
    }
}
