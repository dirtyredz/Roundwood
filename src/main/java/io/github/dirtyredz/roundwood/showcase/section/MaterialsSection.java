package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * The materials wall (0.13.1, replacing the 0.13.0 row of one archway per look): the actual restyling
 * source blocks, each labelled with a wall sign on its front face, for the player to take and use. Two
 * rows of blocks, so the wall is one sample with a clear walkway in front of it: the bottom row holds
 * every wood's planks (the eleven vanilla ones, then up to eight present modded planks), the row above
 * the four masonry blocks and then a barrel with one stack of each of the sixteen dyes (paint is a dye
 * on any look; the dyes are items, so they come in a barrel rather than as blocks). Occupies
 * {@code r 0..R_MAX, f 6..7}; the section's rectangle is checked to hold nothing else.
 */
public final class MaterialsSection implements ShowcaseSection {
    /** The row of the blocks; the signs hang on their front face one row nearer. */
    public static final int F = ShowcaseSpacing.ROW_WALL;
    public static final int R0 = ShowcaseSpacing.FIRST_R;
    /** The most woods the samples allow: eleven vanilla and eight modded. */
    public static final int MAX_WOODS = 19;
    public static final int WOOD_Y = 0;
    public static final int MASONRY_Y = 1;
    public static final int SURFACE_Y = 2;
    /** The last column the wall can use. */
    public static final int R_MAX = R0 + MAX_WOODS - 1;
    /** Dyes in the barrel: a full stack each. */
    public static final int DYE_STACK = 64;

    @Override
    public String id() {
        return "materials";
    }

    /** Frame {@code r} of the plank block of wood {@code index} (in {@link ShowcaseSamples#woods} order). */
    public static int woodR(int index) {
        return R0 + index;
    }

    /** Frame {@code r} of masonry block {@code index} (in {@link ShowcaseSamples#masonry} order). */
    public static int masonryR(int index) {
        return R0 + index;
    }

    /** Frame {@code r} of the dye barrel: the column after the last masonry block. */
    public static int barrelR(int masonryCount) {
        return R0 + masonryCount;
    }

    /** The block a sample's source item places: its planks or masonry block. */
    public static Block block(Appearance look) {
        return BuiltInRegistries.BLOCK.get(look.material());
    }

    /** The dyes of the paint samples, one stack each, in dye order. */
    public static List<ItemStack> dyes(List<Appearance> paints) {
        List<ItemStack> out = new ArrayList<>();
        for (Appearance paint : paints) {
            out.add(new ItemStack(DyeItem.byColor(paint.paint().orElseThrow()), DYE_STACK));
        }
        return out;
    }

    /** A sign's lines for {@code label}: one word per line, the first lines first, at most four. */
    public static String[] lines(String label) {
        String[] words = label.split(" ");
        if (words.length <= 4) {
            return words;
        }
        String[] out = new String[4];
        System.arraycopy(words, 0, out, 0, 3);
        out[3] = String.join(" ", List.of(words).subList(3, words.length));
        return out;
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        List<Appearance> samples = canvas.samples();
        List<Appearance> woods = ShowcaseSamples.woods(samples);
        List<Appearance> masonry = ShowcaseSamples.masonry(samples);
        if (woods.size() > MAX_WOODS) {
            canvas.problem("more woods than the materials wall has room for: " + woods.size());
            woods = woods.subList(0, MAX_WOODS);
        }
        for (int i = 0; i < woods.size(); i++) {
            canvas.set(woodR(i), WOOD_Y, F, block(woods.get(i)).defaultBlockState());
            canvas.wallSign(woodR(i), WOOD_Y, F, lines(ShowcaseSamples.label(woods.get(i))));
        }
        for (int i = 0; i < masonry.size(); i++) {
            canvas.set(masonryR(i), MASONRY_Y, F, block(masonry.get(i)).defaultBlockState());
            canvas.wallSign(masonryR(i), MASONRY_Y, F, lines(ShowcaseSamples.label(masonry.get(i))));
        }
        var surfaces=ShowcaseSamples.surfaces(samples);
        for(int i=0;i<surfaces.size();i++) {
            canvas.set(R0+i,SURFACE_Y,F,block(surfaces.get(i)).defaultBlockState());
            canvas.wallSign(R0+i,SURFACE_Y,F,lines(ShowcaseSamples.label(surfaces.get(i))));
        }
        canvas.barrel(barrelR(masonry.size()), MASONRY_Y, F, dyes(ShowcaseSamples.paints(samples)));
        canvas.wallSign(barrelR(masonry.size()), MASONRY_Y, F, "Dyes", "all sixteen", "paint sources");
    }
}
