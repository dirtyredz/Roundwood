package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.frontHit;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.hold;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeOakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.use;

/**
 * Applying, replacing and clearing camouflage through the real item-use path (main hand, then
 * offhand, as the client sends them), and its precedence over the ordinary toggle.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CamouflageInteractionGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final double[][] CORNERS = {{0.1, 0.1}, {1.9, 0.1}, {0.1, 1.9}, {1.9, 1.9}};

    private CamouflageInteractionGameTests() {
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, Direction facing, DoorHingeSide hinge, String name) {
        return placeOakDoor(helper, ANCHOR, facing, hinge, name + "_placer");
    }

    private static Optional<BlockState> material(GameTestHelper helper, DoorLayout.Assembly assembly) {
        return anchorEntity(helper, assembly).camouflage().map(CamouflageData::material);
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    private static boolean open(GameTestHelper helper, DoorLayout.Assembly assembly) {
        return helper.getLevel().getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN);
    }

    @GameTest(template = TEMPLATE)
    public static void applyReplaceAndClearRefundExactlyOnce(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "cycle");
        ServerPlayer player = player(helper, "cycle", GameType.SURVIVAL, Direction.NORTH);
        BlockHitResult corner = frontHit(assembly, 0.1, 0.1);

        hold(player, new ItemStack(Items.STONE, 2), ItemStack.EMPTY);
        check(use(helper, player, corner).consumesAction(), "applying was not handled");
        check(material(helper, assembly).equals(Optional.of(Blocks.STONE.defaultBlockState())), "stone not applied");
        check(player.getMainHandItem().getCount() == 1, "survival should consume one item, left " + player.getMainHandItem());
        check(anchorEntity(helper, assembly).camouflage().orElseThrow().paid(), "no refund stored");
        check(!open(helper, assembly), "applying toggled the door");

        // The same material again: nothing happens, nothing is consumed.
        use(helper, player, frontHit(assembly, 1.9, 1.9));
        check(player.getMainHandItem().getCount() == 1, "re-applying the same material consumed an item");

        // Replacing gives the stone back after the bricks are in.
        hold(player, new ItemStack(Items.BRICKS), ItemStack.EMPTY);
        use(helper, player, frontHit(assembly, 1.9, 0.1));
        check(material(helper, assembly).equals(Optional.of(Blocks.BRICKS.defaultBlockState())), "bricks did not replace stone");
        check(count(player, Items.STONE) == 1, "the replaced stone was not refunded: " + count(player, Items.STONE));
        check(count(player, Items.BRICKS) == 0, "bricks not consumed");

        // Sneaking with both hands empty clears it and refunds the bricks.
        hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
        player.getInventory().clearContent();
        player.setShiftKeyDown(true);
        check(use(helper, player, frontHit(assembly, 0.1, 1.9)).consumesAction(), "clearing was not handled");
        player.setShiftKeyDown(false);
        check(material(helper, assembly).isEmpty(), "sneak-clear left the camouflage");
        check(count(player, Items.BRICKS) == 1, "clearing did not refund the bricks");
        check(!open(helper, assembly), "clearing toggled the door");

        // Clearing again refunds nothing more (and, with nothing filled, the corner is not a clear target).
        player.getInventory().clearContent();
        player.setShiftKeyDown(true);
        use(helper, player, frontHit(assembly, 0.1, 1.9));
        player.setShiftKeyDown(false);
        check(count(player, Items.BRICKS) == 0, "a second clear refunded again");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyCornerOfEveryFacingAndHingeAcceptsAMaterial(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                String name = facing + "/" + hinge;
                DoorLayout.Assembly assembly = place(helper, facing, hinge, "every_" + facing + "_" + hinge);
                ServerPlayer player = player(helper, "every", GameType.CREATIVE, facing);
                for (double[] c : CORNERS) {
                    anchorEntity(helper, assembly).setCamouflage(null);
                    hold(player, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY);
                    use(helper, player, frontHit(assembly, c[0], c[1]));
                    check(material(helper, assembly).equals(Optional.of(Blocks.OAK_PLANKS.defaultBlockState())),
                            name + ": corner " + c[0] + "," + c[1] + " did not take the material");
                    check(!anchorEntity(helper, assembly).camouflage().orElseThrow().paid(), name + ": creative stored a refund");
                    check(player.getMainHandItem().getCount() == 1, name + ": creative consumed the item");
                    check(!open(helper, assembly), name + ": applying toggled the door");
                }
                helper.setBlock(ANCHOR, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void theOffhandAppliesWhenTheMainHandIsEmpty(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "offhand");
        ServerPlayer player = player(helper, "offhand", GameType.SURVIVAL, Direction.NORTH);
        BlockHitResult corner = frontHit(assembly, 1.9, 1.9);
        hold(player, ItemStack.EMPTY, new ItemStack(Items.STONE, 3));
        // The main hand passes so the offhand gets its turn; the door must not toggle on the way.
        check(!player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, corner).consumesAction(),
                "the empty main hand did not pass to the offhand");
        check(!open(helper, assembly), "the main hand toggled the door");
        use(helper, player, corner);
        check(material(helper, assembly).equals(Optional.of(Blocks.STONE.defaultBlockState())), "the offhand did not apply");
        check(player.getOffhandItem().getCount() == 2, "the offhand item was not consumed once");
        // A non-block main-hand item keeps the ordinary toggle, even with a block in the offhand.
        hold(player, new ItemStack(Items.STICK), new ItemStack(Items.BRICKS));
        use(helper, player, corner);
        check(open(helper, assembly), "a stick in the main hand did not toggle");
        check(material(helper, assembly).equals(Optional.of(Blocks.STONE.defaultBlockState())), "the offhand overrode the main hand");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unacceptableBlocksAreRefusedWithoutToggleOrPlacement(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "invalid");
        ServerPlayer player = player(helper, "invalid", GameType.SURVIVAL, Direction.NORTH);
        BlockHitResult corner = frontHit(assembly, 0.1, 0.1);
        for (Item item : List.of(Items.GLASS, Items.OAK_SLAB, Items.CHEST, Items.GLOWSTONE, Items.TORCH, oakDoor().asItem())) {
            hold(player, new ItemStack(item, 2), ItemStack.EMPTY);
            check(use(helper, player, corner).consumesAction(), item + ": the attempt was not handled");
            check(material(helper, assembly).isEmpty(), item + " was accepted");
            check(player.getMainHandItem().getCount() == 2, item + " was consumed");
            check(!open(helper, assembly), item + " toggled the door");
            BlockPos front = assembly.anchor().relative(Direction.SOUTH);
            check(helper.getLevel().getBlockState(front).isAir(), item + " was placed in front of the door");
        }
        ItemStack tagged = new ItemStack(Items.STONE);
        tagged.getOrCreateTag().put("BlockStateTag", new CompoundTag());
        hold(player, tagged, ItemStack.EMPTY);
        use(helper, player, corner);
        check(material(helper, assembly).isEmpty(), "an item with a BlockStateTag was accepted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void leafClicksAndPlainClicksKeepTheToggle(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "precedence");
        ServerPlayer player = player(helper, "precedence", GameType.SURVIVAL, Direction.NORTH);
        // A block item on the leaf toggles and is neither applied nor placed.
        hold(player, new ItemStack(Items.STONE, 2), ItemStack.EMPTY);
        check(use(helper, player, frontHit(assembly, 0.6, 0.9)).consumesAction(), "leaf click not handled");
        check(open(helper, assembly), "a block item on the leaf did not toggle");
        check(material(helper, assembly).isEmpty() && player.getMainHandItem().getCount() == 2, "a leaf click applied a material");
        use(helper, player, frontHit(assembly, 0.6, 0.9));
        check(!open(helper, assembly), "second leaf click did not close");
        // Filled corners with an empty hand toggle; crouching on the leaf with empty hands still toggles.
        anchorEntity(helper, assembly).setCamouflage(CamouflageData.free(Blocks.STONE.defaultBlockState()));
        hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
        use(helper, player, frontHit(assembly, 0.1, 0.1));
        check(open(helper, assembly), "an empty-hand click on a filled corner did not toggle");
        player.setShiftKeyDown(true);
        use(helper, player, frontHit(assembly, 0.6, 0.9));
        player.setShiftKeyDown(false);
        check(!open(helper, assembly), "crouching on the leaf did not toggle");
        check(material(helper, assembly).isPresent(), "crouching on the leaf cleared the camouflage");
        assertAssembly(helper, assembly, oakDoor(), false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openDoorsTakeMaterialOnTheirStaticCorners(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, Direction.NORTH, DoorHingeSide.RIGHT, "open_apply");
        oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(assembly.anchor()), assembly.anchor(), true);
        ServerPlayer player = player(helper, "open_apply", GameType.SURVIVAL, Direction.NORTH);
        hold(player, new ItemStack(Items.STONE), ItemStack.EMPTY);
        use(helper, player, frontHit(assembly, 0.1, 1.9));
        check(material(helper, assembly).isPresent(), "an open door's corner did not take the material");
        check(open(helper, assembly), "applying closed the door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playersWhoMayNotBuildCannotApply(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "adventure");
        ServerPlayer player = player(helper, "adventure", GameType.ADVENTURE, Direction.NORTH);
        hold(player, new ItemStack(Items.STONE), ItemStack.EMPTY);
        use(helper, player, frontHit(assembly, 0.1, 0.1));
        check(material(helper, assembly).isEmpty(), "an adventure-mode player applied a material");
        check(player.getMainHandItem().getCount() == 1, "an adventure-mode player's item was consumed");
        check(!open(helper, assembly), "a refused application toggled the door");
        helper.succeed();
    }
}
