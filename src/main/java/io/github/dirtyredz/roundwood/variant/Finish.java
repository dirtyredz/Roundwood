package io.github.dirtyredz.roundwood.variant;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Optional;

/**
 * One finish shared by the door and the window of that finish.
 *
 * @param id           registry prefix, e.g. {@code dark_oak} or {@code green}
 * @param displayName  English name prefix
 * @param setType      vanilla wood set: sounds and hand-openability
 * @param baseDoor     vanilla door whose block properties (hardness, sound, flammability) are copied
 * @param planks       crafting material for unpainted finishes; oak for painted ones
 * @param panelTexture door panel and window rim texture
 * @param trimTexture  door perimeter trim texture
 * @param paint        the dye for painted finishes, empty for natural wood
 */
public record Finish(String id, String displayName, BlockSetType setType, Block baseDoor, Item planks,
                     ResourceLocation panelTexture, ResourceLocation trimTexture, Optional<DyeColor> paint) {

    public boolean painted() {
        return paint.isPresent();
    }

    public String doorId() {
        return id + "_round_door";
    }

    public String windowId() {
        return id + "_round_window";
    }
}
