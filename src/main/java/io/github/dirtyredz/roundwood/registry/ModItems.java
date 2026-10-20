package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.RoundDoorItem;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** The block item of every round door and window, in creative-tab order. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Roundwood.MOD_ID);

    private static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    static {
        for (Finish finish : FinishCatalog.ALL) {
            ALL.add(ITEMS.register(finish.doorId(),
                    () -> new RoundDoorItem(ModBlocks.door(finish), new Item.Properties())));
        }
        for (Finish finish : FinishCatalog.ALL) {
            ALL.add(ITEMS.register(finish.windowId(),
                    () -> new BlockItem(ModBlocks.window(finish), new Item.Properties())));
        }
    }

    private ModItems() {
    }

    public static List<Item> all() {
        return ALL.stream().map(RegistryObject::get).toList();
    }
}
