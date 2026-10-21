package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceDrops;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A single-block framed round window: a thin square wooden pane with round glazing. It stays in the
 * vanilla pane hierarchy ({@link IronBarsBlock}, the class glass panes use in 1.20.1), so panes,
 * bars, walls and other windows connect to it and it connects to them, and waterlogging, buckets,
 * fluid ticks and water left behind are inherited. It adds the plane orientation ({@link #AXIS},
 * facing the placing player), its own geometry ({@link WindowShapes}, models from datagen) and
 * stricter connections on its two faces ({@link WindowConnections}). Its wooden face's appearance
 * lives on a {@link RoundWindowBlockEntity}; glass and arms are never restyled.
 */
public class RoundWindowBlock extends IronBarsBlock implements EntityBlock, AppearanceDefaults {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private final Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType;

    public RoundWindowBlock(Properties properties, Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
        // Extend the inherited default (connections off, dry) rather than replacing it.
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public AppearanceFamily appearanceFamily() {
        return AppearanceFamily.WINDOW;
    }

    @Override
    public Appearance defaultAppearance() {
        return Appearance.DEFAULT;
    }

    /** The item picks and drops are made of (the hidden aliases name the canonical window's). */
    public Item familyItem() {
        return asItem();
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoundWindowBlockEntity(entityType.get(), pos, state);
    }

    /** Sneak + planks or dye restyles the wooden face; anything else (buckets included) is the item's own use. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Optional<AppearanceGestures.Gesture> gesture = AppearanceGestures.of(AppearanceFamily.WINDOW, player, hand);
        if (gesture.isEmpty()) {
            return InteractionResult.PASS;
        }
        return AppearanceActions.apply(level, player, gesture.get(),
                () -> level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? Optional.of(entity) : Optional.empty());
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        Appearance appearance = entity instanceof RoundWindowBlockEntity window ? window.appearance() : defaultAppearance();
        return AppearanceDrops.decorate(super.getDrops(state, builder), AppearanceFamily.WINDOW, appearance);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        Appearance appearance = level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window
                ? window.appearance() : defaultAppearance();
        return AppearanceStacks.create(familyItem(), appearance);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    /**
     * Vanilla pane connections and waterlogging, facing the placing player: the plane runs across
     * their line of sight. Across the plane only genuine pane junctions connect ({@link WindowConnections}).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Direction.Axis axis = context.getHorizontalDirection().getClockWise().getAxis();
        return WindowConnections.filterAll(state.setValue(AXIS, axis), context.getLevel(), context.getClickedPos());
    }

    /** Vanilla pane updates, then the front and back faces re-judged: a solid block there never makes an arm. */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return WindowConnections.filter(super.updateShape(state, direction, neighbor, level, pos, neighborPos), direction, neighbor);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.shape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.shape(state);
    }

    /** Connections turn with the block (inherited); a quarter turn also swaps the plane's axis. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            rotated = rotated.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return rotated;
    }

    /**
     * Vanilla panes hide every face towards a pane of the same block above or below. A window above
     * with the other axis covers only a 2 x 2 square of this one's top, so faces are hidden only
     * between windows in the same plane.
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        if (direction.getAxis().isVertical() && adjacent.is(this)) {
            return adjacent.getValue(AXIS) == state.getValue(AXIS);
        }
        return super.skipRendering(state, adjacent, direction);
    }
}
