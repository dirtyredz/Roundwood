package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimActions;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.window.pane.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a section builds with: frame-relative placement helpers over one level that go through the
 * mod's production paths (assembly placement with an appearance, the window entity's setter), the
 * shared sample appearances, and a list of anything that failed.
 */
public final class ShowcaseCanvas {
    private final ServerLevel level;
    private final ShowcaseFrame frame;
    private final List<String> problems = new ArrayList<>();
    private final List<Appearance> samples;
    private final Set<BlockPos> labelCells = new HashSet<>();

    ShowcaseCanvas(ServerLevel level, ShowcaseFrame frame) {
        this.level = level;
        this.frame = frame;
        this.samples = ShowcaseSamples.all();
    }

    /** Every sample look of this build ({@link ShowcaseSamples#all}): woods, then paints, then masonry. */
    public List<Appearance> samples() {
        return samples;
    }

    public ServerLevel level() {
        return level;
    }

    public ShowcaseFrame frame() {
        return frame;
    }

    public BlockPos pos(int r, int y, int f) {
        return frame.pos(r, y, f);
    }

    List<String> problems() {
        return problems;
    }

    public void problem(String message) {
        problems.add(message);
    }

    public void set(int r, int y, int f, BlockState state) {
        level.setBlock(frame.pos(r, y, f), state, Block.UPDATE_ALL);
    }

    /**
     * Places a door in {@code appearance} through {@link DoorPlacement#placeAt}, its lower-left part
     * ({@code anchor}) at ({@code r}, 0, {@code f}), facing away from the builder so its front is the
     * side they see.
     */
    public boolean door(Appearance appearance, int r, int f, DoorHingeSide hinge, boolean open) {
        return door(ModBlocks.roundDoor(), appearance, r, f, hinge, open);
    }

    /** As {@link #door(Appearance, int, int, DoorHingeSide, boolean)} for any door design (0.11.0: the arched door). */
    public boolean door(AssemblyDoorBlock block, Appearance appearance, int r, int f, DoorHingeSide hinge, boolean open) {
        return door(block, appearance, r, f, hinge, open, io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.FRONT);
    }

