package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.showcase.section.arch.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

/** Bounded new-section readback; the complete four-facing showcase remains in the ANU suite. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FittedShowcaseGameTests {
    @GameTest(template="showcase",timeoutTicks=200)
    public static void thinAndFittedSamplesUseProductionPlacementAndSurviveIntegrity(GameTestHelper helper) {
        var frame=ShowcaseFrame.at(helper.absolutePos(new BlockPos(46,2,10)),Direction.SOUTH);
        var bounds=frame.bounds();var level=helper.getLevel();
        for(var pos:BlockPos.betweenClosed(bounds.minX(),frame.floorY(),bounds.minZ(),bounds.maxX(),frame.floorY(),bounds.maxZ()))
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        var canvas=new ShowcaseCanvas(level,frame);
        new ArchPiecesSection().build(canvas);new ArchCompositionsSection().build(canvas);canvas.verifyLabels();
        check(canvas.problems().isEmpty(),"showcase problems: "+canvas.problems());
        ArchSectionsChecks.verify(level,frame);
        helper.runAfterDelay(3,()->{ArchSectionsChecks.verify(level,frame);helper.succeed();});
    }
}
