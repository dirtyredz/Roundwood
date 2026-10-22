package io.github.dirtyredz.roundwood.block.door.assembly;

import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
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
 * Finding a door's anchor entity from any part (extracted from {@code appearance/DoorAppearance},
 * 0.8.0, so the appearance and the trim attachments share it): the anchor cell must be loaded (never
 * loaded on demand) and hold a part of the same assembly. For loot, the anchor as found in the world,
 * else the entity captured with the broken part when that part was the anchor.
 */
public final class DoorBlockEntities {
    private DoorBlockEntities() {
    }

    /** The anchor entity of the assembly {@code pos} belongs to, if it is there. */
    public static Optional<RoundDoorBlockEntity> anchor(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AssemblyDoorBlock)) {
            return Optional.empty();
        }
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        BlockPos anchor = assembly.anchor();
        if (level instanceof Level live && !live.isLoaded(anchor)) {
            return Optional.empty();
        }
        BlockState anchorState = level.getBlockState(anchor);
        if (!(anchorState.getBlock() instanceof AssemblyDoorBlock) || !DoorLayout.of(anchor, anchorState).equals(assembly)) {
            return Optional.empty();
        }
        return level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }

    /** For loot from any part: the anchor as found in the world, else the captured entity if it is the anchor's. */
    public static Optional<RoundDoorBlockEntity> forLoot(LootParams.Builder builder, BlockState state) {
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin != null && state.getBlock() instanceof AssemblyDoorBlock) {
            Optional<RoundDoorBlockEntity> anchor = anchor(builder.getLevel(), BlockPos.containing(origin), state);
            if (anchor.isPresent()) {
                return anchor;
            }
        }
        BlockEntity captured = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return captured instanceof RoundDoorBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
