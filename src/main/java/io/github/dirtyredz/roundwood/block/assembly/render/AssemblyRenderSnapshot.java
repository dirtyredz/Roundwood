package io.github.dirtyredz.roundwood.block.assembly.render;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.Assembly;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.Optional;

/** Logical values only: no world, mutable store, NBT, refund stack or baked sprite. */
public record AssemblyRenderSnapshot(long revision, Assembly<?> assembly, Map<BlockPos, BlockState> members,
        boolean available, Appearance appearance, FaceTrims trims, Optional<DoorArchAttachment> frame,
        Optional<BlockState> camouflage) {
    public AssemblyRenderSnapshot { members = Map.copyOf(members); }
}
