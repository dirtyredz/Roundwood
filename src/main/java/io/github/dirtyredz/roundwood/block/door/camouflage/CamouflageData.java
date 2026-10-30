package io.github.dirtyredz.roundwood.block.door.camouflage;

import net.minecraft.core.HolderLookup;
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
 * Reading, and custody of a refund that cannot be decoded, belong to {@link CamouflageStore}.
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
    public CompoundTag write(boolean withRefund, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put(MATERIAL, NbtUtils.writeBlockState(material));
        if (withRefund && paid()) {
            tag.put(REFUND, CamouflageRefundCodec.encode(refund, registries));
        }
        return tag;
    }

    /** The material a saved {@link #TAG} compound names, when it is an acceptable donor. */
    static Optional<BlockState> readMaterial(CompoundTag tag) {
        if (!tag.contains(MATERIAL, Tag.TAG_COMPOUND)) return Optional.empty();
        BlockState material = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(MATERIAL));
        return CamouflageMaterial.isValid(material) ? Optional.of(material) : Optional.empty();
    }

    /** Whether a decoded refund pays for {@code material}: exactly one item of that donor. */
    static boolean pays(ItemStack refund, BlockState material) {
        return refund.getCount() == 1 && CamouflageMaterial.fromStack(refund).filter(state -> state == material).isPresent();
    }

    static final String REFUND_KEY = REFUND;
}
