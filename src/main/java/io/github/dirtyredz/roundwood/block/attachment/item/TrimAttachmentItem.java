package io.github.dirtyredz.roundwood.block.attachment.item;

import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * An item that is a face-trim attachment (0.9.0): hosts recognise it here, never by a concrete class
 * or registry id, so every historical id of the same attachment behaves alike. The item only names
 * its slot; fitting is the host router's ({@code block/attachment/host/TrimFitting}), never the item's own
 * use.
 */
public interface TrimAttachmentItem {
    TrimSlot trimSlot();

    /** The slot {@code stack} fits, or empty when it is no trim attachment. */
    static Optional<TrimSlot> slotOf(ItemStack stack) {
        return stack.getItem() instanceof TrimAttachmentItem item ? Optional.of(item.trimSlot()) : Optional.empty();
    }
}
