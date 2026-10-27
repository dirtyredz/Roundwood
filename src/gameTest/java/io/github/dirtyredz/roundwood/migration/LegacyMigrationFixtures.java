package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * Puts 0.4.0 content into a GameTest level the way a saved chunk holds it: states written straight
 * into the chunk sections (no placement callbacks, no scheduled ticks) and block entities loaded
 * from their saved NBT, then runs Roundwood's real chunk-load and level-tick handlers. Ids and NBT are
 * spelled out here, independent of the migration tables.
 */
final class LegacyMigrationFixtures {
    private LegacyMigrationFixtures() {
    }

    static Block legacy(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("0.4.0 id not registered: " + id);
        }
        return block;
    }

    /** Writes {@code state} at the absolute {@code pos} with no callbacks, as chunk loading does. */
    static void raw(GameTestHelper helper, BlockPos pos, BlockState state) {
        ServerLevel level = helper.getLevel();
        LevelChunk chunk = level.getChunkAt(pos);
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
        level.getChunkSource().blockChanged(pos);
    }

    /** Loads a saved block entity at the absolute {@code pos} (its state must already be there). */
    static void rawEntity(GameTestHelper helper, BlockPos pos, String snbt) {
        try {
            CompoundTag tag = TagParser.parseTag(snbt);
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            BlockEntity entity = BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos), tag);
            if (entity == null) {
                throw new IllegalStateException("no block entity loaded from " + snbt);
            }
            helper.getLevel().getChunkAt(pos).setBlockEntity(entity);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(snbt, e);
        }
    }

    /** A saved 0.4.0 door, one block id per part ({@code null}: leave that cell alone), anchor entity from {@code anchorSnbt}. */
    static DoorLayout.Assembly rawDoor(GameTestHelper helper, DoorLayout.Assembly door, Block[] parts, boolean open,
                                       String anchorSnbt) {
        java.util.List<DoorLayout.Part> order = DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND);
        for (int i = 0; i < order.size(); i++) {
            if (parts[i] != null) {
                raw(helper, door.pos(order.get(i)), door.stateFor(order.get(i), parts[i], open, false));
            }
        }
        if (anchorSnbt != null && parts[0] != null) {
            rawEntity(helper, door.anchor(), anchorSnbt);
        }
        return door;
    }

    /** A saved 0.4.0 door of one id, all four parts. */
    static DoorLayout.Assembly rawDoor(GameTestHelper helper, BlockPos anchor, String id, DoorHingeSide hinge, boolean open,
                                       String anchorSnbt) {
        Block block = legacy(id);
        DoorLayout.Assembly door = new DoorLayout.Assembly(anchor, Direction.NORTH, hinge, DoorDesign.ROUND);
        return rawDoor(helper, door, new Block[] {block, block, block, block}, open, anchorSnbt);
    }

    /** A saved 0.4.0 trim of one id (no block entity existed in 0.4.0). */
    static TrimLayout.Assembly rawTrim(GameTestHelper helper, BlockPos anchor, String id) {
        Block block = legacy(id);
        TrimLayout.Assembly trim = new TrimLayout.Assembly(anchor, Direction.NORTH);
        for (Quarter part : Quarter.values()) {
            raw(helper, trim.pos(part), trim.stateFor(part, block));
        }
        return trim;
    }

    /** Clears cells silently (no teardown, no drops). */
    static void wipe(GameTestHelper helper, Iterable<BlockPos> cells) {
        for (BlockPos pos : cells) {
            helper.getLevel().removeBlockEntity(pos);
            raw(helper, pos, Blocks.AIR.defaultBlockState());
        }
    }

    /** Fires Roundwood's chunk-load handler for the chunks holding {@code cells}, then its end-of-tick processing. */
    static void loadAndTick(GameTestHelper helper, Iterable<BlockPos> cells) {
        ServerLevel level = helper.getLevel();
        Set<Long> chunks = new HashSet<>();
        for (BlockPos pos : cells) {
            LevelChunk chunk = level.getChunkAt(pos);
            if (chunks.add(chunk.getPos().toLong())) {
                ChunkMigrations.onChunkLoad(new ChunkEvent.Load(chunk, false));
            }
        }
        ChunkMigrations.onLevelTick(new TickEvent.LevelTickEvent(LogicalSide.SERVER, TickEvent.Phase.END, level, () -> true));
    }

    static boolean isCanonicalDoor(BlockState state) {
        return state.getBlock() instanceof RoundDoorBlock && !LegacyChunkMigration.isLegacy(state);
    }

    static boolean isCanonicalTrim(BlockState state) {
        return state.getBlock() instanceof RoundTrimBlock && !LegacyChunkMigration.isLegacy(state);
    }
}
