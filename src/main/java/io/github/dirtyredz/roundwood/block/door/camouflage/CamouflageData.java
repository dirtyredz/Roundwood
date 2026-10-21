package io.github.dirtyredz.roundwood.block.door.camouflage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * What an assembly's corners are camouflaged with: the material drawn, and the one item a survival
 * player paid for it (empty when it was applied in creative). The refund is stored contents, given
 * back exactly once; it is saved with the world but never sent to clients.
 *
 * <p>NBT (inside the anchor block entity): {@code Camouflage: {Material: <block state>, Refund: <item>}}.
 * Reading fails closed: unknown or unacceptable materials, and refunds that do not match the
 * material, read as nothing rather than as an item.
 */
public record CamouflageData(BlockState material, ItemStack refund) {
    public static final String TAG = "Camouflage";
    private static final String MATERIAL = "Material";
    private static final String REFUND = "Refund";

    public CamouflageData {
        refund = refund.isEmpty() ? ItemStack.EMPTY : refund.copyWithCount(1);
    }

    /** Applied by a survival player: one item of {@code paidWith} is kept for the refund. */
    public static CamouflageData paid(BlockState material, ItemStack paidWith) {
        return new CamouflageData(material, paidWith);
    }

    /** Applied in creative: nothing to give back. */
    public static CamouflageData free(BlockState material) {
        return new CamouflageData(material, ItemStack.EMPTY);
    }

    public boolean paid() {
        return !refund.isEmpty();
    }

    /** The refund as a fresh stack (empty if none). */
    public ItemStack refundStack() {
        return refund.copy();
    }

    /** The compound stored under {@link #TAG}; {@code withRefund} false for what clients receive. */
    public CompoundTag write(boolean withRefund) {
        CompoundTag tag = new CompoundTag();
        tag.put(MATERIAL, NbtUtils.writeBlockState(material));
        if (withRefund && paid()) {
            tag.put(REFUND, refund.save(new CompoundTag()));
        }
        return tag;
    }

    /** Reads the {@link #TAG} compound of a block-entity tag; empty when absent or not acceptable. */
    public static Optional<CamouflageData> read(CompoundTag entityTag) {
        if (!entityTag.contains(TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = entityTag.getCompound(TAG);
        if (!tag.contains(MATERIAL, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        BlockState material = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(MATERIAL));
        if (!CamouflageMaterial.isValid(material)) {
            return Optional.empty();
        }
        ItemStack refund = tag.contains(REFUND, Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound(REFUND)) : ItemStack.EMPTY;
        boolean matches = refund.getCount() == 1
                && CamouflageMaterial.fromStack(refund).filter(state -> state == material).isPresent();
        return Optional.of(new CamouflageData(material, matches ? refund : ItemStack.EMPTY));
    }
}
