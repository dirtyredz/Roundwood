package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;

/** Product identity and authored slab dimensions; full pieces retain their historical IDs. */
public enum ArchPieceThickness {
    FULL, THIN;

    public String id(ArchPieceDesign design) { return design.id() + (this == THIN ? "_thin" : ""); }
    public double back(ArchPieceDepth depth) { return this == FULL ? 0 : depth.back(); }
    public double front(ArchPieceDepth depth) { return this == FULL ? 16 : depth.back() + 3; }
}
