package io.github.dirtyredz.roundwood.block.attachment;

import net.minecraft.util.StringRepresentable;

/**
 * The two stable slots a trim attachment can occupy on its host (0.8.0): the host's front and its
 * back. Which world side each slot is on is the host's business (a window's face identity, a door's
 * facing); the slot itself never changes when the host is rotated, mirrored or reloaded, so two
 * different looks never swap. Also the saved key of each slot.
 */
public enum AttachmentFace implements StringRepresentable {
    FRONT("Front"),
    BACK("Back");

    /** How far an attachment stands proud of its host's face, in authored pixels (every host and slot). */
    public static final double PROUD = 1;

    private final String key;

    AttachmentFace(String key) {
        this.key = key;
    }

    /** The compound key the slot is saved under ({@code Front}, {@code Back}). */
    public String key() {
        return key;
    }

    /**
     * The face a point at authored depth {@code depth} addresses on a host whose two faces meet at
     * {@code middle} (the front is the deeper side): the front from the middle on, so a tie is FRONT.
     * Hosts whose face identity follows a pose (windows) decide it themselves.
     */
    public static AttachmentFace at(double depth, double middle) {
        return depth >= middle ? FRONT : BACK;
    }

    public AttachmentFace opposite() {
        return this == FRONT ? BACK : FRONT;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
