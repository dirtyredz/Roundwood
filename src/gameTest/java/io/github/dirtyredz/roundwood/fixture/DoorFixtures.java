package io.github.dirtyredz.roundwood.fixture;

import com.mojang.authlib.GameProfile;
import io.github.dirtyredz.roundwood.door.DoorLayout;
import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Shared world and player setup for the GameTests: a stone floor, fake players, real item use. */
public final class DoorFixtures {
    /** 9 x 6 x 9 of air. */
    public static final String TEMPLATE = "empty";
    /** 17 x 5 x 4 of air: always spans a chunk boundary. */
    public static final String STRIP = "strip";

    private DoorFixtures() {
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /** Stone at relative y = 0 over {@code [0,sizeX) x [0,sizeZ)}; everything is built from y = 1. */
    public static void floor(GameTestHelper helper, int sizeX, int sizeZ) {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    public static void floor(GameTestHelper helper) {
        floor(helper, 9, 9);
    }

    public static RoundDoorBlock oakDoor() {
        return ModBlocks.door(FinishCatalog.PAINT_BASE);
    }

    /** A fake server player in the given game mode, facing {@code facing}, with an empty inventory. */
    public static ServerPlayer player(GameTestHelper helper, String name, GameType mode, Direction facing) {
        ServerLevel level = helper.getLevel();
        UUID id = UUID.nameUUIDFromBytes(("roundwood:" + name).getBytes(StandardCharsets.UTF_8));
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(id, "rw_" + name));
        player.getInventory().clearContent();
        player.setGameMode(mode);
        BlockPos stand = helper.absolutePos(new BlockPos(0, 4, 0));
        player.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, facing.toYRot(), 0);
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
        return player;
    }

    /**
     * Uses {@code stack} on a face of the block at {@code clicked} (relative), through
     * {@link ItemStack#useOn}, so Forge's placement capture and events apply.
     */
    public static InteractionResult useItemOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                              BlockPos clicked, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(clicked);
        Vec3 hit = Vec3.atCenterOf(abs).relative(face, 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    /** Places a door with the anchor (lower-left) at {@code anchor} by clicking the floor below it. */
    public static InteractionResult placeDoor(GameTestHelper helper, ServerPlayer player, Item item, BlockPos anchor) {
        return useItemOn(helper, player, new ItemStack(item), anchor.below(), Direction.UP);
    }

    /** Clicks a block with an empty hand through the real server interaction path. */
    public static InteractionResult click(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos abs = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        return player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
    }

    /**
     * Absolute to relative. {@code GameTestHelper.relativePos} is not the inverse of {@code absolutePos}
     * in 1.20.1, so tests use this instead.
     */
    public static BlockPos rel(GameTestHelper helper, BlockPos absolute) {
        return absolute.subtract(helper.absolutePos(BlockPos.ZERO));
    }

    /** The assembly anchored at a relative position, in absolute coordinates. */
    public static DoorLayout.Assembly assembly(GameTestHelper helper, BlockPos anchor, Direction facing) {
        return new DoorLayout.Assembly(helper.absolutePos(anchor), facing);
    }

    /** Asserts all four parts are present with the expected open state. */
    public static void assertAssembly(GameTestHelper helper, DoorLayout.Assembly assembly, Block block, boolean open) {
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
            BlockState state = helper.getLevel().getBlockState(assembly.pos(part));
            check(assembly.isPart(state, block, part), part + " missing or wrong at " + assembly.pos(part) + ": " + state);
            check(state.getValue(DoorBlock.OPEN) == open, part + " open=" + state.getValue(DoorBlock.OPEN) + ", expected " + open);
        }
    }

    public static void assertNoDoor(GameTestHelper helper, DoorLayout.Assembly assembly) {
        for (BlockPos pos : assembly.positions()) {
            BlockState state = helper.getLevel().getBlockState(pos);
            check(!(state.getBlock() instanceof RoundDoorBlock), "door part left at " + pos + ": " + state);
        }
    }

    /** Total count of dropped items of {@code item} within the test area. */
    public static int dropped(GameTestHelper helper, Item item) {
        AABB area = area(helper);
        List<ItemEntity> entities = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.getItem().is(item));
        return entities.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    /** The largest template (17 x 6 x 9) plus a 1-block margin; tests are spaced further apart than that. */
    private static AABB area(GameTestHelper helper) {
        return new AABB(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(17, 6, 9))).inflate(1);
    }

    /** Removes every dropped item in the area, so a test can count from zero. */
    public static void clearDrops(GameTestHelper helper) {
        AABB area = area(helper);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
    }
}
