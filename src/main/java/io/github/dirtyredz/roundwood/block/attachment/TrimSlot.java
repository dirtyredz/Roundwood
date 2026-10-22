package io.github.dirtyredz.roundwood.block.attachment;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/**
 * Which kind of attachment a face trim is (0.9.0). A host keeps one {@link FaceTrimStore} per slot it
 * supports, each with its own FRONT/BACK pair, payload and loot pools, so two slots on one face are
 * fitted, restyled, removed and dropped independently. The slot also names the canonical item a
 * removed or dropped attachment comes back as.
 * <ul>
 *     <li>{@link #PERIMETER}: the Universal Trim around a window, door or archway face
 *     ({@code roundwood:round_trim}). Every host has it.</li>
 *     <li>{@link #SILL}: the arch sill across an upper arch's bottom edge (planned for 0.10.0, item
 *     {@code roundwood:arch_sill_trim}). No host stores it yet.</li>
 * </ul>
 * The serialized name is the loot condition's {@code slot} value; an omitted slot means perimeter.
 */
public enum TrimSlot implements StringRepresentable {
    PERIMETER("round_trim"),
    SILL("arch_sill_trim");

    private final String itemId;

    TrimSlot(String itemId) {
        this.itemId = itemId;
    }

    /** The registry path of the canonical item for this slot. */
    public String itemId() {
        return itemId;
    }

    /** The canonical item a removed or dropped attachment of this slot becomes. */
    public Item item() {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, itemId));
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
