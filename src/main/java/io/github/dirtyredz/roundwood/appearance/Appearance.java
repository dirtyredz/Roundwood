package io.github.dirtyredz.roundwood.appearance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.Locale;
import java.util.Optional;

/**
 * What a Roundwood door, window or trim looks like: a material and an optional paint. Immutable and
 * purely cosmetic (the block's own behaviour never changes with it).
 *
 * <ul>
 *     <li>{@link Kind#WOOD}: {@code material} is a plank block id; its default state is what is
 *     drawn. Paint covers the wood and keeps it underneath (repainting or re-planking changes it).</li>
 *     <li>{@link Kind#MASONRY}: {@code material} is one of the fixed masonry blocks
 *     ({@code material/MasonryMaterials}); trims only, never painted.</li>
 * </ul>
 * A material id that is not (or no longer) registered is kept as it is, so reinstalling its mod
 * restores the look; clients draw it as oak meanwhile.
 *
 * @param kind     wood or masonry
 * @param material the donor block's registry id
 * @param paint    the dye covering wood, empty for bare wood and always for masonry
 */
public record Appearance(Kind kind, ResourceLocation material, Optional<DyeColor> paint) {
    public static final ResourceLocation OAK_PLANKS = ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks");
    /** Oak, unpainted: what a missing appearance means for the canonical door, window and trim. */
    public static final Appearance DEFAULT = wood(OAK_PLANKS);

    public enum Kind {
        WOOD, MASONRY;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Optional<Kind> byId(String id) {
            for (Kind kind : values()) {
                if (kind.id().equals(id)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }

    public Appearance {
        // Masonry is never painted: normalise rather than carry a paint nothing draws.
        paint = kind == Kind.MASONRY ? Optional.empty() : paint;
    }

    public static Appearance wood(ResourceLocation planks) {
        return new Appearance(Kind.WOOD, planks, Optional.empty());
    }

    public static Appearance masonry(ResourceLocation block) {
        return new Appearance(Kind.MASONRY, block, Optional.empty());
    }

    /** This wood with {@code dye} over it (masonry stays unpainted). */
    public Appearance painted(DyeColor dye) {
        return new Appearance(kind, material, Optional.of(dye));
    }

    public boolean isPainted() {
        return paint.isPresent();
    }

    public boolean isWood() {
        return kind == Kind.WOOD;
    }

    /**
     * A stable, normalised identity: {@code wood/minecraft:spruce_planks}, {@code
     * wood/minecraft:oak_planks/red}, {@code masonry/minecraft:bricks}. Equal appearances have equal
     * keys; used for caches, viewer subtypes and comparisons.
     */
    public String key() {
        return kind.id() + "/" + material + paint.map(dye -> "/" + dye.getName()).orElse("");
    }
}
