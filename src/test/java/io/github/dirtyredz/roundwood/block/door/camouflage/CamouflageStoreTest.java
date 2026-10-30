package io.github.dirtyredz.roundwood.block.door.camouflage;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Camouflage custody: absent, free, paid (legacy or target refund encoding) and locked undecodable refunds. */
class CamouflageStoreTest {
    private static final HolderLookup.Provider REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static CompoundTag entity(String refund) throws Exception {
        CompoundTag camouflage = new CompoundTag();
        camouflage.put("Material", NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        if (refund != null) camouflage.put("Refund", TagParser.parseTag(refund));
        CompoundTag tag = new CompoundTag();
        tag.put(CamouflageData.TAG, camouflage);
        return tag;
    }

    private static CamouflageStore load(CompoundTag tag) {
        CamouflageStore store = new CamouflageStore();
        store.load(tag, REGISTRIES);
        return store;
    }

    @Test void absentAndFreeCamouflageAreEditable() throws Exception {
        CamouflageStore absent = load(new CompoundTag());
        assertTrue(absent.value().isEmpty() && absent.editable());
        CamouflageStore free = load(entity(null));
        assertTrue(free.editable());
        assertFalse(free.value().orElseThrow().paid());
        assertSame(Blocks.STONE.defaultBlockState(), free.value().get().material());
    }

    @Test void aForge1201RefundIsDataFixedOnceAndResavedInTheTargetEncoding() throws Exception {
        CamouflageStore store = load(entity("{id:\"minecraft:stone\",Count:1b,tag:{display:{Name:'{\"text\":\"Quarry\"}'}}}"));
        CamouflageData data = store.value().orElseThrow();
        assertTrue(data.paid() && store.editable());
        ItemStack refund = data.refundStack();
        assertTrue(refund.is(Items.STONE) && refund.getCount() == 1);
        assertEquals("Quarry", refund.getHoverName().getString(), "the name did not survive conversion");
        CompoundTag saved = new CompoundTag();
        store.save(saved, REGISTRIES);
        CompoundTag resaved = saved.getCompound(CamouflageData.TAG).getCompound("Refund");
        assertFalse(CamouflageRefundCodec.legacy(resaved), "the refund was not upgraded");
        CamouflageStore reloaded = load(saved);
        assertTrue(ItemStack.matches(refund, reloaded.value().orElseThrow().refundStack()), "a second load changed the refund");
    }

    @Test void aRefundThatDoesNotPayKeepsItsHistoricalFreeReading() throws Exception {
        CamouflageStore store = load(entity("{id:\"minecraft:dirt\",count:1}"));
        assertTrue(store.editable());
        assertFalse(store.value().orElseThrow().paid());
    }

    @Test void anUndecodableRefundIsLockedCustodyNeverAnEmptyStack() throws Exception {
        CompoundTag tag = entity("{id:\"othermod:missing_stone\",Count:1b,tag:{Secret:1b}}");
        CompoundTag raw = tag.getCompound(CamouflageData.TAG).copy();
        CamouflageStore store = load(tag);
        assertFalse(store.editable(), "an undecodable refund was editable");
        assertFalse(store.value().orElseThrow().paid(), "the material is shown without a refund");
        assertFalse(store.set(null), "a locked camouflage was cleared");
        assertFalse(store.set(CamouflageData.free(Blocks.DIRT.defaultBlockState())), "a locked camouflage was replaced");
        assertTrue(store.take().isEmpty(), "a locked camouflage was withdrawn");
        CompoundTag saved = new CompoundTag();
        store.save(saved, REGISTRIES);
        assertEquals(raw, saved.getCompound(CamouflageData.TAG), "the raw custody changed on disk");
        assertEquals(raw, store.locked());
        CompoundTag update = new CompoundTag();
        store.writeUpdate(update, REGISTRIES);
        assertFalse(update.getCompound(CamouflageData.TAG).contains("Refund"), "the refund reached a client update");
        CamouflageStore target = new CamouflageStore();
        store.transferTo(target);
        assertEquals(raw, target.locked(), "transfer lost the custody");
        assertTrue(store.value().isEmpty() && store.editable());
    }

    @Test void anUnacceptableMaterialWithARefundIsLockedToo() throws Exception {
        CompoundTag tag = entity("{id:\"minecraft:stone\",count:1}");
        tag.getCompound(CamouflageData.TAG).put("Material", NbtUtils.writeBlockState(Blocks.TORCH.defaultBlockState()));
        CamouflageStore store = load(tag);
        assertFalse(store.editable());
        assertTrue(store.value().isEmpty(), "an unacceptable material rendered");
    }
}
