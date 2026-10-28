package io.github.dirtyredz.roundwood.client.material.tint;

import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import java.util.ArrayList;

/** No live BE reads. Resolve the donor at the chunk's owning position through BlockColors. */
public final class StaticBlockColors {
    private static final TintBindings<BlockState> BINDINGS = new TintBindings<>();
    private StaticBlockColors() {}
    @FunctionalInterface public interface Resolver<D, P> { int color(D donor, P pos, int originalIndex); }
    public static <D, P> int resolve(TintBindings.Binding<D> binding, P pos, Resolver<D, P> resolver) {
        return binding == null ? -1 : resolver.color(binding.donor(), pos, binding.index());
    }
    public static int bind(BlockState donor, int index) { return BINDINGS.bind(donor, index); }
    public static void register(RegisterColorHandlersEvent.Block event) {
        var blocks = new ArrayList<Block>(ModBlocks.doors());
        blocks.addAll(LegacyContent.doors());
        event.register((state, level, pos, index) -> resolve(BINDINGS.get(index), pos,
                (donor, sample, original) -> event.getBlockColors().getColor(donor, level, sample, original)), blocks.toArray(Block[]::new));
    }
}
