package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageGeometry;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimGeometry;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimItem;
import io.github.dirtyredz.roundwood.geometry.Box;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The two selection shapes of a part, both the whole assembly, the same in world space from every
 * part (so neither shows quarter seams), and neither collides (collision stays per cell in
 * {@code leaf/DoorShapes}):
 * <ul>
 *     <li><b>Target</b> ({@link #target}, the block's {@code getShape}, what rays hit): the leaf at
 *     rest plus the static corners when they are filled, or while the looking player holds a block
 *     item in either hand (so empty corners can be targeted to apply a material); plus the in-cell
 *     targets of the installed trim rings (0.8.0), and of both rings while the door trim is held (so
 *     an open door can still be fitted).</li>
 *     <li><b>Visible</b> ({@link #visible}, what the client highlights): the leaf at rest plus only the
 *     corners that are actually filled and the rings actually installed, drawn where they are (the
 *     front ring one pixel past the cells). An absent ring is never outlined as installed.</li>
 * </ul>
 * Built once per variant and cached.
 */
public final class DoorSelectionShapes {
    private static final ConcurrentHashMap<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, VoxelShape> STATIC = new ConcurrentHashMap<>();

    private DoorSelectionShapes() {
    }

    /** What rays hit: the leaf, the corners when filled or while a block item is held, and ring targets. */
    public static VoxelShape target(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        int rings = holdsDoorTrim(context) ? 3 : installed(level, assembly);
        return shape(state, holdsBlockItem(context) || filled(level, assembly), rings, true);
    }

    /** What is there to highlight: the leaf at rest, plus the filled corners and the installed rings. */
    public static VoxelShape visible(BlockState state, BlockGetter level, BlockPos pos) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        return shape(state, filled(level, assembly), installed(level, assembly), false);
    }

    /**
     * What stays put while the leaf swings, relative to this part: the filled corners and the installed
     * rings (empty when there are neither).
     */
    public static VoxelShape visibleStatic(BlockState state, BlockGetter level, BlockPos pos) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, state);
        boolean corners = filled(level, assembly);
        int rings = installed(level, assembly);
        if (!corners && rings == 0) {
            return Shapes.empty();
        }
        int turns = VoxelShapeFactory.quarterTurns(state.getValue(DoorBlock.FACING));
        DoorLayout.Part part = DoorLayout.partOf(state);
        int column = part.column.index();
        int row = part.row();
        int key = (turns << 5) | (column << 4) | (row << 3) | ((corners ? 1 : 0) << 2) | rings;
        return STATIC.computeIfAbsent(key, k -> {
            List<Box> boxes = new ArrayList<>(corners ? CamouflageGeometry.corners() : List.of());
            addRings(boxes, rings, false);
            return VoxelShapeFactory.shape(VoxelShapeFactory.rotate(DoorOutlineGeometry.relativeTo(boxes, column, row), turns));
        });
    }

    private static VoxelShape shape(BlockState state, boolean corners, int rings, boolean target) {
        return shape(state.getValue(DoorBlock.FACING), state.getValue(DoorBlock.HINGE), DoorLayout.partOf(state),
                state.getValue(DoorBlock.OPEN), corners, rings, target);
    }

    /**
     * @param rings  bit 0 the FRONT ring, bit 1 the BACK ring
     * @param target true for the rays' in-cell ring targets, false for the rings as drawn
     */
    public static VoxelShape shape(Direction facing, DoorHingeSide hinge, DoorLayout.Part part, boolean open, boolean corners,
                                   int rings, boolean target) {
        int turns = VoxelShapeFactory.quarterTurns(facing);
        int column = part.column.index();
        int row = part.row();
        int key = (turns << 9) | ((hinge == DoorHingeSide.LEFT ? 0 : 1) << 8) | (column << 7) | (row << 6)
                | ((open ? 1 : 0) << 5) | ((corners ? 1 : 0) << 4) | (rings << 1) | (target ? 1 : 0);
        return SHAPES.computeIfAbsent(key, k -> {
            List<Box> boxes = new ArrayList<>(DoorOutlineGeometry.outline(hinge, open, corners));
            addRings(boxes, rings, target);
            return VoxelShapeFactory.shape(VoxelShapeFactory.rotate(DoorOutlineGeometry.relativeTo(boxes, column, row), turns));
        });
    }

    private static void addRings(List<Box> boxes, int rings, boolean target) {
        for (AttachmentFace face : AttachmentFace.values()) {
            if ((rings & bit(face)) != 0) {
                boxes.addAll(target ? DoorTrimGeometry.target(face) : DoorTrimGeometry.ring(face));
            }
        }
    }

    private static int bit(AttachmentFace face) {
        return face == AttachmentFace.FRONT ? 1 : 2;
    }

    /** A player looking with a block item in either hand: empty corners become targets. */
    private static boolean holdsBlockItem(CollisionContext context) {
        return player(context).map(player -> player.getMainHandItem().getItem() instanceof BlockItem
                || player.getOffhandItem().getItem() instanceof BlockItem).orElse(false);
    }

    /** A player looking with the door trim in hand (main, or offhand with the main hand empty): both rings are targets. */
    private static boolean holdsDoorTrim(CollisionContext context) {
        return player(context).map(player -> player.getMainHandItem().getItem() instanceof DoorTrimItem
                || player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof DoorTrimItem).orElse(false);
    }

    private static Optional<Player> player(CollisionContext context) {
        return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
                ? Optional.of(player) : Optional.empty();
    }

    /** Filled corners are always selectable. Only a live level's loaded anchor is asked (never worldgen regions). */
    private static boolean filled(BlockGetter level, DoorLayout.Assembly assembly) {
        return anchor(level, assembly).map(entity -> entity.camouflage().isPresent()).orElse(false);
    }

    /** The installed rings as bits (bit 0 FRONT, bit 1 BACK). */
    private static int installed(BlockGetter level, DoorLayout.Assembly assembly) {
        FaceTrims trims = anchor(level, assembly).map(RoundDoorBlockEntity::trims).orElse(FaceTrims.NONE);
        return (trims.has(AttachmentFace.FRONT) ? 1 : 0) | (trims.has(AttachmentFace.BACK) ? 2 : 0);
    }

    private static Optional<RoundDoorBlockEntity> anchor(BlockGetter level, DoorLayout.Assembly assembly) {
        return level instanceof Level live && live.isLoaded(assembly.anchor())
                && live.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity ? Optional.of(entity) : Optional.empty();
    }
}
