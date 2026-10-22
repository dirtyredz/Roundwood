package io.github.dirtyredz.roundwood.block.attachment.host;

import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import net.minecraft.core.BlockPos;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The narrow writable view {@link TrimFitting} needs of one host's one slot (0.9.0): what is fitted,
 * whether it may change, and how a change is stored. Hosts build it only after their own checks
 * (complete and loaded assembly, build permission over every cell), so the transaction never knows
 * what kind of host it is editing. Hosts build it with {@link #of} (or {@link #anchored} for an anchor
 * entity's own store): a live view that asks its callbacks each time, so it always sees the host's
 * current state.
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

    /** Where the fit or removal is announced (sound and game event): the cell that was used. */
    BlockPos origin();

    /** A live view of {@code slot} over the host's current value, custody and store callbacks, announced at {@code origin}. */
    static TrimHost of(TrimSlot slot, Supplier<FaceTrims> trims, BooleanSupplier editable, Predicate<FaceTrims> store,
                       BlockPos origin) {
        return new TrimHost() {
            @Override
            public TrimSlot slot() {
                return slot;
            }

            @Override
            public FaceTrims trims() {
                return trims.get();
            }

            @Override
            public boolean editable() {
                return editable.getAsBoolean();
            }

            @Override
            public boolean store(FaceTrims next) {
                return store.test(next);
            }

            @Override
            public BlockPos origin() {
                return origin;
            }
        };
    }

    /** {@code anchor}'s own store for {@code slot}, with no host bookkeeping, announced at {@code origin}. */
    static TrimHost anchored(AppearanceAnchorBlockEntity anchor, TrimSlot slot, BlockPos origin) {
        return of(slot, () -> anchor.trims(slot), () -> anchor.trimsEditable(slot), next -> anchor.setTrims(slot, next), origin);
    }
}
