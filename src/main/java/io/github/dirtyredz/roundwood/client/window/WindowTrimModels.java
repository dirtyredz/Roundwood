package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;

/**
 * The window trim's artwork ({@link TrimLooks}): masonry shows the thin-mortar face for the profile,
 * spread once over the round window or the whole 2 x 2 arched window ({@code WindowTrimUv}). Since
 * 0.8.0 the attachment renderer ({@link WindowTrimWorldModels}) builds the trim's quads from it
 * directly; the generated markers {@code window_trim_face} and {@code _edge} are kept as textures but
 * no model uses them. Glass and pane wood are never touched. (The trim item's icon is the Universal
 * Trim's, {@code client/item/UniversalTrimModels}.)
 */
public final class WindowTrimModels {
    private WindowTrimModels() {
    }

    public static TrimLooks.Look look(Appearance appearance, WindowProfile profile) {
        return TrimLooks.of(appearance, material -> MasonryStyles.windowFace(material, profile));
    }
}
