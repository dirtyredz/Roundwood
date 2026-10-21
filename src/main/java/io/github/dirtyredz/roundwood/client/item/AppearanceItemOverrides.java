package io.github.dirtyredz.roundwood.client.item;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * NBT-aware item models: a door, window or trim stack is drawn in its own look (a hidden 0.4.0 alias
 * in its historical one) by resolving, per stack, to a cached retextured copy of the family's oak
 * item model. Finite JSON predicates cannot name arbitrary modded woods, so this is Java. Most
 * families resolve by appearance alone ({@link #wrap}); windows also carry a glass colour, so they
 * resolve from the whole stack ({@link #wrapStack}, 0.8.0). The item model's transforms and GUI
 * lighting are the oak model's own, untouched.
 */
public final class AppearanceItemOverrides extends ItemOverrides {
    private final BakedModel oak;
    private final BiFunction<BakedModel, ItemStack, BakedModel> resolver;

    private AppearanceItemOverrides(BakedModel oak, BiFunction<BakedModel, ItemStack, BakedModel> resolver) {
        this.oak = oak;
        this.resolver = resolver;
    }

    /** {@code oak} with overrides that pick the stack's appearance through {@code resolver}. */
    public static BakedModel wrap(BakedModel oak, BiFunction<BakedModel, Appearance, BakedModel> resolver) {
        return wrapStack(oak, (model, stack) -> resolver.apply(model, AppearanceStacks.read(stack)));
    }

    /** {@code oak} with overrides that resolve each stack through {@code resolver}. */
    public static BakedModel wrapStack(BakedModel oak, BiFunction<BakedModel, ItemStack, BakedModel> resolver) {
        AppearanceItemOverrides overrides = new AppearanceItemOverrides(oak, resolver);
        return new BakedModelWrapper<>(oak) {
            @Override
            public ItemOverrides getOverrides() {
                return overrides;
            }
        };
    }

    @Override
    @Nullable
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity,
                              int seed) {
        return resolver.apply(oak, stack);
    }
}
