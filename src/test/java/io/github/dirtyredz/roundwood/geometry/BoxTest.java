package io.github.dirtyredz.roundwood.geometry;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generic box transforms shared by doors, windows and the model writer. */
class BoxTest {
    @Test
    void fourQuarterTurnsAreIdentityAndOneTurnMovesSouthToWest() {
        Box box = new Box(1, 2, 13, 5, 6, 16);
        assertEquals(box, box.rotateY(4));
        Box turned = box.rotateY(1);
        assertEquals(new Box(0, 2, 1, 3, 6, 5), turned);
        assertEquals(box.volume(), turned.volume(), 1e-9);
    }

    @Test
    void intersectionIsEmptyWhenBoxesOnlyTouch() {
        Box a = new Box(0, 0, 0, 16, 16, 16);
        assertEquals(Optional.empty(), a.intersect(new Box(16, 0, 0, 32, 16, 16)));
        assertEquals(Optional.of(new Box(8, 0, 0, 16, 16, 16)), a.intersect(new Box(8, 0, 0, 32, 16, 16)));
        assertTrue(new Box(1, 1, 1, 2, 2, 2).within(Box.cell()));
    }

    @Test
    void emptyBoxesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Box(1, 0, 0, 1, 1, 1));
    }
}
