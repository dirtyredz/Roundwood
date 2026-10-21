package io.github.dirtyredz.roundwood.block.attachment;

import io.github.dirtyredz.roundwood.appearance.Appearance;

import java.util.Optional;

/**
 * The trims fitted to a host's two faces (0.8.0): each slot independently absent or present with
 * its own look. Presence never depends on the look (an oak trim is still a trim). Immutable.
 */
public record FaceTrims(Optional<Appearance> front, Optional<Appearance> back) {
    public static final FaceTrims NONE = new FaceTrims(Optional.empty(), Optional.empty());

    public FaceTrims {
        front = front == null ? Optional.empty() : front;
        back = back == null ? Optional.empty() : back;
    }

    /** The same look on both faces (how a 0.7.0 shared-face trim converts). */
    public static FaceTrims both(Appearance look) {
        return new FaceTrims(Optional.of(look), Optional.of(look));
    }

    public Optional<Appearance> get(AttachmentFace face) {
        return face == AttachmentFace.FRONT ? front : back;
    }

    public boolean has(AttachmentFace face) {
        return get(face).isPresent();
    }

    /** True when either face has a trim. */
    public boolean any() {
        return front.isPresent() || back.isPresent();
    }

    public int count() {
        return (front.isPresent() ? 1 : 0) + (back.isPresent() ? 1 : 0);
    }

    public FaceTrims with(AttachmentFace face, Appearance look) {
        return face == AttachmentFace.FRONT ? new FaceTrims(Optional.of(look), back) : new FaceTrims(front, Optional.of(look));
    }

    public FaceTrims without(AttachmentFace face) {
        return face == AttachmentFace.FRONT ? new FaceTrims(Optional.empty(), back) : new FaceTrims(front, Optional.empty());
    }

    /** The slots exchanged (a host whose identity flipped without its looks moving). */
    public FaceTrims swapped() {
        return new FaceTrims(back, front);
    }
}
