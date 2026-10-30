package io.github.dirtyredz.roundwood.migration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.fixture.migration.SaveUpgradeFixtures;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

/**
 * Every registration a Forge 1.20.1 release saved (0.13.3 in the pack, 0.17.0 released) resolves to itself here,
 * with the exact blockstate domains that release wrote. The manifests are captured from the releases, never from
 * this build; a missing name would otherwise load as air or the default item.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistryCompatibilityGameTests {
    private RegistryCompatibilityGameTests() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, path);
    }

    private static Set<String> strings(JsonObject json, String key) {
        Set<String> out = new TreeSet<>();
        for (JsonElement element : json.getAsJsonArray(key)) out.add(element.getAsString());
        return out;
    }

    @GameTest(template = TEMPLATE)
    public static void everyHistoricalNameResolvesExactly(GameTestHelper helper) {
        for (String version : SaveUpgradeFixtures.VERSIONS) {
            JsonObject manifest = SaveUpgradeFixtures.manifest(version);
            for (String block : strings(manifest, "blocks")) {
                check(BuiltInRegistries.BLOCK.containsKey(id(block)), version + ": block " + block + " is not registered");
                check(BuiltInRegistries.BLOCK.getKey(BuiltInRegistries.BLOCK.get(id(block))).equals(id(block)),
                        version + ": block " + block + " resolves to a fallback");
            }
            for (String item : strings(manifest, "items")) {
                check(BuiltInRegistries.ITEM.containsKey(id(item)), version + ": item " + item + " is not registered");
                check(BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.get(id(item))).equals(id(item)),
                        version + ": item " + item + " resolves to a fallback");
            }
            for (String type : strings(manifest, "blockEntities"))
                check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id(type)), version + ": block entity " + type);
            for (String condition : strings(manifest, "lootConditions"))
                check(BuiltInRegistries.LOOT_CONDITION_TYPE.containsKey(id(condition)), version + ": loot condition " + condition);
            for (String serializer : strings(manifest, "recipeSerializers"))
                check(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id(serializer)), version + ": recipe serializer " + serializer);
            for (String tab : strings(manifest, "creativeTabs"))
                check(helper.getLevel().registryAccess().registryOrThrow(Registries.CREATIVE_MODE_TAB).containsKey(id(tab)), version + ": tab " + tab);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void frozenAliasesAndRetiredCrossedWindowsStayRegistered(GameTestHelper helper) {
        JsonObject families = SaveUpgradeFixtures.manifest("0.13.3").getAsJsonObject("families");
        Set<String> aliases = new HashSet<>();
        for (String family : new String[]{"round_door", "round_window", "frame"})
            for (JsonElement member : families.getAsJsonObject(family).getAsJsonArray("members"))
                if (!member.getAsString().startsWith("round_")) aliases.add(member.getAsString());
        check(aliases.size() == 49, "expected 49 frozen aliases, found " + aliases.size());
        Set<String> crossed = SaveUpgradeFixtures.manifest("0.17.0").getAsJsonArray("blocks").asList().stream()
                .map(JsonElement::getAsString).filter(block -> block.endsWith("_cross")).collect(Collectors.toSet());
        check(crossed.size() == 13, "expected 13 retired crossed windows, found " + crossed.size());
        for (String block : crossed) {
            check(BuiltInRegistries.ITEM.containsKey(id(block)), block + " lost its item");
        }
        helper.succeed();
    }

    /** Every historical property keeps its exact domain; a property a release never wrote reads as its recorded value. */
    @GameTest(template = TEMPLATE)
    public static void historicalBlockstateDomainsAreExact(GameTestHelper helper) {
        for (String version : SaveUpgradeFixtures.VERSIONS) {
            JsonObject manifest = SaveUpgradeFixtures.manifest(version);
            JsonObject added = manifest.getAsJsonObject("addedSince");
            for (Map.Entry<String, JsonElement> family : manifest.getAsJsonObject("families").entrySet()) {
                JsonObject properties = family.getValue().getAsJsonObject().getAsJsonObject("properties");
                JsonObject later = added.has(family.getKey()) ? added.getAsJsonObject(family.getKey()) : new JsonObject();
                for (JsonElement member : family.getValue().getAsJsonObject().getAsJsonArray("members")) {
                    Block block = BuiltInRegistries.BLOCK.get(id(member.getAsString()));
                    String what = version + " " + member.getAsString();
                    Set<String> names = new TreeSet<>();
                    for (Property<?> property : block.getStateDefinition().getProperties()) names.add(property.getName());
                    Set<String> expected = new TreeSet<>(properties.keySet());
                    expected.addAll(later.keySet());
                    check(names.equals(expected), what + ": properties " + names + ", expected " + expected);
                    for (Map.Entry<String, JsonElement> property : properties.entrySet()) {
                        Property<?> actual = block.getStateDefinition().getProperty(property.getKey());
                        Set<String> values = actual.getPossibleValues().stream().map(value -> name(actual, value)).collect(Collectors.toCollection(TreeSet::new));
                        Set<String> wanted = property.getValue().getAsJsonArray().asList().stream().map(JsonElement::getAsString)
                                .collect(Collectors.toCollection(TreeSet::new));
                        check(values.equals(wanted), what + " " + property.getKey() + ": " + values + ", expected " + wanted);
                    }
                    // A state saved by this release, with only the properties it knew, reads back with the later defaults.
                    CompoundTag saved = new CompoundTag();
                    saved.putString("Name", id(member.getAsString()).toString());
                    CompoundTag values = new CompoundTag();
                    for (Map.Entry<String, JsonElement> property : properties.entrySet())
                        values.putString(property.getKey(), property.getValue().getAsJsonArray().get(0).getAsString());
                    saved.put("Properties", values);
                    BlockState read = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),
                            SaveUpgradeFixtures.upgradeBlockState(saved, SaveUpgradeFixtures.dataVersion(version)));
                    check(read.is(block), what + ": a saved state read as " + read);
                    for (Map.Entry<String, JsonElement> property : later.entrySet()) {
                        Property<?> actual = block.getStateDefinition().getProperty(property.getKey());
                        check(name(actual, read.getValue(actual)).equals(property.getValue().getAsString()),
                                what + ": a missing " + property.getKey() + " read as " + read.getValue(actual));
                    }
                }
            }
        }
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String name(Property property, Object value) {
        return property.getName((Comparable) value);
    }
}
