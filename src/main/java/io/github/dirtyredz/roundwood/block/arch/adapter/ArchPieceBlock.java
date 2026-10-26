package io.github.dirtyredz.roundwood.block.arch.adapter;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceDrops;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.appearance.ArchPieceAppearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceShapes;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyHarvest;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyIntegrity;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyRemoval;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Minecraft callbacks only; assembly services own completeness, removal, harvesting and anchors. */
public final class ArchPieceBlock extends Block implements EntityBlock, AppearanceDefaults {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ArchPieceCell> CELL = EnumProperty.create("cell", ArchPieceCell.class);
    public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
    private final ArchPieceDesign design;
    private final Supplier<BlockEntityType<ArchPieceBlockEntity>> entityType;

    public ArchPieceBlock(Properties properties, ArchPieceDesign design, Supplier<BlockEntityType<ArchPieceBlockEntity>> entityType) {
        super(properties);
        this.design = design;
        this.entityType = entityType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(CELL, ArchPieceCell.LOWER_LEFT).setValue(SIDE, Side.LEFT));
    }
    public ArchPieceDesign design() { return design; }
    @Override public AppearanceFamily appearanceFamily() { return AppearanceFamily.TRIM; }
    @Override public Appearance defaultAppearance() { return Appearance.DEFAULT; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, CELL, SIDE); }
    @Override @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(CELL) == ArchPieceCell.LOWER_LEFT ? new ArchPieceBlockEntity(entityType.get(), pos, state) : null;
    }
    /** Only whole-piece installation is supported; there is no vanilla single-cell placement. */
    @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) { return null; }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ArchPieceShapes.shape(design, state.getValue(SIDE), state.getValue(FACING), state.getValue(CELL));
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ArchPieceShapes.collision(design, state.getValue(SIDE), state.getValue(FACING), state.getValue(CELL));
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE || !state.getValue(CELL).in(design)) return state;
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
                .setValue(CELL, state.getValue(CELL).mirrored(design))
                .setValue(SIDE, design.shoulder() ? state.getValue(SIDE).mirrored() : state.getValue(SIDE));
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        AssemblyIntegrity.scheduleCheck(this, oldState, level, pos);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Commands can name a cell outside a smaller product's footprint. Never resolve that
        // invalid cell into a neighbouring valid assembly or use it to tear that neighbour down.
        if (!state.getValue(CELL).in(design)) { level.destroyBlock(pos, false); return; }
        AssemblyIntegrity.check(this, ArchPieceLayout.of(pos, state), level, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getValue(CELL).in(design))
            AssemblyRemoval.onRemove(this, level, pos, newState, () -> ArchPieceLayout.of(pos, state));
        super.onRemove(state, level, pos, newState, moving);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return AssemblyHarvest.destroyedByPlayer(this, state, level, pos, player, willHarvest,
                () -> super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid));
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
        AssemblyHarvest.playerDestroy(level, pos, () -> super.playerDestroy(level, player, pos, state, entity, tool));
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return AppearanceDrops.decorate(super.getDrops(state, builder), asItem(), ArchPieceAppearance.forDrops(builder, state));
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return AppearanceStacks.create(asItem(), ArchPieceAppearance.of(level, pos, state));
    }
}
