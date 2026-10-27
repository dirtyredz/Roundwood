package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.hardware.*;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.*;
import io.github.dirtyredz.roundwood.showcase.section.arch.ArchCompositionsSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import java.util.Optional;

/** Four component examples beside the fitted row, without a material/design cross product. */
public final class DoorHardwareSection implements ShowcaseSection {
    public static final int F=ArchCompositionsSection.FITTED_F;
    public static final int R0=ShowcaseSpacing.FIRST_R+4*ShowcaseSpacing.pitch(3)-ShowcaseSpacing.LABEL_OFFSET;
    public static final Appearance HINGES=surface("iron_block"), KNOBS=surface("gold_block");
    public static final DoorHardware HARDWARE=new DoorHardware(Optional.of(HINGES),Optional.of(KNOBS));
    public static int r(int index){return R0+index*ShowcaseSpacing.pitch(3);}
    public static Appearance body(int index){return index%2==0?surface("oxidized_copper"):surface("quartz_bricks");}
    private static Appearance surface(String block){return Appearance.surface(ResourceLocation.fromNamespaceAndPath("minecraft",block));}
    @Override public String id(){return "door_hardware";}
    @Override public void build(ShowcaseCanvas canvas){
        var blocks=ModBlocks.doors();
        for(int i=0;i<blocks.size();i++){
            canvas.sign(r(i),F,"Independent hardware","Iron hinges","Gold knobs","Click a component");
            if(canvas.door(blocks.get(i),body(i),r(i),F,DoorHingeSide.LEFT,false)
                    && canvas.level().getBlockEntity(canvas.pos(r(i),0,F)) instanceof RoundDoorBlockEntity entity)
                entity.setHardware(HARDWARE);
            else canvas.problem("hardware sample missing at "+r(i));
        }
    }
}
