package io.github.dirtyredz.roundwood.block.door;

import io.github.dirtyredz.roundwood.client.door.DoorParticles;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorColumn;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorIntegrity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorRemoval;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorStateController;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageDrops;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageInteraction;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorShapes;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One quarter of a round door. Stays a {@link DoorBlock} (same properties, wooden-door tags, hand
 * opening) so villager AI, pathfinding and other mods see an ordinary wooden door; it adds only the
 * {@link #COLUMN} identity. The callbacks here hand off to the assembly, selection and camouflage
 * services; the lower-left part carries the {@link RoundDoorBlockEntity} the client draws the leaf
 * and the camouflage from.
 */
public class RoundDoorBlock extends DoorBlock implements EntityBlock {
    public static final EnumProperty<DoorColumn> COLUMN = EnumProperty.create("column", DoorColumn.class);

    private final Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType;

    public RoundDoorBlock(Properties properties, BlockSetType type, Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType) {
        super(properties, type);
        this.entityType = entityType;
        // Extend the inherited default (never stateDefinition.any()): other mods inject properties into DoorBlock.
        registerDefaultState(defaultBlockState().setValue(COLUMN, DoorColumn.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLUMN);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return DoorLayout.partOf(state) == DoorLayout.Part.LOWER_LEFT
                ? new RoundDoorBlockEntity(entityType.get(), pos, state) : null;
    }

    /**
     * The target shape rays hit: the whole assembly, from every part (corners included when
     * targetable). The client highlights {@link DoorSelectionShapes#visible} instead.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return DoorSelectionShapes.target(state, level, pos, context);
    }

    /** Collision stays this cell's own clipped piece of the leaf; support and visual shapes follow it. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return DoorShapes.shape(state);
    }

    /** Break and hit particles from this cell's own piece, not from the whole-door outline. */
    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(DoorParticles.INSTANCE); // only ever called on the client
    }

    /** Assemblies are placed only through {@link RoundDoorItem}; there is no single-cell placement. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    /** The item writes all four cells itself; the vanilla upper-half write must not run. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult camouflage = CamouflageInteraction.use(this, state, level, pos, player, hand, hit);
        return camouflage != null ? camouflage : DoorStateController.use(this, state, level, pos, player);
    }

    /** A creative player's break discards stored camouflage instead of dropping it. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player.getAbilities().instabuild) {
            return CamouflageDrops.discardingRefunds(level, () -> super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid));
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
        DoorStateController.setOpen(this, state, level, pos, open, entity);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        DoorStateController.neighborChanged(this, state, level, pos, source);
    }

    /**
     * Vanilla re-derives a door half from the half above/below it. Here the vertical partner must
     * be the same column of the same assembly, or this part goes (with its normal loot).
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (direction.getAxis() == Direction.Axis.Y && lower == (direction == Direction.UP) && !verticalPartner(state, neighbor)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Mirroring swaps which column is on the left as well as the physical hinge (vanilla cycles the hinge). */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : mirrored.cycle(COLUMN);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        DoorStateController.reconcileExternalOpen(this, oldState, state, level, pos);
        DoorIntegrity.scheduleCheck(this, oldState, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        DoorIntegrity.check(this, state, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        DoorRemoval.onRemove(this, state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moving);
    }

    private boolean verticalPartner(BlockState state, BlockState neighbor) {
        return neighbor.is(this)
                && neighbor.getValue(HALF) != state.getValue(HALF)
                && neighbor.getValue(FACING) == state.getValue(FACING)
                && neighbor.getValue(HINGE) == state.getValue(HINGE)
                && neighbor.getValue(COLUMN) == state.getValue(COLUMN);
    }
}
