package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Which items can camouflage a door, and as what. Only the appearance of an ordinary full cube is
 * copied: the donor block's default state, drawn with its own model's face textures. Nothing else
 * transfers (hardness, sounds, redstone, gravity, light). Checked on the server when a player
 * applies a material and again whenever saved data is loaded.
 */
public final class CamouflageMaterial {
    private CamouflageMaterial() {
    }

    /** The material {@code stack} would apply, or empty if it is not an acceptable donor. */
    public static Optional<BlockState> fromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && (tag.contains(BlockItem.BLOCK_ENTITY_TAG) || tag.contains(BlockItem.BLOCK_STATE_TAG))) {
            return Optional.empty(); // stored contents or a non-default state: never copied
        }
        BlockState state = item.getBlock().defaultBlockState();
        return isValid(state) ? Optional.of(state) : Optional.empty();
    }

    /**
     * A full, opaque, ordinary cube in its default state: full collision and occlusion, a normal
     * baked model, no fluid, no block entity and no light. Air and Roundwood doors are refused.
     */
    public static boolean isValid(BlockState state) {
        Block block = state.getBlock();
        if (state.isAir() || block instanceof RoundDoorBlock || state != block.defaultBlockState()) {
            return false;
        }
        BlockGetter level = EmptyBlockGetter.INSTANCE;
        BlockPos origin = BlockPos.ZERO;
        return state.getRenderShape() == RenderShape.MODEL
                && state.canOcclude()
                && state.isCollisionShapeFullBlock(level, origin)
                && Block.isShapeFullBlock(state.getOcclusionShape(level, origin))
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && state.getLightEmission(level, origin) == 0;
    }
}
