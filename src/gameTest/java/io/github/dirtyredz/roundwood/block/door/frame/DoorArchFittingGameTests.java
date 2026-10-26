package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.*;

/** Reproduction through the ordinary server click path, including block interaction. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorArchFittingGameTests {
    @GameTest(template = TEMPLATE)
    public static void doorFitsExistingTopDespiteOccupiedNoncollidingCell(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(3, 1, 3));
        var top = new ArchPieceLayout.Assembly(anchor.above(), Direction.NORTH,
                ArchPieceDesign.TOP_1X1, Side.LEFT);
        var block = ModBlocks.archPiece(top.design());
        check(ArchPiecePlacement.placeAt(level, block, top, Appearance.DEFAULT), "top fixture refused");
        check(level.getBlockState(top.anchor()).getCollisionShape(level, top.anchor()).isEmpty(),
                "reproduction requires empty top collision");
        var player = player(helper, "arch_fit", GameType.SURVIVAL, Direction.NORTH);
        var stack = new ItemStack(ModBlocks.archedDoor().asItem(), 2);
        // A solid crown pixel on the front face; the ordinary click must derive the lower door anchor.
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(top.anchor()).add(.03125, .96875, 1),
                Direction.SOUTH, top.anchor(), false);
        AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, hit, false);
        check(level.getBlockState(anchor).is(ModBlocks.archedDoor()),
                "matching top with empty collision rejected ordinary door fitting");
        check(stack.getCount() == 1, "fitting must consume exactly one door");
        helper.succeed();
    }
}
