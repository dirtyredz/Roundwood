package io.github.dirtyredz.roundwood.data.client;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Shared display transforms for Roundwood's inventory models (ordinary baked 3D models, no custom
 * item renderer): front GUI lighting, and every transform computed so the model's own centre lands
 * where vanilla puts a block's, whatever the model's size.
 */
final class ItemModelTransforms {
    private ItemModelTransforms() {
    }

    /** A two-block-wide assembly (door, frame): drawn at half a block's scale throughout. */
    static void assembly(ItemModelBuilder builder, Vector3f centre) {
        builder.guiLight(BlockModel.GuiLight.FRONT);
        display(builder, ItemDisplayContext.GUI, 0, 0, 0, 0.5F, centre, 0, 0, 0);
        display(builder, ItemDisplayContext.FIXED, 0, 180, 0, 0.375F, centre, 0, 0, 0);
        display(builder, ItemDisplayContext.GROUND, 0, 0, 0, 0.25F, centre, 0, 3, 0);
        display(builder, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 75, 45, 0, 0.1875F, centre, 0, 2.5F, 0);
        display(builder, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0, 45, 0, 0.2F, centre, 0, 0, 0);
    }

    /** A one-block model (window). */
    static void block(ItemModelBuilder builder, Vector3f centre) {
        builder.guiLight(BlockModel.GuiLight.FRONT);
        display(builder, ItemDisplayContext.GUI, 0, 0, 0, 0.875F, centre, 0, 0, 0);
        display(builder, ItemDisplayContext.FIXED, 0, 180, 0, 0.75F, centre, 0, 0, 0);
        display(builder, ItemDisplayContext.GROUND, 0, 0, 0, 0.25F, centre, 0, 3, 0);
        display(builder, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 75, 45, 0, 0.375F, centre, 0, 2.5F, 0);
        display(builder, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0, 45, 0, 0.4F, centre, 0, 0, 0);
    }

    /**
     * Adds one display transform whose translation puts {@code centre} (model pixels) at
     * ({@code x}, {@code y}, {@code z}) pixels from the slot/hand origin. Vanilla applies
     * translate, then rotate (XYZ), then scale, around the model's (8, 8, 8).
     */
    private static void display(ItemModelBuilder builder, ItemDisplayContext context, float rx, float ry, float rz,
                                float scale, Vector3f centre, float x, float y, float z) {
        Vector3f offset = new Vector3f(centre).sub(8, 8, 8).mul(scale);
        new Quaternionf().rotationXYZ((float) Math.toRadians(rx), (float) Math.toRadians(ry), (float) Math.toRadians(rz))
                .transform(offset);
        builder.transforms().transform(context)
                .rotation(rx, ry, rz)
                .translation(round(x - offset.x), round(y - offset.y), round(z - offset.z))
                .scale(scale)
                .end().end();
    }

    private static float round(float value) {
        return Math.round(value * 1000F) / 1000F;
    }
}
