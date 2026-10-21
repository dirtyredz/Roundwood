package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageGeometry;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The two selection shapes of a part, both the whole assembly, the same in world space from every
 * part (so neither shows quarter seams), and neither collides (collision stays per cell in
 * {@code leaf/DoorShapes}):
 * <ul>
 *     <li><b>Target</b> ({@link #target}, the block's {@code getShape}, what rays hit): the leaf at
 *     rest plus the static corners when they are filled, or while the looking player holds a block
 *     item in either hand (so empty corners can be targeted to apply a material).</li>
 *     <li><b>Visible</b> ({@link #visible}, what the client highlights): the leaf at rest plus only the
 *     corners that are actually filled. An empty corner is a target but nothing is there to outline.</li>
 * </ul>
 * At most 128 shapes, built once and cached.
 */
public final class DoorSelectionShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, VoxelShape> CORNERS = new ConcurrentHashMap<>();

    private DoorSelectionShapes() {
    }

    /** What rays hit: the leaf, plus the corners when filled or while a block item is held. */
    public static VoxelShape target(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        return shape(state, holdsBlockItem(context) || filled(level, assembly));
    }

    /** What is there to highlight: the leaf at rest, plus the corners only once they are filled. */
    public static VoxelShape visible(BlockState state, BlockGetter level, BlockPos pos) {
        return shape(state, filled(level, DoorLayout.of(pos, state)));
    }

    /** The filled corners alone, relative to this part (empty when none are filled): what stays put while the leaf swings. */
    public static VoxelShape visibleCorners(BlockState state, BlockGetter level, BlockPos pos) {
        if (!filled(level, DoorLayout.of(pos, state))) {
            return Shapes.empty();
        }
        int turns = VoxelShapeFactory.quarterTurns(state.getValue(DoorBlock.FACING));
        DoorLayout.Part part = DoorLayout.partOf(state);
        int column = part.column.index();
        int row = part.row();
        return CORNERS.computeIfAbsent((turns << 2) | (column << 1) | row, k -> VoxelShapeFactory.shape(VoxelShapeFactory.rotate(
                DoorOutlineGeometry.relativeTo(CamouflageGeometry.corners(), column, row), turns)));
    }

    private static VoxelShape shape(BlockState state, boolean corners) {
        return shape(state.getValue(DoorBlock.FACING), state.getValue(DoorBlock.HINGE), DoorLayout.partOf(state),
                state.getValue(DoorBlock.OPEN), corners);
    }

    public static VoxelShape shape(Direction facing, DoorHingeSide hinge, DoorLayout.Part part, boolean open, boolean corners) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        int column = part.column.index();
        int row = part.row();
        int key = (turns << 5) | ((hinge == DoorHingeSide.LEFT ? 0 : 1) << 4) | (column << 3) | (row << 2)
                | ((open ? 1 : 0) << 1) | (corners ? 1 : 0);
        return SHAPES.computeIfAbsent(key, k -> VoxelShapeFactory.shape(VoxelShapeFactory.rotate(
                DoorOutlineGeometry.relativeTo(DoorOutlineGeometry.outline(hinge, open, corners), column, row), turns)));
    }

    /** A player looking with a block item in either hand: empty corners become targets. */
    private static boolean holdsBlockItem(CollisionContext context) {
        return context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && (player.getMainHandItem().getItem() instanceof BlockItem
                || player.getOffhandItem().getItem() instanceof BlockItem);
    }

    /** Filled corners are always selectable. Only a live level's loaded anchor is asked (never worldgen regions). */
    private static boolean filled(BlockGetter level, DoorLayout.Assembly assembly) {
        return level instanceof Level live && live.isLoaded(assembly.anchor())
                && live.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity
                && entity.camouflage().isPresent();
    }
}
