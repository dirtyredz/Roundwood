package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.DoorRays;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorArchCamouflageGameTests {
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void bothOrdersKeepLowerCornerAndRefundOnce(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        for (var hinge : DoorHingeSide.values()) for (var depth : DoorDepth.values())
            for (var thickness : ArchPieceThickness.values()) for (boolean camouflageFirst : new boolean[]{false, true}) {
            var door = new DoorLayout.Assembly(helper.absolutePos(new BlockPos(3, 1, 3)), Direction.NORTH, hinge, DoorDesign.ROUND, depth);
            check(DoorPlacement.placeAt(level, ModBlocks.roundDoor(), door, false), "door fixture");
            var player = player(helper, "arch_camo", GameType.SURVIVAL, Direction.NORTH);
            var stone = new ItemStack(Items.STONE, 2);
            if (camouflageFirst) {
                hold(player, stone, ItemStack.EMPTY);
                AppearanceFixtures.use(helper, player, stone, ItemStack.EMPTY, DoorRays.through(helper, player, door, .03125, .03125, true), false);
            }
            var slab = thickness == ArchPieceThickness.FULL ? ArchPieceDepth.FRONT
                    : depth == DoorDepth.BACK ? ArchPieceDepth.FRONT : ArchPieceDepth.BACK;
            var top = new ItemStack(ModBlocks.archPiece(DoorArchFitPlan.topFor(door.design()), thickness), 2);
            hold(player, top, ItemStack.EMPTY);
            double h = slab == ArchPieceDepth.FRONT ? .875 : .125;
            var hit = DoorRays.through(helper, player, door, .9, h, true);
            AppearanceFixtures.use(helper, player, top, ItemStack.EMPTY, hit, false);
            var entity = anchorEntity(helper, door);
            check(entity.arch().isPresent() && entity.arch().get().depth() == slab && top.getCount() == 1, "arch fitting/payment");
            if (!camouflageFirst) {
                hold(player, stone, ItemStack.EMPTY);
                AppearanceFixtures.use(helper, player, stone, ItemStack.EMPTY, DoorRays.through(helper, player, door, .03125, .03125, true), false);
            }
            check(stone.getCount() == 1 && entity.camouflage().isPresent(), "lower corner acquisition/payment");
            var saved = entity.saveWithFullMetadata();
            entity.load(saved);
            check(entity.camouflage().orElseThrow().paid() && entity.arch().isPresent(), "reload lost custody");
            player.getInventory().clearContent();
            hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
            var clear = DoorRays.through(helper, player, door, .03125, .03125, true);
            AppearanceFixtures.use(helper, player, ItemStack.EMPTY, ItemStack.EMPTY, clear, true);
            check(entity.camouflage().isEmpty() && entity.arch().isPresent(), "clear disturbed fitted frame");
            check(player.getInventory().countItem(Items.STONE) == 1, "clear refund");
            level.removeBlock(door.anchor(), false);
            check(dropped(helper, Items.STONE) == 0, "second camouflage refund on teardown");
            clearDrops(helper);
        }
        helper.succeed();
    }
}
