package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPlacement;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimActions;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
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
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.List;

/**
 * What a section builds with: frame-relative placement helpers over one level that go through the
 * mod's production paths (assembly placement with an appearance, the window entity's setter), the
 * shared sample appearances, and a list of anything that failed.
 */
public final class ShowcaseCanvas {
    private final ServerLevel level;
    private final ShowcaseFrame frame;
    private final List<String> problems = new ArrayList<>();
    private final List<Appearance> trimSamples;

    ShowcaseCanvas(ServerLevel level, ShowcaseFrame frame) {
        this.level = level;
        this.frame = frame;
        this.trimSamples = ShowcaseSamples.all();
    }

    /** This build's bare wood samples ({@link ShowcaseSamples#woods}). */
    public List<Appearance> woods() {
        return ShowcaseSamples.woods(trimSamples);
    }

    /** This build's paint samples ({@link ShowcaseSamples#paints}). */
    public List<Appearance> paints() {
        return ShowcaseSamples.paints(trimSamples);
    }

    public List<Appearance> masonry() {
        return ShowcaseSamples.masonry(trimSamples);
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
        DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), hinge, block.design());
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

    /**
     * Places an archway (0.12.0) in {@code appearance} through {@link ArchwayPlacement#placeAt}, its
     * lower-left cell at ({@code r}, 0, {@code f}), facing away from the builder like the doors, then
     * fits its face trims the way a Universal Trim does.
     */
    public boolean archway(ArchwayBlock block, Appearance appearance, FaceTrims trims, int r, int f) {
        ArchwayLayout.Assembly assembly = new ArchwayLayout.Assembly(frame.pos(r, 0, f), frame.forward(), block.design());
        if (!ArchwayPlacement.placeAt(level, block, assembly, appearance)) {
            problem("archway not placed at " + assembly.anchor().toShortString());
            return false;
        }
        for (AttachmentFace face : AttachmentFace.values()) {
            trims.get(face).ifPresent(look -> {
                if (!ArchwayTrimActions.install(level, assembly.anchor(), face, look)) {
                    problem(face + " archway trim not fitted at " + assembly.anchor().toShortString());
                }
            });
        }
        return true;
    }

    /**
     * Places a legacy freestanding frame (0.8.0: compatibility content that only older saves hold) in
     * {@code appearance} through {@link TrimPlacement#placeAt}, its lower-left quarter at ({@code r},
     * 0, {@code f}), facing away from the builder like the doors.
     */
    public boolean trim(Appearance appearance, int r, int f) {
        TrimLayout.Assembly assembly = new TrimLayout.Assembly(frame.pos(r, 0, f), frame.forward());
        boolean placed = TrimPlacement.placeAt(level, ModBlocks.roundTrim(), assembly, appearance);
        if (!placed) {
            problem("frame not placed at " + assembly.anchor().toShortString());
        }
        return placed;
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
            FaceTrims fitted = slot == TrimSlot.SILL ? window.sills() : window.trims();
            for (AttachmentFace face : AttachmentFace.values()) {
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

    /** A standing sign on the floor at ({@code r}, 0, {@code f}) readable from the builder's side. */
    public void sign(int r, int f, String... lines) {
        int rotation = Math.floorMod(Math.round(frame.forward().getOpposite().toYRot() / 22.5F), 16);
        BlockPos pos = frame.pos(r, 0, f);
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

    /** The direction a pane runs along when it follows the builder's right. */
    public Direction.Axis acrossAxis() {
        return frame.right().getAxis();
    }
}
