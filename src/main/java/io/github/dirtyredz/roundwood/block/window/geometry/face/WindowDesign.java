package io.github.dirtyredz.roundwood.block.window.geometry.face;

import java.util.ArrayList;
import java.util.List;

/**
 * What a window block's face is, fixed per registration: its {@link WindowProfile} (which pixels are
 * glazed) and its {@link WindowPattern} (plain or crossed by beams). The pair names the block
 * ({@code round_window_arch_left_cross}) and keys every geometry, shape and model cache.
 */
public record WindowDesign(WindowProfile profile, WindowPattern pattern) {
    /** The ten designs in creative order: each plain piece followed by its crossed variant. */
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
