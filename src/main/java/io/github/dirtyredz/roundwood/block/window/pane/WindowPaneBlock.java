package io.github.dirtyredz.roundwood.block.window.pane;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceUseTarget;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceDrops;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.connection.WindowConnections;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowShapes;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowInteraction;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPlacement;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import java.util.function.UnaryOperator;
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
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * What every single-cell window shares (the round window, the arches and the bottom corners, plain or
 * crossed): a thin wooden pane that stays in the vanilla pane hierarchy ({@link IronBarsBlock}, the
 * class glass panes use in 1.20.1), so panes, bars, walls and windows connect to it and it to them,
 * and waterlogging, buckets, fluid ticks and water left behind are inherited. Each subclass is an
 * adapter for one blockstate encoding of a {@link WindowPose}; everything else reads the pose:
 * placement from the targeted zone ({@link WindowPlacement}), connections ({@link WindowConnections}),
 * shapes and culling ({@link WindowShapes}), models (datagen). The design (profile and pattern) is
 * fixed per registration. The frame's appearance, the glass colour and the two face trims (0.8.0)
 * live on a {@link RoundWindowBlockEntity}; the trims are drawn by its renderer and may project one
 * pixel past the cell. Clicks route through {@link WindowInteraction}.
 */
public abstract class WindowPaneBlock extends IronBarsBlock implements EntityBlock, AppearanceDefaults, AppearanceUseTarget {
    /**
     * Either face has a window trim: a derived compatibility summary since 0.8.0 (the faces live on
     * the block entity, {@link RoundWindowBlockEntity#trims}). Absent in older saves, which load
     * untrimmed; a 0.7.0 window with it set and no payload loads with oak trims on both faces.
     */
    public static final BooleanProperty TRIMMED = BooleanProperty.create("trimmed");
    /**
     * The attachment FRONT slot faces away from the pose front (0.8.0, {@link WindowFaceIdentity}):
     * keeps each face trim on its world side through rotations and mirrors. Changes no geometry.
     */
    public static final BooleanProperty FACE_REVERSED = BooleanProperty.create("face_reversed");

    private final WindowDesign design;
    private final Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType;

    protected WindowPaneBlock(Properties properties, WindowDesign design,
                              Supplier<BlockEntityType<RoundWindowBlockEntity>> entityType) {
        super(properties);
        this.design = design;
        this.entityType = entityType;
        registerDefaultState(defaultBlockState().setValue(TRIMMED, false).setValue(FACE_REVERSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TRIMMED, FACE_REVERSED);
    }

    /** The face this block draws: profile and pattern. */
    public WindowDesign design() {
        return design;
    }

    public WindowProfile profile() {
        return design.profile();
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

    public static boolean trimmed(BlockState state) {
        return state.hasProperty(TRIMMED) && state.getValue(TRIMMED);
    }

    public static boolean faceReversed(BlockState state) {
        return state.hasProperty(FACE_REVERSED) && state.getValue(FACE_REVERSED);
    }

    /** The world side the attachment FRONT slot of a window state faces. */
    public Direction attachmentFront(BlockState state) {
        return WindowFaceIdentity.front(pose(state), faceReversed(state));
    }

    /**
     * {@code after} (a rotated or mirrored {@code before}) with its face identity recomputed so the
     * attachment FRONT stays on the world side {@code move} takes it to.
     */
    protected BlockState keepFaces(BlockState before, BlockState after, UnaryOperator<Direction> move) {
        Direction front = move.apply(attachmentFront(before));
        return after.setValue(FACE_REVERSED, WindowFaceIdentity.reversed(pose(after), front));
    }

    @Override
    public AppearanceFamily appearanceFamily() {
        return AppearanceFamily.WINDOW;
    }

    @Override
    public Appearance defaultAppearance() {
        return Appearance.DEFAULT;
    }

    /** The sneak-use bridge lets through exactly the clicks the router would handle ({@link WindowInteraction#claims}). */
    @Override
    public boolean claimsUse(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                             BlockHitResult hit) {
        return WindowInteraction.claims(state, level, pos, player, hand, hit);
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

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return WindowInteraction.use(state, level, pos, player, hand, hit);
    }

    /**
     * The window in its frame look and glass colour; each face trim drops through its own loot pool
     * (a styled window trim), never through here.
     */
    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        RoundWindowBlockEntity window = entity instanceof RoundWindowBlockEntity found ? found : null;
        Appearance appearance = window != null ? window.appearance() : defaultAppearance();
        List<ItemStack> drops = AppearanceDrops.decorate(super.getDrops(state, builder), AppearanceFamily.WINDOW, appearance);
        if (window != null) {
            drops.stream().filter(stack -> stack.is(familyItem())).forEach(stack -> WindowGlassStacks.write(stack, window.glass()));
        }
        return drops;
    }

    /** The window in its frame look and glass colour (never its trims). */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        RoundWindowBlockEntity window = level.getBlockEntity(pos) instanceof RoundWindowBlockEntity found ? found : null;
        ItemStack stack = AppearanceStacks.create(familyItem(), window != null ? window.appearance() : defaultAppearance());
        if (window != null) {
            WindowGlassStacks.write(stack, window.glass());
        }
        return stack;
    }

    /** A {@code trimmed} summary loaded out of step with the faces is corrected here ({@link RoundWindowBlockEntity#onLoad}). */
    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        WindowTrimActions.reconcile(level, pos);
    }

    /**
     * Vanilla pane connections and waterlogging, the pose from the targeted zone, then every side
     * re-judged for that pose ({@link WindowConnections}). A placed window has no trims.
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

    /** Outline and rays: the pane, its junction and the in-cell part of each fitted trim and sill. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.selection(design, pose(state), connections(state),
                attachedSides(state, level, pos, TrimSlot.PERIMETER), attachedSides(state, level, pos, TrimSlot.SILL));
    }

    /**
     * The world sides (along the normal) whose face carries an attachment in {@code slot}, read from
     * the entity when it is there (nothing while the {@code trimmed} summary says none).
     */
    public Set<Direction> attachedSides(BlockState state, BlockGetter level, BlockPos pos, TrimSlot slot) {
        if (!trimmed(state) || !(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window)) {
            return Set.of();
        }
        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        for (AttachmentFace face : AttachmentFace.values()) {
            if (window.trims(slot).has(face)) {
                sides.add(WindowFaceIdentity.side(pose(state), faceReversed(state), face));
            }
        }
        return sides;
    }

    /** The trim is decorative: only the pane and its junction collide. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WindowShapes.collision(design, pose(state), connections(state));
    }

    /**
     * A face on the cell boundary is hidden only when the neighbouring window's geometry covers
     * everything this one draws there (not because the blocks or their connections match, as vanilla
     * panes assume). Other neighbours never hide a window face.
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        if (adjacent.getBlock() instanceof WindowPaneBlock other) {
            return WindowShapes.hides(other.design(), other.pose(adjacent), connections(adjacent),
                    design, pose(state), connections(state), direction);
        }
        return false;
    }
}
