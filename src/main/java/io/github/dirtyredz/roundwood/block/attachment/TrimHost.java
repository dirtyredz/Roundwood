package io.github.dirtyredz.roundwood.block.attachment;

import net.minecraft.core.BlockPos;

/**
 * The narrow writable view {@link TrimFitting} needs of one host's one slot (0.9.0): what is fitted,
 * whether it may change, and how a change is stored. Hosts build it only after their own checks
 * (complete and loaded assembly, build permission over every cell), so the transaction never knows
 * what kind of host it is editing.
 */
public interface TrimHost {
    TrimSlot slot();

    FaceTrims trims();

    /** False while the host holds a payload it does not understand. */
    boolean editable();

    /**
     * Stores {@code next}, saves and tells clients, and runs any host bookkeeping (a window's
     * {@code trimmed} summary). False when refused.
     */
    boolean store(FaceTrims next);

    /** Where the fit or removal is announced (sound and game event). */
    BlockPos origin();
}
