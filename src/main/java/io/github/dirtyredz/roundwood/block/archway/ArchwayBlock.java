package io.github.dirtyredz.roundwood.block.archway;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceUseTarget;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceDrops;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.archway.appearance.ArchwayAppearance;
import io.github.dirtyredz.roundwood.block.archway.appearance.ArchwayInteraction;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayShapes;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyHarvest;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyIntegrity;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyRemoval;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * One cell of a static 2 x 2 archway (0.12.0), ordinary or keyhole ({@link #design}): an open,
 * walk-through frame with no leaf, glass, swing, camouflage or redstone. Its identity is the
 * {@link #FACING} the placing player looked along plus which {@link #PART} it is. The anchor
 * (lower-left) cell carries the {@link ArchwayBlockEntity} holding the archway's look (a trim
 * appearance: wood, paint or one of the four masonry materials) and its two face trims, and the client
 * draws all four cells from it (the world model itself is empty). Each cell's shape is the frame in
 * that cell only ({@link ArchwayShapes}): rays hit the whole frame, and what collides is the lower jambs
 * alone (0.13.1), so the passage is open to players and mobs.
 * The callbacks hand off to the archway's assembly, appearance and trim services; placement only
 * ever happens whole, through {@link ArchwayItem}.
 */
public class ArchwayBlock extends Block implements EntityBlock, AppearanceDefaults, AppearanceUseTarget {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ArchwayPart> PART = EnumProperty.create("part", ArchwayPart.class);

    private final ArchwayDesign design;
    private final Supplier<BlockEntityType<ArchwayBlockEntity>> entityType;

    public ArchwayBlock(Properties properties, ArchwayDesign design, Supplier<BlockEntityType<ArchwayBlockEntity>> entityType) {
        super(properties);
        this.design = design;
        this.entityType = entityType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, ArchwayPart.LOWER_LEFT));
    }

    /** The archway's profile: ordinary or keyhole. */
    public ArchwayDesign design() {
        return design;
    }

    @Override
    public AppearanceFamily appearanceFamily() {
        return AppearanceFamily.TRIM;
    }

    @Override
    public Appearance defaultAppearance() {
        return Appearance.DEFAULT;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ArchwayPart.LOWER_LEFT ? new ArchwayBlockEntity(entityType.get(), pos, state) : null;
    }

    /** The sneak-use bridge lets through exactly the clicks the router handles ({@link ArchwayInteraction#claims}). */
    @Override
    public boolean claimsUse(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                             BlockHitResult hit) {
        return ArchwayInteraction.claims(state, level, pos, player, hand, hit);
    }

    /** Face trims and restyling ({@link ArchwayInteraction}); anything else is the item's own use. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ArchwayInteraction.use(this, state, level, pos, player, hand, hit);
    }

    /** A survival harvest keeps the cell until its loot has read the anchor ({@link AssemblyHarvest}). */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return AssemblyHarvest.destroyedByPlayer(this, state, level, pos, player, willHarvest,
                () -> super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
        AssemblyHarvest.playerDestroy(level, pos, () -> super.playerDestroy(level, player, pos, state, entity, tool));
    }

    /**
     * The ordinary loot (one archway per assembly) in the archway's look, plus one Universal Trim per
     * trimmed face in that face's own look. Whichever cell broke, the loot is evaluated with the
     * anchor's entity, so the trim pools see the faces. Only the archway item takes the archway's look:
     * the trims are the same family and keep theirs.
     */
    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        Appearance appearance = ArchwayAppearance.forDrops(builder, state);
        ArchwayAppearance.ANCHORS.forLoot(builder, state).ifPresent(anchor -> builder.withParameter(LootContextParams.BLOCK_ENTITY, anchor));
        return AppearanceDrops.decorate(super.getDrops(state, builder), asItem(), appearance);
    }

    /** Picking any cell gives the archway in its look. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return AppearanceStacks.create(asItem(), ArchwayAppearance.of(level, pos, state));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** Only the frame in this cell: rays pass through the passage. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ArchwayShapes.shape(design, state.getValue(FACING), state.getValue(PART));
    }

    /** The lower jambs collide; the crown is drawn and targeted but never collides, and the passage is open. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ArchwayShapes.collision(design, state.getValue(FACING), state.getValue(PART));
    }

    /** Archways are placed only whole ({@link ArchwayItem}); there is no single-cell placement. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    /** The lower cells stand on a sturdy floor; the upper ones on them. */
    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(PART).lower()) {
            return true;
        }
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /** Losing the floor under a lower cell breaks it (with its loot); its partners follow silently. */
    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (AssemblyMutationBatch.isMutating(level, pos)) {
            return state; // Roundwood is rewriting this archway (placement, teardown): no reactions
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
        AssemblyIntegrity.check(this, ArchwayLayout.of(pos, state), level, pos);
    }

    /** Removing one cell removes the archway; its loot (the archway and its trims) drops once ({@link AssemblyRemoval}). */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        AssemblyRemoval.onRemove(this, level, pos, newState, () -> ArchwayLayout.of(pos, state));
        super.onRemove(state, level, pos, newState, moving);
    }
}
