package io.github.dirtyredz.roundwood.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.RenderHighlightEvent;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** Shared hand precedence and live-client guard; each preview keeps its own placement and drawing. */
public final class PlacementHighlights {
    private PlacementHighlights() {}

    @Nullable
    public static InteractionHand held(Player player, Predicate<ItemStack> placeable) {
        if (placeable.test(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (player.getMainHandItem().isEmpty() && placeable.test(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    @FunctionalInterface
    public interface Preview<T> {
        Optional<T> of(Player player, Level level, BlockHitResult hit);
    }

    public static <T> void draw(RenderHighlightEvent.Block event, Preview<T> preview, BiConsumer<Minecraft, T> draw) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            preview.of(client.player, client.level, event.getTarget()).ifPresent(value -> draw.accept(client, value));
        }
    }
}
