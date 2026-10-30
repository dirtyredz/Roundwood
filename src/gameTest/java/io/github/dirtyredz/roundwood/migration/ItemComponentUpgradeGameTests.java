package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.item.StackPayloads;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.fixture.migration.SaveUpgradeFixtures;
import io.github.dirtyredz.roundwood.migration.item.ArchwayStacks;
import io.github.dirtyredz.roundwood.migration.retired.ArchwayStackMigration;
import io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;

/**
 * Forge 1.20.1 item stacks after vanilla's 1.20.5 componentization: Roundwood's own keys arrive intact in
 * {@code custom_data}, vanilla metadata in its native components, placed-block contents in
 * {@code block_entity_data}, and the retired-archway conversion keeps the whole component patch on the top piece.
 * Stacks are converted in their real holders (player inventory and ender chest, a chest, a dropped item, a chest
 * minecart) through vanilla's player, block-entity and entity fixers.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemComponentUpgradeGameTests {
    private ItemComponentUpgradeGameTests() {
    }

    private static net.minecraft.world.item.Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path));
    }

    private static ItemStack parse(CompoundTag upgraded, String what) {
        ItemStack stack = ItemStack.parseOptional(TestRegistries.get(), upgraded);
        check(!stack.isEmpty(), what + ": the upgraded stack did not parse: " + upgraded);
        return stack;
    }

    @GameTest(template = TEMPLATE)
    public static void legacyStacksKeepRoundwoodDataAndNativeMetadata(GameTestHelper helper) {
        for (String version : SaveUpgradeFixtures.VERSIONS) {
            int dataVersion = SaveUpgradeFixtures.dataVersion(version);
            for (CompoundTag fixture : SaveUpgradeFixtures.cases(version, "stacks")) {
                String what = version + " " + fixture.getString("name");
                ItemStack stack = parse(SaveUpgradeFixtures.upgradeStack(fixture.getCompound("stack"), dataVersion), what);
                expect(helper, what, stack, fixture.getCompound("expect"));
                ItemStack resaved = ItemStack.parseOptional(TestRegistries.get(), (CompoundTag) stack.save(TestRegistries.get()));
                check(ItemStack.matches(stack, resaved), what + ": a target save/load changed the stack");
            }
        }
        helper.succeed();
    }

    static void expect(GameTestHelper helper, String what, ItemStack stack, CompoundTag expect) {
        check(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(expect.getString("item")), what + ": item " + stack.getItem());
        check(stack.getCount() == expect.getInt("count"), what + ": count " + stack.getCount());
        if (expect.contains("appearance")) {
            check(expect.getCompound("appearance").equals(StackPayloads.customCompound(stack, AppearanceNbt.TAG)),
                    what + ": appearance payload " + StackPayloads.customData(stack));
            check(AppearanceNbt.encode(AppearanceStacks.read(stack)).equals(expect.getCompound("appearance")), what + ": read look");
        } else {
            check(!StackPayloads.hasCustom(stack, AppearanceNbt.TAG), what + ": an appearance appeared");
        }
        if (expect.contains("glass"))
            check(WindowGlassStacks.read(stack).map(color -> color.getName()).orElse("").equals(expect.getString("glass")), what + ": glass");
        if (expect.contains("name"))
            check(stack.has(DataComponents.CUSTOM_NAME) && stack.getHoverName().getString().equals(expect.getString("name")), what + ": name");
        if (expect.contains("lore"))
            check(stack.getOrDefault(DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY).lines().size() == expect.getInt("lore"), what + ": lore");
        if (expect.contains("repairCost"))
            check(stack.getOrDefault(DataComponents.REPAIR_COST, 0) == expect.getInt("repairCost"), what + ": repair cost");
        if (expect.contains("enchantment")) {
            var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
            check(enchantments.keySet().stream().anyMatch(holder -> holder.is(ResourceLocation.parse(expect.getString("enchantment")))),
                    what + ": enchantments " + enchantments);
        }
        if (expect.contains("custom")) {
            CompoundTag custom = expect.getCompound("custom");
            for (String key : custom.getAllKeys())
                check(custom.get(key).equals(StackPayloads.custom(stack, key)), what + ": custom " + key + " " + StackPayloads.custom(stack, key));
        }
        if (expect.contains("blockEntity")) {
            CompoundTag contents = StackPayloads.blockEntityData(stack);
            check(contents != null, what + ": block-entity contents lost");
            CompoundTag wanted = expect.getCompound("blockEntity");
            for (String key : wanted.getAllKeys()) check(wanted.get(key).equals(contents.get(key)), what + ": contents " + key);
        }
        if (expect.contains("crossed")) {
            CompoundTag crossed = expect.getCompound("crossed");
            check(AppearanceNbt.encode(CrossWindowCompatibility.appearance(stack)).equals(crossed.getCompound("appearance")), what + ": crossed look");
            check(CrossWindowCompatibility.glass(stack).map(color -> color.getName()).orElse("").equals(crossed.getString("glass")), what + ": crossed glass");
        }
        if (expect.contains("hardwareReadable"))
            check(DoorHardwareStacks.readable(stack) == expect.getBoolean("hardwareReadable"), what + ": hardware readability");
        if (expect.contains("archway")) archway(what, stack, expect.getCompound("archway"));
    }

    /** One top per old item keeping the whole patch; keyholes add two plain-looking shoulders per item. */
    private static void archway(String what, ItemStack source, CompoundTag expect) {
        List<ItemStack> replaced = ArchwayStacks.replacements(source);
        check(!replaced.isEmpty(), what + ": no replacement");
        ItemStack top = replaced.get(0);
        check(top.is(ModItems.archPiece(ArchPieceDesign.TOP_1X2)) && top.getCount() == expect.getInt("tops"), what + ": top " + top);
        check(top.getComponentsPatch().equals(source.getComponentsPatch()), what + ": the top lost metadata " + top.getComponentsPatch());
        int shoulders = 0;
        for (ItemStack shoulder : replaced.subList(1, replaced.size())) {
            check(shoulder.is(ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER)), what + ": extra " + shoulder);
            check(AppearanceStacks.read(shoulder).equals(AppearanceStacks.read(source)), what + ": shoulder look");
            check(!shoulder.has(DataComponents.CUSTOM_NAME), what + ": a shoulder inherited the name");
            shoulders += shoulder.getCount();
        }
        check(shoulders == expect.getInt("shoulders"), what + ": shoulders " + shoulders);
    }

    @GameTest(template = TEMPLATE)
    public static void playerHoldersConvertThroughThePlayerFixer(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        CompoundTag saved = SaveUpgradeFixtures.compound("0.13.3", "holders").getCompound("player");
        CompoundTag player = SaveUpgradeFixtures.upgradePlayer(saved, saved.getInt("DataVersion"));
        var owner = player(helper, "legacy_owner", GameType.SURVIVAL, Direction.NORTH);
        owner.getInventory().load(player.getList("Inventory", Tag.TAG_COMPOUND));
        owner.getEnderChestInventory().fromTag(player.getList("EnderItems", Tag.TAG_COMPOUND), TestRegistries.get());
        check(owner.getInventory().getItem(0).is(item("keyhole_archway")) && owner.getInventory().getItem(0).getHoverName().getString().equals("Pack"),
                "keyhole archway did not load " + owner.getInventory().getItem(0));
        check(ArchwayStackMigration.convert(level, owner.getInventory(), owner.position()), "the inventory did not convert");
        check(ArchwayStackMigration.convert(level, owner.getEnderChestInventory(), owner.position()), "the ender chest did not convert");
        ItemStack top = owner.getInventory().getItem(0);
        check(top.is(ModItems.archPiece(ArchPieceDesign.TOP_1X2)) && top.getCount() == 2 && top.getHoverName().getString().equals("Pack"), "inventory top " + top);
        check(count(owner.getInventory(), ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER)) == 4, "inventory shoulders");
        check(owner.getInventory().getItem(1).is(ModItems.ROUND_DOOR.get()), "an unrelated door moved");
        check(owner.getEnderChestInventory().getItem(3).is(ModItems.archPiece(ArchPieceDesign.TOP_1X2)), "ender top");
        check(!ArchwayStackMigration.convert(level, owner.getInventory(), owner.position()), "a second pass converted again");
        check(count(owner.getInventory(), ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER)) == 4, "a second pass added shoulders");
        CompoundTag modern = SaveUpgradeFixtures.compound("0.17.0", "holders").getCompound("player");
        CompoundTag upgraded = SaveUpgradeFixtures.upgradePlayer(modern, modern.getInt("DataVersion"));
        var keeper = player(helper, "modern_owner", GameType.SURVIVAL, Direction.NORTH);
        keeper.getInventory().load(upgraded.getList("Inventory", Tag.TAG_COMPOUND));
        keeper.getEnderChestInventory().fromTag(upgraded.getList("EnderItems", Tag.TAG_COMPOUND), TestRegistries.get());
        check(keeper.getInventory().getItem(0).getCount() == 7 && keeper.getInventory().getItem(0).getHoverName().getString().equals("Crown"), "0.17.0 thin arch");
        check(DoorHardwareStacks.readable(keeper.getInventory().getItem(1)), "0.17.0 door hardware unreadable");
        check(keeper.getInventory().getItem(2).is(item("round_window_edge_left_cross")),
                "a retired crossed window changed identity " + keeper.getInventory().getItem(2));
        check(WindowGlassStacks.read(keeper.getEnderChestInventory().getItem(0)).map(color -> color.getName()).orElse("").equals("lime"), "0.17.0 frameless glass");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void containerAndEntityHoldersConvertThroughTheirFixers(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        CompoundTag holders = SaveUpgradeFixtures.compound("0.13.3", "holders");
        int dataVersion = SaveUpgradeFixtures.dataVersion("0.13.3");
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(at, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(at);
        chest.loadWithComponents(SaveUpgradeFixtures.upgradeBlockEntity(holders.getCompound("chest"), dataVersion), TestRegistries.get());
        check(ArchwayStackMigration.convert(level, chest, Vec3.atCenterOf(at)), "the chest did not convert");
        check(chest.getItem(4).is(ModItems.archPiece(ArchPieceDesign.TOP_1X2)) && chest.getItem(4).getCount() == 3
                && AppearanceStacks.read(chest.getItem(4)).material().getPath().equals("spruce_planks"), "chest top " + chest.getItem(4));

        BlockPos loot = helper.absolutePos(new BlockPos(4, 1, 2));
        level.setBlockAndUpdate(loot, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity unopened = (ChestBlockEntity) level.getBlockEntity(loot);
        unopened.loadWithComponents(SaveUpgradeFixtures.upgradeBlockEntity(holders.getCompound("lootChest"), dataVersion), TestRegistries.get());
        check(unopened.getLootTable() != null, "the loot table did not survive conversion");
        check(!ArchwayStackMigration.convert(level, unopened, Vec3.atCenterOf(loot)), "an unopened loot chest was rolled");

        CompoundTag item = SaveUpgradeFixtures.upgradeEntity(holders.getCompound("itemEntity"), dataVersion);
        var dropped = EntityType.create(item, level).orElseThrow();
        check(dropped instanceof ItemEntity entity && entity.getItem().is(item("keyhole_archway"))
                && AppearanceStacks.read(entity.getItem()).material().getPath().equals("stone_bricks"), "dropped item " + dropped);

        CompoundTag cart = SaveUpgradeFixtures.upgradeEntity(holders.getCompound("minecart"), dataVersion);
        var minecart = EntityType.create(cart, level).orElseThrow();
        minecart.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 1, 2))));
        check(minecart instanceof Container, "the minecart lost its container");
        Container contents = (Container) minecart;
        List<ItemStack> spilled = new ArrayList<>();
        check(ArchwayStacks.convert(contents, stacks -> spilled.addAll(stacks)), "the minecart did not convert");
        int shoulders = count(contents, ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER))
                + spilled.stream().mapToInt(ItemStack::getCount).sum();
        check(contents.getItem(0).is(ModItems.archPiece(ArchPieceDesign.TOP_1X2)) && shoulders == 2, "minecart conversion " + contents.getItem(0));
        helper.succeed();
    }

    private static int count(Container container, net.minecraft.world.item.Item item) {
        int total = 0;
        int size = container instanceof Inventory inventory ? inventory.items.size() : container.getContainerSize();
        for (int slot = 0; slot < size; slot++) if (container.getItem(slot).is(item)) total += container.getItem(slot).getCount();
        return total;
    }
}
