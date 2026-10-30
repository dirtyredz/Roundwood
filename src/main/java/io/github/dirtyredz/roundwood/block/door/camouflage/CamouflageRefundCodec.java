package io.github.dirtyredz.roundwood.block.door.camouflage;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * The one nested ItemStack Roundwood saves: {@code Camouflage.Refund}. Vanilla's world upgrade converts the
 * item stacks of block entities it knows; it never visits a modded block entity's custom path, so a refund
 * saved on Forge 1.20.1 still has the legacy {@code {id, Count, tag}} shape after the world is opened here.
 * This codec runs vanilla's ItemStack data fixer on that shape (from the captured 1.20.1 data version) and
 * then decodes with the target registries; a stack already in the target {@code {id, count, components}}
 * shape is decoded directly and never fixed twice.
 *
 * <p>Decoding fails closed: an unknown item, an invalid component or any parse failure reports
 * {@link Decoded.Failed}, never {@link ItemStack#EMPTY}, so the caller keeps the raw data in custody.
 */
public final class CamouflageRefundCodec {
    /** Minecraft 1.20.1's data version: what Forge 1.20.1 Roundwood saves carry (fixtures verify it). */
    public static final int LEGACY_DATA_VERSION = 3465;

    private CamouflageRefundCodec() {
    }

    /** The outcome of decoding one saved refund. */
    public sealed interface Decoded {
        /** A target stack (count 1 by construction elsewhere). */
        record Stack(ItemStack stack) implements Decoded { }

        /** Undecodable here; {@code raw} is the untouched saved compound. */
        record Failed(CompoundTag raw) implements Decoded { }
    }

    /** Whether {@code refund} has the Forge 1.20.1 encoding (capital {@code Count}, optional {@code tag}). */
    public static boolean legacy(CompoundTag refund) {
        return refund.contains("Count", Tag.TAG_ANY_NUMERIC) && !refund.contains("count");
    }

    public static Decoded decode(CompoundTag refund, HolderLookup.Provider registries) {
        CompoundTag target = legacy(refund) ? upgrade(refund) : refund;
        if (target == null) return new Decoded.Failed(refund.copy());
        Optional<ItemStack> parsed;
        try {
            parsed = ItemStack.parse(registries, target);
        } catch (RuntimeException e) {
            parsed = Optional.empty();
        }
        return parsed.filter(stack -> !stack.isEmpty())
                .<Decoded>map(Decoded.Stack::new)
                .orElseGet(() -> new Decoded.Failed(refund.copy()));
    }

    /** The target encoding of {@code stack}. */
    public static Tag encode(ItemStack stack, HolderLookup.Provider registries) {
        return stack.save(registries);
    }

    /** Vanilla's 1.20.1 to current ItemStack conversion of one legacy compound; null when it yields no compound. */
    static CompoundTag upgrade(CompoundTag legacy) {
        try {
            Tag fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, legacy.copy()),
                    LEGACY_DATA_VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
            return fixed instanceof CompoundTag compound ? compound : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
