package io.github.dirtyredz.roundwood.block.window.pane;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.Optional;
import java.util.Map;
import net.minecraft.core.Direction;

/**
 * The immutable model data a window hands its chunk-baked model: the frame's appearance and the
 * glass colour (0.8.0; empty is clear), beams, perimeter and sill attachments. Holds
 * no Minecraft client classes, so it is safe on a dedicated server (the block entity builds it).
 */
public record WindowAppearanceData(Appearance appearance, Optional<DyeColor> glass, Optional<Appearance> beams,
                                   Map<Direction, WindowPaneState.Snapshot> neighbours,
                                   io.github.dirtyredz.roundwood.block.attachment.FaceTrims trims,
                                   io.github.dirtyredz.roundwood.block.attachment.FaceTrims sills) {
    public WindowAppearanceData {
        neighbours = Map.copyOf(neighbours);
    }
    public WindowAppearanceData(Appearance appearance, Optional<DyeColor> glass) {
        this(appearance, glass, Optional.empty(), Map.of(), io.github.dirtyredz.roundwood.block.attachment.FaceTrims.NONE,
                io.github.dirtyredz.roundwood.block.attachment.FaceTrims.NONE);
    }
    public static final ModelProperty<WindowAppearanceData> PROPERTY = new ModelProperty<>();
}
