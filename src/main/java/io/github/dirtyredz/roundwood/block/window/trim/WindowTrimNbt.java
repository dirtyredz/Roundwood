package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimsNbt;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * The saved form of a window's face trims, nested so the frame's own top-level
 * {@code RoundwoodAppearance} keeps its meaning. 0.8.0 writes version 2,
 * {@code WindowTrim: {Version: 2, Front: {...}, Back: {...}}} ({@link FaceTrimsNbt}), always, empty
 * included, through a {@link FaceTrimStore}, and owns the adapter from 0.7.0 ({@link #legacy}):
 * <ul>
 *     <li>no payload: both faces in oak when the old state was {@code trimmed}, else none;</li>
 *     <li>version 1 ({@code {Version: 1, RoundwoodAppearance: {...}}}, one look shared by both faces)
 *     or no version: both faces in that look when the old state was {@code trimmed} (0.7.0 ignored
 *     the data otherwise), else none;</li>
 *     <li>version 2: authoritative, explicit empty data included (a stale {@code trimmed} never
 *     resurrects a removed trim);</li>
 *     <li>any other version: not understood. It reads as no trims, and the raw payload is kept so
 *     it is saved back unchanged (fail closed: the window refuses trim edits meanwhile).</li>
 * </ul>
 */
public final class WindowTrimNbt {
    public static final String TAG = "WindowTrim";
    public static final int VERSION = 2;
    static final int SHARED_LOOK_VERSION = 1;
    public static final FaceTrimStore.Format FORMAT = new FaceTrimStore.Format(TAG, VERSION, true);

    private WindowTrimNbt() {
    }

    public static void write(CompoundTag parent, FaceTrims trims) {
        FaceTrimStore.write(FORMAT, parent, trims);
    }

    /**
     * @param legacyTrimmed the window's {@code trimmed} blockstate value as loaded (only consulted for
     *                      0.7.0 data and for a missing payload)
     */
    public static FaceTrimStore.Read read(@Nullable CompoundTag parent, boolean legacyTrimmed) {
        return FaceTrimStore.read(FORMAT, parent, legacy(legacyTrimmed));
    }

    /** The 0.7.0 adapter for a window whose loaded {@code trimmed} summary is {@code legacyTrimmed}. */
    public static FaceTrimStore.Legacy legacy(boolean legacyTrimmed) {
        return (payload, version) -> {
            if (payload == null) {
                return legacyTrimmed ? FaceTrims.both(Appearance.DEFAULT) : FaceTrims.NONE;
            }
            if (version.isPresent() && version.get() != SHARED_LOOK_VERSION) {
                return null;
            }
            if (!legacyTrimmed) {
                return FaceTrims.NONE;
            }
            return FaceTrims.both(AppearanceNbt.read(payload).filter(AppearanceFamily.TRIM::accepts).orElse(Appearance.DEFAULT));
        };
    }
}
