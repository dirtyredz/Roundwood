package io.github.dirtyredz.roundwood.fixture;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Stacks carrying the native equivalents of 1.20.1's {@code BlockEntityTag} ({@code block_entity_data}) and
 * {@code BlockStateTag} ({@code block_state}) placed-block payloads, for donor-refusal checks.
 */
public final class PlacedPayloads {
    private PlacedPayloads() {
    }

    /** {@code legacyKey} names the historical tag ({@code "BlockEntityTag"} or {@code "BlockStateTag"}). */
    public static ItemStack with(Item item, String legacyKey) {
        ItemStack stack = new ItemStack(item);
        switch (legacyKey) {
            case "BlockEntityTag" -> BlockItem.setBlockEntityData(stack, BlockEntityType.CHEST, new CompoundTag());
            case "BlockStateTag" -> stack.set(DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY.with(BlockStateProperties.AXIS, Direction.Axis.X));
            default -> throw new IllegalArgumentException(legacyKey);
        }
        return stack;
    }
}
