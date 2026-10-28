package io.github.dirtyredz.roundwood.client.render.world;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * One cell's static layers: a body model (or none, {@link #casings}), then one casing per trimmed face, in
 * {@link AttachmentFace} order. Families supply only the (cached) body and the face-to-casing baker;
 * the composition and its order are shared, so every family's chunk quads are assembled alike. Generic in
 * the model type only so the order is testable without a baked-model runtime.
 */
public final class CellLayers {
    private CellLayers() {}

    /** {@code body} followed by each face {@code trims} carries, baked by {@code casing}. */
    public static <M> List<M> of(M body, FaceTrims trims, BiFunction<AttachmentFace, Appearance, M> casing) {
        var models = new ArrayList<M>();
        models.add(body);
        addCasings(models, trims, casing);
        return models;
    }

    /** Casings only, for hosts whose body is drawn elsewhere (a door's animated leaf). */
    public static <M> List<M> casings(FaceTrims trims, BiFunction<AttachmentFace, Appearance, M> casing) {
        var models = new ArrayList<M>();
        addCasings(models, trims, casing);
        return models;
    }

    private static <M> void addCasings(List<M> models, FaceTrims trims, BiFunction<AttachmentFace, Appearance, M> casing) {
        for (var face : AttachmentFace.values()) trims.get(face).ifPresent(look -> models.add(casing.apply(face, look)));
    }
}
