package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceSamples;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.material.MasonryMaterials;
import io.github.dirtyredz.roundwood.appearance.material.PlankMaterials;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.block.window.RoundWindowArchBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
        this.trimSamples = sampleSet();
    }

    /**
     * The shared bounded samples (every vanilla wood, up to eight present modded planks, every paint on
     * oak, the four masonry materials), discovered now, so a tag or resource reload shows next build.
     */
    public static List<Appearance> sampleSet() {
        return AppearanceSamples.forFamily(AppearanceFamily.TRIM, PlankMaterials.present(), MasonryMaterials.blocks());
    }

    /** Every bare wood sample: the vanilla woods, then up to eight present modded planks. */
    public List<Appearance> woods() {
        return trimSamples.stream().filter(a -> a.isWood() && !a.isPainted()).toList();
    }

    /** One oak sample per dye, in dye order. */
    public List<Appearance> paints() {
        return trimSamples.stream().filter(Appearance::isPainted).toList();
    }

    public List<Appearance> masonry() {
        return trimSamples.stream().filter(a -> !a.isWood()).toList();
    }

    /** A short sign label for an appearance: the material's name, or the dye's. */
    public static String label(Appearance appearance) {
        if (appearance.paint().isPresent()) {
            String dye = appearance.paint().get().getName().replace('_', ' ');
            return Character.toUpperCase(dye.charAt(0)) + dye.substring(1) + " paint";
        }
        ResourceLocation id = appearance.material();
        return BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.get(id).getName().getString() : id.toString();
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
        DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), hinge);
        boolean placed = DoorPlacement.placeAt(level, ModBlocks.roundDoor(), assembly, open, appearance);
        if (!placed) {
            problem("door not placed at " + assembly.anchor().toShortString());
        }
        return placed;
    }

    /**
     * Places a frame in {@code appearance} through {@link TrimPlacement#placeAt}, its lower-left
     * quarter at ({@code r}, 0, {@code f}), facing away from the builder like the doors.
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
    public void arch(RoundWindowArchBlock piece, Appearance appearance, int r, int y, int f, WindowPose pose) {
        pane(appearance, r, y, f, RoundWindowArchBlock.withPose(piece.defaultBlockState(), pose), false);
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
