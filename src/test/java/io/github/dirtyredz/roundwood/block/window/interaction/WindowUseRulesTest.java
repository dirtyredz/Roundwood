package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.block.window.interaction.WindowHitRegions.Region;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.Action;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.Item;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The complete window routing table (0.8.0 spec section 5), cell by cell: every item on every region.
 * Written out in full so a changed rule is a visible diff here.
 */
class WindowUseRulesTest {
    @Test
    void theTableIsExactlyTheContract() {
        Map<Region, Action> trim = row(Action.FIT_TRIM, Action.FIT_TRIM, Action.FIT_TRIM, Action.FIT_TRIM, Action.PASS);
        Map<Region, Action> planks = row(Action.RESTYLE_TRIM, Action.RESTYLE_FRAME, Action.RESTYLE_FRAME, Action.RESTYLE_FRAME, Action.PASS);
        Map<Region, Action> dye = row(Action.RESTYLE_TRIM, Action.RESTYLE_FRAME, Action.TINT_GLASS, Action.TINT_GLASS, Action.PASS);
        Map<Region, Action> masonry = row(Action.RESTYLE_TRIM, Action.REFUSE_MASONRY, Action.REFUSE_MASONRY, Action.REFUSE_MASONRY,
                Action.REFUSE_MASONRY);
        Map<Region, Action> shears = row(Action.REMOVE_TRIM, Action.PASS, Action.PASS, Action.PASS, Action.PASS);
        Map<Region, Action> other = row(Action.PASS, Action.PASS, Action.PASS, Action.PASS, Action.PASS);
        Map<Item, Map<Region, Action>> table = new EnumMap<>(Item.class);
        table.put(Item.UNIVERSAL_TRIM, trim);
        table.put(Item.PLANKS, planks);
        table.put(Item.DYE, dye);
        table.put(Item.MASONRY, masonry);
        table.put(Item.SHEARS, shears);
        table.put(Item.OTHER, other);
        for (Item item : Item.values()) {
            for (Region region : Region.values()) {
                assertEquals(table.get(item).get(region), WindowUseRules.decide(item, region), item + " on " + region);
            }
        }
    }

    private static Map<Region, Action> row(Action trim, Action frame, Action glass, Action connector, Action none) {
        Map<Region, Action> row = new EnumMap<>(Region.class);
        row.put(Region.TRIM, trim);
        row.put(Region.FRAME, frame);
        row.put(Region.GLASS, glass);
        row.put(Region.CONNECTOR_GLASS, connector);
        row.put(Region.NONE, none);
        return row;
    }
}
