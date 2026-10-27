package io.github.dirtyredz.roundwood.block.window.geometry.face;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime geometry value: profile plus effective beam pattern. Canonical products use plain profiles;
 * the crossed IDs remain as hidden compatibility registrations. Both patterns still key shape caches.
 */
public record WindowDesign(WindowProfile profile, WindowPattern pattern) {
    /** Every runtime profile/pattern combination, including blank panes with fitted beams. */
    public static List<WindowDesign> all() {
        List<WindowDesign> out = new ArrayList<>();
        for (WindowProfile profile : WindowProfile.values()) {
            for (WindowPattern pattern : WindowPattern.values()) {
                out.add(new WindowDesign(profile, pattern));
            }
        }
        return List.copyOf(out);
    }

    public static WindowDesign plain(WindowProfile profile) {
        return new WindowDesign(profile, WindowPattern.PLAIN);
    }

    /** The geometry identifier; only ModBlocks.WindowProducts names registered block/item designs. */
    public String id() {
        return profile.id() + pattern.suffix();
    }

    /** True when half-pixel cell (hu, hv) of the face (0..31) is glass: in the opening and not on a beam. */
    public boolean glazed(int hu, int hv) {
        return profile.inOpening(hu / 2, hv / 2) && !WindowMuntinGeometry.isBeam(this, hu, hv);
    }
}
