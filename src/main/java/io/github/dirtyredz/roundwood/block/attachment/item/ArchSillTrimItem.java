package io.github.dirtyredz.roundwood.block.attachment.item;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;

/** The upper-arch face attachment; appearance and inert use are shared with Cross Beams. */
public class ArchSillTrimItem extends StyledAttachmentItem {
    public static final String ID = TrimSlot.SILL.itemId();
    public ArchSillTrimItem(Properties properties) { super(TrimSlot.SILL, properties); }
}
