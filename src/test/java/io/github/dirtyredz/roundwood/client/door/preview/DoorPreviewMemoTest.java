package io.github.dirtyredz.roundwood.client.door.preview;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DoorPreviewMemoTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    record Target(int hit,int held,int revision) {}
    @Test void repeatedFramesReuseProposalsAndGeometryIncludingMisses() {
        var memo=new DoorPreviewMemo<Target,Optional<Object>>();var calls=new AtomicInteger();
        var target=new Target(1,1,1);
        java.util.function.Supplier<Optional<Object>> build=()->{calls.incrementAndGet();return Optional.of(new Object());};
        var first=memo.get(target,build);
        for(int frame=0;frame<1000;frame++)assertSame(first,memo.get(new Target(1,1,1),build));
        assertEquals(1,calls.get());
        memo.get(new Target(2,1,1),build);memo.get(new Target(2,2,1),build);memo.get(new Target(2,2,2),build);
        assertEquals(4,calls.get());
        memo.clear();memo.get(new Target(2,2,2),build);assertEquals(5,calls.get());
        var miss=new Target(0,0,0);
        for(int frame=0;frame<1000;frame++)assertTrue(memo.get(miss,()->{calls.incrementAndGet();return Optional.empty();}).isEmpty());
        assertEquals(6,calls.get());
    }
}
