package io.github.dirtyredz.roundwood.block.door.camouflage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A door anchor's camouflage custody: absent, a decoded camouflage (free or paid), or <em>locked</em>.
 * A saved {@code Camouflage} compound is locked when its refund exists but cannot be decoded here
 * (an unknown item, an invalid component), or when it carries a refund for a material that is no longer an
 * acceptable donor. A locked store keeps the raw compound on disk byte for byte, still shows a valid
 * material, refuses every edit (replace, clear, take) and is carried opaquely on the door's ordinary drop,
 * so a refund is neither manufactured nor silently deleted. Refunds that decode but do not pay for the
 * material keep their historical reading: a free camouflage.
 */
public final class CamouflageStore {
    @Nullable private CamouflageData data;
    /** The raw saved compound while locked; null when the store is editable. */
    @Nullable private CompoundTag locked;

    public Optional<CamouflageData> value() {
        return Optional.ofNullable(data);
    }

    public boolean editable() {
        return locked == null;
    }

    /** The raw compound held in custody, or null when the store is editable. */
    public @Nullable CompoundTag locked() {
        return locked == null ? null : locked.copy();
    }

    /** Replaces (or with null clears) the camouflage; refused while locked. */
    public boolean set(@Nullable CamouflageData next) {
        if (locked != null) return false;
        data = next;
        return true;
    }

    /** Withdraws the camouflage so its taker is the only one who can refund it; nothing while locked or absent. */
    public Optional<CamouflageData> take() {
        if (locked != null || data == null) return Optional.empty();
        Optional<CamouflageData> taken = Optional.of(data);
        data = null;
        return taken;
    }

    /** Moves the whole custody (decoded or locked) into {@code target}, leaving this store empty. */
    public void transferTo(CamouflageStore target) {
        target.data = data;
        target.locked = locked;
        data = null;
        locked = null;
    }

    /** Replaces everything with what {@code entityTag} holds (a save or an update). */
    public void load(CompoundTag entityTag, HolderLookup.Provider registries) {
        data = null;
        locked = null;
        if (!entityTag.contains(CamouflageData.TAG, Tag.TAG_COMPOUND)) return;
        CompoundTag tag = entityTag.getCompound(CamouflageData.TAG);
        Optional<BlockState> material = CamouflageData.readMaterial(tag);
        boolean hasRefund = tag.contains(CamouflageData.REFUND_KEY, Tag.TAG_COMPOUND);
        if (!hasRefund) {
            data = material.map(CamouflageData::free).orElse(null);
            return;
        }
        if (material.isEmpty()) {
            locked = tag.copy();
            return;
        }
        switch (CamouflageRefundCodec.decode(tag.getCompound(CamouflageData.REFUND_KEY), registries)) {
            case CamouflageRefundCodec.Decoded.Stack decoded -> data = CamouflageData.pays(decoded.stack(), material.get())
                    ? CamouflageData.paid(material.get(), decoded.stack()) : CamouflageData.free(material.get());
            case CamouflageRefundCodec.Decoded.Failed failed -> {
                locked = tag.copy();
                data = CamouflageData.free(material.get());
            }
        }
    }

    /** On disk: the refund (target encoding), or the untouched raw compound while locked. */
    public void save(CompoundTag entityTag, HolderLookup.Provider registries) {
        if (locked != null) entityTag.put(CamouflageData.TAG, locked.copy());
        else if (data != null) entityTag.put(CamouflageData.TAG, data.write(true, registries));
    }

    /** For clients: the material only, and an explicit empty compound when there is none to show. */
    public void writeUpdate(CompoundTag entityTag, HolderLookup.Provider registries) {
        entityTag.put(CamouflageData.TAG, data == null ? new CompoundTag() : data.write(false, registries));
    }
}
