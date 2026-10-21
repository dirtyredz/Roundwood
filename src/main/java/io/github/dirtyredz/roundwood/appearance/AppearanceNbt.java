package io.github.dirtyredz.roundwood.appearance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.Optional;

/**
 * The one serialised form of an {@link Appearance}, shared by item stacks and block entities:
 * {@code RoundwoodAppearance: {Version: 1, Kind: "wood"|"masonry", Material: "<block id>", Paint: "<dye>"}}.
 * Reading never fails: a missing compound, an unknown kind or an unparseable id reads as nothing
 * (the caller's default); an unknown dye reads as unpainted. A well-formed id that is not registered
 * is kept (see {@link Appearance}).
 */
public final class AppearanceNbt {
    public static final String TAG = "RoundwoodAppearance";
    public static final int VERSION = 1;
    private static final String VERSION_KEY = "Version";
    private static final String KIND = "Kind";
    private static final String MATERIAL = "Material";
    private static final String PAINT = "Paint";

    private AppearanceNbt() {
    }

    public static CompoundTag encode(Appearance appearance) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, VERSION);
        tag.putString(KIND, appearance.kind().id());
        tag.putString(MATERIAL, appearance.material().toString());
        appearance.paint().ifPresent(dye -> tag.putString(PAINT, dye.getName()));
        return tag;
    }

    /** Reads the {@link #TAG} compound inside {@code parent}; empty when absent or unusable. */
    public static Optional<Appearance> read(CompoundTag parent) {
        if (parent == null || !parent.contains(TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return decode(parent.getCompound(TAG));
    }

    /** Puts {@code appearance} under {@link #TAG} in {@code parent}. */
    public static void write(CompoundTag parent, Appearance appearance) {
        parent.put(TAG, encode(appearance));
    }

    public static Optional<Appearance> decode(CompoundTag tag) {
        Optional<Appearance.Kind> kind = Appearance.Kind.byId(tag.getString(KIND));
        ResourceLocation material = ResourceLocation.tryParse(tag.getString(MATERIAL));
        if (kind.isEmpty() || material == null || tag.getString(MATERIAL).isEmpty()) {
            return Optional.empty();
        }
        Optional<DyeColor> paint = tag.contains(PAINT, Tag.TAG_STRING)
                ? Optional.ofNullable(DyeColor.byName(tag.getString(PAINT), null)) : Optional.empty();
        return Optional.of(new Appearance(kind.get(), material, paint));
    }
}
