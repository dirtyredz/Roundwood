package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.leaf.DoorGeometry;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.window.WindowGeometry;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Inventory models for all 30 items: ordinary baked 3D models, no block entity or custom item
 * renderer. Doors show the whole closed leaf (left hinge) face-on; windows the whole square housing
 * with its round glazing. Each family has one template carrying the display transforms (front GUI
 * lighting, every transform computed so the model's centre lands where vanilla puts a block's);
 * each finish only binds textures.
 */
public class RoundwoodItemModelProvider extends ItemModelProvider {
    static final String DOOR_TEMPLATE = "template_round_door";
    static final String WINDOW_TEMPLATE = "template_round_window";

    /** Model-space centres in pixels: the disc's middle, and the window block's middle. */
    private static final Vector3f DOOR_CENTRE = new Vector3f(DoorGeometry.DIAMETER / 2F, DoorGeometry.DIAMETER / 2F,
            (float) DoorGeometry.PIVOT_Z);
    private static final Vector3f WINDOW_CENTRE = new Vector3f(WindowGeometry.SIZE / 2F, WindowGeometry.SIZE / 2F,
            WindowGeometry.SIZE / 2F);

    public RoundwoodItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood item models";
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder door = getBuilder(DOOR_TEMPLATE)
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + DoorModelProvider.LEAF_TEMPLATE
                        + DoorHingeSide.LEFT.getSerializedName())))
                .guiLight(BlockModel.GuiLight.FRONT);
        // The door is two blocks across, so it is drawn at half a block's scale throughout.
        display(door, ItemDisplayContext.GUI, 0, 0, 0, 0.5F, DOOR_CENTRE, 0, 0, 0);
        display(door, ItemDisplayContext.FIXED, 0, 180, 0, 0.375F, DOOR_CENTRE, 0, 0, 0);
        display(door, ItemDisplayContext.GROUND, 0, 0, 0, 0.25F, DOOR_CENTRE, 0, 3, 0);
        display(door, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 75, 45, 0, 0.1875F, DOOR_CENTRE, 0, 2.5F, 0);
        display(door, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0, 45, 0, 0.2F, DOOR_CENTRE, 0, 0, 0);

        ItemModelBuilder window = getBuilder(WINDOW_TEMPLATE)
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + WindowModelProvider.TEMPLATE)))
                .guiLight(BlockModel.GuiLight.FRONT);
        display(window, ItemDisplayContext.GUI, 0, 0, 0, 0.875F, WINDOW_CENTRE, 0, 0, 0);
        display(window, ItemDisplayContext.FIXED, 0, 180, 0, 0.75F, WINDOW_CENTRE, 0, 0, 0);
        display(window, ItemDisplayContext.GROUND, 0, 0, 0, 0.25F, WINDOW_CENTRE, 0, 3, 0);
        display(window, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 75, 45, 0, 0.375F, WINDOW_CENTRE, 0, 2.5F, 0);
        display(window, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0, 45, 0, 0.4F, WINDOW_CENTRE, 0, 0, 0);

        for (Finish finish : FinishCatalog.ALL) {
            getBuilder(finish.doorId()).parent(door)
                    .texture(DoorGeometry.PANEL, finish.panelTexture())
                    .texture(DoorGeometry.TRIM, finish.trimTexture())
                    .texture(DoorGeometry.BRASS, modLoc("block/brass"))
                    .texture("particle", finish.panelTexture());
            getBuilder(finish.windowId()).parent(window)
                    .texture(WindowGeometry.FRAME, finish.panelTexture())
                    .texture("particle", finish.panelTexture());
        }
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
