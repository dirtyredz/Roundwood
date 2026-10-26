package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoorHitResolverTest {
    @Test
    void everyRestingLeafSurfaceKeepsItsPointAndOwnAssembly() {
        for (DoorDesign design : DoorDesign.values()) for (var depth : io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values()) {
            for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                for (DoorHingeSide hinge : DoorHingeSide.values()) {
                    for (boolean open : new boolean[] {false, true}) {
                        var assembly = new DoorLayout.Assembly(new BlockPos(-17, 8, 31), facing, hinge, design, depth);
                        for (DoorLayout.Part part : assembly.parts()) {
                            var shape = DoorSelectionShapes.shape(design, facing, hinge, part, open, false, 0, true, depth.offset());
                            var visible = DoorSelectionShapes.shape(design, facing, hinge, part, open, false, 0, false, depth.offset());
                            assertEquals(visible.toAabbs(), shape.toAabbs(), "highlight and target disagree");
                            for (var box : shape.toAabbs()) {
                                for (Direction side : Direction.values()) {
                                    Vec3 centre = box.getCenter().add(Vec3.atLowerCornerOf(assembly.pos(part)));
                                    BlockHitResult raw = shape.clip(centre.relative(side, 4), centre, assembly.pos(part));
                                    assertNotNull(raw);
                                    var resolved = DoorHitResolver.resolve(assembly, open, raw, assembly.positions()::contains);
                                    assertTrue(resolved.isPresent(), design + " " + facing + " " + hinge + " open=" + open + " " + raw.getLocation());
                                    assertTrue(assembly.positions().contains(resolved.orElseThrow().getBlockPos()));
                                    assertEquals(raw.getLocation(), resolved.orElseThrow().getLocation());
                                    assertEquals(raw.getDirection(), resolved.orElseThrow().getDirection());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void projectionDoesNotClaimAirOrUnloadedPartsOrAClosedDoor() {
        var assembly = new DoorLayout.Assembly(BlockPos.ZERO, Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.ROUND);
        var hit = new BlockHitResult(new Vec3(0.1875, 0.9, -0.9), Direction.EAST, BlockPos.ZERO, false);
        assertTrue(DoorHitResolver.resolve(assembly, false, hit, assembly.positions()::contains).isEmpty());
        assertTrue(DoorHitResolver.resolve(assembly, true, hit, pos -> false).isEmpty());
        var air = new BlockHitResult(new Vec3(0.8, 0.9, -0.9), Direction.EAST, BlockPos.ZERO, false);
        assertTrue(DoorHitResolver.resolve(assembly, true, air, assembly.positions()::contains).isEmpty());
    }

    @Test
    void openRoundFreeEdgeIsAnActionableLeafHit() {
        var assembly = new DoorLayout.Assembly(BlockPos.ZERO, Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.ROUND);
        // A real ray into the free edge, almost two blocks along the open leaf from the hinge.
        var shape = DoorSelectionShapes.shape(DoorDesign.ROUND, Direction.NORTH, DoorHingeSide.LEFT,
                DoorLayout.Part.LOWER_LEFT, true, false, 0, true);
        BlockHitResult raw = shape.clip(new Vec3(1, 0.9, -0.9), new Vec3(-1, 0.9, -0.9), BlockPos.ZERO);
        assertNotNull(raw);
        assertFalse(assembly.positions().contains(DoorHitResolver.containingCell(raw)));
        var resolved = DoorHitResolver.resolve(assembly, true, raw, assembly.positions()::contains);
        assertTrue(resolved.isPresent(), "the highlighted free edge must route to the door for toggling");
        assertEquals(raw.getLocation(), resolved.orElseThrow().getLocation());
    }
}
