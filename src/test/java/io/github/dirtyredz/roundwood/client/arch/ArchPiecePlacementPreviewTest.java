package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchPiecePlacementPreviewTest {
    static { MinecraftBootstrap.ensure(); }

    @Test void interactiveTargetsCannotPromisePlacement() {
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.CHEST, Blocks.LEVER, Blocks.OAK_DOOR, Blocks.OAK_FENCE_GATE, Blocks.CRAFTING_TABLE})
            assertFalse(ArchPiecePlacementPreview.passive(block.defaultBlockState()), block.toString());
        assertTrue(ArchPiecePlacementPreview.passive(Blocks.STONE.defaultBlockState()));
        assertTrue(ArchPiecePlacementPreview.passive(Blocks.OAK_PLANKS.defaultBlockState()));
    }

    @Test void footprintContainsEveryReservedCellInEveryFacing() {
        for (var design : ArchPieceDesign.values()) for (var facing : Direction.Plane.HORIZONTAL) for (var side : Side.values()) {
            var assembly = new ArchPieceLayout.Assembly(BlockPos.ZERO, facing, design, side);
            var box = ArchPieceGeometry.footprint(design, facing);
            assertEquals(design.columns() * design.rows(), assembly.positions().size());
            for (var pos : assembly.positions()) {
                assertTrue(pos.getX() * 16 >= box.minX() && (pos.getX() + 1) * 16 <= box.maxX());
                assertTrue(pos.getY() * 16 >= box.minY() && (pos.getY() + 1) * 16 <= box.maxY());
                assertTrue(pos.getZ() * 16 >= box.minZ() && (pos.getZ() + 1) * 16 <= box.maxZ());
            }
        }
    }

    @Test void landingFaceCoordinatesCoverAllSixFaces() {
        for (var face : Direction.values()) {
            assertEquals(.37, ArchPiecePlacementPreview.facePoint(face, .37, .2, .8).get(face.getAxis()));
        }
    }
}
