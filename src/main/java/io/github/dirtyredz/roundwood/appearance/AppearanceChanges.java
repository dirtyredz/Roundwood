package io.github.dirtyredz.roundwood.appearance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * The restyling rules, as pure functions of the current appearance. Each gesture either changes the
 * look (one material or dye is then paid), leaves it exactly as it is (free, still handled), or is
 * refused (masonry cannot be painted).
 * <ul>
 *     <li>Planks set the wood and remove any paint, so re-planking a painted object with its own wood
 *     is a change (it strips the paint).</li>
 *     <li>Dye paints wood, keeping the wood underneath.</li>
 *     <li>Masonry (trims only) replaces whatever was there and clears paint.</li>
 * </ul>
 */
public final class AppearanceChanges {
    public enum Outcome {
        CHANGED, UNCHANGED, REFUSED
    }

    /** What a gesture does: the outcome and the appearance afterwards ({@code current} unless changed). */
    public record Change(Outcome outcome, Appearance next) {
        public boolean changed() {
            return outcome == Outcome.CHANGED;
        }
    }

    private AppearanceChanges() {
    }

    public static Change wood(Appearance current, ResourceLocation planks) {
        return to(current, Appearance.wood(planks));
    }

    public static Change paint(Appearance current, DyeColor dye) {
        if (!current.isWood()) {
            return new Change(Outcome.REFUSED, current);
        }
        return to(current, current.painted(dye));
    }

    public static Change masonry(Appearance current, ResourceLocation block) {
        return to(current, Appearance.masonry(block));
    }

    public static Change surface(Appearance current, ResourceLocation block) {
        return to(current, Appearance.surface(block));
    }

    private static Change to(Appearance current, Appearance next) {
        return next.equals(current) ? new Change(Outcome.UNCHANGED, current) : new Change(Outcome.CHANGED, next);
    }
}
