package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.hold;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.point;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fitToDoor;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fittedTo;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;

/**
 * A legacy frame in use with its door (frames are compatibility content, written by the fixture
 * writer): rays (and clicks) passing through the open centre and the corners to the door, only the
 * ring quarter itself being a target, and camouflage still reachable.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimInteractionGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private TrimInteractionGameTests() {
    }

    private static DoorLayout.Assembly door(GameTestHelper helper, Direction facing, String name) {
        ServerPlayer placer = player(helper, name, GameType.SURVIVAL, facing);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), name + ": door");
        return assembly(helper, ANCHOR, facing, DoorHingeSide.LEFT);
    }

    private static TrimLayout.Assembly fit(GameTestHelper helper, DoorLayout.Assembly door, String name) {
        ServerPlayer fitter = player(helper, name + "_fitter", GameType.SURVIVAL, door.facing());
        check(fitToDoor(helper, fitter, new ItemStack(oakTrim().asItem()), door, 0.5, 0.5).consumesAction(), name + ": fitting");
        return fittedTo(door);
    }

    /** A ray from well in front of the frame, straight at the door's front plane at (a, h). */
    private static BlockHitResult ray(GameTestHelper helper, DoorLayout.Assembly door, double a, double h, Entity entity) {
        return helper.getLevel().clip(new ClipContext(point(door, a, h, -2.5), point(door, a, h, 0.5),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
    }

    private static boolean onDoor(GameTestHelper helper, BlockHitResult hit) {
        return hit.getType() == HitResult.Type.BLOCK && helper.getLevel().getBlockState(hit.getBlockPos()).getBlock() instanceof RoundDoorBlock;
    }

    @GameTest(template = TEMPLATE)
    public static void raysPassThroughTheApertureAndOnlyTheRingIsATarget(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            DoorLayout.Assembly door = door(helper, facing, "aperture_" + facing);
            TrimLayout.Assembly trim = fit(helper, door, "aperture_" + facing);
            assertTrim(helper, trim, oakTrim());
            // The open centre and a corner (outside the ring): straight through to the door.
            for (double[] p : new double[][] {{1.0, 1.0}, {0.6, 1.4}, {1.4, 0.6}}) {
                BlockHitResult hit = ray(helper, door, p[0], p[1], null);
                check(onDoor(helper, hit), facing + ": the aperture at " + p[0] + "," + p[1] + " did not reach the door: " + hit);
            }
            // The ring itself, in each quarter: credited to that quarter's own cell.
            double edge = 0.5 / 16; // half a pixel in from the outer edge, at mid height or mid width
            double[][] ring = {{edge, 0.9}, {2 - edge, 0.9}, {edge, 1.1}, {2 - edge, 1.1}};
            TrimPart[] parts = {TrimPart.LOWER_LEFT, TrimPart.LOWER_RIGHT, TrimPart.UPPER_LEFT, TrimPart.UPPER_RIGHT};
            for (int i = 0; i < ring.length; i++) {
                BlockHitResult hit = ray(helper, door, ring[i][0], ring[i][1], null);
                check(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(trim.pos(parts[i])),
                        facing + ": the ring at " + ring[i][0] + "," + ring[i][1] + " was not targeted on " + parts[i] + ": " + hit.getBlockPos());
            }
            helper.getLevel().setBlockAndUpdate(trim.anchor(), Blocks.AIR.defaultBlockState());
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clicksThroughTheApertureOpenTheDoorAndCamouflageStillApplies(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly door = door(helper, Direction.NORTH, "through");
        TrimLayout.Assembly trim = fit(helper, door, "through");
        ServerPlayer player = player(helper, "through_user", GameType.SURVIVAL, Direction.NORTH);
        // Empty hand through the centre: the resolved hit opens the door.
        hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
        BlockHitResult centre = DoorHitResolver.resolve(helper.getLevel(), ray(helper, door, 0.7, 1.2, player)).orElseThrow();
        check(onDoor(helper, centre), "the centre ray did not land on the door");
        check(use(helper, player, centre).consumesAction(), "the click through the aperture did nothing");
        check(helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN), "the door did not open through the frame");
        oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(door.anchor()), door.anchor(), false);
        // A block in hand at a corner (outside the ring): the camouflage is applied to the door.
        hold(player, new ItemStack(Items.STONE_BRICKS), ItemStack.EMPTY);
        BlockHitResult corner = DoorHitResolver.resolve(helper.getLevel(), ray(helper, door, 0.1, 0.1, player)).orElseThrow();
        check(onDoor(helper, corner), "the corner ray did not land on the door: " + corner);
        check(use(helper, player, corner).consumesAction(), "camouflage was not applied through the frame");
        check(anchorEntity(helper, door).camouflage().map(c -> c.material().is(Blocks.STONE_BRICKS)).orElse(false),
                "the door has no stone-brick camouflage");
        assertTrim(helper, trim, oakTrim());
        helper.succeed();
    }
}
