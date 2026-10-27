package io.github.dirtyredz.roundwood.block.assembly.render;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AssemblyRenderPublicationTest {
    @Test void publishesBeforeRefreshAndInvalidatesOldAndNewCells() {
        var publication = new AssemblyRenderPublication<String, String>();
        var calls = new ArrayList<String>();
        publication.publish("first", Set.of("15,15,15", "16,16,16"), () -> {
            assertEquals("first", publication.current()); calls.add("refresh");
        }, calls::add);
        assertEquals("refresh", calls.get(0));
        calls.clear();
        publication.publish("second", Set.of("16,16,16", "17,16,16"), () -> {
            assertEquals("second", publication.current()); calls.add("refresh");
        }, calls::add);
        assertEquals("refresh", calls.remove(0));
        assertEquals(Set.of("15,15,15", "16,16,16", "17,16,16"), Set.copyOf(calls));
        assertEquals(3, calls.size());
        calls.clear();
        publication.publish(null, Set.of(), () -> assertNull(publication.current()), calls::add);
        assertEquals(Set.of("16,16,16", "17,16,16"), Set.copyOf(calls));
    }
}
