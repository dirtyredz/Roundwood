package io.github.dirtyredz.roundwood.block.window.interaction;

import io.github.dirtyredz.roundwood.block.window.interaction.WindowHitRegions.Region;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.Action;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowUseRules.Item;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The complete window routing table (0.8.0 spec section 5; the arch sill since 0.10.0), cell by cell:
 * every item on every region, on a window that takes a sill and on one that does not. Written out in
 * full so a changed rule is a visible diff here.
 */
class WindowUseRulesTest {
    @Test
    void theTableIsExactlyTheContract() {
        for (boolean takesSill : new boolean[] {true, false}) {
            Action sillFit = takesSill ? Action.FIT_SILL : Action.REFUSE_SILL;
            Map<Item, Map<Region, Action>> table = new EnumMap<>(Item.class);
            table.put(Item.UNIVERSAL_TRIM, row(Action.FIT_TRIM, Action.FIT_TRIM, Action.FIT_TRIM, Action.FIT_TRIM, Action.FIT_TRIM,
                    Action.PASS));
            table.put(Item.ARCH_SILL, row(sillFit, sillFit, sillFit, sillFit, sillFit, Action.PASS));
            table.put(Item.PLANKS, row(Action.RESTYLE_TRIM, Action.RESTYLE_TRIM, Action.RESTYLE_FRAME, Action.RESTYLE_FRAME,
                    Action.RESTYLE_FRAME, Action.PASS));
            table.put(Item.DYE, row(Action.RESTYLE_TRIM, Action.RESTYLE_TRIM, Action.RESTYLE_FRAME, Action.TINT_GLASS,
                    Action.TINT_GLASS, Action.PASS));
            table.put(Item.MASONRY, row(Action.RESTYLE_TRIM, Action.RESTYLE_TRIM, Action.REFUSE_MASONRY, Action.REFUSE_MASONRY,
                    Action.REFUSE_MASONRY, Action.REFUSE_MASONRY));
            table.put(Item.SHEARS, row(Action.REMOVE_TRIM, Action.REMOVE_TRIM, Action.PASS, Action.PASS, Action.PASS, Action.PASS));
            table.put(Item.OTHER, row(Action.PASS, Action.PASS, Action.PASS, Action.PASS, Action.PASS, Action.PASS));
            for (Item item : Item.values()) {
                for (Region region : Region.values()) {
                    assertEquals(table.get(item).get(region), WindowUseRules.decide(item, region, takesSill),
                            item + " on " + region + (takesSill ? " (takes a sill)" : ""));
                }
            }
        }
    }

    private static Map<Region, Action> row(Action trim, Action sill, Action frame, Action glass, Action connector, Action none) {
        Map<Region, Action> row = new EnumMap<>(Region.class);
        row.put(Region.TRIM, trim);
        row.put(Region.SILL, sill);
        row.put(Region.FRAME, frame);
        row.put(Region.GLASS, glass);
        row.put(Region.CONNECTOR_GLASS, connector);
        row.put(Region.NONE, none);
        return row;
    }
}
