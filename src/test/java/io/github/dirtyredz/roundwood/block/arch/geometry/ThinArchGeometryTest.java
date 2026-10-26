package io.github.dirtyredz.roundwood.block.arch.geometry;

import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceTargeting;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThinArchGeometryTest {
    @Test void casingTargetsStayInCellAndNeverExpandCollision() {
        io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure();
        var cell = io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell.LOWER_LEFT;
        for (var design : ArchPieceDesign.values()) for (var side : KeyholeShoulderProfile.Side.values())
            for (var depth : ArchPieceDepth.values()) for (int casings = 0; casings < 4; casings++) {
                var target = ArchPieceShapes.shape(design, side, Direction.NORTH, cell, ArchPieceThickness.THIN, depth, casings);
                assertEquals(Math.max(0, depth.back() - ((casings & 2) != 0 ? 1 : 0)) / 16, target.min(Direction.Axis.Z));
                assertEquals(Math.min(16, depth.back() + 3 + ((casings & 1) != 0 ? 1 : 0)) / 16, target.max(Direction.Axis.Z));
                var collision = ArchPieceShapes.collision(design, side, Direction.NORTH, cell, ArchPieceThickness.THIN, depth);
                if (design.shoulder()) {
                    assertEquals(depth.back() / 16, collision.min(Direction.Axis.Z));
                    assertEquals((depth.back() + 3) / 16, collision.max(Direction.Axis.Z));
                } else assertTrue(collision.isEmpty());
            }
    }
    @Test void thinSlabsPreserveEveryMaskAndCellAtAllDepths() {
        for (var design : ArchPieceDesign.values()) for (var side : KeyholeShoulderProfile.Side.values())
            for (var depth : ArchPieceDepth.values()) {
                var boxes = ArchPieceGeometry.body(design, side, ArchPieceThickness.THIN, depth);
                for (var box : boxes) {
                    assertEquals(depth.back(), box.minZ());
                    assertEquals(depth.back() + 3, box.maxZ());
                }
                for (int y = 0; y < design.height(); y++) for (int x = 0; x < design.width(); x++) {
                    final int px = x, py = y;
                    assertEquals(design.contains(side, x, y), boxes.stream().anyMatch(b -> b.containsStrictly(px + .5, py + .5, depth.back() + 1.5)));
                    var cell = ArchPieceGeometry.cell(design, side, x / 16, y / 16, ArchPieceThickness.THIN, depth);
                    assertEquals(design.contains(side, x, y), cell.stream().anyMatch(b -> b.containsStrictly(px % 16 + .5, py % 16 + .5, depth.back() + 1.5)));
                }
                for (var face : AttachmentFace.values()) {
                    var band = ArchPieceTrimGeometry.depth(face, depth.back(), depth.back() + 3);
                    assertEquals(1, band[1] - band[0]);
                    assertTrue(band[1] <= depth.back() || band[0] >= depth.back() + 3);
                }
            }
    }

    @Test void overlayIntersectionsCoverEachFaceAndChooseTheirOwnPlacement() {
        for (var facing : Direction.Plane.HORIZONTAL) for (var face : Direction.values()) for (boolean shoulder : new boolean[]{true, false}) {
            double area = 0;
            for (var region : ArchPieceTargeting.regions(facing, face, shoulder)) {
                var polygon = region.polygon();
                double signed = 0;
                for (int i = 0; i < polygon.size(); i++) {
                    var a = polygon.get(i); var b = polygon.get((i + 1) % polygon.size());
                    signed += a.u() * b.v() - b.u() * a.v();
                }
                area += Math.abs(signed) / 2;
                if (Math.abs(signed) < 1e-10) continue;
                double u = polygon.stream().mapToDouble(ArchPieceTargeting.Point::u).average().orElseThrow();
                double v = polygon.stream().mapToDouble(ArchPieceTargeting.Point::v).average().orElseThrow();
                double x = face.getAxis() == Direction.Axis.X ? .5 : u;
                double y = face.getAxis() == Direction.Axis.Y ? .5 : v;
                double z = face.getAxis() == Direction.Axis.Y ? v : face.getAxis() == Direction.Axis.X ? u : .5;
                if (shoulder) assertEquals(region.target(), ArchPieceTargeting.target(facing, face, x, y, z));
                assertEquals(region.depth(), ArchPieceDepth.target(region.target().facing(), face, x, y, z));
            }
            assertEquals(1, area, 1e-9);
        }
    }
}
