package io.github.dirtyredz.roundwood.block.attachment;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Locale;

/**
 * Attachment identity and scope, shared by storage, fitting and loot. Face slots address Front/Back;
 * a whole-host slot normalizes both physical faces to one Front record.
 */
public enum TrimSlot implements StringRepresentable {
    PERIMETER("round_trim"),
    SILL("arch_sill_trim"),
    CROSS_BEAMS("cross_beams", Scope.WHOLE_HOST);

    public enum Scope { FACE, WHOLE_HOST }

    private final String itemId;
    private final Scope scope;

    TrimSlot(String itemId) {
        this(itemId, Scope.FACE);
    }

    TrimSlot(String itemId, Scope scope) {
        this.itemId = itemId;
        this.scope = scope;
    }

    public Scope scope() {
        return scope;
    }

    /** Both physical faces address the single whole-host record. */
    public AttachmentFace address(AttachmentFace face) {
        return scope == Scope.WHOLE_HOST ? AttachmentFace.FRONT : face;
    }

    public java.util.List<AttachmentFace> addresses() {
        return scope == Scope.WHOLE_HOST ? java.util.List.of(AttachmentFace.FRONT)
                : java.util.List.of(AttachmentFace.FRONT, AttachmentFace.BACK);
    }

    /** The registry path of the canonical item for this slot. */
    public String itemId() {
        return itemId;
    }

    /** The canonical item a removed or dropped attachment of this slot becomes. */
    public Item item() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, itemId));
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
