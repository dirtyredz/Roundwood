package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.Surface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The visible-surface extraction behind every window model (0.7.0). The first test is the reproducer
 * for the owner's 0.6.1 report: the centred round window's glazing, rasterised as stacked glass boxes,
 * drew translucent sheets inside the glass at heights 3, 4, 6, 10, 12 and 13.
 */
class WindowSurfaceGeometryTest {
    private static final WindowPose CENTRED = WindowPosition.CENTER.pose(Direction.Axis.X);
    private static final Set<String> TRANSLUCENT = Set.of(WindowPaneGeometry.GLASS);

    private static List<Surface> surfaces(WindowDesign design, WindowPose pose) {
        return WindowSurfaceGeometry.surfaces(WindowPaneGeometry.panel(design, pose), TRANSLUCENT);
    }

    @Test
    void theStackedGlassBoxesNeedHorizontalFacesButNoneIsDrawn() {
        WindowDesign round = WindowDesign.plain(WindowProfile.ROUND);
        List<Piece> panel = WindowPaneGeometry.panel(round, CENTRED);
        Set<Double> glassRowEdges = new HashSet<>();
        for (Piece piece : panel) {
            if (piece.texture().equals(WindowPaneGeometry.GLASS)) {
                glassRowEdges.add(piece.box().minY());
                glassRowEdges.add(piece.box().maxY());
            }
        }
        // The logical raster still has the 0.6.1 row boundaries inside the glazing ...
        assertTrue(glassRowEdges.containsAll(List.of(3.0, 4.0, 6.0, 10.0, 12.0, 13.0)), glassRowEdges.toString());
        // ... but no glass surface lies on any of them: glass faces only the air in front and behind.
        for (Surface surface : WindowSurfaceGeometry.surfaces(panel, TRANSLUCENT)) {
            if (surface.texture().equals(WindowPaneGeometry.GLASS)) {
                assertEquals(Direction.Axis.Z, surface.face().getAxis(), "glass sheet facing " + surface.face() + ": " + surface);
            }
        }
    }

    /** Every design, every pose: glass front and back exactly once over the glazing. */
    @Test
    void glassCoversTheGlazingExactlyOnceOnEachSide() {
        for (WindowDesign design : WindowDesign.all()) {
            for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
                {
                    String what = design.id() + " " + pose;
                    int[] front = new int[32 * 32];
                    int[] back = new int[32 * 32];
                    for (Surface surface : surfaces(design, pose)) {
                        if (!surface.texture().equals(WindowPaneGeometry.GLASS) || surface.face().getAxis() != pose.normal()) {
                            continue;
                        }
                        int[] side = surface.face() == pose.front() ? front : back;
                        double[] u = pose.uRange(surface.backing());
                        double[] v = pose.vRange(surface.backing());
                        for (int hu = (int) Math.round(u[0] * 2); hu < Math.round(u[1] * 2); hu++) {
                            for (int hv = (int) Math.round(v[0] * 2); hv < Math.round(v[1] * 2); hv++) {
                                side[hu * 32 + hv]++;
                            }
                        }
                    }
                    for (int hu = 0; hu < 32; hu++) {
                        for (int hv = 0; hv < 32; hv++) {
                            if (front[hu * 32 + hv] > 1 || back[hu * 32 + hv] > 1) {
                                fail(what + ": glass drawn twice at " + hu + "," + hv);
                            }
                            if (!design.glazed(hu, hv) && front[hu * 32 + hv] != 0) {
                                fail(what + ": glass over wood at " + hu + "," + hv);
                            }
                        }
                    }
                    for (int hu = 0; hu < 32; hu++) {
                        for (int hv = 0; hv < 32; hv++) {
                            int expected = design.glazed(hu, hv) ? 1 : 0;
                            if (front[hu * 32 + hv] != expected || back[hu * 32 + hv] != expected) {
                                fail(what + ": front/back glass " + front[hu * 32 + hv] + "/" + back[hu * 32 + hv] + " at " + hu + "," + hv);
                            }
                        }
                    }
                }
            }
        }
    }

    /** The wood's reveal round the opening (and every beam's sides) stays visible through the glazing. */
    @Test
    void openingAndBeamRevealsAreDrawn() {
        for (WindowDesign design : WindowDesign.all()) {
            List<Surface> surfaces = surfaces(design, CENTRED);
            long reveals = surfaces.stream().filter(s -> s.texture().equals(WindowPaneGeometry.FRAME)
                    && s.face().getAxis() != Direction.Axis.Z).count();
            assertTrue(reveals > 0, design.id() + ": no reveal");
            if (design.pattern().name().equals("CROSS")) {
                // the vertical beam's two sides face east and west at x 7.5 and 8.5
                assertTrue(surfaces.stream().anyMatch(s -> s.face() == Direction.WEST && s.backing().minX() == 7.5
                        && s.texture().equals(WindowPaneGeometry.BEAM)), design.id() + ": beam side missing");
                assertTrue(surfaces.stream().anyMatch(s -> s.face() == Direction.EAST && s.backing().maxX() == 8.5
                        && s.texture().equals(WindowPaneGeometry.BEAM)), design.id() + ": beam side missing");
            }
        }
    }

    /** Surfaces never leave the cell, are half-pixel aligned and are positive-volume backing boxes on their own face. */
    @Test
    void surfacesAreWellFormed() {
        for (WindowDesign design : WindowDesign.all()) {
            for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
                for (Surface surface : surfaces(design, pose)) {
                    Box box = surface.backing();
                    assertTrue(box.within(Box.cell()), surface.toString());
                    assertEquals(0.5, box.volume() / extent(box, surface.face()), 1e-9, "backing is half a pixel thick: " + surface);
                }
            }
        }
    }

    private static double extent(Box box, Direction face) {
        return switch (face.getAxis()) {
            case X -> (box.maxY() - box.minY()) * (box.maxZ() - box.minZ());
            case Y -> (box.maxX() - box.minX()) * (box.maxZ() - box.minZ());
            case Z -> (box.maxX() - box.minX()) * (box.maxY() - box.minY());
        };
    }
}
