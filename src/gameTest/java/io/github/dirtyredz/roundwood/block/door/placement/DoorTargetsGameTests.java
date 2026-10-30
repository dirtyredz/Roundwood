package io.github.dirtyredz.roundwood.block.door.placement;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.door.DoorRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Bounded production-path placement, state migration and nine-cell lifecycle checks. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorTargetsGameTests {
    @GameTest(template = TEMPLATE)
    public static void tallDoorKeepsPaintTrimsRedstoneAndSingleStyledDrops(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var block = ModBlocks.tallDoubleArchedDoor();
        var look = Appearance.DEFAULT.painted(DyeColor.MAGENTA);
        var trimLook = Appearance.DEFAULT.painted(DyeColor.LIME);
        for (var broken : DoorLayout.Part.of(block.design())) {
            var assembly = new DoorLayout.Assembly(helper.absolutePos(new BlockPos(3,1,3)), Direction.NORTH,
                    DoorHingeSide.LEFT, block.design(), DoorDepth.MIDDLE);
            check(DoorPlacement.placeAt(level, block, assembly, false), "tall placement refused");
            var player = player(helper, "tall_lifecycle", GameType.SURVIVAL, Direction.NORTH);
            var dye = new ItemStack(Items.MAGENTA_DYE);
            AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY,
                    DoorRays.through(helper,player,assembly,1.5,1.5,true), false);
            check(anchorEntity(helper,assembly).appearance().equals(look), "paint did not reach anchor");
            var trims = styled(ModItems.ROUND_TRIM.get(),trimLook);
            trims.setCount(2);
            for (boolean front : new boolean[]{true,false})
                AppearanceFixtures.use(helper,player,trims,ItemStack.EMPTY,
                        DoorRays.through(helper,player,assembly,1.5,1.5,front),false);
            check(trims.isEmpty(), "trim payment");
            check(anchorEntity(helper,assembly).trims().equals(FaceTrims.both(trimLook)),"both trims missing");
            var entity = anchorEntity(helper,assembly);
            var loaded = BlockEntity.loadStatic(assembly.anchor(),level.getBlockState(assembly.anchor()),entity.saveWithFullMetadata());
            check(loaded instanceof RoundDoorBlockEntity copy
                    && copy.appearance().equals(look) && copy.trims().equals(entity.trims()),"look/trim reload");
            for (var part : assembly.parts()) {
                var power = assembly.pos(part).north();
                level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
                for (var pos : assembly.positions()) check(level.getBlockState(pos).getValue(DoorBlock.OPEN)
                        && level.getBlockState(pos).getValue(DoorBlock.POWERED),"redstone missed partner");
                level.removeBlock(power,false);
                for (var pos : assembly.positions()) check(!level.getBlockState(pos).getValue(DoorBlock.OPEN),"power-off missed partner");
            }
            clearDrops(helper);
            player.gameMode.destroyBlock(assembly.pos(broken));
            assertNoDoor(helper,assembly);
            check(dropped(helper,block.asItem(),look)==1,"expected one painted tall door from "+broken);
            check(dropped(helper,ModItems.ROUND_TRIM.get(),trimLook)==2,"expected two trims from "+broken);
            clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aimPreviewAndWrittenStatesAgreeAtEveryDepth(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(3, 1, 3));
        for (var block : java.util.List.of(ModBlocks.archedDoor(), ModBlocks.roundDoor(), ModBlocks.doubleArchedDoor(), ModBlocks.tallDoubleArchedDoor())) {
            for (var facing : Direction.Plane.HORIZONTAL) for (var depth : DoorDepth.values()) for (var hinge : DoorHingeSide.values()) {
                var player = player(helper, "depth_target", GameType.SURVIVAL, facing);
                var stack = new ItemStack(block.asItem(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                double across = hinge == DoorHingeSide.LEFT ? -.25 : .25;
                double along = depth == DoorDepth.FRONT ? -.375 : depth == DoorDepth.BACK ? .375 : 0;
                Vec3 point = Vec3.atLowerCornerOf(anchor).add(.5, 0, .5)
                        .relative(facing.getClockWise(), across).relative(facing, along);
                // Ray against the real floor, rather than an invented face hit.
                BlockHitResult hit = Blocks.STONE.defaultBlockState().getShape(level, anchor.below())
                        .clip(point.add(0, 1, 0), point.add(0, -1, 0), anchor.below());
                check(hit != null, "floor ray missed");
                var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
                var plan = DoorPlacement.of(block, context).orElseThrow();
                check(plan.assembly().depth() == depth, "wrong aimed depth");
                check(plan.assembly().hinge() == hinge, "depth changed hinge targeting");
                check(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "item refused");
                check(stack.getCount() == 1, "incorrect item charge");
                var assembly = plan.assembly();
                check(assembly.complete(level, block), "preview differs from installed assembly");
                for (var part : assembly.parts()) {
                    var state = level.getBlockState(assembly.pos(part));
                    check(DoorLayout.of(assembly.pos(part), state).equals(assembly), "cell resolves wrong anchor");
                }
                block.setOpen(player, level, level.getBlockState(anchor), anchor, true);
                for (var pos : assembly.positions()) check(level.getBlockState(pos).getValue(DoorBlock.OPEN), "partner stayed closed");
                block.setOpen(player, level, level.getBlockState(anchor), anchor, false);
                for (var pos : assembly.positions()) check(!level.getBlockState(pos).getValue(DoorBlock.OPEN), "partner stayed open");
                level.removeBlock(assembly.pos(assembly.parts().get(assembly.parts().size() - 1)), false);
                for (var pos : assembly.positions()) check(level.getBlockState(pos).isAir(), "partner survived teardown");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oldRoundStatesDefaultToTheirHistoricalDepth(GameTestHelper helper) {
        for (var block : java.util.List.of(ModBlocks.archedDoor(), ModBlocks.roundDoor()))
        for (var facing : Direction.Plane.HORIZONTAL) for (var hinge : DoorHingeSide.values()) {
            var assembly = new DoorLayout.Assembly(BlockPos.ZERO, facing, hinge, block.design());
            for (var part : assembly.parts()) {
                var original = assembly.stateFor(part, block, true, true);
                var tag = NbtUtils.writeBlockState(original);
                tag.getCompound("Properties").remove("depth");
                var loaded = NbtUtils.readBlockState(helper.getLevel().holderLookup(Registries.BLOCK), tag);
                check(loaded.equals(original), "historical state changed when depth was absent");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void allTwelveCellsSurviveIntegrityAndNeighbourUpdates(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var block = ModBlocks.tallDoubleArchedDoor();
        var assembly = new DoorLayout.Assembly(helper.absolutePos(new BlockPos(3, 1, 3)), Direction.NORTH,
                DoorHingeSide.LEFT, block.design(), DoorDepth.MIDDLE);
        check(DoorPlacement.placeAt(level, block, assembly, false), "placement refused");
        for (var pos : assembly.positions()) level.updateNeighborsAt(pos, block);
        helper.runAfterDelay(3, () -> {
            check(assembly.complete(level, block), "twelve-cell assembly lost a cell");
            assertOneAnchorEntity(helper, assembly);
            helper.succeed();
        });
    }
}
