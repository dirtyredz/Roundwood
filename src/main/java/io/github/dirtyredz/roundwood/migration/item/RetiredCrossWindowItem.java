package io.github.dirtyredz.roundwood.migration.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowItem;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import java.util.Map;

/** Keeps its ID, count and arbitrary stack NBT until used. It never claims the plain block's item map. */
public final class RetiredCrossWindowItem extends RoundWindowItem {
    private final WindowDesign design;

    public RetiredCrossWindowItem(WindowDesign design, Properties properties) {
        super(ModBlocks.window(WindowDesign.plain(design.profile())), properties, Appearance.DEFAULT, design.id());
        this.design = design;
    }

    public WindowDesign design() { return design; }

    @Override public void registerBlocks(Map<Block, Item> map, Item item) { map.put(ModBlocks.window(design), item); }
    @Override public void removeFromBlockToItemMap(Map<Block, Item> map, Item item) { map.remove(ModBlocks.window(design), item); }
    @Override public InteractionResult place(BlockPlaceContext context) { return CrossWindowPlacement.place(this, context); }
}
