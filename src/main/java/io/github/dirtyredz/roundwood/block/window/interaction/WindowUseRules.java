package io.github.dirtyredz.roundwood.block.window.interaction;

/**
 * The complete window routing contract (0.8.0; the arch sill since 0.10.0) as a pure table: what an
 * item does on a hit region. "Attachment" is the perimeter trim on a trim band and the arch sill on a
 * sill; each is fitted, restyled and removed on its own. No action needs sneaking, and sneaking does
 * the same. Plain Java.
 *
 * <table>
 *     <tr><th>Item</th><th>Region</th><th>Action</th></tr>
 *     <tr><td>Universal Trim (any id)</td><td>any face target</td><td>fit the clicked face's perimeter (occupied: free no-op)</td></tr>
 *     <tr><td>arch sill</td><td>any face target of an upper arch</td><td>fit the clicked face's sill (occupied: free no-op)</td></tr>
 *     <tr><td>arch sill</td><td>any other window</td><td>refused with a hint, nothing used</td></tr>
 *     <tr><td>planks</td><td>trim band or sill</td><td>restyle that attachment (removes its paint)</td></tr>
 *     <tr><td>planks</td><td>frame, beam, glass</td><td>restyle the frame and beams; glass kept</td></tr>
 *     <tr><td>dye</td><td>trim band or sill</td><td>paint that attachment (masonry refuses, free)</td></tr>
 *     <tr><td>dye</td><td>frame or beam</td><td>paint the frame and beams</td></tr>
 *     <tr><td>dye</td><td>glass, connector glass</td><td>tint the whole window's glass</td></tr>
 *     <tr><td>masonry</td><td>trim band or sill</td><td>restyle that attachment</td></tr>
 *     <tr><td>masonry</td><td>elsewhere</td><td>refused, nothing used or placed</td></tr>
 *     <tr><td>shears</td><td>trim band or sill</td><td>remove that attachment only</td></tr>
 *     <tr><td>anything else</td><td>any</td><td>the item's ordinary use</td></tr>
 * </table>
 */
public final class WindowUseRules {
    /** What the used item is, as far as windows care. */
    public enum Item {
        UNIVERSAL_TRIM, ARCH_SILL, PLANKS, DYE, MASONRY, SHEARS, OTHER
    }

    public enum Action {
        FIT_TRIM, FIT_SILL, RESTYLE_TRIM, REMOVE_TRIM, RESTYLE_FRAME, TINT_GLASS, REFUSE_MASONRY, REFUSE_SILL,
        /** Not the window's business: the item's own use proceeds. */
        PASS
    }

    private WindowUseRules() {
    }

    /**
     * @param takesSill whether the clicked window's profile carries an arch sill
     */
    public static Action decide(Item item, WindowHitRegions.Region region, boolean takesSill) {
        if (region == WindowHitRegions.Region.NONE) {
            return item == Item.MASONRY ? Action.REFUSE_MASONRY : Action.PASS;
        }
        boolean attachment = region.slot() != null;
        boolean glass = region == WindowHitRegions.Region.GLASS || region == WindowHitRegions.Region.CONNECTOR_GLASS;
        return switch (item) {
            case UNIVERSAL_TRIM -> Action.FIT_TRIM;
            case ARCH_SILL -> takesSill ? Action.FIT_SILL : Action.REFUSE_SILL;
            case PLANKS -> attachment ? Action.RESTYLE_TRIM : Action.RESTYLE_FRAME;
            case DYE -> attachment ? Action.RESTYLE_TRIM : glass ? Action.TINT_GLASS : Action.RESTYLE_FRAME;
            case MASONRY -> attachment ? Action.RESTYLE_TRIM : Action.REFUSE_MASONRY;
            case SHEARS -> attachment ? Action.REMOVE_TRIM : Action.PASS;
            case OTHER -> Action.PASS;
        };
    }
}
