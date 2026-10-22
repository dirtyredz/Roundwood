package io.github.dirtyredz.roundwood.block.attachment.storage;

import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;

/**
 * One host's face trims in one {@link TrimSlot}, and custody of a saved payload this build does not
 * understand (0.9.0, extracted from the door and window entities). The host names its saved form
 * ({@link Format}: key, version, whether an empty pair is written) and, when loading, how an older
 * layout reads ({@link Legacy}); everything else is shared:
 * <ul>
 *     <li>the current version decodes through {@link FaceTrimsNbt};</li>
 *     <li>a missing payload, or an older one, reads through the host's {@link Legacy};</li>
 *     <li>anything else is an unknown version: it reads as no trims, is kept and saved back
 *     byte-for-byte, and edits are refused ({@link #editable}) until a build that understands it
 *     loads it. Loading never writes anything else (no blockstate change);</li>
 *     <li>update packets always carry the payload, explicit-empty while an unknown one is held, so a
 *     removal reaches clients and clients never see what they could not read.</li>
 * </ul>
 * Several stores may share one parent compound as long as their keys differ.
 */
public final class FaceTrimStore {
    /**
     * What an older or missing payload means for this host, or null when it is not understood.
     * {@code payload} is null when the key is absent; {@code version} is empty when the payload has none.
     */
    @FunctionalInterface
    public interface Legacy {
        /** No older layout: a missing payload is no trims, any other version is not understood. */
        Legacy NONE = (payload, version) -> payload == null ? FaceTrims.NONE : null;

        @Nullable
        FaceTrims read(@Nullable CompoundTag payload, Optional<Integer> version);
    }

    /**
     * A host's saved form.
     *
     * @param saveEmpty write an explicit empty payload when no face is trimmed (windows: it overrides
     *                  a stale 0.7.0 summary); otherwise empty is omitted (doors)
     */
    public record Format(String tag, int version, boolean saveEmpty, TrimSlot.Scope scope) {
        public Format(String tag, int version, boolean saveEmpty) {
            this(tag, version, saveEmpty, TrimSlot.Scope.FACE);
        }

        public boolean accepts(FaceTrims trims) {
            return scope != TrimSlot.Scope.WHOLE_HOST || (!trims.has(AttachmentFace.BACK)
                    && trims.front().map(AppearanceFamily.TRIM::accepts).orElse(true));
        }
    }

    /** What was read: the faces, and a payload of an unknown version to keep (null when understood). */
    public record Read(FaceTrims trims, @Nullable Tag unknown) {
    }

    private final Format format;
    private FaceTrims trims = FaceTrims.NONE;
    @Nullable
    private Tag unknown;

    public FaceTrimStore(Format format) {
        this.format = format;
    }

    /** Reads {@code format}'s payload from {@code parent}. */
    public static Read read(Format format, @Nullable CompoundTag parent, Legacy legacy) {
        Tag raw = parent == null ? null : parent.get(format.tag());
        if (format.scope() == TrimSlot.Scope.WHOLE_HOST && raw != null
                && (!(raw instanceof CompoundTag compound) || !FaceTrimsNbt.validSingleton(compound, format.version()))) {
            return new Read(FaceTrims.NONE, raw.copy());
        }
        CompoundTag payload = parent != null && parent.contains(format.tag(), Tag.TAG_COMPOUND) ? parent.getCompound(format.tag()) : null;
        Optional<Integer> version = payload == null ? Optional.empty() : FaceTrimsNbt.version(payload);
        if (payload != null && version.isPresent() && version.get() == format.version()) {
            return new Read(FaceTrimsNbt.decodeFaces(payload), null);
        }
        FaceTrims older = legacy.read(payload, version);
        if (older != null) {
            return new Read(older, null);
        }
        return payload == null ? new Read(FaceTrims.NONE, null) : new Read(FaceTrims.NONE, payload.copy());
    }

    /** Writes {@code trims} as {@code format}'s current payload into {@code parent} (always, empty included). */
    public static void write(Format format, CompoundTag parent, FaceTrims trims) {
        if (!format.accepts(trims)) {
            throw new IllegalArgumentException("A whole-host attachment cannot contain Back");
        }
        parent.put(format.tag(), FaceTrimsNbt.encode(trims, format.version()));
    }

    public Format format() {
        return format;
    }

    /** What is fitted (none while an unknown payload is held). */
    public FaceTrims trims() {
        return trims;
    }

    /** False while an unknown payload is held: nothing may be fitted, restyled or removed. */
    public boolean editable() {
        return unknown == null;
    }

    /** Replaces the faces; refused (false) while an unknown payload is held. The host saves and syncs. */
    public boolean set(FaceTrims next) {
        if (!editable() || !format.accepts(next)) {
            return false;
        }
        trims = next;
        return true;
    }

    /** Replaces everything with what {@code parent} holds (a save, or an update packet). */
    public void load(@Nullable CompoundTag parent, Legacy legacy) {
        Read read = read(format, parent, legacy);
        trims = read.trims();
        unknown = read.unknown();
    }

    /** The saved form: an unknown payload unchanged, else the faces (empty only when the format writes it). */
    public void save(CompoundTag parent) {
        if (unknown != null) {
            parent.put(format.tag(), unknown.copy());
        } else if (format.saveEmpty() || trims.any()) {
            write(format, parent, trims);
        }
    }

    /** The update-packet form: always the payload, explicit empty while an unknown one is held. */
    public void writeUpdate(CompoundTag parent) {
        write(format, parent, editable() ? trims : FaceTrims.NONE);
    }
}
