package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageCoverage;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Which part of a door a click landed on: a static corner (outside the disc, at the closed leaf's
 * depth) or the leaf. Decided once, from the hit point in the assembly frame and the authoritative
 * open state, so camouflage and restyling agree; where the open leaf crosses a corner the leaf wins.
 */
public final class DoorHitRegion {
    public enum Region {
        CORNER, LEAF
    }

    private DoorHitRegion() {
    }

    public static Region classify(BlockState state, BlockPos pos, BlockHitResult hit) {
        return classify(state, pos, hit, CamouflageCoverage.ALL);
    }

    public static Region classify(BlockState state, BlockPos pos, BlockHitResult hit, CamouflageCoverage coverage) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        Vec3 local = assembly.toAuthored(DoorHitResolver.insidePoint(hit));
        boolean corner = CamouflageGeometry.isCornerHit(assembly.design(), state.getValue(DoorBlock.HINGE), state.getValue(DoorBlock.OPEN),
                local.x, local.y, local.z, coverage);
        return corner ? Region.CORNER : Region.LEAF;
    }
}
