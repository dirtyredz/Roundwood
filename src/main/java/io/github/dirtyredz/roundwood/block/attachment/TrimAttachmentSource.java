package io.github.dirtyredz.roundwood.block.attachment;

/**
 * A block entity that carries face trims (a window, a door's anchor). Read-only: what is fitted
 * now. The loot condition ({@code loot/TrimFaceCondition}) reads it from the captured entity, and
 * renderers draw from it; fitting, removal and restyling stay host-specific.
 */
public interface TrimAttachmentSource {
    FaceTrims trims();
}
