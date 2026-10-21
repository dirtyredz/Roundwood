package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * The immutable model data a window hands its chunk-baked model: just its appearance. Holds no
 * Minecraft client classes, so it is safe on a dedicated server (the block entity builds it).
 */
public record WindowAppearanceData(Appearance appearance) {
    public static final ModelProperty<WindowAppearanceData> PROPERTY = new ModelProperty<>();
}
