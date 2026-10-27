package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;

/** Effective corner mask; fitting hides upper fill without changing its donor or refund custody. */
public enum CamouflageCoverage {
    ALL, LOWER, NONE;

    public static CamouflageCoverage of(DoorDesign design, boolean fitted) {
        return !fitted ? ALL : design == DoorDesign.ROUND ? LOWER : NONE;
    }

    public boolean contains(DoorDesign design, int u, int v) {
        return this != NONE && (this != LOWER || v < 16) && CamouflageGeometry.inCorner(design, u, v);
    }
}
