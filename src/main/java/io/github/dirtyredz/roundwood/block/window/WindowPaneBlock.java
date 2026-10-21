package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceDrops;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.connection.WindowConnections;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowShapes;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPlacement;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * What every single-cell window shares (the round window and both arch pieces): a thin wooden pane
 * that stays in the vanilla pane hierarchy ({@link IronBarsBlock}, the class glass panes use in
 * 1.20.1), so panes, bars, walls and windows connect to it and it to them, and waterlogging, buckets,
 * fluid ticks and water left behind are inherited. Each subclass is an adapter for one blockstate
 * encoding of a {@link WindowPose}; everything else reads the pose: placement from the targeted zone
 * ({@link WindowPlacement}), connections ({@link WindowConnections}), shapes and culling
 * ({@link WindowShapes}), models (datagen). The wooden face's appearance lives on a
 * {@link RoundWindowBlockEntity}; glass and junctions are never restyled.
 */
public abstract class WindowPaneBlock extends IronBarsBlock implements EntityBlock, AppearanceDefaults {
    private final WindowProfile profile;
    private final Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType;

    protected WindowPaneBlock(Properties properties, WindowProfile profile,
                              Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType) {
        super(properties);
        this.profile = profile;
        this.entityType = entityType;
    }

    /** The face this block draws. */
    public WindowProfile profile() {
        return profile;
    }

    /** How a state of this block sits in its cell. */
    public abstract WindowPose pose(BlockState state);

    /** {@code state} placed as {@code request} says (connections are judged afterwards). */
    protected abstract BlockState placed(BlockState state, WindowPlacement.Request request);

    /** The horizontal sides a state is connected on. */
    public static Set<Direction> connections(BlockState state) {
        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
                sides.add(side);
            }
        }
        return sides;
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

    /**
     * Vanilla pane connections and waterlogging, the pose from the targeted zone, then every side
     * re-judged for that pose ({@link WindowConnections}).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return WindowConnections.filterAll(placed(state, WindowPlacement.of(context)), context.getLevel(), context.getClickedPos());
    }

    /** Vanilla pane updates (water ticks included), then the side re-judged for this pose. */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return WindowConnections.filter(super.updateShape(state, direction, neighbor, level, pos, neighborPos), direction, neighbor);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.shape(profile, pose(state), connections(state));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.shape(profile, pose(state), connections(state));
    }

    /**
     * A face on the cell boundary is hidden only when the neighbouring window's geometry covers
     * everything this one draws there (not because the blocks or their connections match, as vanilla
     * panes assume). Other neighbours never hide a window face.
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        if (adjacent.getBlock() instanceof WindowPaneBlock other) {
            return WindowShapes.hides(other.profile(), other.pose(adjacent), connections(adjacent),
                    profile, pose(state), connections(state), direction);
        }
        return false;
    }
}
