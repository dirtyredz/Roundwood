package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.showcase.section.PiecesSection;
import io.github.dirtyredz.roundwood.showcase.section.door.DoorHardwareSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowEdgesSection;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackShowcaseLayoutTest {
    @Test void everyPieceFitsTheExistingFloorAndBlankHasAnAssemblyExample(){
        assertTrue(PiecesSection.R_MAX<ShowcaseFrame.WIDTH,"pieces extend beyond showcase floor");
        for(var facing:Direction.Plane.HORIZONTAL){
            var frame=ShowcaseFrame.at(BlockPos.ZERO,facing);
            assertEquals(13,PiecesSection.windows(frame).size());
            assertEquals(1,WindowEdgesSection.windows(frame).stream().filter(w->w.design().profile()==WindowProfile.BLANK).count());
        }
    }
    @Test void hardwareExamplesHaveIndependentLabelsAndKeepTheFittedRowGap(){
        int lastFitted=ShowcaseSpacing.FIRST_R+3*ShowcaseSpacing.pitch(3)+2;
        assertTrue(ShowcaseCanvas.labelR(DoorHardwareSection.R0)-lastFitted>ShowcaseSpacing.GAP);
        for(int i=0;i<4;i++){
            assertTrue(DoorHardwareSection.r(i)+2<ShowcaseFrame.WIDTH);
            if(i>0)assertTrue(DoorHardwareSection.r(i)-(DoorHardwareSection.r(i-1)+2)>ShowcaseSpacing.GAP);
        }
        assertTrue(DoorHardwareSection.F<ShowcaseFrame.DEPTH);
    }
}
