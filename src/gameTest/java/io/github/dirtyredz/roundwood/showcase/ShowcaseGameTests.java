package io.github.dirtyredz.roundwood.showcase;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.window.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.section.BrickArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.CamouflageSection;
import io.github.dirtyredz.roundwood.showcase.section.DoorsSection;
import io.github.dirtyredz.roundwood.showcase.section.PaintsSection;
import io.github.dirtyredz.roundwood.showcase.section.RedstoneSection;
import io.github.dirtyredz.roundwood.showcase.section.TrimCombinationsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowPositionsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowsSection;
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
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * The showcase builder in an 86 x 10 x 86 template filled with gold: everything it promises is
 * present, complete and in the appearance its section names, and no gold outside its bounds was
 * touched (in each facing).
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShowcaseGameTests {
    private static final String TEMPLATE = "showcase";
    private static final int SIZE = 86;
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
        check(across == ShowcaseFrame.WIDTH + 2 && along == ShowcaseFrame.DEPTH + 2 && bounds.getYSpan() == 8,
                "bounds are not 78 x 8 x 82: " + bounds);

        ShowcaseFrame frame = ShowcaseFrame.at(feet, facing);
        verifyBounds(helper, bounds);
        verifyDoors(level, frame);
        verifyCamouflage(level, frame);
        verifyWindows(level, frame);
        verifyWindowPositions(level, frame);
        verifyWindowArches(level, frame);
        ShowcaseWindowChecks.verifyNewWindowSections(level, frame);
        verifyRedstone(level, frame);
        verifyTrims(level, frame);
        verifyArches(level, frame);
        verifyPaints(level, frame);
        // Integrity checks run a tick later: everything must still be whole afterwards.
        helper.runAfterDelay(3, () -> {
            verifyTrims(level, frame);
            verifyArches(level, frame);
            verifyDoors(level, frame);
            verifyPaints(level, frame);
            verifyWindowPositions(level, frame);
            verifyWindowArches(level, frame);
            ShowcaseWindowChecks.verifyNewWindowSections(level, frame);
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

    private static List<Appearance> woods() {
        return ShowcaseCanvas.sampleSet().stream().filter(a -> a.isWood() && !a.isPainted()).toList();
    }

    private static List<Appearance> paints() {
        return ShowcaseCanvas.sampleSet().stream().filter(Appearance::isPainted).toList();
    }

    private static List<Appearance> masonry() {
        return ShowcaseCanvas.sampleSet().stream().filter(a -> !a.isWood()).toList();
    }

    private static Appearance doorLook(ServerLevel level, BlockPos anchor) {
        return level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity ? entity.appearance() : null;
    }

    private static Appearance trimLook(ServerLevel level, BlockPos anchor) {
        return level.getBlockEntity(anchor) instanceof RoundTrimBlockEntity entity ? entity.appearance() : null;
    }

    private static Appearance windowLook(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? entity.appearance() : null;
    }

    private static boolean signSays(ServerLevel level, BlockPos pos, String first) {
        return level.getBlockEntity(pos) instanceof SignBlockEntity sign
                && sign.getFrontText().getMessage(0, false).getString().startsWith(first);
    }

    private static void verifyDoors(ServerLevel level, ShowcaseFrame frame) {
        RoundDoorBlock block = ModBlocks.roundDoor();
        List<Appearance> woods = woods();
        check(woods.size() >= 11, "fewer than the eleven vanilla woods: " + woods.size());
        for (int i = 0; i < woods.size(); i++) {
            Appearance wood = woods.get(i);
            for (int variant = 0; variant < 4; variant++) {
                int r = DoorsSection.r(i, variant);
                int f = DoorsSection.f(i);
                DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), DoorsSection.hinge(variant));
                String what = wood.key() + " variant " + variant;
                check(assembly.complete(level, block), what + ": door missing or incomplete");
                check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN) == DoorsSection.open(variant), what + ": wrong open state");
                check(wood.equals(doorLook(level, assembly.anchor())), what + ": anchor shows " + doorLook(level, assembly.anchor()));
                check(signSays(level, frame.pos(r, 0, f - 1), ShowcaseCanvas.label(wood)), what + ": label sign missing");
            }
            BlockPos windowPos = frame.pos(WindowsSection.ROW_R + i, 0, WindowsSection.ROW_F);
            BlockState state = level.getBlockState(windowPos);
            check(state.is(ModBlocks.roundWindow()), wood.key() + ": window missing from the row");
            check(wood.equals(windowLook(level, windowPos)), wood.key() + ": window shows " + windowLook(level, windowPos));
            check(state.getValue(RoundWindowBlock.AXIS) == frame.right().getAxis(), wood.key() + ": window axis");
            check(connects(state, frame.right()) == (i < woods.size() - 1), wood.key() + ": right-hand connection");
            check(connects(state, frame.right().getOpposite()) == (i > 0), wood.key() + ": left-hand connection");
        }
    }

    private static void verifyCamouflage(ServerLevel level, ShowcaseFrame frame) {
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < CamouflageSection.MATERIALS.size(); i++) {
                int r = CamouflageSection.r(i);
                int f = CamouflageSection.f(row);
                BlockPos anchor = frame.pos(r, 0, f);
                BlockState door = level.getBlockState(anchor);
                check(door.is(ModBlocks.roundDoor()) && door.getValue(DoorBlock.OPEN) == (row == 1),
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
        RoundWindowBlock oak = ModBlocks.roundWindow();
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

    /** The round window in each position, the junction demos and the practice blocks, in this facing's world directions. */
    private static void verifyWindowPositions(ServerLevel level, ShowcaseFrame frame) {
        Direction forward = frame.forward();
        Direction right = frame.right();
        int f = WindowPositionsSection.ROW_F;
        expectPosition(level, frame, WindowPositionsSection.CENTRE_R, f, WindowPosition.CENTER);
        check(level.getBlockState(frame.pos(WindowPositionsSection.CENTRE_R, 0, f)).getValue(RoundWindowBlock.AXIS) == right.getAxis(),
                "centred window axis");
        expectPosition(level, frame, WindowPositionsSection.FORWARD_R, f, WindowPosition.flush(forward));
        expectPosition(level, frame, WindowPositionsSection.BACK_R, f, WindowPosition.flush(forward.getOpposite()));
        expectPosition(level, frame, WindowPositionsSection.RIGHT_R, f, WindowPosition.flush(right));
        expectPosition(level, frame, WindowPositionsSection.LEFT_R, f, WindowPosition.flush(right.getOpposite()));
        expectPosition(level, frame, WindowPositionsSection.CEILING_R, f, WindowPosition.UP);
        expectPosition(level, frame, WindowPositionsSection.CEILING_R, WindowPositionsSection.FLOOR_F, WindowPosition.DOWN);
        int d = WindowPositionsSection.DEMO_F;
        BlockState pane = expectPosition(level, frame, WindowPositionsSection.PANE_R, d, WindowPosition.flush(forward));
        check(WindowPaneBlock.connections(pane).isEmpty(), "flush window bridged to the glass pane on its right: " + pane);
        BlockState corner = expectPosition(level, frame, WindowPositionsSection.CORNER_R, d, WindowPosition.flush(forward));
        BlockState cornerSide = expectPosition(level, frame, WindowPositionsSection.CORNER_R + 1, d,
                WindowPosition.flush(right.getOpposite()));
        check(WindowPaneBlock.connections(corner).isEmpty() && WindowPaneBlock.connections(cornerSide).isEmpty(),
                "flush windows in an L kept connections: " + corner + " / " + cornerSide);
        BlockState mixed = expectPosition(level, frame, WindowPositionsSection.MIXED_R, d, WindowPosition.flush(forward));
        check(WindowPaneBlock.connections(mixed).isEmpty(), "flush window bridged to the centred one beside it");
        BlockState solid = expectPosition(level, frame, WindowPositionsSection.SOLID_R, d, WindowPosition.flush(forward));
        check(!connects(solid, right) && !connects(solid, forward), "stone made a flush window grow glass: " + solid);
        BlockState wet = expectPosition(level, frame, WindowPositionsSection.WATER_R, d, WindowPosition.flush(forward));
        check(wet.getValue(BlockStateProperties.WATERLOGGED), "flush window not waterlogged");
        int p = WindowPositionsSection.PRACTICE_F;
        check(level.getBlockState(frame.pos(WindowPositionsSection.SLAB_R, 0, p)).is(Blocks.OAK_SLAB)
                && level.getBlockState(frame.pos(WindowPositionsSection.STAIRS_R, 0, p)).is(Blocks.OAK_STAIRS)
                && level.getBlockState(frame.pos(WindowPositionsSection.WALL_R, 1, p)).is(Blocks.STONE_BRICKS), "practice blocks missing");
    }

    private static BlockState expectPosition(ServerLevel level, ShowcaseFrame frame, int r, int f, WindowPosition position) {
        BlockState state = level.getBlockState(frame.pos(r, 0, f));
        check(state.is(ModBlocks.roundWindow()) && state.getValue(RoundWindowBlock.POSITION) == position,
                "expected a " + position + " window at r " + r + " f " + f + ", found " + state);
        check(Appearance.DEFAULT.equals(windowLook(level, frame.pos(r, 0, f))), "window look at r " + r + " f " + f);
        return state;
    }

    /** Every arch pair: left on the builder's left, the pose placement would choose, the named look. */
    private static void verifyWindowArches(ServerLevel level, ShowcaseFrame frame) {
        Direction forward = frame.forward();
        int f = WindowArchesSection.ROW_F;
        expectPair(level, frame, WindowArchesSection.CENTRED_R, f, WindowArchesSection.centred(forward), Appearance.DEFAULT);
        expectPair(level, frame, WindowArchesSection.FLUSH_R, f, WindowArchesSection.flush(forward), Appearance.DEFAULT);
        expectPair(level, frame, WindowArchesSection.FLOOR_R, f, WindowArchesSection.floor(forward), Appearance.DEFAULT);
        expectPair(level, frame, WindowArchesSection.CEILING_R, f, WindowArchesSection.ceiling(forward), Appearance.DEFAULT);
        expectArch(level, frame, ModBlocks.archLeft(), WindowArchesSection.SINGLE_LEFT_R, WindowArchesSection.SINGLES_F,
                WindowArchesSection.centred(forward), Appearance.DEFAULT);
        expectArch(level, frame, ModBlocks.archRight(), WindowArchesSection.SINGLE_RIGHT_R, WindowArchesSection.SINGLES_F,
                WindowArchesSection.centred(forward), Appearance.DEFAULT);
        List<Appearance> looks = WindowArchesSection.looks();
        for (int i = 0; i < looks.size(); i++) {
            expectPair(level, frame, WindowArchesSection.LOOKS_R + WindowArchesSection.LOOKS_PITCH * i, WindowArchesSection.LOOKS_F,
                    WindowArchesSection.centred(forward), looks.get(i));
        }
        // Placement's own rule agrees with the showcase: a builder facing forward puts the left piece on their left.
        check(WindowArchesSection.centred(forward).right() == frame.right(), "centred pair's right is not the builder's right");
        check(WindowArchesSection.floor(forward).right() == frame.right(), "floor pair's right is not the builder's right");
        check(WindowArchesSection.ceiling(forward).right() == frame.right(), "ceiling pair's right is not the builder's right");
    }

    private static void expectPair(ServerLevel level, ShowcaseFrame frame, int r, int f, WindowPose pose, Appearance look) {
        expectArch(level, frame, ModBlocks.archLeft(), r, f, pose, look);
        expectArch(level, frame, ModBlocks.archRight(), r + 1, f, pose, look);
        check(!connects(level.getBlockState(frame.pos(r, 0, f)), frame.right()) || pose.depth() == WindowDepth.CENTER,
                "a flush or floor pair bridged between its own pieces at r " + r);
    }

    private static void expectArch(ServerLevel level, ShowcaseFrame frame, OrientedWindowBlock piece, int r, int f, WindowPose pose,
                                   Appearance look) {
        BlockPos pos = frame.pos(r, 0, f);
        BlockState state = level.getBlockState(pos);
        check(state.is(piece) && piece.pose(state).equals(pose), "expected " + piece + " in " + pose + " at r " + r + " f " + f + ", found " + state);
        check(look.equals(windowLook(level, pos)), piece + " at r " + r + " shows " + windowLook(level, pos));
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
        List<Appearance> materials = new ArrayList<>(woods());
        materials.addAll(masonry());
        for (int i = 0; i < materials.size(); i++) {
            Appearance material = materials.get(i);
            for (int j = 0; j < TrimCombinationsSection.SAMPLES; j++) {
                int r = TrimCombinationsSection.r(i, j);
                int f = TrimCombinationsSection.f(i);
                String what = material.key() + " sample " + j;
                DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(),
                        TrimCombinationsSection.hinge(i, j));
                check(assembly.complete(level, ModBlocks.roundDoor()), what + ": door missing or incomplete");
                check(TrimCombinationsSection.door(j).equals(doorLook(level, assembly.anchor())), what + ": door look");
                check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN) == TrimCombinationsSection.open(j), what + ": open state");
                boolean camouflaged = level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity
                        && entity.camouflage().map(c -> c.material().is(Blocks.STONE_BRICKS)).orElse(false);
                check(camouflaged == TrimCombinationsSection.camouflaged(j), what + ": camouflage");
                TrimLayout.Assembly trim = new TrimLayout.Assembly(frame.pos(r, 0, f - 1), frame.forward());
                check(trim.complete(level, ModBlocks.roundTrim()), what + ": frame missing or incomplete");
                check(material.equals(trimLook(level, trim.anchor())), what + ": frame shows " + trimLook(level, trim.anchor()));
                check(signSays(level, frame.pos(r, 0, f - 2), ShowcaseCanvas.label(material)), what + ": label sign missing");
            }
        }
    }

    private static void verifyArches(ServerLevel level, ShowcaseFrame frame) {
        List<Appearance> masonry = masonry();
        check(masonry.size() == 4, "expected four masonry arches, have " + masonry.size());
        for (int i = 0; i < masonry.size(); i++) {
            TrimLayout.Assembly trim = new TrimLayout.Assembly(frame.pos(BrickArchesSection.r(i), 0, BrickArchesSection.F), frame.forward());
            check(trim.complete(level, ModBlocks.roundTrim()), masonry.get(i).key() + ": freestanding arch missing");
            check(masonry.get(i).equals(trimLook(level, trim.anchor())), masonry.get(i).key() + ": arch look");
            check(level.getBlockEntity(frame.pos(BrickArchesSection.r(i) - 1, 0, BrickArchesSection.F)) instanceof SignBlockEntity,
                    masonry.get(i).key() + ": arch label missing");
        }
    }

    private static void verifyPaints(ServerLevel level, ShowcaseFrame frame) {
        List<Appearance> paints = paints();
        check(paints.size() == 16, "expected all sixteen paints, have " + paints.size());
        for (int i = 0; i < paints.size(); i++) {
            Appearance paint = paints.get(i);
            int r = PaintsSection.r(i);
            int f = PaintsSection.f(i);
            DoorLayout.Assembly door = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), DoorHingeSide.LEFT);
            check(door.complete(level, ModBlocks.roundDoor()) && paint.equals(doorLook(level, door.anchor())), paint.key() + ": door");
            TrimLayout.Assembly trim = new TrimLayout.Assembly(frame.pos(r + PaintsSection.TRIM_OFFSET, 0, f), frame.forward());
            check(trim.complete(level, ModBlocks.roundTrim()) && paint.equals(trimLook(level, trim.anchor())), paint.key() + ": trim");
            BlockPos window = frame.pos(r + PaintsSection.WINDOW_OFFSET, 0, f);
            check(level.getBlockState(window).is(ModBlocks.roundWindow()) && paint.equals(windowLook(level, window)), paint.key() + ": window");
            check(signSays(level, frame.pos(r, 0, f - 1), ShowcaseCanvas.label(paint)), paint.key() + ": label sign missing");
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
