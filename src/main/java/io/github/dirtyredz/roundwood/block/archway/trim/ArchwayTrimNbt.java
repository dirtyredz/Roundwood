package io.github.dirtyredz.roundwood.block.archway.trim;

import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimsNbt;

/**
 * The saved form of an archway's face trims (0.12.0), on the anchor's entity beside its appearance:
 * {@code ArchwayTrim: {Version: 1, Front: {...}, Back: {...}}} ({@link FaceTrimsNbt}), kept by a
 * {@link FaceTrimStore}. Archways are new, so there is no older layout: missing means no trims and an
 * untrimmed archway writes nothing. Any other version is unknown: kept and saved back unchanged, the
 * archway refusing trim edits meanwhile.
 */
public final class ArchwayTrimNbt {
    public static final String TAG = "ArchwayTrim";
    public static final int VERSION = 1;
    public static final FaceTrimStore.Format FORMAT = new FaceTrimStore.Format(TAG, VERSION, false);

    private ArchwayTrimNbt() {
    }
}
