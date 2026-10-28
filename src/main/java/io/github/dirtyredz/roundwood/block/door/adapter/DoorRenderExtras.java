package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderExtras;
import io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

/** A door's static-render extras: its fitted arch custody and its camouflage donor. */
public record DoorRenderExtras(Optional<DoorArchAttachment> frame, Optional<BlockState> camouflage)
        implements AssemblyRenderExtras {
    public static final DoorRenderExtras NONE = new DoorRenderExtras(Optional.empty(), Optional.empty());

    /** {@code snapshot}'s door extras, or none when it carries another family's (or no) extras. */
    public static DoorRenderExtras of(AssemblyRenderSnapshot snapshot) {
        return snapshot.extras() instanceof DoorRenderExtras extras ? extras : NONE;
    }
}
