package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassNbt;
import net.minecraft.world.item.DyeColor;
import java.util.Optional;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Retained registry identities, never inventory rewrites. Frozen 0.4 aliases are a separate table. */
public final class CrossWindowCompatibility {
    public static final List<String> IDS = List.of("round_window_cross", "round_window_arch_left_cross",
            "round_window_arch_right_cross", "round_window_bottom_left_cross", "round_window_bottom_right_cross",
            "round_window_arch_bottom_left_cross", "round_window_arch_bottom_right_cross", "round_window_top_left_cross",
            "round_window_top_right_cross", "round_window_edge_top_cross", "round_window_edge_left_cross",
            "round_window_edge_bottom_cross", "round_window_edge_right_cross");

    private CrossWindowCompatibility() { }

    public static boolean isRetired(BlockState state) {
        return state.getBlock() instanceof WindowPaneBlock window && window.design().pattern() == WindowPattern.CROSS;
    }

    public static List<WindowPaneBlock> blocks() {
        return WindowDesign.all().stream().filter(d -> d.pattern() == WindowPattern.CROSS).map(ModBlocks::window).toList();
    }

    public static List<Item> items() {
        return WindowDesign.all().stream().filter(d -> d.pattern() == WindowPattern.CROSS).map(ModItems::window).map(Item.class::cast).toList();
    }

    /** Historical stack contents may live in BlockEntityTag; explicit stack values take precedence. */
    public static Appearance appearance(ItemStack stack) {
        return AppearanceNbt.read(stack.getTag()).orElseGet(() -> AppearanceNbt.read(stack.getTagElement("BlockEntityTag"))
                .orElse(AppearanceStacks.read(stack)));
    }

    public static Optional<DyeColor> glass(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(WindowGlassNbt.TAG)
                ? WindowGlassStacks.read(stack) : WindowGlassNbt.read(stack.getTagElement("BlockEntityTag"));
    }

    /** Pending blocks yield one lossless hidden stack; no separate attachment refunds. */
    public static ItemStack stack(WindowPaneBlock block, @Nullable RoundWindowBlockEntity entity) {
        var stack = AppearanceStacks.create(ModItems.window(block.design()), entity == null ? block.defaultAppearance() : entity.appearance());
        if (entity != null) {
            stack.getOrCreateTag().put("BlockEntityTag", entity.saveWithFullMetadata());
            WindowGlassStacks.write(stack, entity.glass());
        }
        return stack;
    }
}
