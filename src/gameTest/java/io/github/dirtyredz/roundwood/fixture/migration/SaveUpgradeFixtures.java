package io.github.dirtyredz.roundwood.fixture.migration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The immutable Forge 1.20.1 save fixtures under {@code compatibility/forge-1.20.1/<version>/}: a manifest of the
 * release's registrations and blockstate contract, plus compact SNBT case lists (stacks, block entities, item
 * holders) written in that release's encoding. Every case runs through vanilla's own data fixer, from the fixture's
 * data version to the running one, before Roundwood's target serializers see it. Expected values live in the case
 * itself; nothing here calls the production converters under test.
 */
public final class SaveUpgradeFixtures {
    public static final List<String> VERSIONS = List.of("0.13.3", "0.17.0");
    private static final String ROOT = "/compatibility/forge-1.20.1/";

    private SaveUpgradeFixtures() {
    }

    public static JsonObject manifest(String version) {
        return JsonParser.parseString(text(version, "manifest.json")).getAsJsonObject();
    }

    /** The data version every case of {@code version} was saved at (the manifest's, 3465 for 1.20.1). */
    public static int dataVersion(String version) {
        return manifest(version).getAsJsonObject("source").get("dataVersion").getAsInt();
    }

    /** The {@code cases} list of {@code <version>/<file>.snbt}, each a compound with at least a {@code name}. */
    public static List<CompoundTag> cases(String version, String file) {
        try {
            CompoundTag root = TagParser.parseTag(text(version, file + ".snbt"));
            ListTag list = root.getList("cases", Tag.TAG_COMPOUND);
            List<CompoundTag> out = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) out.add(list.getCompound(i));
            return out;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(version + "/" + file + ": " + e.getMessage(), e);
        }
    }

    /** The whole compound of {@code <version>/<file>.snbt} (holder fixtures keyed by holder kind). */
    public static CompoundTag compound(String version, String file) {
        try {
            return TagParser.parseTag(text(version, file + ".snbt"));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(version + "/" + file + ": " + e.getMessage(), e);
        }
    }

    /** Vanilla's conversion of one {@code type} value saved at {@code fromVersion}. */
    public static CompoundTag upgrade(DSL.TypeReference type, CompoundTag saved, int fromVersion) {
        Tag fixed = DataFixers.getDataFixer().update(type, new Dynamic<>(NbtOps.INSTANCE, saved.copy()), fromVersion,
                SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        if (!(fixed instanceof CompoundTag compound)) throw new IllegalStateException("fixer returned " + fixed);
        return compound;
    }

    public static CompoundTag upgradeStack(CompoundTag saved, int fromVersion) {
        return upgrade(References.ITEM_STACK, saved, fromVersion);
    }

    public static CompoundTag upgradeBlockState(CompoundTag saved, int fromVersion) {
        return upgrade(References.BLOCK_STATE, saved, fromVersion);
    }

    public static CompoundTag upgradeBlockEntity(CompoundTag saved, int fromVersion) {
        return upgrade(References.BLOCK_ENTITY, saved, fromVersion);
    }

    public static CompoundTag upgradeEntity(CompoundTag saved, int fromVersion) {
        return upgrade(References.ENTITY, saved, fromVersion);
    }

    public static CompoundTag upgradePlayer(CompoundTag saved, int fromVersion) {
        return upgrade(References.PLAYER, saved, fromVersion);
    }

    private static String text(String version, String file) {
        String path = ROOT + version + "/" + file;
        try (InputStream in = SaveUpgradeFixtures.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing fixture " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
