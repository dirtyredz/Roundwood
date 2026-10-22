package io.github.dirtyredz.roundwood.block.window.geometry.face;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime geometry value: profile plus effective beam pattern. Canonical products use plain profiles;
 * the crossed IDs remain as hidden compatibility registrations. Both patterns still key shape caches.
 */
public record WindowDesign(WindowProfile profile, WindowPattern pattern) {
    /** The twenty-six runtime designs, also covering the retained compatibility identities. */
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

    /** The registry path of the block (and item) with this design, also the stem of its model names. */
    public String id() {
        return profile.id() + pattern.suffix();
    }

    /** True when half-pixel cell (hu, hv) of the face (0..31) is glass: in the opening and not on a beam. */
    public boolean glazed(int hu, int hv) {
        return profile.inOpening(hu / 2, hv / 2) && !WindowMuntinGeometry.isBeam(this, hu, hv);
    }
}
