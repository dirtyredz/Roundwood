package io.github.dirtyredz.roundwood.client.item;

import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.archway.ArchwayItem;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem;
import io.github.dirtyredz.roundwood.client.arch.ArchPieceModels;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.item.TrimAttachmentItem;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.client.archway.ArchwayModels;
import io.github.dirtyredz.roundwood.client.door.DoorAppearanceModels;
import io.github.dirtyredz.roundwood.client.window.WindowAppearanceModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;

/** Typed item binding in precedence order: archway design, door design, window, then attachments. */
public final class FamilyItemModels {
    private FamilyItemModels() {
    }

    /** {@code model} wrapped for {@code item}'s kind, or {@code model} itself when the item has no appearance family. */
    public static BakedModel wrap(Item item, BakedModel model) {
        if (item instanceof ArchPieceItem piece) {
            return AppearanceItemOverrides.wrap(model, (oak, look) -> ArchPieceModels.item(piece.piece().design(), oak, look));
        }
        if (item instanceof ArchwayItem archway) {
            return AppearanceItemOverrides.wrap(model, (oak, look) -> ArchwayModels.item(archway.archway().design(), oak, look));
        }
        if (item instanceof RoundDoorItem door) {
            return AppearanceItemOverrides.wrap(model, (oak, look) -> DoorAppearanceModels.item(door.door().design(), oak, look));
        }
        AppearanceFamily family = item instanceof AppearanceDefaults defaults ? defaults.appearanceFamily() : null;
        if (family == AppearanceFamily.DOOR) {
            throw new IllegalArgumentException("Door appearance item has no door design: " + item);
        }
        if (family == AppearanceFamily.WINDOW) {
            return AppearanceItemOverrides.wrapStack(model, WindowAppearanceModel::item);
        }
        if (family == AppearanceFamily.TRIM) {
            TrimSlot slot = item instanceof TrimAttachmentItem attachment ? attachment.trimSlot() : TrimSlot.PERIMETER;
            return AppearanceItemOverrides.wrap(model, (oak, look) -> TrimItemModels.item(slot, oak, look));
        }
        return model;
    }

}
