package io.github.dirtyredz.roundwood.variant;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Optional;

/**
 * One finish shared by the door, the window and the wooden frame of that finish.
 *
 * @param id           registry prefix, e.g. {@code dark_oak} or {@code green}
 * @param displayName  English name prefix
 * @param setType      vanilla wood set: sounds and hand-openability
 * @param baseDoor     vanilla door whose block properties (hardness, sound, flammability) are copied
 * @param planks       crafting material for unpainted finishes; oak for painted ones
 * @param panelTexture the window's wooden face (vanilla planks, or Roundwood's painted panel)
 * @param strippedTexture the wooden frame texture (the stripped log) for natural finishes
 * @param paint        the dye for painted finishes, empty for natural wood
 */
public record Finish(String id, String displayName, BlockSetType setType, Block baseDoor, Item planks,
                     ResourceLocation panelTexture, ResourceLocation strippedTexture, Optional<DyeColor> paint) {

    public boolean painted() {
        return paint.isPresent();
    }

    public String doorId() {
        return id + "_round_door";
    }

    public String windowId() {
        return id + "_round_window";
    }

    /** The door leaf's own vertical-board panel: one 32 x 32 image across the whole door (generated). */
    public ResourceLocation doorPanelTexture() {
        return ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "block/door_panel_" + id);
    }
}