    public boolean door(AssemblyDoorBlock block, Appearance appearance, int r, int f, DoorHingeSide hinge, boolean open,
                        io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth depth) {
        DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), hinge, block.design(), depth);
        boolean placed = DoorPlacement.placeAt(level, block, assembly, open, appearance);
        if (!placed) {
            problem("door not placed at " + assembly.anchor().toShortString());
        }
        return placed;
    }

    /**
     * Fits a door trim in {@code appearance} on {@code face} of the door whose anchor is at ({@code r},
     * 0, {@code f}), the way a Universal Trim fits it (0.8.0).
     */
    public boolean doorTrim(Appearance appearance, int r, int f, AttachmentFace face) {
        boolean fitted = DoorTrimActions.install(level, pos(r, 0, f), face, appearance);
        if (!fitted) {
            problem(face + " door trim not fitted at " + pos(r, 0, f).toShortString());
        }
        return fitted;
    }

    /** Fits a top through the same proposal/transaction as ordinary clicks, then styles its own casings. */
    public boolean fittedArch(io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness thickness,
            io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth depth, Appearance appearance, FaceTrims trims, int r, int f) {
        var anchor = pos(r, 0, f);
        if (!io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.fitAt(level, anchor, thickness, depth, appearance)) {
            problem("arch did not fit at " + anchor.toShortString()); return false;
        }
        var entity = (io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity) level.getBlockEntity(anchor);
        var door = DoorLayout.of(anchor, level.getBlockState(anchor));
        for (var face : AttachmentFace.values()) trims.get(face).ifPresent(look -> {
            var hit = new io.github.dirtyredz.roundwood.block.door.frame.DoorArchInteraction.Hit(entity, door, face, true);
            if (!io.github.dirtyredz.roundwood.block.attachment.host.TrimFitting.install(
                    io.github.dirtyredz.roundwood.block.door.frame.DoorArchInteraction.host(hit, anchor), face, look)) problem("fitted casing refused");
        });
        return true;
    }

    /** Places one whole modular piece and its independent casings through production transactions. */
    public boolean archPiece(ArchPieceDesign design, Side side, Appearance appearance, FaceTrims trims, int r, int y, int f) {
        return archPiece(design, side, io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL,
                io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT, appearance, trims, r, y, f);
    }
    public boolean archPiece(ArchPieceDesign design, Side side,
            io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness thickness,
            io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth depth,
            Appearance appearance, FaceTrims trims, int r, int y, int f) {
        var assembly = new ArchPieceLayout.Assembly(pos(r, y, f), frame.forward(), design, side, thickness, depth);
        if (!ArchPiecePlacement.placeAt(level, ModBlocks.archPiece(design, thickness), assembly, appearance)) {
            problem("arch piece not placed at " + assembly.anchor().toShortString());
            return false;
        }
        for (AttachmentFace face : AttachmentFace.values()) {
            trims.get(face).ifPresent(look -> {
                if (!ArchPieceTrimActions.install(level, assembly.anchor(), face, look)) {
                    problem(face + " arch casing not fitted at " + assembly.anchor().toShortString());
                }
            });
        }
        return true;
    }

    /** A centred round window across {@code axis} (see {@link #pane}). */
    public void window(Appearance appearance, int r, int y, int f, Direction.Axis axis, boolean waterlogged) {
        pane(appearance, r, y, f, ModBlocks.roundWindow().defaultBlockState().setValue(RoundWindowBlock.AXIS, axis), waterlogged);
    }

    /** A round window flush against the cell's {@code side} (a world direction; see {@link #pane}). */
    public void flushWindow(Appearance appearance, int r, int y, int f, Direction side, boolean waterlogged) {
        pane(appearance, r, y, f, ModBlocks.roundWindow().defaultBlockState()
                .setValue(RoundWindowBlock.AXIS, acrossAxis())
                .setValue(RoundWindowBlock.POSITION, WindowPosition.flush(side)), waterlogged);
    }

    /** An arch piece in {@code pose} (world directions; see {@link #pane}). */
    public void arch(OrientedWindowBlock piece, Appearance appearance, int r, int y, int f, WindowPose pose) {
        pane(appearance, r, y, f, OrientedWindowBlock.withPose(piece.defaultBlockState(), pose), false);
    }

    /**
     * Places {@code window} (see {@link #pane}), then fits its face trims and arch sills the way the
     * items do, and tints its glass.
     */
    public void window(ShowcaseWindow window) {
        pane(window.pane(), window.r(), window.y(), window.f(), window.state(), false);
        BlockPos pos = frame.pos(window.r(), window.y(), window.f());
        for (TrimSlot slot : TrimSlot.values()) {
            FaceTrims fitted = switch (slot) {
                case SILL -> window.sills();
                case PERIMETER -> window.trims();
                case CROSS_BEAMS -> window.beams().map(look -> FaceTrims.NONE.with(AttachmentFace.FRONT, look)).orElse(FaceTrims.NONE);
            };
            for (AttachmentFace face : slot.addresses()) {
                fitted.get(face).ifPresent(look -> {
                    if (!WindowTrimActions.install(level, pos, slot, face, look)) {
                        problem(face + " " + slot.getSerializedName() + " not fitted at " + pos.toShortString());
                    }
                });
            }
        }
        if (window.glass().isPresent() && level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity) {
            entity.setGlass(window.glass());
        }
    }

    /**
     * Sets a single-cell window state at ({@code r}, {@code y}, {@code f}) with the pane connections
     * its neighbours give it (they update on the write), then gives it {@code appearance} through its
     * block entity.
     */
    public void pane(Appearance appearance, int r, int y, int f, BlockState state, boolean waterlogged) {
        BlockPos pos = frame.pos(r, y, f);
        BlockState wet = state.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        level.setBlock(pos, Block.updateFromNeighbourShapes(wet, level, pos), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity) {
            entity.setAppearance(appearance);
        } else {
            problem("window not placed at " + pos.toShortString());
        }
    }

    /**
     * The column a label stands in for a piece whose lower-left cell is in column {@code pieceR}:
     * {@link ShowcaseSpacing#LABEL_OFFSET} columns to the builder's left, so the sign sits beside the
     * piece, with a free block between them, rather than in front of it.
     */
    public static int labelR(int pieceR) {
        return pieceR + ShowcaseSpacing.LABEL_OFFSET;
    }

    /**
     * Labels the piece whose lower-left cell is in column {@code pieceR} and row {@code f}: a standing
     * sign on the floor at {@link #labelR} in the piece's own row (0.13.1: beside it, so the walkway in
     * front stays clear), turned to face the builder so it reads from where they stand to look at the
     * piece without covering it. A cell that is not free, or a sign a later section overwrites, is a
     * build problem ({@link #verifyLabels}).
     */
    public void sign(int pieceR, int f, String... lines) {
        int rotation = Math.floorMod(Math.round(frame.forward().getOpposite().toYRot() / 22.5F), 16);
        BlockPos pos = frame.pos(labelR(pieceR), 0, f);
        if (!level.getBlockState(pos).isAir()) {
            problem("sign would replace " + level.getBlockState(pos) + " at " + pos.toShortString());
        }
        labelCells.add(pos);
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation),
                Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < lines.length && i < 4; i++) {
                text = text.setMessage(i, Component.literal(lines[i]));
            }
            sign.setText(text, true);
            sign.setWaxed(true);
        } else {
            problem("sign not placed at " + pos.toShortString());
        }
    }

    /**
     * Hangs a wall sign on the front (builder-facing) face of the block at ({@code r}, {@code y},
     * {@code f}), one row nearer, reading {@code lines}. The block must already stand there.
     */
    public void wallSign(int r, int y, int f, String... lines) {
        BlockPos pos = frame.pos(r, y, f - 1);
        if (!level.getBlockState(pos).isAir()) {
            problem("wall sign would replace " + level.getBlockState(pos) + " at " + pos.toShortString());
        }
        labelCells.add(pos);
        level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, frame.forward().getOpposite()),
                Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < lines.length && i < 4; i++) {
                text = text.setMessage(i, Component.literal(lines[i]));
            }
            sign.setText(text, true);
            sign.setWaxed(true);
        } else {
            problem("wall sign not placed at " + pos.toShortString());
        }
    }

    /** A barrel at ({@code r}, {@code y}, {@code f}) holding {@code stacks} from slot 0 (a source the player may take from). */
    public void barrel(int r, int y, int f, List<ItemStack> stacks) {
        BlockPos pos = frame.pos(r, y, f);
        level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, frame.forward().getOpposite()), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel && stacks.size() <= barrel.getContainerSize()) {
            for (int i = 0; i < stacks.size(); i++) {
                barrel.setItem(i, stacks.get(i));
            }
        } else {
            problem("barrel not stocked at " + pos.toShortString());
        }
    }

    /** Reports every label a later section overwrote; called once after the last section has built. */
    void verifyLabels() {
        for (BlockPos pos : labelCells) {
            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity)) {
                problem("label overwritten at " + pos.toShortString() + " by " + level.getBlockState(pos));
            }
        }
    }

    /** The direction a pane runs along when it follows the builder's right. */
    public Direction.Axis acrossAxis() {
        return frame.right().getAxis();
    }
}
