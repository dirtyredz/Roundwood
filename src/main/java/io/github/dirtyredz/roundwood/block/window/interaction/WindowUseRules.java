package io.github.dirtyredz.roundwood.block.window.interaction;

/**
 * The complete window routing contract (0.8.0) as a pure table: what an item does on a hit region.
 * No action needs sneaking, and sneaking does the same. Plain Java.
 *
 * <table>
 *     <tr><th>Item</th><th>Region</th><th>Action</th></tr>
 *     <tr><td>window trim</td><td>any face target</td><td>fit the clicked face (occupied: free no-op)</td></tr>
 *     <tr><td>planks</td><td>trim band</td><td>restyle that face's trim (removes its paint)</td></tr>
 *     <tr><td>planks</td><td>frame, beam, glass</td><td>restyle the frame and beams; glass kept</td></tr>
 *     <tr><td>dye</td><td>trim band</td><td>paint that face's trim (masonry refuses, free)</td></tr>
 *     <tr><td>dye</td><td>frame or beam</td><td>paint the frame and beams</td></tr>
 *     <tr><td>dye</td><td>glass, connector glass</td><td>tint the whole window's glass</td></tr>
 *     <tr><td>masonry</td><td>trim band</td><td>restyle that face's trim</td></tr>
 *     <tr><td>masonry</td><td>elsewhere</td><td>refused, nothing used or placed</td></tr>
 *     <tr><td>shears</td><td>trim band</td><td>remove that face's trim only</td></tr>
 *     <tr><td>anything else</td><td>any</td><td>the item's ordinary use</td></tr>
 * </table>
 */
public final class WindowUseRules {
    /** What the used item is, as far as windows care. */
    public enum Item {
        WINDOW_TRIM, PLANKS, DYE, MASONRY, SHEARS, OTHER
    }

    public enum Action {
        FIT_TRIM, RESTYLE_TRIM, REMOVE_TRIM, RESTYLE_FRAME, TINT_GLASS, REFUSE_MASONRY,
        /** Not the window's business: the item's own use proceeds. */
        PASS
    }

    private WindowUseRules() {
    }

    public static Action decide(Item item, WindowHitRegions.Region region) {
        if (region == WindowHitRegions.Region.NONE) {
            return item == Item.MASONRY ? Action.REFUSE_MASONRY : Action.PASS;
        }
        boolean trim = region == WindowHitRegions.Region.TRIM;
        boolean glass = region == WindowHitRegions.Region.GLASS || region == WindowHitRegions.Region.CONNECTOR_GLASS;
        return switch (item) {
            case WINDOW_TRIM -> Action.FIT_TRIM;
            case PLANKS -> trim ? Action.RESTYLE_TRIM : Action.RESTYLE_FRAME;
            case DYE -> trim ? Action.RESTYLE_TRIM : glass ? Action.TINT_GLASS : Action.RESTYLE_FRAME;
            case MASONRY -> trim ? Action.RESTYLE_TRIM : Action.REFUSE_MASONRY;
            case SHEARS -> trim ? Action.REMOVE_TRIM : Action.PASS;
            case OTHER -> Action.PASS;
        };
    }
}
