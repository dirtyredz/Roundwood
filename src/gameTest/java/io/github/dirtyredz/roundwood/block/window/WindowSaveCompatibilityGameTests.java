package io.github.dirtyredz.roundwood.block.window;

import com.mojang.serialization.Dynamic;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.migration.block.LegacyWindowBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.windowLook;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * Worlds saved by 0.5.0: chunk palette entries without {@code position} decode as centred windows
 * with their axis, connections and water, through both of Minecraft's state readers, and the
 * window's saved look loads with them. Every 0.6.0 window state survives a save and reload.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowSaveCompatibilityGameTests {
    private WindowSaveCompatibilityGameTests() {
    }

    /** A palette entry exactly as 0.5.0 wrote it (written by hand, not by the current code). */
    private static CompoundTag palette050(String id, String axis, boolean north, boolean east, boolean south, boolean west, boolean water) {
        CompoundTag properties = new CompoundTag();
        properties.putString("axis", axis);
        properties.putString("north", String.valueOf(north));
        properties.putString("east", String.valueOf(east));
        properties.putString("south", String.valueOf(south));
        properties.putString("west", String.valueOf(west));
        properties.putString("waterlogged", String.valueOf(water));
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", "roundwood:" + id);
        tag.put("Properties", properties);
        return tag;
    }

    private static BlockState readNbt(GameTestHelper helper, CompoundTag tag) {
        return NbtUtils.readBlockState(helper.getLevel().holderLookup(Registries.BLOCK), tag);
    }

    private static BlockState readCodec(CompoundTag tag) {
        return BlockState.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag)).result()
                .orElseThrow(() -> new AssertionError("codec could not read " + tag));
    }

    @GameTest(template = TEMPLATE)
    public static void a050PaletteEntryDecodesCentredThroughBothReaders(GameTestHelper helper) {
        CompoundTag tag = palette050("round_window", "z", true, false, true, false, true);
        for (BlockState state : List.of(readNbt(helper, tag), readCodec(tag))) {
            check(state.is(ModBlocks.roundWindow()), "not a round window: " + state);
            check(state.getValue(RoundWindowBlock.POSITION) == WindowPosition.CENTER, "a 0.5.0 window did not load centred: " + state);
            check(state.getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z, "axis lost: " + state);
            check(state.getValue(CrossCollisionBlock.NORTH) && state.getValue(CrossCollisionBlock.SOUTH)
                    && !state.getValue(CrossCollisionBlock.EAST) && !state.getValue(CrossCollisionBlock.WEST), "connections lost: " + state);
            check(state.getValue(BlockStateProperties.WATERLOGGED), "water lost: " + state);
            check(!state.getValue(WindowPaneBlock.TRIMMED), "a 0.5.0 window loaded trimmed: " + state);
        }
        CompoundTag alias = palette050("spruce_round_window", "x", false, true, false, false, false);
        BlockState legacy = readNbt(helper, alias);
        check(legacy.getBlock() instanceof LegacyWindowBlock && legacy.getValue(RoundWindowBlock.POSITION) == WindowPosition.CENTER,
                "a 0.4.0 alias window did not load centred: " + legacy);
        helper.succeed();
    }

    /** The decoded state placed with its 0.5.0 block entity data shows the saved look. */
    @GameTest(template = TEMPLATE)
    public static void aSavedWindowKeepsItsLook(GameTestHelper helper) {
        BlockState state = readNbt(helper, palette050("round_window", "x", false, false, false, false, false));
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS);
        CompoundTag look = new CompoundTag();
        look.putInt("Version", 1);
        look.putString("Kind", "wood");
        look.putString("Material", "minecraft:spruce_planks");
        CompoundTag entity = new CompoundTag();
        entity.put("RoundwoodAppearance", look);
        helper.getLevel().getBlockEntity(pos).load(entity);
        check(SPRUCE.equals(windowLook(helper, pos)), "saved look not read: " + windowLook(helper, pos));
        check(helper.getLevel().getBlockState(pos).getValue(RoundWindowBlock.POSITION) == WindowPosition.CENTER, "not centred");
        check(!Appearance.DEFAULT.equals(windowLook(helper, pos)), "look reset to oak");
        helper.succeed();
    }

    /** A 0.6.x arch palette entry (orientation and depth, written by hand, no trim property) decodes as it was, untrimmed. */
    @GameTest(template = TEMPLATE)
    public static void a061ArchPaletteEntryDecodesUntrimmed(GameTestHelper helper) {
        CompoundTag properties = new CompoundTag();
        properties.putString("orientation", "up_east");
        properties.putString("depth", "positive");
        for (String side : List.of("north", "east", "south", "west")) {
            properties.putString(side, "false");
        }
        properties.putString("waterlogged", "true");
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", "roundwood:round_window_arch_right");
        tag.put("Properties", properties);
        for (BlockState state : List.of(readNbt(helper, tag), readCodec(tag))) {
            check(state.is(ModBlocks.archRight()), "not the right arch: " + state);
            check(ModBlocks.archRight().pose(state).equals(new io.github.dirtyredz.roundwood.block.window.geometry.WindowPose(
                    Direction.UP, Direction.EAST, io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth.POSITIVE)), "pose lost: " + state);
            check(state.getValue(BlockStateProperties.WATERLOGGED) && !state.getValue(WindowPaneBlock.TRIMMED), "water or trim wrong: " + state);
        }
        // 0.6.x entity data (the pane look only) on that window: the look loads, no trim appears.
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(pos, readNbt(helper, tag), Block.UPDATE_CLIENTS);
        CompoundTag look = new CompoundTag();
        look.putInt("Version", 1);
        look.putString("Kind", "wood");
        look.putString("Material", "minecraft:spruce_planks");
        CompoundTag entity = new CompoundTag();
        entity.put("RoundwoodAppearance", look);
        helper.getLevel().getBlockEntity(pos).load(entity);
        check(SPRUCE.equals(windowLook(helper, pos)), "saved look not read");
        check(!((RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos)).trims().any(), "an untrimmed 0.6 window shows a trim");
        helper.succeed();
    }

    /** Every state of every window block (the ten windows and an alias) writes and reads back unchanged. */
    @GameTest(template = TEMPLATE)
    public static void everyWindowStateSurvivesSaveAndReload(GameTestHelper helper) {
        List<Block> blocks = new java.util.ArrayList<>(ModBlocks.windows());
        blocks.add(io.github.dirtyredz.roundwood.migration.LegacyContent.windows().get(0));
        for (Block block : blocks) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                CompoundTag saved = NbtUtils.writeBlockState(state);
                check(readNbt(helper, saved).equals(state), "NbtUtils changed " + state);
                check(readCodec(saved).equals(state), "the codec changed " + state);
            }
        }
        helper.succeed();
    }
}
