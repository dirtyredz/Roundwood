package io.github.dirtyredz.roundwood.variant;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * One material for the round frames. Wooden frames come from the finishes (natural woods show their
 * stripped log, tiled like a block; painted ones the painted door panel, spread across the ring);
 * masonry frames are declared alongside them in {@link FinishCatalog} and show generated radial
 * brickwork spread across the ring.
 *
 * @param id          registry prefix, e.g. {@code oak} or {@code stone_brick}
 * @param displayName English name prefix
 * @param base        vanilla block whose properties (hardness, sound, tool, map colour) are copied
 * @param material    the crafting material (four in the grid corners): planks or brick blocks
 * @param faceTexture the ring's front and back
 * @param edgeTexture the ring's sides
 * @param wholeFace   true: one image across the whole ring ({@code AssemblyFaceUv}); false: tiled per block
 * @param masonry     brick frames (pickaxe) rather than wood (axe)
 * @param paint       the dye for painted frames, empty otherwise
 */
public record TrimMaterial(String id, String displayName, Block base, Item material, ResourceLocation faceTexture,
                           ResourceLocation edgeTexture, boolean wholeFace, boolean masonry, Optional<DyeColor> paint) {

    public String trimId() {
        return id + "_round_trim";
    }

    public boolean painted() {
        return paint.isPresent();
    }

    /** A wooden frame for a finish: natural woods in their stripped log, painted ones in their door panel. */
    static TrimMaterial wood(Finish finish) {
        Block planks = Block.byItem(finish.planks());
        if (finish.painted()) {
            return new TrimMaterial(finish.id(), finish.displayName(), planks, finish.planks(), finish.doorPanelTexture(),
                    finish.doorPanelTexture(), true, false, finish.paint());
        }
        return new TrimMaterial(finish.id(), finish.displayName(), planks, finish.planks(), finish.strippedTexture(),
                finish.strippedTexture(), false, false, Optional.empty());
    }

    /** A masonry frame: generated radial brick face and granular sides. */
    static TrimMaterial masonry(String id, String displayName, Block bricks) {
        return new TrimMaterial(id, displayName, bricks, bricks.asItem(), texture("trim_radial_" + id),
                texture("trim_edge_" + id), true, true, Optional.empty());
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/" + name);
    }
}
