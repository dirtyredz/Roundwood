package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
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
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayList;
import java.util.List;

/** What a section builds with: frame-relative placement helpers over one level, and a list of anything that failed. */
public final class ShowcaseCanvas {
    private final ServerLevel level;
    private final ShowcaseFrame frame;
    private final List<String> problems = new ArrayList<>();

    ShowcaseCanvas(ServerLevel level, ShowcaseFrame frame) {
        this.level = level;
        this.frame = frame;
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
     * Places a door through {@link DoorPlacement#placeAt}, its lower-left part ({@code anchor}) at
     * ({@code r}, 0, {@code f}), facing away from the builder so its front is the side they see.
     */
    public boolean door(RoundDoorBlock block, int r, int f, DoorHingeSide hinge, boolean open) {
        DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), hinge);
        boolean placed = DoorPlacement.placeAt(level, block, assembly, open);
        if (!placed) {
            problem("door not placed at " + assembly.anchor().toShortString());
        }
        return placed;
    }

    /**
     * Places a frame through {@link TrimPlacement#placeAt}, its lower-left quarter at
     * ({@code r}, 0, {@code f}), facing away from the builder like the doors.
     */
    public boolean trim(RoundTrimBlock block, int r, int f) {
        TrimLayout.Assembly assembly = new TrimLayout.Assembly(frame.pos(r, 0, f), frame.forward());
        boolean placed = TrimPlacement.placeAt(level, block, assembly);
        if (!placed) {
            problem("frame not placed at " + assembly.anchor().toShortString());
        }
        return placed;
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
