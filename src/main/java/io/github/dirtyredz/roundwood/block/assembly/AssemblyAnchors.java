package io.github.dirtyredz.roundwood.block.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Finding an assembly's anchor entity from any of its parts, for every family. The anchor cell must be
 * loaded (never loaded on demand), hold a block of the same family ({@link Family#member}) and
 * reconstruct the same layout ({@link Family#layout}) as the part asked from: anchor identity, which
 * deliberately lets a canonical block and its 0.4.0 alias share one assembly, unlike exact part
 * membership ({@link Assembly#isPart}). For loot, the anchor as found in the world, else the entity
 * captured with the broken part (when it was the anchor's), as permissive as before.
 */
public final class AssemblyAnchors {
    /**
     * How a family recognises its parts and reconstructs a part's assembly from its own state.
     *
     * @param member true for a state of this family's block class (canonical or alias)
     * @param layout the assembly a member part at a position belongs to
     */
    public record Family<A extends Assembly<?>>(Predicate<BlockState> member, BiFunction<BlockPos, BlockState, A> layout) {
    }

    private AssemblyAnchors() {
    }

    /** The anchor entity of the assembly the part {@code state} at {@code pos} belongs to, if it is there. */
    public static <E extends BlockEntity> Optional<E> anchor(BlockGetter level, BlockPos pos, BlockState state, Family<?> family,
                                                             Class<E> type) {
        if (!family.member().test(state)) {
            return Optional.empty();
        }
        Assembly<?> assembly = family.layout().apply(pos, state);
        BlockPos anchor = assembly.anchor();
        if (level instanceof Level live && !live.isLoaded(anchor)) {
            return Optional.empty();
        }
        BlockState anchorState = level.getBlockState(anchor);
        if (!family.member().test(anchorState) || !family.layout().apply(anchor, anchorState).equals(assembly)) {
            return Optional.empty();
        }
        BlockEntity entity = level.getBlockEntity(anchor);
        return type.isInstance(entity) ? Optional.of(type.cast(entity)) : Optional.empty();
    }

    /** For loot from any part: the anchor as found in the world, else the captured entity if it is the right type. */
    public static <E extends BlockEntity> Optional<E> forLoot(LootParams.Builder builder, BlockState state, Family<?> family,
                                                              Class<E> type) {
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin != null && family.member().test(state)) {
            Optional<E> anchor = anchor(builder.getLevel(), BlockPos.containing(origin), state, family, type);
            if (anchor.isPresent()) {
                return anchor;
            }
        }
        BlockEntity captured = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return type.isInstance(captured) ? Optional.of(type.cast(captured)) : Optional.empty();
    }
}
