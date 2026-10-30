package io.github.dirtyredz.roundwood.appearance.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The custom-data bridge: copies never alias, writes keep unrelated keys, empty custom data disappears. */
class StackPayloadsTest {
    private static final Appearance SPRUCE = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    @Test void readsAreCopiesAndWritesNeverAliasTheCaller() {
        ItemStack stack = new ItemStack(Items.STICK);
        CompoundTag value = new CompoundTag();
        value.putString("Kept", "a");
        StackPayloads.putCustom(stack, "Owner", value);
        value.putString("Kept", "changed after the write");
        assertEquals("a", StackPayloads.customCompound(stack, "Owner").getString("Kept"));
        StackPayloads.customData(stack).remove("Owner");
        StackPayloads.customCompound(stack, "Owner").putString("Kept", "changed through a read");
        assertEquals("a", StackPayloads.customCompound(stack, "Owner").getString("Kept"));
    }

    @Test void copiedStacksAreCopyOnWrite() {
        ItemStack original = new ItemStack(Items.STICK);
        StackPayloads.putCustom(original, "Key", StringTag.valueOf("one"));
        ItemStack copy = original.copy();
        StackPayloads.putCustom(copy, "Key", StringTag.valueOf("two"));
        assertEquals("one", StackPayloads.custom(original, "Key").getAsString());
        assertEquals("two", StackPayloads.custom(copy, "Key").getAsString());
    }

    @Test void removingOneKeyKeepsUnrelatedDataAndAnEmptyResultStacksWithDefaults() {
        ItemStack stack = new ItemStack(ModItems.ROUND_DOOR.get());
        AppearanceStacks.write(stack, SPRUCE);
        StackPayloads.putCustom(stack, "OtherMod", StringTag.valueOf("keep"));
        AppearanceStacks.write(stack, AppearanceStacks.read(new ItemStack(ModItems.ROUND_DOOR.get())));
        assertFalse(StackPayloads.hasCustom(stack, AppearanceNbt.TAG), "the default look was stored");
        assertEquals("keep", StackPayloads.custom(stack, "OtherMod").getAsString(), "an unrelated key was dropped");
        StackPayloads.removeCustom(stack, "OtherMod");
        assertNull(stack.get(DataComponents.CUSTOM_DATA), "empty custom data remained");
        assertTrue(ItemStack.isSameItemSameComponents(stack, new ItemStack(ModItems.ROUND_DOOR.get())), "a default door does not stack");
    }

    @Test void styledStacksStoreOnlyTheirOwnKeyAndDefaultsStoreNothing() {
        ItemStack styled = AppearanceStacks.create(ModItems.ROUND_DOOR.get(), SPRUCE);
        assertEquals(Set.of(AppearanceNbt.TAG), StackPayloads.customData(styled).getAllKeys());
        assertEquals(SPRUCE, AppearanceStacks.read(styled));
        ItemStack plain = AppearanceStacks.create(ModItems.ROUND_DOOR.get(), AppearanceStacks.read(new ItemStack(ModItems.ROUND_DOOR.get())));
        assertTrue(plain.getComponentsPatch().isEmpty(), "a default stack carries components");
    }

    @Test void nativePlacedBlockPayloadsAreRecognised() {
        ItemStack plain = new ItemStack(Items.OAK_PLANKS);
        assertFalse(StackPayloads.hasPlacedBlockPayload(plain));
        ItemStack entity = new ItemStack(Items.CHEST);
        CompoundTag contents = new CompoundTag();
        contents.putString("Lock", "key"); // an empty compound is no payload: vanilla removes the component
        BlockItem.setBlockEntityData(entity, BlockEntityType.CHEST, contents);
        assertTrue(StackPayloads.hasPlacedBlockPayload(entity), "block_entity_data");
        ItemStack state = new ItemStack(Items.OAK_LOG);
        state.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(BlockStateProperties.AXIS, Direction.Axis.X));
        assertTrue(StackPayloads.hasPlacedBlockPayload(state), "block_state");
        ItemStack emptyState = new ItemStack(Items.OAK_LOG);
        emptyState.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        assertFalse(StackPayloads.hasPlacedBlockPayload(emptyState), "empty overrides are no payload");
    }

    @Test void componentChangesAreJudgedAgainstItemDefaults() {
        ItemStack stack = new ItemStack(Items.STICK);
        assertTrue(StackPayloads.onlyChanges(stack, Set.of(), Set.of()), "defaults are not changes");
        StackPayloads.putCustom(stack, "Key", StringTag.valueOf("v"));
        assertTrue(StackPayloads.onlyChanges(stack, Set.of(DataComponents.CUSTOM_DATA), Set.of()));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("named"));
        assertFalse(StackPayloads.onlyChanges(stack, Set.of(DataComponents.CUSTOM_DATA), Set.of()), "a name is a change");
        ItemStack removed = new ItemStack(Items.STICK);
        removed.remove(DataComponents.RARITY);
        assertFalse(StackPayloads.onlyChanges(removed, Set.of(DataComponents.RARITY), Set.of()), "a removed default is a change");
        assertTrue(StackPayloads.onlyChanges(removed, Set.of(), Set.of(DataComponents.RARITY)));
    }
}
