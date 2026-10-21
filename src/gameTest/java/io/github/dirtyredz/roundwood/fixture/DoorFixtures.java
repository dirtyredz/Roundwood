package io.github.dirtyredz.roundwood.fixture;

import com.mojang.authlib.GameProfile;
import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.door.assembly.DoorLayout;
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
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
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

    /**
     * Places a door whose hinge is picked by the click point: 0.25 of the way along the player's
     * clockwise axis inside the anchor cell gives LEFT, 0.75 gives RIGHT (nothing else decides it in open space).
     */
    public static InteractionResult placeDoor(GameTestHelper helper, ServerPlayer player, Item item, BlockPos anchor,
                                              DoorHingeSide hinge) {
        Direction across = player.getDirection().getClockWise();
        double along = hinge == DoorHingeSide.LEFT ? 0.25 : 0.75;
        BlockPos floor = helper.absolutePos(anchor.below());
        Vec3 hit = Vec3.atCenterOf(floor).add(0, 0.5, 0)
                .add(across.getStepX() * (along - 0.5), 0, across.getStepZ() * (along - 0.5));
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, floor, false)));
    }

    /** Clicks a block with an empty hand through the real server interaction path. */
    public static InteractionResult click(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos abs = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        return player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
    }

    /**
     * A world point in an assembly's own frame: {@code a} blocks across from the left edge (0..2),
     * {@code h} blocks up (0..2), {@code d} blocks deep from the front face (the placing player's
     * side, 0) into the cells (1 = back of the cells; 1..2 is where the open leaf projects).
     */
    public static Vec3 point(DoorLayout.Assembly assembly, double a, double h, double d) {
        Direction facing = assembly.facing();
        Direction across = facing.getClockWise();
        Direction out = facing.getOpposite();
        BlockPos anchor = assembly.anchor();
        // Start at the anchor cell's front-left-bottom corner.
        double x = anchor.getX() + (across.getStepX() < 0 || out.getStepX() > 0 ? 1 : 0);
        double z = anchor.getZ() + (across.getStepZ() < 0 || out.getStepZ() > 0 ? 1 : 0);
        x += across.getStepX() * a - out.getStepX() * d;
        z += across.getStepZ() * a - out.getStepZ() * d;
        return new Vec3(x, anchor.getY() + h, z);
    }

    /**
     * A hit on the assembly's front face (d = 0) at ({@code a}, {@code h}), credited to the cell
     * that contains it, facing the placing player: what a player looking straight at it sends.
     */
    public static BlockHitResult frontHit(DoorLayout.Assembly assembly, double a, double h) {
        Vec3 location = point(assembly, a, h, 0);
        Direction out = assembly.facing().getOpposite();
        BlockPos cell = BlockPos.containing(location.subtract(Vec3.atLowerCornerOf(out.getNormal()).scale(1e-4)));
        return new BlockHitResult(location, out, cell, false);
    }

    /**
     * Uses whatever the player holds on {@code hit} the way the client does: the main hand, then the
     * offhand if the main hand did not act, each through the real server path
     * ({@code ServerPlayerGameMode.useItemOn}). Returns the deciding result.
     */
    public static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockHitResult hit) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = player.gameMode.useItemOn(player, helper.getLevel(), stack, hand, hit);
            if (result.consumesAction()) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    /** Puts {@code main} and {@code off} in the player's hands (either may be {@link ItemStack#EMPTY}). */
    public static void hold(ServerPlayer player, ItemStack main, ItemStack off) {
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
    }

    /** The round-door block entity at the assembly's anchor (fails the test if missing). */
    public static RoundDoorBlockEntity anchorEntity(GameTestHelper helper, DoorLayout.Assembly assembly) {
        if (helper.getLevel().getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity) {
            return entity;
        }
        throw new GameTestAssertException("no round-door block entity at the anchor " + assembly.anchor());
    }

    /**
     * Absolute to relative. {@code GameTestHelper.relativePos} is not the inverse of {@code absolutePos}
     * in 1.20.1, so tests use this instead.
     */
    public static BlockPos rel(GameTestHelper helper, BlockPos absolute) {
        return absolute.subtract(helper.absolutePos(BlockPos.ZERO));
    }

    /** The assembly anchored at a relative position, in absolute coordinates. */
    public static DoorLayout.Assembly assembly(GameTestHelper helper, BlockPos anchor, Direction facing, DoorHingeSide hinge) {
        return new DoorLayout.Assembly(helper.absolutePos(anchor), facing, hinge);
    }

    /** As above with the hinge a centre click gives in open space. */
    public static DoorLayout.Assembly assembly(GameTestHelper helper, BlockPos anchor, Direction facing) {
        return assembly(helper, anchor, facing, DoorHingeSide.LEFT);
    }

    /** How many of the assembly's four cells hold a round-door block entity. */
    public static int blockEntities(GameTestHelper helper, DoorLayout.Assembly assembly) {
        int count = 0;
        for (BlockPos pos : assembly.positions()) {
            if (helper.getLevel().getBlockEntity(pos) instanceof RoundDoorBlockEntity) {
                count++;
            }
        }
        return count;
    }

    /** Asserts exactly one block entity in the assembly, at the anchor. */
    public static void assertOneAnchorEntity(GameTestHelper helper, DoorLayout.Assembly assembly) {
        int count = blockEntities(helper, assembly);
        check(count == 1, "expected exactly one door block entity, found " + count);
        check(helper.getLevel().getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity, "block entity is not at the anchor");
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
