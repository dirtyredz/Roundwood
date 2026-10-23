package io.github.dirtyredz.roundwood.block.window.geometry.face;

import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross beams (0.7.0, owner ruling): every crossed piece has its own one-pixel cross through its
 * middle ({@code 7.5..8.5} both ways), clipped to its own opening, at the frame's full depth, with the
 * glass cut away under it; plain pieces have none.
 */
class WindowMuntinGeometryTest {
    private static final WindowPose CENTRED = WindowPosition.CENTER.pose(Direction.Axis.X);

    @Test
    void eachCrossedPieceHasItsOwnCrossClippedToItsOpening() {
        for (WindowProfile profile : WindowProfile.values()) {
            WindowDesign cross = new WindowDesign(profile, WindowPattern.CROSS);
            WindowDesign plain = WindowDesign.plain(profile);
            for (int hu = 0; hu < 32; hu++) {
                for (int hv = 0; hv < 32; hv++) {
                    boolean opening = profile.inOpening(hu / 2, hv / 2);
                    boolean onCross = hu == 15 || hu == 16 || hv == 15 || hv == 16;
                    assertEquals(opening && !onCross, cross.glazed(hu, hv), profile + " crossed " + hu + "," + hv);
                    assertEquals(opening, plain.glazed(hu, hv), profile + " plain " + hu + "," + hv);
                }
            }
        }
    }

    @Test
    void beamsAreWoodAtFullDepthAndNeverOverlapTheGlass() {
        for (WindowProfile profile : WindowProfile.values()) {
            WindowDesign cross = new WindowDesign(profile, WindowPattern.CROSS);
            List<Piece> panel = WindowPaneGeometry.panel(cross, CENTRED);
            for (int hu = 0; hu < 32; hu++) {
                for (int hv = 0; hv < 32; hv++) {
                    if (!profile.inOpening(hu / 2, hv / 2) || !(hu == 15 || hu == 16 || hv == 15 || hv == 16)) {
                        continue;
                    }
                    double x = (hu + 0.5) / 2;
                    double y = (hv + 0.5) / 2;
                    for (double z : new double[] {7.25, 8.0, 8.75}) {
                        assertEquals(WindowPaneGeometry.BEAM, textureAt(panel, x, y, z), profile + " beam at " + x + "," + y + "," + z);
                    }
                }
            }
            for (int i = 0; i < panel.size(); i++) {
                for (int j = i + 1; j < panel.size(); j++) {
                    assertFalse(panel.get(i).box().intersect(panel.get(j).box()).isPresent(),
                            profile + ": overlapping pieces " + panel.get(i) + " " + panel.get(j));
                }
            }
        }
    }

    private static String textureAt(List<Piece> pieces, double x, double y, double z) {
        for (Piece piece : pieces) {
            if (piece.box().containsStrictly(x, y, z)) {
                return piece.texture();
            }
        }
        return null;
    }
}
