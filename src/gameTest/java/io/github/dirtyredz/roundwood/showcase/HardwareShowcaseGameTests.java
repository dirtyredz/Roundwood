package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.showcase.section.MaterialsSection;
import io.github.dirtyredz.roundwood.showcase.section.door.*;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowEdgesSection;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.block.window.geometry.face.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HardwareShowcaseGameTests {
    @GameTest(template="showcase",timeoutTicks=200)
    public static void feedbackExamplesSurviveIntegrity(GameTestHelper helper){
        var frame=ShowcaseFrame.at(helper.absolutePos(new BlockPos(46,2,10)),Direction.SOUTH);
        var bounds=frame.bounds();var level=helper.getLevel();
        for(var pos:BlockPos.betweenClosed(bounds.minX(),frame.floorY(),bounds.minZ(),bounds.maxX(),frame.floorY(),bounds.maxZ()))
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        var canvas=new ShowcaseCanvas(level,frame);
        new io.github.dirtyredz.roundwood.showcase.section.PiecesSection().build(canvas);
        new DoorHardwareSection().build(canvas);new MaterialsSection().build(canvas);new WindowEdgesSection().build(canvas);canvas.verifyLabels();
        check(canvas.problems().isEmpty(),"feedback showcase problems: "+canvas.problems());
        HardwareShowcaseChecks.verify(level,frame);
        helper.runAfterDelay(3,()->{
            HardwareShowcaseChecks.verify(level,frame);
            io.github.dirtyredz.roundwood.showcase.section.SectionChecks.verifyPieces(level,frame);
            io.github.dirtyredz.roundwood.showcase.section.SectionChecks.verifyMaterials(level,frame);
            var centre=frame.pos(WindowEdgesSection.WIDE_R+1,1,WindowEdgesSection.ROW_F);
            check(level.getBlockState(centre).is(ModBlocks.window(WindowDesign.plain(WindowProfile.BLANK))),"frameless centre missing");
            var surfaces=ShowcaseSamples.surfaces(canvas.samples());check(surfaces.size()==14,"surface wall count");
            for(int i=0;i<surfaces.size();i++)check(level.getBlockState(frame.pos(MaterialsSection.R0+i,MaterialsSection.SURFACE_Y,MaterialsSection.F)).is(MaterialsSection.block(surfaces.get(i))),"surface wall missing");
            helper.succeed();
        });
    }
}
