package io.github.dirtyredz.roundwood.client.archway;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimGeometry;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.client.material.AppearanceModelCache;
import io.github.dirtyredz.roundwood.client.material.MasonryStyles;
import io.github.dirtyredz.roundwood.client.material.TrimLooks;
import io.github.dirtyredz.roundwood.client.render.PixelSlabModels;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

/**
 * Baked archway models in one look (0.12.0), all built from the design's frame mask
 * ({@link ArchwayDesign#inFrame}) by {@link PixelSlabModels}, so what is drawn is what is hit
 * (collision keeps only the lower jambs):
 * <ul>
 *     <li>{@link #body}: one cell's piece of the frame, full block deep, for {@link ArchwayRenderer};</li>
 *     <li>{@link #casing}: one cell's piece of a face's Universal Trim casing, one pixel proud;</li>
 *     <li>{@link #item}: the whole archway, face-on, for a styled stack (the oak stack keeps the generated
 *     item model, whose transforms and lighting the styled one copies).</li>
 * </ul>
 * Looks are trim looks ({@link TrimLooks}): wood shows its stripped log tiled like a block; paint the
 * painted boards and masonry the archway's own contour artwork ({@link MasonryStyles#archwayFace}),
 * each spread once over the 32 x 32 face, the back mirrored; edges tiled (masonry: granular). Cached
 * by design, layer, cell, facing and look, never by position; cleared with every appearance cache.
 */
public final class ArchwayModels {
    private static final PixelSlabModels.Extent EXTENT = new PixelSlabModels.Extent(ArchwayDesign.SIZE, ArchwayDesign.SIZE,
            ArchwayDesign.SIZE);

    /** What a model shows: the frame, a face's casing, or the whole item. */
    private enum Layer {
        BODY, FRONT, BACK, ITEM
    }

    private record Key(ArchwayDesign design, Layer layer, ArchwayPart part, Direction facing, TrimLooks.Look look) {
    }

    private static final AppearanceModelCache<Key, BakedModel> MODELS = new AppearanceModelCache<>(AppearanceModelCache.MODELS);

    private ArchwayModels() {
    }

    /** What {@code appearance} draws on {@code design} (masonry follows its contour). */
    public static TrimLooks.Look look(ArchwayDesign design, Appearance appearance) {
        return TrimLooks.of(appearance, material -> MasonryStyles.archwayFace(material, design));
    }

    /** The frame in {@code part}'s cell of an archway facing {@code facing}. */
    public static BakedModel body(ArchwayDesign design, ArchwayPart part, Direction facing, TrimLooks.Look look) {
        return MODELS.get(new Key(design, Layer.BODY, part, facing, look), key -> PixelSlabModels.cell(design::inFrame,
                part.column, part.row, ArchwayDesign.BACK, ArchwayDesign.FRONT, facing, look, EXTENT));
    }

    /** {@code face}'s casing in {@code part}'s cell of an archway facing {@code facing}. */
    public static BakedModel casing(ArchwayDesign design, AttachmentFace face, ArchwayPart part, Direction facing,
                                    TrimLooks.Look look) {
        Layer layer = face == AttachmentFace.FRONT ? Layer.FRONT : Layer.BACK;
        double[] depth = ArchwayTrimGeometry.depth(face);
        return MODELS.get(new Key(design, layer, part, facing, look), key -> PixelSlabModels.cell(
                (u, v) -> ArchwayTrimGeometry.inCasing(design, u, v), part.column, part.row, depth[0], depth[1], facing, look,
                EXTENT));
    }

    /** {@code oakItem} ({@code design}'s generated item model) in {@code appearance}. */
    public static BakedModel item(ArchwayDesign design, BakedModel oakItem, Appearance appearance) {
        TrimLooks.Look look = look(design, appearance);
        if (look.equals(TrimLooks.OAK)) {
            return oakItem;
        }
        return MODELS.get(new Key(design, Layer.ITEM, ArchwayPart.LOWER_LEFT, Direction.NORTH, look), key -> PixelSlabModels.whole(
                design::inFrame, 2, 2, ArchwayDesign.BACK, ArchwayDesign.FRONT, look, EXTENT, oakItem));
    }
}
