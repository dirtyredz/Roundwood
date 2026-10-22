package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimGeometry;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.client.render.PixelSlabModels;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

/**
 * Baked models of one cell's piece of a door casing in one look (0.11.0), built from the design's
 * casing outline ({@link DoorTrimGeometry#inCasing}) rather than from the legacy frame's quarter
 * models, for {@link DoorTrimRenderer}: the arched door's jambs and crown. Each piece is the
 * casing's visible surfaces inside the cell, its front and back included (the opening leaf exposes
 * the back), built by the shared {@link PixelSlabModels} (0.12.0; the archways use it too): wood
 * tiles like a block (its stripped log); paint and masonry spread one image over the whole door's
 * face (the board panel's left half for paint, the arched casing's own masonry course for masonry),
 * the edges granular. Cached per design, face, cell, facing and look; cleared with every appearance
 * cache on reload.
 */
public final class DoorTrimModels {
    private record Key(DoorDesign design, AttachmentFace face, DoorLayout.Part part, Direction facing, TrimLooks.Look look) {
    }

    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private DoorTrimModels() {
    }

    /** What {@code appearance} draws on {@code design}'s casing (masonry follows the casing's contour). */
    public static TrimLooks.Look look(DoorDesign design, Appearance appearance) {
        return TrimLooks.of(appearance, material -> design == DoorDesign.ROUND
                ? MasonryStyles.doorFace(material) : MasonryStyles.archedDoorCasingFace(material));
    }

    /** The piece of {@code face}'s casing in {@code part}'s cell of a door facing {@code facing}, in {@code look}. */
    public static BakedModel casing(DoorDesign design, AttachmentFace face, DoorLayout.Part part, Direction facing,
                                    TrimLooks.Look look) {
        return MODELS.get(new Key(design, face, part, facing, look), DoorTrimModels::build);
    }

    private static BakedModel build(Key key) {
        boolean front = key.face() == AttachmentFace.FRONT;
        double back = front ? DoorGeometry.FRONT : DoorGeometry.BACK - AttachmentFace.PROUD;
        double out = front ? DoorGeometry.FRONT + AttachmentFace.PROUD : DoorGeometry.BACK;
        return PixelSlabModels.cell((u, v) -> DoorTrimGeometry.inCasing(key.design(), u, v), key.part().column.index(),
                key.part().row(), back, out, key.facing(), key.look(),
                new PixelSlabModels.Extent(key.design().width(), DoorDesign.HEIGHT, DoorGeometry.DIAMETER));
    }
}
