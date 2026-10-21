package io.github.dirtyredz.roundwood.showcase;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.section.BrickArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.CamouflageSection;
import io.github.dirtyredz.roundwood.showcase.section.DoorsSection;
import io.github.dirtyredz.roundwood.showcase.section.RedstoneSection;
import io.github.dirtyredz.roundwood.showcase.section.TrimCombinationsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowsSection;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.variant.TrimMaterial;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * The showcase builder in a 76 x 10 x 76 template filled with gold: everything it promises is
 * present and complete, and no gold outside its bounds was touched (in each facing).
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShowcaseGameTests {
    private static final String TEMPLATE = "showcase";
    private static final int SIZE = 76;
    private static final int HEIGHT = 10;

    private ShowcaseGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void buildsFacingSouth(GameTestHelper helper) {
        buildAndVerify(helper, Direction.SOUTH);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void buildsFacingEast(GameTestHelper helper) {
        buildAndVerify(helper, Direction.EAST);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void buildsFacingNorth(GameTestHelper helper) {
        buildAndVerify(helper, Direction.NORTH);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void buildsFacingWest(GameTestHelper helper) {
        buildAndVerify(helper, Direction.WEST);
    }

    @GameTest(template = "empty")
    public static void refusesToLeaveTheBuildLimits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos top = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), level.getMaxBuildHeight() - 2, helper.absolutePos(BlockPos.ZERO).getZ());
        BlockPos bottom = top.atY(level.getMinBuildHeight());
        check(!ShowcaseBuilder.build(level, top, Direction.SOUTH).built(), "built through the height limit");
        check(!ShowcaseBuilder.build(level, bottom, Direction.SOUTH).built(), "built below the bottom of the world");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandNeedsOp(GameTestHelper helper) {
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        ServerPlayer player = player(helper, "showcase_op", GameType.CREATIVE, Direction.SOUTH);
        CommandSourceStack plain = player.createCommandSourceStack().withPermission(0);
        CommandSourceStack op = player.createCommandSourceStack().withPermission(2);
        boolean rejected = false;
        try {
            dispatcher.execute("roundwood showcase", plain);
        } catch (CommandSyntaxException expected) {
            rejected = true;
        }
        check(rejected, "a non-op ran /roundwood showcase");
        ParseResults<CommandSourceStack> parsed = dispatcher.parse("roundwood showcase", op);
        check(parsed.getExceptions().isEmpty() && !parsed.getReader().canRead() && parsed.getContext().getCommand() != null,
                "an op (level 2) cannot run /roundwood showcase");
        helper.succeed();
    }

    private static void buildAndVerify(GameTestHelper helper, Direction facing) {
        ServerLevel level = helper.getLevel();
        // The bounds run from 2 to DEPTH + 3 blocks ahead of the feet: centre them in the template.
        BlockPos feetRel = new BlockPos(SIZE / 2, 2, SIZE / 2).relative(facing.getOpposite(), (ShowcaseFrame.DEPTH + 5) / 2);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.GOLD_BLOCK);
                }
            }
        }
        BlockPos feet = helper.absolutePos(feetRel);
        ShowcaseBuilder.Result result = ShowcaseBuilder.build(level, feet, facing);
        check(result.built(), "refused: " + result.message());
        check(result.problems().isEmpty(), "placement problems: " + result.problems());
        BoundingBox bounds = result.bounds();
        int across = Math.min(bounds.getXSpan(), bounds.getZSpan());
        int along = Math.max(bounds.getXSpan(), bounds.getZSpan());
        check(across == 62 && along == 72 && bounds.getYSpan() == 8, "bounds are not 62 x 8 x 72: " + bounds);

        ShowcaseFrame frame = ShowcaseFrame.at(feet, facing);
        verifyBounds(helper, bounds);
        verifyDoors(level, frame);
        verifyCamouflage(level, frame);
        verifyWindows(level, frame);
        verifyRedstone(level, frame);
        verifyTrims(level, frame);
        verifyArches(level, frame);
        // Integrity checks run a tick later: everything must still be whole afterwards.
        helper.runAfterDelay(3, () -> {
            verifyTrims(level, frame);
            verifyArches(level, frame);
            verifyDoors(level, frame);
            clearSilently(helper);
            helper.succeed();
        });
    }

    /**
     * Leaves nothing behind: the framework later clears this area for other tests with shape updates,
     * which would break doors and frames that lose their floor and drop items into those tests.
     */
    private static void clearSilently(GameTestHelper helper) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        for (int x = 0; x < SIZE; x++) {
            for (int y = HEIGHT - 1; y >= 0; y--) {
                for (int z = 0; z < SIZE; z++) {
                    helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState(), flags);
                }
            }
        }
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(SIZE, HEIGHT, SIZE))).inflate(2);
        helper.getLevel().getEntitiesOfClass(Entity.class, area, entity -> !(entity instanceof Player)).forEach(Entity::discard);
    }

    private static void verifyBounds(GameTestHelper helper, BoundingBox bounds) {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    BlockPos abs = helper.absolutePos(new BlockPos(x, y, z));
                    boolean gold = helper.getLevel().getBlockState(abs).is(Blocks.GOLD_BLOCK);
                    boolean inside = bounds.isInside(abs);
                    check(inside != gold, inside ? "inside block not rewritten at " + abs : "outside block changed at " + abs);
                }
            }
        }
    }

    private static void verifyDoors(ServerLevel level, ShowcaseFrame frame) {
        for (int i = 0; i < FinishCatalog.ALL.size(); i++) {
            Finish finish = FinishCatalog.ALL.get(i);
            RoundDoorBlock block = ModBlocks.door(finish);
            for (int variant = 0; variant < 4; variant++) {
                int r = DoorsSection.r(i, variant);
                int f = DoorsSection.f(i);
                DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), DoorsSection.hinge(variant));
                check(assembly.complete(level, block), finish.id() + " variant " + variant + ": door missing or incomplete");
                check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN) == DoorsSection.open(variant),
                        finish.id() + " variant " + variant + ": wrong open state");
                check(level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity, finish.id() + ": no anchor entity");
                check(level.getBlockEntity(frame.pos(r, 0, f - 1)) instanceof SignBlockEntity sign
                                && sign.getFrontText().getMessage(0, false).getString().equals(finish.displayName()),
                        finish.id() + " variant " + variant + ": label sign missing");
            }
            RoundWindowBlock window = ModBlocks.window(finish);
            BlockState state = level.getBlockState(frame.pos(WindowsSection.ROW_R + i, 0, WindowsSection.ROW_F));
            check(state.is(window), finish.id() + ": window missing from the row");
            check(state.getValue(RoundWindowBlock.AXIS) == frame.right().getAxis(), finish.id() + ": window axis");
            check(connects(state, frame.right()) == (i < FinishCatalog.ALL.size() - 1), finish.id() + ": right-hand connection");
            check(connects(state, frame.right().getOpposite()) == (i > 0), finish.id() + ": left-hand connection");
        }
    }

    private static void verifyCamouflage(ServerLevel level, ShowcaseFrame frame) {
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < CamouflageSection.MATERIALS.size(); i++) {
                int r = CamouflageSection.r(i);
                int f = CamouflageSection.f(row);
                BlockPos anchor = frame.pos(r, 0, f);
                BlockState door = level.getBlockState(anchor);
                check(door.is(ModBlocks.door(FinishCatalog.PAINT_BASE)) && door.getValue(DoorBlock.OPEN) == (row == 1),
                        "camouflage door missing or wrong state at row " + row + " index " + i);
                check(level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity
                                && entity.camouflage().isPresent()
                                && entity.camouflage().get().material().is(CamouflageSection.MATERIALS.get(i)),
                        "camouflage not applied at row " + row + " index " + i);
            }
        }
    }

    private static void verifyWindows(ServerLevel level, ShowcaseFrame frame) {
        Direction right = frame.right();
        RoundWindowBlock oak = ModBlocks.window(FinishCatalog.PAINT_BASE);
        BlockState middle = level.getBlockState(frame.pos(WindowsSection.JUNCTION_R + 1, 0, WindowsSection.DEMO_F));
        check(middle.is(oak) && connects(middle, frame.forward()), "junction window does not connect to the glass pane");
        check(level.getBlockState(frame.pos(WindowsSection.JUNCTION_R + 1, 0, WindowsSection.DEMO_F + 1)).is(Blocks.GLASS_PANE),
                "glass pane missing from the junction");
        BlockState lower = level.getBlockState(frame.pos(WindowsSection.STACK_R, 0, WindowsSection.DEMO_F));
        BlockState upper = level.getBlockState(frame.pos(WindowsSection.STACK_R, 1, WindowsSection.DEMO_F));
        check(lower.is(oak) && upper.is(oak) && lower.getValue(RoundWindowBlock.AXIS) == right.getAxis()
                && upper.getValue(RoundWindowBlock.AXIS) != lower.getValue(RoundWindowBlock.AXIS), "stacked pair axes");
        BlockState wet = level.getBlockState(frame.pos(WindowsSection.WATER_R, 0, WindowsSection.DEMO_F));
        check(wet.is(oak) && wet.getValue(BlockStateProperties.WATERLOGGED), "waterlogged window missing");
        BlockState solid = level.getBlockState(frame.pos(WindowsSection.SOLID_R, 0, WindowsSection.DEMO_F));
        check(solid.is(oak) && level.getBlockState(frame.pos(WindowsSection.SOLID_R, 0, WindowsSection.DEMO_F + 1)).is(Blocks.STONE),
                "stone-behind window demo missing");
        check(!connects(solid, frame.forward()), "a window grew an arm towards the stone behind it");
    }

    private static void verifyRedstone(ServerLevel level, ShowcaseFrame frame) {
        BlockPos door = frame.pos(RedstoneSection.DOOR_R, 0, RedstoneSection.DOOR_F);
        BlockPos leverPos = frame.pos(RedstoneSection.LEVER_R, 0, RedstoneSection.DOOR_F);
        BlockState lever = level.getBlockState(leverPos);
        check(lever.is(Blocks.LEVER) && !lever.getValue(LeverBlock.POWERED), "lever missing or already on");
        check(!level.getBlockState(door).getValue(DoorBlock.OPEN), "redstone door starts open");
        ((LeverBlock) Blocks.LEVER).pull(lever, level, leverPos);
        check(level.getBlockState(door).getValue(DoorBlock.OPEN), "flipping the lever did not open the door");
    }

    private static void verifyTrims(ServerLevel level, ShowcaseFrame frame) {
        for (int i = 0; i < FinishCatalog.TRIMS.size(); i++) {
            TrimMaterial material = FinishCatalog.TRIMS.get(i);
            for (int j = 0; j < TrimCombinationsSection.SAMPLES; j++) {
                int r = TrimCombinationsSection.r(i, j);
                int f = TrimCombinationsSection.f(i);
                String what = material.id() + " sample " + j;
                RoundDoorBlock door = ModBlocks.door(TrimCombinationsSection.finish(j));
                DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(),
                        TrimCombinationsSection.hinge(i, j));
                check(assembly.complete(level, door), what + ": door missing or incomplete");
                check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN) == TrimCombinationsSection.open(j), what + ": open state");
                boolean camouflaged = level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity
                        && entity.camouflage().map(c -> c.material().is(Blocks.STONE_BRICKS)).orElse(false);
                check(camouflaged == TrimCombinationsSection.camouflaged(j), what + ": camouflage");
                TrimLayout.Assembly trim = new TrimLayout.Assembly(frame.pos(r, 0, f - 1), frame.forward());
                check(trim.complete(level, ModBlocks.trim(material)), what + ": frame missing or incomplete");
                check(level.getBlockEntity(frame.pos(r, 0, f - 2)) instanceof SignBlockEntity sign
                                && sign.getFrontText().getMessage(0, false).getString().startsWith(material.displayName()),
                        what + ": label sign missing");
            }
        }
    }

    private static void verifyArches(ServerLevel level, ShowcaseFrame frame) {
        for (int i = 0; i < FinishCatalog.MASONRY.size(); i++) {
            TrimMaterial material = FinishCatalog.MASONRY.get(i);
            TrimLayout.Assembly trim = new TrimLayout.Assembly(frame.pos(BrickArchesSection.r(i), 0, BrickArchesSection.F), frame.forward());
            check(trim.complete(level, ModBlocks.trim(material)), material.id() + ": freestanding arch missing");
            check(level.getBlockEntity(frame.pos(BrickArchesSection.r(i) - 1, 0, BrickArchesSection.F)) instanceof SignBlockEntity,
                    material.id() + ": arch label missing");
        }
    }

    private static boolean connects(BlockState state, Direction direction) {
        return state.getValue(switch (direction) {
            case NORTH -> IronBarsBlock.NORTH;
            case SOUTH -> IronBarsBlock.SOUTH;
            case EAST -> IronBarsBlock.EAST;
            case WEST -> IronBarsBlock.WEST;
            default -> throw new IllegalArgumentException(direction.toString());
        });
    }
}
