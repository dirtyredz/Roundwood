package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceUseTarget;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceDrops;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyHarvest;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyIntegrity;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyRemoval;
import io.github.dirtyredz.roundwood.block.door.appearance.DoorAppearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorStateController;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageDrops;
import io.github.dirtyredz.roundwood.block.door.interaction.DoorInteraction;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
 * One part of a Roundwood door assembly, whatever its design (extracted from {@link RoundDoorBlock}
 * in 0.11.0, when the arched door joined it). Stays a {@link DoorBlock} (same properties,
 * wooden-door tags, hand opening) so villager AI, pathfinding and other mods see an ordinary wooden
 * door. The callbacks hand off to the assembly, selection and camouflage services, which read the
 * design ({@link #design}) and the membership ({@link DoorLayout}); the lower-left part carries the
 * {@link RoundDoorBlockEntity} the client draws the leaf, camouflage and casings from, and which
 * holds the door's appearance (picked and dropped with the item). Subclasses add only the design
 * and any state identity of their own (the round door's {@code column}).
 */
public abstract class AssemblyDoorBlock extends DoorBlock implements EntityBlock, AppearanceDefaults, AppearanceUseTarget, io.github.dirtyredz.roundwood.block.assembly.render.StaticRenderBlock {
    @Override public DoorLayout.Assembly renderAssembly(BlockPos pos, BlockState state) { return DoorLayout.of(pos, state); }

    private final Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType;

    protected AssemblyDoorBlock(Properties properties, BlockSetType type, Supplier<BlockEntityType<RoundDoorBlockEntity>> entityType) {
        super(type, properties);
        this.entityType = entityType;
    }

    /** The leaf's design: its outline, the cells it owns and its hardware. */
    public abstract DoorDesign design();

    @Override
    public AppearanceFamily appearanceFamily() {
        return AppearanceFamily.DOOR;
    }

    /** Oak, unpainted; the hidden 0.4.0 aliases override this with their historical finish. */
    @Override
    public Appearance defaultAppearance() {
        return Appearance.DEFAULT;
    }

    /** The item picks and drops are made of: this block's own (the aliases name the canonical door's). */
    public Item familyItem() {
        return asItem();
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

    /** Assemblies are placed only through {@link RoundDoorItem}; there is no single-cell placement. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    /** The item writes every cell itself; the vanilla upper-half write must not run. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    }

    /** The sneak-use bridge lets through the clicks the router handles: trim casings and restyling ({@link DoorInteraction#claims}). */
    @Override
    public boolean claimsUse(BlockState state, BlockGetter level, BlockPos pos, Player player, InteractionHand hand,
                             BlockHitResult hit) {
        return DoorInteraction.claims(state, level, pos, player, hand, hit);
    }

    /** Door trim, then restyling, then camouflage, then the toggle ({@link DoorInteraction}). */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return io.github.dirtyredz.roundwood.block.BlockUseResults.itemUse(route(state, level, pos, player, hand, hit));
    }

    private InteractionResult route(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return DoorInteraction.use(this, state, level, pos, player, hand, hit);
    }

    /**
     * A creative player's break discards stored camouflage instead of dropping it. A survival harvest
     * keeps the part in place until its loot has read the anchor ({@link AssemblyHarvest}).
     */
    /**
     * NeoForge 21.1 runs this before {@code onDestroyedByPlayer}. A creative break's partner removal (vanilla's
     * door half handling) tears the assembly down here, so it runs inside the same refund-discarding scope.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.getAbilities().instabuild) {
            return CamouflageDrops.discardingRefunds(level, () -> super.playerWillDestroy(level, pos, state, player));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player.getAbilities().instabuild) {
            return CamouflageDrops.discardingRefunds(level, () -> super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid));
        }
        return AssemblyHarvest.destroyedByPlayer(level, pos, player, willHarvest,
                () -> super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
        AssemblyHarvest.playerDestroy(level, pos, () -> super.playerDestroy(level, player, pos, state, entity, tool));
    }

    /**
     * The ordinary loot (one door per assembly) carrying the door's appearance, plus one Universal Trim
     * per trimmed face (0.8.0). Whichever part broke, the loot is evaluated with the anchor's entity,
     * so the trim pools see the door's faces.
     */
    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        Appearance appearance = DoorAppearance.forDrops(builder, state);
        DoorAppearance.ANCHORS.forLoot(builder, state).ifPresent(anchor -> builder.withParameter(LootContextParams.BLOCK_ENTITY, anchor));
        var ordinary = AppearanceDrops.decorate(super.getDrops(state, builder), AppearanceFamily.DOOR, appearance);
        DoorAppearance.ANCHORS.forLoot(builder,state).ifPresent(anchor -> ordinary.stream().filter(stack -> stack.getItem()==familyItem())
                .forEach(stack -> io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks.decorate(stack,anchor)));
        return DoorAppearance.ANCHORS.forLoot(builder, state).map(anchor -> {
            var drops = io.github.dirtyredz.roundwood.block.door.frame.DoorArchLoot.append(ordinary, anchor, this, builder);
            io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageDrops.carryLocked(drops, anchor, familyItem());
            return drops;
        }).orElse(ordinary);
    }

    /** Picking any part gives the door in its appearance. */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        var stack = AppearanceStacks.create(familyItem(), DoorAppearance.of(level, pos, state));
        DoorAppearance.ANCHORS.anchor(level,pos,state).ifPresent(entity -> io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks.decorate(stack,entity));
        return stack;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult hit) {
            var frame = io.github.dirtyredz.roundwood.block.door.frame.DoorArchInteraction.hit(level, pos, state, hit);
            if (frame.isPresent()) {
                var attachment = frame.get().entity().arch().orElseThrow();
                return AppearanceStacks.create(io.github.dirtyredz.roundwood.registry.ModBlocks.archPiece(attachment.product(), attachment.thickness()).asItem(), attachment.appearance());
            }
        }
        return getCloneItemStack(level, pos, state);
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
        if (AssemblyMutationBatch.isMutating(level, pos)) {
            return state; // Roundwood is rewriting this assembly (placement, migration): no reactions
        }
        if (design().doubleLeaf()) {
            var assembly = DoorLayout.of(pos, state);
            if (direction.getAxis() == Direction.Axis.Y) {
                int row = DoorLayout.partOf(state).row() + direction.getStepY();
                if (row >= 0 && row < 3) {
                    var partner = DoorLayout.Part.at(DoorLayout.partOf(state).columnIndex(), row);
                    if (!assembly.isPart(neighbor, this, partner)) return Blocks.AIR.defaultBlockState();
                } else if (row < 0 && !neighbor.isFaceSturdy(level, neighborPos, Direction.UP)) return Blocks.AIR.defaultBlockState();
            }
            return state;
        }
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (direction.getAxis() == Direction.Axis.Y && lower == (direction == Direction.UP) && !verticalPartner(state, neighbor)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        DoorStateController.reconcileExternalOpen(this, oldState, state, level, pos);
        AssemblyIntegrity.scheduleCheck(this, oldState, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        AssemblyIntegrity.check(this, DoorLayout.of(pos, state), level, pos);
    }

    /**
     * Removing one part removes the door ({@link AssemblyRemoval}); stored camouflage is released first,
     * while the anchor still exists, found from this part's old state ({@link CamouflageDrops}).
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        AssemblyRemoval.onRemove(this, level, pos, newState, () -> DoorLayout.of(pos, state),
                () -> CamouflageDrops.releaseOnTeardown(this, state, level, pos));
        super.onRemove(state, level, pos, newState, moving);
    }

    private boolean verticalPartner(BlockState state, BlockState neighbor) {
        return neighbor.is(this)
                && neighbor.getValue(HALF) != state.getValue(HALF)
                && neighbor.getValue(FACING) == state.getValue(FACING)
                && neighbor.getValue(HINGE) == state.getValue(HINGE)
                && DoorLayout.columnOf(neighbor) == DoorLayout.columnOf(state)
                && DoorLayout.depthOf(neighbor) == DoorLayout.depthOf(state);
    }
}
