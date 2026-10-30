package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareSlot;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.fixture.migration.SaveUpgradeFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

/**
 * Forge 1.20.1 block states and Roundwood block entities, from 0.13.3 and 0.17.0 saves, through vanilla's
 * blockstate and block-entity fixers and the target loaders: every look, attachment slot, glass, hardware,
 * fitted frame and camouflage arrives as saved, unknown payloads stay locked and byte-identical, and a target
 * save/load round trip is idempotent. World-level assembly conversion (archways, crossed windows, aliases)
 * stays with the structure-fixture migration suites.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SaveUpgradeGameTests {
    private SaveUpgradeGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void historicalBlockEntitiesLoadAndResaveLosslessly(GameTestHelper helper) {
        for (String version : SaveUpgradeFixtures.VERSIONS) {
            int dataVersion = SaveUpgradeFixtures.dataVersion(version);
            for (CompoundTag fixture : SaveUpgradeFixtures.cases(version, "block_entities")) {
                String what = version + " " + fixture.getString("name");
                CompoundTag expect = fixture.getCompound("expect");
                BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),
                        SaveUpgradeFixtures.upgradeBlockState(fixture.getCompound("state"), dataVersion));
                check(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(expect.getString("block")), what + ": block " + state);
                CompoundTag states = expect.getCompound("state");
                for (String name : states.getAllKeys()) {
                    Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
                    check(property != null && valueName(property, state).equals(states.getString(name)),
                            what + ": " + name + " read as " + (property == null ? "absent" : valueName(property, state)));
                }
                BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                CompoundTag entityTag = SaveUpgradeFixtures.upgradeBlockEntity(fixture.getCompound("entity"), dataVersion);
                BlockEntity loaded = BlockEntity.loadStatic(pos, state, entityTag, TestRegistries.get());
                check(loaded instanceof AppearanceAnchorBlockEntity, what + ": no Roundwood anchor loaded");
                AppearanceAnchorBlockEntity anchor = (AppearanceAnchorBlockEntity) loaded;
                anchor.setLevel(helper.getLevel());
                checkContents(what, anchor, expect);
                CompoundTag first = anchor.saveWithFullMetadata(TestRegistries.get());
                for (String key : expect.getCompound("keeps").getAllKeys())
                    check(expect.getCompound("keeps").get(key).equals(first.get(key)), what + ": " + key + " changed on disk");
                BlockEntity reloaded = BlockEntity.loadStatic(pos, state, first, TestRegistries.get());
                checkContents(what + " (reloaded)", (AppearanceAnchorBlockEntity) reloaded, expect);
                check(reloaded.saveWithFullMetadata(TestRegistries.get()).equals(first), what + ": a second save differs");
            }
        }
        helper.succeed();
    }

    private static void checkContents(String what, AppearanceAnchorBlockEntity anchor, CompoundTag expect) {
        if (expect.contains("appearance"))
            check(AppearanceNbt.encode(anchor.appearance()).equals(expect.getCompound("appearance")), what + ": look " + anchor.appearance());
        if (expect.getBoolean("defaultAppearance"))
            check(anchor.appearance().equals(AppearanceDefaults.of(anchor.getBlockState().getBlock())), what + ": not the block's default look");
        for (Tag raw : expect.getList("trims", Tag.TAG_COMPOUND)) {
            CompoundTag trims = (CompoundTag) raw;
            TrimSlot slot = slot(trims.getString("slot"));
            for (AttachmentFace face : AttachmentFace.values()) {
                String key = face == AttachmentFace.FRONT ? "front" : "back";
                Optional<Appearance> actual = anchor.trims(slot).get(face);
                if (!trims.contains(key)) {
                    check(actual.isEmpty(), what + ": unexpected " + slot + " " + face + " " + actual);
                    continue;
                }
                CompoundTag wanted = trims.getCompound(key);
                check(actual.isPresent() && (wanted.isEmpty() ? actual.get().equals(Appearance.DEFAULT) : AppearanceNbt.encode(actual.get()).equals(wanted)),
                        what + ": " + slot + " " + face + " " + actual);
            }
        }
        for (Tag raw : expect.getList("locked", Tag.TAG_STRING))
            check(!anchor.trimsEditable(slot(raw.getAsString())), what + ": " + raw.getAsString() + " became editable");
        if (expect.contains("glass"))
            check(((RoundWindowBlockEntity) anchor).glass().map(color -> color.getName()).orElse("").equals(expect.getString("glass")), what + ": glass");
        if (anchor instanceof RoundDoorBlockEntity door) checkDoor(what, door, expect);
    }

    private static void checkDoor(String what, RoundDoorBlockEntity door, CompoundTag expect) {
        CompoundTag hardware = expect.getCompound("hardware");
        for (DoorHardwareSlot slot : DoorHardwareSlot.values()) {
            Optional<Appearance> actual = door.hardware().get(slot);
            check(hardware.contains(slot.name()) ? actual.map(AppearanceNbt::encode).filter(hardware.getCompound(slot.name())::equals).isPresent() : actual.isEmpty(),
                    what + ": hardware " + slot + " " + actual);
        }
        if (expect.contains("arch")) {
            CompoundTag arch = expect.getCompound("arch");
            check(door.arch().filter(fitted -> fitted.product().name().equals(arch.getString("product"))
                    && fitted.thickness().name().equals(arch.getString("thickness")) && fitted.depth().name().equals(arch.getString("depth"))).isPresent(),
                    what + ": fitted frame " + door.arch());
        }
        CompoundTag camouflage = expect.getCompound("camouflage");
        if (camouflage.isEmpty()) {
            check(door.camouflage().isEmpty(), what + ": unexpected camouflage");
            return;
        }
        Optional<CamouflageData> data = door.camouflage();
        check(data.isPresent() && BuiltInRegistries.BLOCK.getKey(data.get().material().getBlock()).toString().equals(camouflage.getString("material")),
                what + ": camouflage material " + data);
        boolean locked = camouflage.getBoolean("locked");
        check(door.camouflageEditable() != locked, what + ": camouflage custody " + (locked ? "not locked" : "locked"));
        if (locked) {
            check(!data.get().paid(), what + ": a locked refund was decoded");
            return;
        }
        check(data.get().paid() == camouflage.contains("refund"), what + ": refund presence");
        if (!camouflage.contains("refund")) return;
        var refund = data.get().refundStack();
        check(BuiltInRegistries.ITEM.getKey(refund.getItem()).toString().equals(camouflage.getString("refund")) && refund.getCount() == 1,
                what + ": refund " + refund);
        if (camouflage.contains("refundName"))
            check(refund.getHoverName().getString().equals(camouflage.getString("refundName")), what + ": refund name " + refund.getHoverName());
        if (camouflage.contains("refundEnchantment"))
            check(refund.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(ResourceLocation.parse(camouflage.getString("refundEnchantment")))),
                    what + ": refund enchantments " + refund.getEnchantments());
        check(!door.getUpdateTag(TestRegistries.get()).getCompound(CamouflageData.TAG).contains("Refund"), what + ": the refund reached clients");
    }

    private static TrimSlot slot(String name) {
        for (TrimSlot slot : TrimSlot.values()) if (slot.getSerializedName().equals(name)) return slot;
        throw new IllegalArgumentException(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String valueName(Property property, BlockState state) {
        return property.getName(state.getValue(property));
    }
}
