package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.block.door.hardware.*;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettledDoorGeometryTest {
    private static volatile Object retained;
    @Test void warmedOpenAndClosedLookupAllocationsAreEqual() {
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        for(boolean open:new boolean[]{false,true})measure(bean,open,20000);
        long closed=measure(bean,false,100000),open=measure(bean,true,100000);
        System.out.printf("Settled envelope lookup: closed=%.2f B/frame open=%.2f B/frame (100000 frames)%n",closed/100000.0,open/100000.0);
        assertTrue(open<=closed+100000L*8,"open pose allocates more than closed: "+open+" / "+closed);
    }
    private static long measure(com.sun.management.ThreadMXBean bean,boolean open,int count) {
        long thread=Thread.currentThread().getId(),before=bean.getThreadAllocatedBytes(thread);
        for(int i=0;i<count;i++)retained=DoorGeometry.placedEnvelope(DoorDesign.TALL_DOUBLE_ARCHED,DoorHingeSide.LEFT,open);
        return bean.getThreadAllocatedBytes(thread)-before;
    }
    @Test void settledOpenEnvelopeDoesNotRebuildForEveryProjectedRay() {
        for(var design:DoorDesign.values()) for(var hinge:DoorHingeSide.values()) for(boolean open:new boolean[]{false,true}) {
            var first=DoorGeometry.placedEnvelope(design,hinge,open);
            for(int frame=0;frame<1000;frame++)assertSame(first,DoorGeometry.placedEnvelope(design,hinge,open),"settled "+design+" open="+open+" rebuilt envelope");
        }
    }
    @Test void donorHardwareDoesNotRebuildForEveryRayAndPreview() {
        for(var design:DoorDesign.values()) for(var hinge:DoorHingeSide.values()) for(boolean open:new boolean[]{false,true}) {
            var first=DoorHardwareGeometry.placed(design,hinge,open);
            for(int frame=0;frame<1000;frame++)assertSame(first,DoorHardwareGeometry.placed(design,hinge,open),"donor rebuilt hardware");
        }
    }
}
