package io.github.dirtyredz.roundwood.block.attachment;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
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
