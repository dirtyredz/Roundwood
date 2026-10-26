package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import java.util.Optional;

/** Pure compatibility and anchor decisions, shared by both placement orders and previews. */
public record DoorArchFitPlan(DoorLayout.Assembly door, ArchPieceLayout.Assembly arch) {
    public static ArchPieceDesign topFor(DoorDesign design) {
        return switch (design) {
            case ARCHED -> ArchPieceDesign.TOP_1X1;
            case ROUND -> ArchPieceDesign.TOP_1X2;
            case DOUBLE_ARCHED, TALL_DOUBLE_ARCHED -> ArchPieceDesign.TOP_2X3;
        };
    }
    public static int topOffset(DoorDesign design) { return design == DoorDesign.TALL_DOUBLE_ARCHED ? 2 : 1; }
    public static Optional<DoorArchFitPlan> of(DoorLayout.Assembly door, ArchPieceLayout.Assembly arch, boolean camouflage) {
        if (camouflage || arch.design() != topFor(door.design()) || arch.facing() != door.facing()
                || !arch.anchor().equals(door.anchor().above(topOffset(door.design())))
                || !DoorArchClearance.accepts(door.design(), arch.thickness(), arch.depth())) return Optional.empty();
        return Optional.of(new DoorArchFitPlan(new DoorLayout.Assembly(door.anchor(), door.facing(), door.hinge(), door.design(), DoorDepth.BACK), arch));
    }
    public static Optional<DoorArchFitPlan> fromDoor(DoorLayout.Assembly door, ArchPieceThickness thickness, ArchPieceDepth depth, boolean camouflage) {
        return of(door, new ArchPieceLayout.Assembly(door.anchor().above(topOffset(door.design())), door.facing(),
                topFor(door.design()), Side.LEFT, thickness, depth), camouflage);
    }
    public static Optional<DoorArchFitPlan> fromArch(DoorDesign design, ArchPieceLayout.Assembly arch, DoorHingeSide hinge) {
        return of(new DoorLayout.Assembly(arch.anchor().below(topOffset(design)), arch.facing(), hinge, design, DoorDepth.BACK), arch, false);
    }
}
