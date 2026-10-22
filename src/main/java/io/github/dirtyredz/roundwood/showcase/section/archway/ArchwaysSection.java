package io.github.dirtyredz.roundwood.showcase.section.archway;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The two archways (0.12.0): a row of ordinary archways, then a row of keyhole archways behind it,
 * each with a sign in front: oak and three other woods, a paint, the four masonry materials, and the
 * Universal Trim's face casings (front only; different front and back). Walk through any of them.
 * The section lists every archway ({@link #archways}) so building and the GameTest check read the same
 * list. Occupies {@code r 26..58, f 35..41} (a free band between the doors, paints, trim combinations
 * and corners; the check verifies it holds nothing else, {@link #R0}..{@link #R1} x
 * {@link #F0}..{@link #F1}).
 */
public final class ArchwaysSection implements ShowcaseSection {
    public static final int R0 = 26;
    public static final int R1 = 58;
    public static final int F0 = 35;
    public static final int F1 = 41;
    private static final int PITCH = 3;
    /** The archway rows (their signs stand one block nearer the builder). */
    private static final int[] ROW_F = {37, 40};

    /** One exhibited archway: its anchor at ({@code r}, 0, {@code f}), design, look, face trims and sign text. */
    public record Exhibit(int r, int f, ArchwayDesign design, Appearance look, FaceTrims trims, String label) {
    }

    @Override
    public String id() {
        return "archways";
    }

    /** Every archway of the section, in building order. */
    public static List<Exhibit> archways() {
        Appearance oak = Appearance.DEFAULT;
        Appearance spruce = wood("spruce_planks");
        Appearance stoneBrick = masonry("stone_bricks");
        Appearance deepslate = masonry("deepslate_bricks");
        List<Appearance> looks = List.of(oak, spruce, wood("dark_oak_planks"), wood("cherry_planks"), oak.painted(DyeColor.BLUE),
                masonry("bricks"), stoneBrick, deepslate, masonry("mud_bricks"));
        List<String> labels = List.of("oak", "spruce", "dark oak", "cherry", "blue paint", "bricks", "stone bricks",
                "deepslate bricks", "mud bricks");
        List<Exhibit> out = new ArrayList<>();
        for (ArchwayDesign design : ArchwayDesign.values()) {
            int f = ROW_F[design.ordinal()];
            for (int i = 0; i < looks.size(); i++) {
                out.add(new Exhibit(r(i), f, design, looks.get(i), FaceTrims.NONE, labels.get(i)));
            }
            out.add(new Exhibit(r(looks.size()), f, design, oak, new FaceTrims(Optional.of(stoneBrick), Optional.empty()),
                    "front trim"));
            out.add(new Exhibit(r(looks.size() + 1), f, design, spruce,
                    new FaceTrims(Optional.of(deepslate), Optional.of(oak.painted(DyeColor.RED))), "two trims"));
        }
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        for (Exhibit archway : archways()) {
            canvas.sign(archway.r(), archway.f() - 1, archway.design() == ArchwayDesign.KEYHOLE ? "Keyhole archway" : "Archway",
                    archway.label());
            canvas.archway(ModBlocks.archway(archway.design()), archway.look(), archway.trims(), archway.r(), archway.f());
        }
    }

    private static int r(int index) {
        return R0 + PITCH * index;
    }

    private static Appearance wood(String block) {
        return Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", block));
    }

    private static Appearance masonry(String block) {
        return Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", block));
    }
}
