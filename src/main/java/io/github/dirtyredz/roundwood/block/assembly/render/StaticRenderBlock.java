package io.github.dirtyredz.roundwood.block.assembly.render;

import io.github.dirtyredz.roundwood.block.assembly.Assembly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port a host block implements so its static geometry is drawn by the chunk model from a published
 * {@link AssemblyRenderSnapshot}. The shared publication reads layouts only through this, never by
 * naming a family. Server-safe.
 */
public interface StaticRenderBlock {
    /** The assembly {@code state} at {@code pos} belongs to for rendering, or null when it draws nothing. */
    @Nullable Assembly<?> renderAssembly(BlockPos pos, BlockState state);
}
