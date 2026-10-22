package io.github.dirtyredz.roundwood.block.attachment.storage;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/**
 * The shared saved form of a pair of face trims (0.8.0), nested under a host-specific key:
 * {@code {Version: n, Front: {RoundwoodAppearance: {...}}, Back: {...}}}. A face is present exactly
 * when its key is present; an empty face compound is a present oak trim (default looks are never
 * stored, so presence cannot depend on them). A present face whose look is unusable reads as oak.
 * Each host owns its version number and any older layout; this class only reads the layout of the
 * version the host names, so an unknown future version is never misread.
 */
public final class FaceTrimsNbt {
    public static final String VERSION_KEY = "Version";

    private FaceTrimsNbt() {
    }

    /** Strict whole-host encoding: future fields and malformed appearances remain in custody. */
    public static boolean validSingleton(CompoundTag payload, int version) {
        if (!payload.contains(VERSION_KEY, Tag.TAG_INT) || payload.getInt(VERSION_KEY) != version
                || !java.util.Set.of(VERSION_KEY, "Front").containsAll(payload.getAllKeys())) {
            return false;
        }
        if (!payload.contains("Front")) {
            return true;
        }
        if (!payload.contains("Front", Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag front = payload.getCompound("Front");
        if (front.isEmpty()) {
            return true;
        }
        return front.getAllKeys().equals(java.util.Set.of(AppearanceNbt.TAG))
                && AppearanceNbt.read(front).filter(AppearanceFamily.TRIM::accepts)
                .map(look -> AppearanceNbt.encode(look).equals(front.get(AppearanceNbt.TAG))).orElse(false);
    }

    /** The payload compound for {@code trims} at {@code version}. */
    public static CompoundTag encode(FaceTrims trims, int version) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, version);
        for (AttachmentFace face : AttachmentFace.values()) {
            trims.get(face).ifPresent(look -> tag.put(face.key(), encodeFace(look)));
        }
        return tag;
    }

    /** One face's compound: empty for oak, else the look under {@code RoundwoodAppearance}. */
    public static CompoundTag encodeFace(Appearance look) {
        CompoundTag face = new CompoundTag();
        if (!look.equals(Appearance.DEFAULT)) {
            AppearanceNbt.write(face, look);
        }
        return face;
    }

    /** The faces of a payload already known to use this layout. */
    public static FaceTrims decodeFaces(CompoundTag payload) {
        FaceTrims trims = FaceTrims.NONE;
        for (AttachmentFace face : AttachmentFace.values()) {
            if (payload.contains(face.key(), Tag.TAG_COMPOUND)) {
                trims = trims.with(face, decodeFace(payload.getCompound(face.key())));
            }
        }
        return trims;
    }

    /** A present face's look: its stored trim look, else oak. */
    public static Appearance decodeFace(CompoundTag face) {
        return AppearanceNbt.read(face).filter(AppearanceFamily.TRIM::accepts).orElse(Appearance.DEFAULT);
    }

    /** The payload's version, or empty when it has none (an integer {@code Version} is required). */
    public static Optional<Integer> version(CompoundTag payload) {
        return payload.contains(VERSION_KEY, Tag.TAG_ANY_NUMERIC) ? Optional.of(payload.getInt(VERSION_KEY)) : Optional.empty();
    }
}
