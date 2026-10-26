package io.github.dirtyredz.roundwood.block.arch.trim;

import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimsNbt;

/**
 * The saved form of a piece's face trims (0.14.0), on the anchor's entity beside its appearance:
 * {@code ArchPieceTrim: {Version: 1, Front: {...}, Back: {...}}} ({@link FaceTrimsNbt}), kept by a
 * {@link FaceTrimStore}. Arch pieces are new, so there is no older layout: missing means no trims and an
 * untrimmed piece writes nothing. Any other version is unknown: kept and saved back unchanged, the
 * piece refusing trim edits meanwhile.
 */
public final class ArchPieceTrimNbt {
    public static final String TAG = "ArchPieceTrim";
    public static final int VERSION = 1;
    public static final FaceTrimStore.Format FORMAT = new FaceTrimStore.Format(TAG, VERSION, false);

    private ArchPieceTrimNbt() {
    }
}
