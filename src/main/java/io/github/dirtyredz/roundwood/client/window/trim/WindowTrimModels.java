package io.github.dirtyredz.roundwood.client.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimLayout;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;

/**
 * A window attachment's artwork ({@link TrimLooks}): masonry shows the thin-mortar face of the
 * attachment's layout ({@link WindowTrimLayout}: per profile for the perimeter trim, its own course
 * for the arch sill), spread once over the piece's artwork extent ({@code WindowTrimUv}). Since 0.8.0
 * the attachment renderer ({@link WindowTrimWorldModels}) builds the quads from it directly; the
 * generated markers {@code window_trim_face} and {@code _edge} are kept as textures but no model uses
 * them. Glass and pane wood are never touched. (The attachment items' icons are
 * {@code client/item/TrimItemModels}.)
 */
public final class WindowTrimModels {
    private WindowTrimModels() {
    }

    public static TrimLooks.Look look(Appearance appearance, TrimSlot slot, WindowProfile profile) {
        WindowTrimLayout layout = WindowTrimLayout.of(slot, profile);
        return TrimLooks.of(appearance, material -> MasonryStyles.windowFace(material, layout));
    }
}
