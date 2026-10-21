package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimsNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The saved form of a window's face trims, nested so the frame's own top-level
 * {@code RoundwoodAppearance} keeps its meaning. 0.8.0 writes version 2,
 * {@code WindowTrim: {Version: 2, Front: {...}, Back: {...}}} ({@link FaceTrimsNbt}), and owns the
 * adapter from 0.7.0:
 * <ul>
 *     <li>no payload: both faces in oak when the old state was {@code trimmed}, else none;</li>
 *     <li>version 1 ({@code {Version: 1, RoundwoodAppearance: {...}}}, one look shared by both faces):
 *     both faces in that look when the old state was {@code trimmed} (0.7.0 ignored the data
 *     otherwise), else none;</li>
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

    /** What was read: the faces, and a payload of an unknown version to keep (null when understood). */
    public record Read(FaceTrims trims, @Nullable CompoundTag unknown) {
    }

    private WindowTrimNbt() {
    }

    public static void write(CompoundTag parent, FaceTrims trims) {
        parent.put(TAG, FaceTrimsNbt.encode(trims, VERSION));
    }

    /**
     * @param legacyTrimmed the window's {@code trimmed} blockstate value as loaded (only consulted for
     *                      0.7.0 data and for a missing payload)
     */
    public static Read read(@Nullable CompoundTag parent, boolean legacyTrimmed) {
        if (parent == null || !parent.contains(TAG, Tag.TAG_COMPOUND)) {
            return new Read(legacyTrimmed ? FaceTrims.both(Appearance.DEFAULT) : FaceTrims.NONE, null);
        }
        CompoundTag payload = parent.getCompound(TAG);
        Optional<Integer> version = FaceTrimsNbt.version(payload);
        if (version.isEmpty() || version.get() == SHARED_LOOK_VERSION) {
            if (!legacyTrimmed) {
                return new Read(FaceTrims.NONE, null);
            }
            Appearance look = AppearanceNbt.read(payload).filter(AppearanceFamily.TRIM::accepts).orElse(Appearance.DEFAULT);
            return new Read(FaceTrims.both(look), null);
        }
        if (version.get() == VERSION) {
            return new Read(FaceTrimsNbt.decodeFaces(payload), null);
        }
        return new Read(FaceTrims.NONE, payload.copy());
    }
}
