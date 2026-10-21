package io.github.dirtyredz.roundwood.fixture;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.point;

/**
 * Frame helpers for the GameTests: the blocks, real item use for freestanding and fitted placement
 * (through {@code ServerPlayerGameMode.useItemOn}, so sneaking, block use and Forge's placement
 * capture all apply), precise hits and assertions.
 */
public final class TrimFixtures {
    private TrimFixtures() {
    }

    /** Brick masonry, the look the masonry tests give the (one, canonical) trim. */
    public static final Appearance BRICK = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));

    /** The canonical round trim; its default look is oak. */
    public static RoundTrimBlock oakTrim() {
        return ModBlocks.roundTrim();
    }

    /** One trim item in brick masonry. */
    public static ItemStack brickStack() {
        return AppearanceStacks.create(oakTrim().asItem(), BRICK);
    }

    /** The frame anchored at a relative position, in absolute coordinates. */
    public static TrimLayout.Assembly trimAssembly(GameTestHelper helper, BlockPos anchor, Direction facing) {
        return new TrimLayout.Assembly(helper.absolutePos(anchor), facing);
    }

    /** The frame that fits a door: same facing, one block towards the door's front. */
    public static TrimLayout.Assembly fittedTo(DoorLayout.Assembly door) {
        return new TrimLayout.Assembly(door.anchor().relative(door.facing().getOpposite()), door.facing());
    }

    /**
     * Uses {@code stack} (in the main hand) on the block at {@code clickedAbs} with the given face and
     * hit point, through the real server path, optionally sneaking. Returns the result.
     */
    public static InteractionResult useAt(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos clickedAbs,
                                          Direction face, Vec3 location, boolean sneaking) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(sneaking);
        try {
            return player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                    new BlockHitResult(location, face, clickedAbs, false));
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    /** Freestanding placement: clicks the top of the floor block under {@code anchor} (relative), not sneaking. */
    public static InteractionResult placeFreestanding(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos anchor) {
        BlockPos floor = helper.absolutePos(anchor.below());
        return useAt(helper, player, stack, floor, Direction.UP, Vec3.atCenterOf(floor).add(0, 0.5, 0), false);
    }

    /**
     * Fitted placement: sneak-uses the stack on the door's front face at ({@code a}, {@code h}) in the
     * door's frame (blocks across from its left edge, blocks up), credited to the containing part.
     */
    public static InteractionResult fitToDoor(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                              DoorLayout.Assembly door, double a, double h) {
        BlockHitResult hit = DoorFixtures.frontHit(door, a, h);
        return useAt(helper, player, stack, hit.getBlockPos(), hit.getDirection(), hit.getLocation(), true);
    }

    /** A hit on the frame's front face at ({@code a}, {@code h}), as a player standing in front sends it. */
    public static BlockHitResult frontHit(TrimLayout.Assembly trim, double a, double h) {
        // The frame's front face is one pixel in front of the back of its cells: d = 15/16 from the cells' front.
        DoorLayout.Assembly frame = new DoorLayout.Assembly(trim.anchor(), trim.facing(), DoorHingeSide.LEFT);
        Vec3 location = point(frame, a, h, 15.0 / 16);
        Direction out = trim.facing().getOpposite();
        BlockPos cell = BlockPos.containing(location.add(Vec3.atLowerCornerOf(trim.facing().getNormal()).scale(1e-4)));
        return new BlockHitResult(location, out, cell, false);
    }

    /** Asserts all four quarters are present for {@code block}. */
    public static void assertTrim(GameTestHelper helper, TrimLayout.Assembly assembly, RoundTrimBlock block) {
        for (TrimPart part : TrimPart.values()) {
            BlockState state = helper.getLevel().getBlockState(assembly.pos(part));
            check(assembly.isPart(state, block, part), part + " missing or wrong at " + assembly.pos(part) + ": " + state);
        }
    }

    /** Asserts no frame quarter of any material is left in the assembly's cells. */
    public static void assertNoTrim(GameTestHelper helper, TrimLayout.Assembly assembly) {
        for (BlockPos pos : assembly.positions()) {
            BlockState state = helper.getLevel().getBlockState(pos);
            check(!(state.getBlock() instanceof RoundTrimBlock), "frame quarter left at " + pos + ": " + state);
        }
    }
}
