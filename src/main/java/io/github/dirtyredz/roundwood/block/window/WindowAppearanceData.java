package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.Optional;

/**
 * The immutable model data a window hands its chunk-baked model: the pane's appearance and, when a
 * window trim is fitted, the trim's own look. Holds no Minecraft client classes, so it is safe on a
 * dedicated server (the block entity builds it).
 */
public record WindowAppearanceData(Appearance appearance, Optional<Appearance> trim) {
    public static final ModelProperty<WindowAppearanceData> PROPERTY = new ModelProperty<>();
}
