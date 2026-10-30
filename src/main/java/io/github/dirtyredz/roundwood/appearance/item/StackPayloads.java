package io.github.dirtyredz.roundwood.appearance.item;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Copy-on-write access to Roundwood's stack payloads on 1.21 item components. Roundwood's own keys
 * ({@code RoundwoodAppearance}, {@code WindowGlass}, {@code DoorHardware}, {@code DoorArch}) keep their
 * historical NBT shape inside {@code minecraft:custom_data}, so the existing strict and opaque codecs stay
 * the single authority; vanilla's 1.20.5 conversion moves unconverted stack NBT there unchanged. Placed-block
 * payloads are the native {@code block_entity_data} and {@code block_state} components.
 *
 * <p>Every read returns a copy and every write replaces the component, so a caller never mutates a value that
 * another stack (or a cached prototype) shares. A write that leaves custom data empty removes the component, so
 * default items keep stacking. This class owns representation access only; which keys or components a
 * family accepts stays with that family.
 */
public final class StackPayloads {
    private StackPayloads() {
    }

    /** A copy of the stack's custom data, or {@code null} when it has none (the historical "no tag"). */
    public static @Nullable CompoundTag customData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || data.isEmpty() ? null : data.copyTag();
    }

    /**
     * Reads the custom data in place without copying (render and comparison hot paths). {@code reader} receives
     * {@code null} when there is none and must neither mutate nor retain the tag.
     */
    public static <T> T readCustom(ItemStack stack, Function<@Nullable CompoundTag, T> reader) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return reader.apply(data == null || data.isEmpty() ? null : data.getUnsafe());
    }

    /** Whether the stack's custom data contains {@code key}. */
    public static boolean hasCustom(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(key);
    }

    /** A copy of one custom-data entry, or {@code null}. */
    public static @Nullable Tag custom(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(key)) return null;
        Tag value = data.getUnsafe().get(key);
        return value == null ? null : value.copy();
    }

    /** A copy of one compound custom-data entry, or {@code null} when absent or not a compound. */
    public static @Nullable CompoundTag customCompound(ItemStack stack, String key) {
        return custom(stack, key) instanceof CompoundTag compound ? compound : null;
    }

    /** Edits a copy of the custom data and stores it; an empty result removes the component. */
    public static void editCustom(ItemStack stack, Consumer<CompoundTag> edit) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, edit);
    }

    /** Stores {@code value} (copied) under {@code key}. */
    public static void putCustom(ItemStack stack, String key, Tag value) {
        Tag copy = value.copy();
        editCustom(stack, tag -> tag.put(key, copy));
    }

    /** Removes only {@code key}; other keys stay, and empty custom data is removed. */
    public static void removeCustom(ItemStack stack, String key) {
        if (hasCustom(stack, key)) editCustom(stack, tag -> tag.remove(key));
    }

    /** Replaces the whole custom data with a copy of {@code tag} (absent when null or empty). */
    public static void setCustomData(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** A copy of the native placed block-entity payload ({@code block_entity_data}), or {@code null}. */
    public static @Nullable CompoundTag blockEntityData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null || data.isEmpty() ? null : data.copyTag();
    }

    /** The native placed blockstate overrides ({@code block_state}), or {@code null} when none. */
    public static @Nullable BlockItemStateProperties blockState(ItemStack stack) {
        BlockItemStateProperties state = stack.get(DataComponents.BLOCK_STATE);
        return state == null || state.isEmpty() ? null : state;
    }

    /** Whether the stack carries a native placed-block payload (block-entity data or blockstate overrides). */
    public static boolean hasPlacedBlockPayload(ItemStack stack) {
        return blockEntityData(stack) != null || blockState(stack) != null;
    }

    /**
     * Whether every component change relative to the item's defaults is one of {@code allowed}. A removed
     * default is a change too and is refused unless its type is in {@code allowedRemovals}.
     */
    public static boolean onlyChanges(ItemStack stack, Set<DataComponentType<?>> allowed, Set<DataComponentType<?>> allowedRemovals) {
        DataComponentPatch patch = stack.getComponentsPatch();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            boolean removal = entry.getValue().isEmpty();
            if (removal ? !allowedRemovals.contains(entry.getKey()) : !allowed.contains(entry.getKey())) return false;
        }
        return true;
    }
}
