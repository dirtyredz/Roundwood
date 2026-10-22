package io.github.dirtyredz.roundwood.block.attachment.host;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The live, callback-backed host view ({@link TrimHost#of}) the window, door and archway hand the shared transaction. */
class TrimHostTest {
    private static final BlockPos ORIGIN = new BlockPos(4, 70, -2);

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void theViewReadsTheHostLiveAndNamesItsSlotAndOrigin() {
        AtomicReference<FaceTrims> stored = new AtomicReference<>(FaceTrims.NONE);
        AtomicBoolean editable = new AtomicBoolean(true);
        TrimHost host = TrimHost.of(TrimSlot.SILL, stored::get, editable::get, next -> {
            stored.set(next);
            return true;
        }, ORIGIN);
        assertEquals(TrimSlot.SILL, host.slot());
        assertEquals(ORIGIN, host.origin());
        assertEquals(FaceTrims.NONE, host.trims());

        FaceTrims later = FaceTrims.both(Appearance.DEFAULT);
        stored.set(later);
        editable.set(false);
        assertEquals(later, host.trims(), "a later change is seen");
        assertFalse(host.editable(), "a later custody change is seen");
    }

    @Test
    void installStoresThroughTheCallbackAndARefusalChangesNothing() {
        AtomicReference<FaceTrims> stored = new AtomicReference<>(FaceTrims.NONE);
        AtomicBoolean refuse = new AtomicBoolean(false);
        TrimHost host = TrimHost.of(TrimSlot.PERIMETER, stored::get, () -> true, next -> {
            if (refuse.get()) {
                return false;
            }
            stored.set(next);
            return true;
        }, ORIGIN);
        assertTrue(TrimFitting.install(host, AttachmentFace.FRONT, Appearance.DEFAULT));
        assertTrue(stored.get().has(AttachmentFace.FRONT));

        refuse.set(true);
        FaceTrims before = stored.get();
        assertFalse(TrimFitting.install(host, AttachmentFace.BACK, Appearance.DEFAULT));
        assertEquals(before, stored.get(), "a refused store left the host untouched");
    }

    @Test
    void aHostThatIsNotEditableIsNeverWritten() {
        AtomicReference<FaceTrims> stored = new AtomicReference<>(FaceTrims.NONE);
        AtomicBoolean called = new AtomicBoolean(false);
        TrimHost host = TrimHost.of(TrimSlot.PERIMETER, stored::get, () -> false, next -> {
            called.set(true);
            return true;
        }, ORIGIN);
        assertFalse(TrimFitting.install(host, AttachmentFace.FRONT, Appearance.DEFAULT));
        assertFalse(called.get(), "the store callback ran for a host that is not editable");
        assertEquals(FaceTrims.NONE, stored.get());
    }
}
