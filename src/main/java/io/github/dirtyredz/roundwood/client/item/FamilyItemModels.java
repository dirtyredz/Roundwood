package io.github.dirtyredz.roundwood.client.item;

import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.item.TrimAttachmentItem;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.client.archway.ArchwayModels;
import io.github.dirtyredz.roundwood.client.door.DoorAppearanceModels;
import io.github.dirtyredz.roundwood.client.window.WindowAppearanceModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Which NBT-aware item model wraps each kind of appearance item. A table of (kind, wrapper) rows in
 * match order, so a new item kind adds a row instead of another branch in the client event handler:
 * archways first (they are trim-family items with their own design), then the families' defaults.
 */
public final class FamilyItemModels {
    private record Row(Predicate<Item> kind, BiFunction<Item, BakedModel, BakedModel> wrapper) {
    }

    private static final List<Row> ROWS = List.of(
            new Row(item -> item instanceof ArchwayItem, (item, model) -> {
                ArchwayItem archway = (ArchwayItem) item;
                return AppearanceItemOverrides.wrap(model, (oak, look) -> ArchwayModels.item(archway.archway().design(), oak, look));
            }),
            new Row(item -> family(item) == AppearanceFamily.DOOR, (item, model) -> {
                DoorDesign design = item instanceof BlockItem block && block.getBlock() instanceof AssemblyDoorBlock door
                        ? door.design() : DoorDesign.ROUND;
                return AppearanceItemOverrides.wrap(model, (oak, look) -> DoorAppearanceModels.item(design, oak, look));
            }),
            new Row(item -> family(item) == AppearanceFamily.WINDOW, (item, model) ->
                    AppearanceItemOverrides.wrapStack(model, WindowAppearanceModel::item)),
            new Row(item -> family(item) == AppearanceFamily.TRIM, (item, model) -> {
                TrimSlot slot = item instanceof TrimAttachmentItem attachment ? attachment.trimSlot() : TrimSlot.PERIMETER;
                return AppearanceItemOverrides.wrap(model, (oak, look) -> TrimItemModels.item(slot, oak, look));
            }));

    private FamilyItemModels() {
    }

    /** {@code model} wrapped for {@code item}'s kind, or {@code model} itself when the item has no appearance family. */
    public static BakedModel wrap(Item item, BakedModel model) {
        for (Row row : ROWS) {
            if (row.kind().test(item)) {
                return row.wrapper().apply(item, model);
            }
        }
        return model;
    }

    private static AppearanceFamily family(Item item) {
        return item instanceof AppearanceDefaults defaults ? defaults.appearanceFamily() : null;
    }
}
