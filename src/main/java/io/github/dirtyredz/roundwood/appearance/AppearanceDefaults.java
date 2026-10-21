package io.github.dirtyredz.roundwood.appearance;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The configured default of an appearance-bearing block or item: what it looks like when nothing is
 * stored. The canonical door, window and trim (and their items) default to {@link Appearance#DEFAULT};
 * the hidden 0.4.0 aliases default to their historical finish. Stored appearances are normalised
 * against this default, so default stacks stack together and nothing redundant is saved.
 */
public interface AppearanceDefaults {
    AppearanceFamily appearanceFamily();

    Appearance defaultAppearance();

    /** The default of {@code block}, or oak when it is not an appearance-bearing block. */
    static Appearance of(Block block) {
        return block instanceof AppearanceDefaults defaults ? defaults.defaultAppearance() : Appearance.DEFAULT;
    }

    /** The default of {@code item}, or oak when it is not an appearance-bearing item. */
    static Appearance of(Item item) {
        return item instanceof AppearanceDefaults defaults ? defaults.defaultAppearance() : Appearance.DEFAULT;
    }
}
