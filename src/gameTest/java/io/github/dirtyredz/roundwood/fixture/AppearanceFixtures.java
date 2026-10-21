package io.github.dirtyredz.roundwood.fixture;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Appearance helpers for the GameTests: named looks, real (optionally sneaking) use through the
 * server interaction path (so Forge's right-click event, the sneak bypass and the block's router all
 * run), where to aim on a door, reading back what an object shows, and pick-block.
 */
public final class AppearanceFixtures {
    public static final Appearance SPRUCE = wood("spruce_planks");
    public static final Appearance BIRCH = wood("birch_planks");
    public static final Appearance WARPED = wood("warped_planks");
    /** Not a vanilla plank: the GameTest resources add it to {@code roundwood:appearance_planks}, standing in for a modded wood. */
    public static final Appearance MUSHROOM = wood("mushroom_stem");
    public static final Appearance BRICKS = Appearance.masonry(id("bricks"));
    public static final Appearance STONE_BRICKS = Appearance.masonry(id("stone_bricks"));

    private AppearanceFixtures() {
    }

    public static Appearance wood(String block) {
        return Appearance.wood(id(block));
    }

    public static Appearance oakPainted(DyeColor dye) {
        return Appearance.DEFAULT.painted(dye);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    /**
     * Uses what {@code player} holds on {@code hit}, main hand then offhand as the client sends them,
     * through {@code ServerPlayerGameMode.useItemOn}, sneaking as given. Returns the deciding result.
     */
    public static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockHitResult hit, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        try {
            return DoorFixtures.use(helper, player, hit);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    /** Puts {@code main} and {@code off} in the hands and uses them on {@code hit}. */
    public static InteractionResult use(GameTestHelper helper, ServerPlayer player, ItemStack main, ItemStack off,
                                        BlockHitResult hit, boolean sneaking) {
        DoorFixtures.hold(player, main, off);
        return use(helper, player, hit, sneaking);
    }

    /** The middle of the door's front face: the leaf, closed or open (the open leaf still covers the hinge column). */
    public static BlockHitResult leaf(DoorLayout.Assembly door, boolean open) {
        return DoorFixtures.frontHit(door, open ? (door.hinge() == net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT
                ? 0.08 : 1.92) : 1.0, 1.0);
    }

    /** A static corner of the door's front face (lower left). */
    public static BlockHitResult corner(DoorLayout.Assembly door) {
        return DoorFixtures.frontHit(door, 0.1, 0.1);
    }

    public static Appearance doorLook(GameTestHelper helper, DoorLayout.Assembly door) {
        if (helper.getLevel().getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity) {
            return entity.appearance();
        }
        throw new GameTestAssertException("no door anchor entity at " + door.anchor());
    }

    public static Appearance trimLook(GameTestHelper helper, TrimLayout.Assembly trim) {
        if (helper.getLevel().getBlockEntity(trim.anchor()) instanceof RoundTrimBlockEntity entity) {
            return entity.appearance();
        }
        throw new GameTestAssertException("no trim anchor entity at " + trim.anchor());
    }

    /** {@code pos} is absolute. */
    public static Appearance windowLook(GameTestHelper helper, BlockPos pos) {
        if (helper.getLevel().getBlockEntity(pos) instanceof RoundWindowBlockEntity entity) {
            return entity.appearance();
        }
        throw new GameTestAssertException("no window entity at " + pos);
    }

    /** Pick-block on the absolute {@code pos}, as the client asks for it. */
    public static ItemStack pick(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getLevel().getBlockState(pos);
        return state.getBlock().getCloneItemStack(helper.getLevel(), pos, state);
    }
}
