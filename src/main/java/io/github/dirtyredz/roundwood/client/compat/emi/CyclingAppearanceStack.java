package io.github.dirtyredz.roundwood.client.compat.emi;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceBlockItem;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One EMI index entry per canonical item whose icon cycles through sample appearances. Everything
 * EMI reasons with (identity, key, NBT, comparison, item retrieval, recipe lookup) is the default
 * oak stack's; only the drawing (and a tooltip naming the previewed look) changes over time, and
 * nothing cycling ever enters the index's identity or hashes. Always drawn unbatched, since a
 * batched icon would freeze on one look.
 */
public final class CyclingAppearanceStack extends ItemEmiStack {
    /** How long each sample shows. */
    static final long PERIOD_MS = 1200;

    private final ItemStack base;
    private final List<ItemStack> samples;
    private final List<ItemEmiStack> icons;

    /**
     * @param base    the default canonical stack (what EMI gives, compares and looks recipes up by)
     * @param samples the looks to cycle through, each the same item with an appearance
     */
    public CyclingAppearanceStack(ItemStack base, List<ItemStack> samples) {
        super(base.copyWithCount(1));
        this.base = base.copyWithCount(1);
        this.samples = List.copyOf(samples);
        this.icons = this.samples.stream().map(ItemEmiStack::new).toList();
    }

    /** The sample shown at {@code timeMillis}. */
    static int index(long timeMillis, int count) {
        return count == 0 ? 0 : (int) ((timeMillis / PERIOD_MS) % count);
    }

    private int current() {
        return index(System.currentTimeMillis(), icons.size());
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        if (icons.isEmpty()) {
            super.render(draw, x, y, delta, flags);
        } else {
            icons.get(current()).render(draw, x, y, delta, flags);
        }
    }

    @Override
    public boolean isUnbatchable() {
        return true;
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        List<ClientTooltipComponent> tooltip = new ArrayList<>(super.getTooltip());
        if (!samples.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.roundwood.preview").withStyle(ChatFormatting.GOLD));
            lines.addAll(AppearanceBlockItem.describe(AppearanceStacks.read(samples.get(current()))));
            lines.add(Component.translatable("tooltip.roundwood.preview_note").withStyle(ChatFormatting.GOLD));
            lines.forEach(line -> tooltip.add(ClientTooltipComponent.create(line.getVisualOrderText())));
        }
        return tooltip;
    }

    @Override
    public EmiStack copy() {
        CyclingAppearanceStack copy = new CyclingAppearanceStack(base, samples);
        copy.setAmount(getAmount());
        copy.setChance(getChance());
        copy.comparison = comparison;
        copy.setRemainder(getRemainder().copy());
        return copy;
    }

    /** The ordinary default stack, for serialisation. */
    public EmiStack plain() {
        return EmiStack.of(base, getAmount());
    }
}
