package io.github.dirtyredz.roundwood.block.window.design;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.windowLook;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * Cross-beam variants (0.7.0): each crossed item places its own crossed block with exactly the plain
 * piece's targeting (centred and flush edge zones agree with the preview), keeps its pattern through
 * restyling, picks and drops, and a flush crossed window still grows no glass bridge.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowMuntinGameTests {
    private WindowMuntinGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void plainPanesWithFittedBeamsKeepPlacementPickAndSeparateDrops(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "cross_place", GameType.SURVIVAL, Direction.NORTH);
        BlockPos at = new BlockPos(4, 1, 4);
        Appearance look = AppearanceFixtures.oakPainted(DyeColor.PURPLE);
        for (WindowDesign design : WindowDesign.all()) {
            if (design.pattern() != WindowPattern.CROSS) {
                continue;
            }
            WindowDesign plain = WindowDesign.plain(design.profile());
            for (WindowTargetZones.Zone zone : new WindowTargetZones.Zone[] {WindowTargetZones.Zone.CENTER, WindowTargetZones.Zone.U_MIN}) {
                var hit = WindowFixtures.zone(helper, at.below(), Direction.UP, zone);
                BlockState plainPredicted = WindowFixtures.predict(player, new ItemStack(ModItems.window(plain)), hit);
                ItemStack stack = styled(ModItems.window(plain), look);
                check(WindowFixtures.place(player, stack, hit).consumesAction(), design.id() + " not placed");
                check(io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions.install(helper.getLevel(), helper.absolutePos(at),
                        io.github.dirtyredz.roundwood.block.attachment.TrimSlot.CROSS_BEAMS, io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.FRONT, look), "beams not fitted");
                BlockState placed = helper.getBlockState(at);
                check(placed.is(ModBlocks.window(plain)), design.id() + ": placed " + placed);
                check(WindowFixtures.pose(placed).equals(WindowFixtures.pose(plainPredicted)), design.id() + " " + zone
                        + ": pose differs from the plain piece's " + placed);
                check(look.equals(windowLook(helper, helper.absolutePos(at))), design.id() + ": look lost");
                if (zone != WindowTargetZones.Zone.CENTER) {
                    check(WindowPaneBlock.connections(placed).isEmpty(), design.id() + ": a flush crossed window connected " + placed);
                }
                ItemStack pick = AppearanceFixtures.pick(helper, helper.absolutePos(at));
                check(pick.is(ModItems.window(plain)) && AppearanceStacks.read(pick).equals(look), design.id() + ": pick " + pick);
                clearDrops(helper);
                player.gameMode.destroyBlock(helper.absolutePos(at));
                check(dropped(helper, ModItems.window(plain), look) == 1 && dropped(helper, ModItems.CROSS_BEAMS.get(), look) == 1,
                        design.id() + ": drop");
                clearDrops(helper);
                helper.setBlock(at, Blocks.AIR);
            }
        }
        helper.succeed();
    }
}
