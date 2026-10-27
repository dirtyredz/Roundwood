package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;

/** Closed-plane compatibility. Swing intersections are intentional, for full and thin tops. */
public final class DoorArchClearance {
    private DoorArchClearance() {}

    public static boolean accepts(ArchPieceThickness thickness, ArchPieceDepth arch, DoorDepth door) {
        return thickness == ArchPieceThickness.FULL ? arch == ArchPieceDepth.FRONT
                : arch.back() != 13 + door.offset();
    }
}
