package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.item.StackPayloads;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.fixture.migration.SaveUpgradeFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;

/**
 * The nested {@code Camouflage.Refund} stack is the one ItemStack inside a Roundwood block entity, and vanilla's
 * world upgrade never reaches it. A Forge 1.20.1 refund (named, enchanted) must decode through the data fixer,
 * come back exactly once, resave in the target encoding and never reach clients; an undecodable refund is locked
 * custody that edits refuse and that travels opaquely on the door's drop instead of vanishing.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CamouflageRefundUpgradeGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private CamouflageRefundUpgradeGameTests() {
    }

    /** A placed oak door whose anchor reloads with a Forge 1.20.1 Camouflage compound (vanilla-fixed as a whole entity). */
    private static RoundDoorBlockEntity legacyDoor(GameTestHelper helper, String refund) {
        floor(helper);
        ServerPlayer placer = player(helper, "refund_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "placement failed");
        RoundDoorBlockEntity entity = anchorEntity(helper, assembly(helper, ANCHOR, Direction.NORTH));
        CompoundTag saved = entity.saveWithFullMetadata(TestRegistries.get());
        CompoundTag camouflage = new CompoundTag();
        camouflage.put("Material", snbt("{Name:\"minecraft:stone\"}"));
        camouflage.put("Refund", snbt(refund));
        saved.put(CamouflageData.TAG, camouflage);
        entity.loadWithComponents(SaveUpgradeFixtures.upgradeBlockEntity(saved, SaveUpgradeFixtures.dataVersion("0.13.3")), TestRegistries.get());
        clearDrops(helper);
        return entity;
    }

    private static CompoundTag snbt(String text) {
        try {
            return TagParser.parseTag(text);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalArgumentException(text, e);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void aForgeRefundDecodesOnceAndIsRefundedExactlyOnce(GameTestHelper helper) {
        RoundDoorBlockEntity entity = legacyDoor(helper,
                "{id:\"minecraft:stone\",Count:1b,tag:{display:{Name:'{\"text\":\"Quarry\"}'},Enchantments:[{id:\"minecraft:unbreaking\",lvl:3s}]}}");
        CamouflageData data = entity.camouflage().orElseThrow();
        check(data.paid() && entity.camouflageEditable(), "the legacy refund was not decoded");
        ItemStack refund = data.refundStack();
        var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        check(refund.getHoverName().getString().equals("Quarry") && refund.getEnchantments().getLevel(unbreaking) == 3, "refund metadata lost " + refund);
        CompoundTag resaved = entity.saveWithFullMetadata(TestRegistries.get()).getCompound(CamouflageData.TAG).getCompound("Refund");
        check(!CamouflageRefundCodec.legacy(resaved) && resaved.contains("components"), "the refund was not resaved in the target encoding " + resaved);
        check(!entity.getUpdateTag(TestRegistries.get()).getCompound(CamouflageData.TAG).contains("Refund"), "the refund reached a client update");
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH);
        ServerPlayer breaker = player(helper, "refund_breaker", GameType.SURVIVAL, Direction.NORTH);
        check(breaker.gameMode.destroyBlock(door.pos(DoorLayout.Part.UPPER_LEFT)), "break refused");
        var refunds = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(ANCHOR)).inflate(4),
                item -> item.getItem().is(Items.STONE));
        check(refunds.size() == 1 && refunds.get(0).getItem().getCount() == 1
                && refunds.get(0).getItem().getHoverName().getString().equals("Quarry"), "expected one named refund, got " + refunds);
        check(dropped(helper, oakDoor().asItem()) == 1, "the door did not drop");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void anUndecodableRefundIsKeptLockedAndTravelsOnTheDoor(GameTestHelper helper) {
        RoundDoorBlockEntity entity = legacyDoor(helper, "{id:\"othermod:marble\",Count:1b,tag:{Quarried:1b}}");
        check(!entity.camouflageEditable(), "an undecodable refund was editable");
        CompoundTag raw = entity.lockedCamouflage();
        check(raw != null && raw.getCompound("Refund").getString("id").equals("othermod:marble"), "the raw refund was not kept " + raw);
        check(!entity.setCamouflage(null) && !entity.setCamouflage(CamouflageData.free(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState())),
                "a locked camouflage was edited");
        check(entity.takeCamouflage().isEmpty(), "a locked camouflage was withdrawn");
        check(entity.saveWithFullMetadata(TestRegistries.get()).getCompound(CamouflageData.TAG).equals(raw), "disk custody changed");
        check(!entity.getUpdateTag(TestRegistries.get()).getCompound(CamouflageData.TAG).contains("Refund"), "the raw refund reached clients");
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH);
        ServerPlayer breaker = player(helper, "locked_breaker", GameType.SURVIVAL, Direction.NORTH);
        check(breaker.gameMode.destroyBlock(door.pos(DoorLayout.Part.LOWER_LEFT)), "break refused");
        var doors = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(ANCHOR)).inflate(4),
                item -> item.getItem().is(oakDoor().asItem()));
        check(doors.size() == 1, "expected one door drop, got " + doors);
        ItemStack carried = doors.get(0).getItem();
        check(raw.equals(StackPayloads.customCompound(carried, CamouflageData.TAG)), "the door drop lost the locked custody " + carried.getComponentsPatch());
        check(CamouflageDrops.carriesLocked(carried), "the drop is not recognised as locked custody");
        doors.get(0).discard();
        ServerPlayer placer = player(helper, "locked_placer", GameType.SURVIVAL, Direction.NORTH);
        ItemStack held = carried.copy();
        check(!placeDoor(helper, placer, held, ANCHOR, DoorHingeSide.LEFT).consumesAction() && held.getCount() == 1,
                "a door carrying locked custody was placed");
        helper.succeed();
    }
}
