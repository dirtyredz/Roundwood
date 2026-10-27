package io.github.dirtyredz.roundwood.block.window.geometry.trim;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometryTest;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowShapes;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The arch sill (0.10.0): only the upper arches take one; it covers exactly the bottom two pixel rows
 * inside the opening (written out independently: the radius-14 circle around the arch centre), never a
 * perimeter band pixel, so a sill and a trim on the same face are never coplanar; it stands one pixel
 * proud of either face at the band's depth in every pose, is drawn with a face and reveals like the
 * band, never collides and is targeted only inside the cell.
 */
class WindowSillGeometryTest {
    private static boolean expectedSill(WindowProfile profile, int u, int v) {
        if (profile != WindowProfile.ARCH_LEFT && profile != WindowProfile.ARCH_RIGHT) {
            return false;
        }
        double x = u + 0.5 + (profile == WindowProfile.ARCH_RIGHT ? 16 : 0) - 16;
        double y = v + 0.5;
        return v < 2 && x * x + y * y <= 196;
    }

    @Test
    void onlyUpperArchesTakeASillInsideTheirOpening() {
        for (WindowProfile profile : WindowProfile.values()) {
            assertEquals(profile == WindowProfile.ARCH_LEFT || profile == WindowProfile.ARCH_RIGHT, profile.takesSill(), profile.id());
            assertEquals(profile.takesSill(), WindowTrimGeometry.supports(TrimSlot.SILL, profile));
            assertEquals(profile != WindowProfile.BLANK, WindowTrimGeometry.supports(TrimSlot.PERIMETER, profile));
            int count = 0;
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    boolean sill = WindowSillGeometry.inSill(profile, u, v);
                    assertEquals(expectedSill(profile, u, v), sill, profile + " sill at " + u + "," + v);
                    if (sill) {
                        count++;
                        assertTrue(profile.inOpening(u, v), "a sill pixel outside the opening");
                        assertFalse(WindowTrimGeometry.inBand(profile, u, v), profile + ": sill and band overlap at " + u + "," + v);
                    }
                    assertEquals(sill, WindowTrimGeometry.covers(TrimSlot.SILL, profile, u, v));
                    assertEquals(WindowTrimGeometry.inBand(profile, u, v), WindowTrimGeometry.covers(TrimSlot.PERIMETER, profile, u, v));
                }
            }
            assertEquals(profile.takesSill() ? 28 : 0, count, profile + ": sill pixels (14 columns, two rows)");
            assertEquals(profile.takesSill(), !WindowTrimGeometry.band(TrimSlot.SILL, profile).isEmpty());
        }
    }

    @Test
    void theSillStandsProudOfEitherFaceAtTheBandsDepthInEveryPose() {
        for (WindowProfile profile : List.of(WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT)) {
            for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
                for (Direction side : sides(pose)) {
                    double[] band = WindowTrimGeometry.depth(pose, side);
                    List<Piece> pieces = WindowTrimGeometry.pieces(TrimSlot.SILL, profile, pose, side);
                    assertFalse(pieces.isEmpty());
                    for (Piece piece : pieces) {
                        double[] n = range(piece.box(), pose.normal());
                        assertEquals(band[0], n[0], 1e-9, profile + " " + pose + " " + side);
                        assertEquals(band[1], n[1], 1e-9, profile + " " + pose + " " + side);
                    }
                    List<Surface> surfaces = WindowTrimSurfaces.surfaces(TrimSlot.SILL, profile, pose, side);
                    assertTrue(surfaces.stream().anyMatch(s -> s.texture().equals(WindowTrimGeometry.FACE) && s.face() == side),
                            "an outward face");
                    assertTrue(surfaces.stream().anyMatch(s -> s.texture().equals(WindowTrimGeometry.EDGE)), "reveals");
                }
            }
        }
    }

    @Test
    void theSillNeverCollidesAndIsTargetedOnlyInTheCell() {
        for (WindowProfile profile : List.of(WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT)) {
            WindowDesign design = WindowDesign.plain(profile);
            for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
                for (Direction side : sides(pose)) {
                    List<Box> plain = WindowShapes.selectionBoxes(design, pose, Set.of(), Set.of());
                    List<Box> silled = WindowShapes.selectionBoxes(design, pose, Set.of(), Set.of(side));
                    double[] n = WindowTrimGeometry.depth(pose, side);
                    boolean inCell = n[0] >= 0 && n[1] <= 16;
                    if (inCell != (silled.size() > plain.size())) {
                        fail(profile + " " + pose + " " + side + ": in-cell " + inCell + " but " + silled.size());
                    }
                    silled.forEach(box -> assertTrue(box.within(Box.cell()), "selection leaves the cell: " + box));
                }
            }
        }
        WindowDesign round = WindowDesign.plain(WindowProfile.ROUND);
        WindowPose pose = WindowPaneGeometryTest.allPoses().get(0);
        assertEquals(WindowShapes.selectionBoxes(round, pose, Set.of(), Set.of()),
                WindowShapes.selectionBoxes(round, pose, Set.of(), Set.of(sides(pose))), "a round window has no sill to target");
    }

    private static Direction[] sides(WindowPose pose) {
        return new Direction[] {pose.front(), pose.front().getOpposite()};
    }

    private static double[] range(Box box, Direction.Axis axis) {
        return switch (axis) {
            case X -> new double[] {box.minX(), box.maxX()};
            case Y -> new double[] {box.minY(), box.maxY()};
            case Z -> new double[] {box.minZ(), box.maxZ()};
        };
    }
}
