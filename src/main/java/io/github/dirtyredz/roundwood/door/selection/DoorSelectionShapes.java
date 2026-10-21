package io.github.dirtyredz.roundwood.door.selection;

import io.github.dirtyredz.roundwood.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.door.assembly.DoorLayout;
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
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The outline (selection) shape of a part: the whole assembly's outline, the same in world space
 * from every part, so the highlight never shows quarter seams. The static corners join it when
 * they are filled, or while the looking player holds a block item (so empty corners can be
 * targeted to apply a material). Nothing here collides; collision stays per cell in
 * {@code leaf/DoorShapes}. At most 128 shapes, built once and cached.
 */
public final class DoorSelectionShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private DoorSelectionShapes() {
    }

    public static VoxelShape outline(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        boolean corners = holdsBlockItem(context) || filled(level, assembly);
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
