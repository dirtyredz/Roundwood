package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceDrops;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyHarvest;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyIntegrity;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyRemoval;
import io.github.dirtyredz.roundwood.block.trim.appearance.TrimAppearance;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.block.trim.geometry.TrimShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * One quarter of a round frame: a purely decorative ring piece (no collision, no occlusion, no
 * redstone). Its identity is the {@link #FACING} the placing player looked along plus which
 * {@link #PART} it is. The anchor (lower-left) quarter carries the {@link RoundTrimBlockEntity}
 * that holds the frame's appearance and that the client draws all four quarters from (the world
 * model itself is empty). The callbacks hand off to the frame's assembly services; placement only
 * ever happens whole, through {@code assembly/TrimPlacement} (0.8.0: frames are compatibility content
 * from older saves; the {@code round_trim} item now attaches to doors and never places one).
 */
public class RoundTrimBlock extends Block implements EntityBlock, AppearanceDefaults {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<TrimPart> PART = EnumProperty.create("part", TrimPart.class);

    private final Supplier<BlockEntityType<RoundTrimBlockEntity>> entityType;

    public RoundTrimBlock(Properties properties, Supplier<BlockEntityType<RoundTrimBlockEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, TrimPart.LOWER_LEFT));
    }

    @Override
    public AppearanceFamily appearanceFamily() {
        return AppearanceFamily.TRIM;
    }

    @Override
    public Appearance defaultAppearance() {
        return Appearance.DEFAULT;
    }

    /** The item picks and drops are made of (the hidden aliases name the canonical trim's). */
    public Item familyItem() {
        return asItem();
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == TrimPart.LOWER_LEFT ? new RoundTrimBlockEntity(entityType.get(), pos, state) : null;
    }

    /** Sneak + planks or masonry, or dye, restyles the whole frame; everything else is the item's own use. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Optional<AppearanceGestures.Gesture> gesture = AppearanceGestures.of(AppearanceFamily.TRIM, player, hand);
        if (gesture.isEmpty()) {
            return InteractionResult.PASS;
        }
        return AppearanceActions.apply(level, player, gesture.get(), () -> TrimAppearance.target(this, state, level, pos));
    }

    /** A survival harvest keeps the quarter until its loot has read the anchor ({@link AssemblyHarvest}). */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return AssemblyHarvest.destroyedByPlayer(this, state, level, pos, player, willHarvest,
                () -> super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
        AssemblyHarvest.playerDestroy(level, pos, () -> super.playerDestroy(level, player, pos, state, entity, tool));
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return AppearanceDrops.decorate(super.getDrops(state, builder), AppearanceFamily.TRIM, TrimAppearance.forDrops(builder, state));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return AppearanceStacks.create(familyItem(), TrimAppearance.of(level, pos, state));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** Only the ring piece in this cell is targetable: rays pass through the open centre. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TrimShapes.shape(state.getValue(FACING), state.getValue(PART));
    }

    /** Decorative: nothing collides, stands on it or is pathed around it. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Frames are placed only whole ({@code assembly/TrimPlacement}); there is no single-cell placement. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    /** The lower quarters stand on a sturdy floor; the upper ones on them. */
    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(PART).lower()) {
            return true;
        }
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /** Losing the floor under a lower quarter breaks it (with its loot); its partners follow silently. */
    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (AssemblyMutationBatch.isMutating(level, pos)) {
            return state; // Roundwood is rewriting this frame (placement, migration): no reactions
        }
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /** Mirroring reverses the facing and swaps which column is on the left. */
    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(PART, state.getValue(PART).mirrored());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        AssemblyIntegrity.scheduleCheck(this, oldState, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AssemblyIntegrity.check(this, TrimLayout.of(pos, state), level, pos);
    }

    /** Removing one quarter removes the frame; a door it was fitted to is another assembly ({@link AssemblyRemoval}). */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        AssemblyRemoval.onRemove(this, level, pos, newState, () -> TrimLayout.of(pos, state));
        super.onRemove(state, level, pos, newState, moving);
    }
}
