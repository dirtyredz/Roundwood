package io.github.dirtyredz.roundwood.block.archway.assembly;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Finding an archway's anchor entity from any cell (0.12.0), shared by its appearance, its face trims
 * and its loot: the anchor cell must be loaded (never loaded on demand) and hold a cell of the same
 * assembly. For loot, the anchor as found in the world, else the entity captured with the broken cell
 * when that cell was the anchor.
 */
public final class ArchwayBlockEntities {
    private ArchwayBlockEntities() {
    }

    /** The anchor entity of the archway {@code pos} belongs to, if it is there. */
    public static Optional<ArchwayBlockEntity> anchor(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ArchwayBlock)) {
            return Optional.empty();
        }
        ArchwayLayout.Assembly assembly = ArchwayLayout.of(pos, state);
        BlockPos anchor = assembly.anchor();
        if (level instanceof Level live && !live.isLoaded(anchor)) {
            return Optional.empty();
        }
        BlockState anchorState = level.getBlockState(anchor);
        if (!(anchorState.getBlock() instanceof ArchwayBlock) || !ArchwayLayout.of(anchor, anchorState).equals(assembly)) {
            return Optional.empty();
        }
        return level.getBlockEntity(anchor) instanceof ArchwayBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }

    /** For loot from any cell: the anchor as found in the world, else the captured entity if it is the anchor's. */
    public static Optional<ArchwayBlockEntity> forLoot(LootParams.Builder builder, BlockState state) {
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin != null && state.getBlock() instanceof ArchwayBlock) {
            Optional<ArchwayBlockEntity> anchor = anchor(builder.getLevel(), BlockPos.containing(origin), state);
            if (anchor.isPresent()) {
                return anchor;
            }
        }
        BlockEntity captured = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return captured instanceof ArchwayBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
