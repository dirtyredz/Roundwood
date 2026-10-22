package io.github.dirtyredz.roundwood.block.attachment;

/**
 * A block entity that carries face trims (a window, a door's anchor). Read-only: what is fitted
 * now. The loot condition ({@code loot/TrimFaceCondition}) reads it from the captured entity, and
 * renderers draw from it; changes go through {@link TrimFitting} with a host-built {@link TrimHost}.
 */
public interface TrimAttachmentSource {
    /** The perimeter trims (every host has them). */
    FaceTrims trims();

    /** The trims in {@code slot}; a host without that slot has none (0.9.0). */
    default FaceTrims trims(TrimSlot slot) {
        return slot == TrimSlot.PERIMETER ? trims() : FaceTrims.NONE;
    }
}
