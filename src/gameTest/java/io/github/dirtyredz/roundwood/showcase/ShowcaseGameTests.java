package io.github.dirtyredz.roundwood.showcase;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.showcase.section.SectionChecks;
import io.github.dirtyredz.roundwood.showcase.section.door.DoorSectionsChecks;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowSectionsChecks;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * The showcase builder in a 92 x 10 x 92 template (the bounds are 90 x 8 x 28) filled with gold: everything it promises is
 * present, complete and in the appearance its section names, and no gold outside its bounds was
 * touched (in each facing).
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShowcaseGameTests {
    private static final String TEMPLATE = "showcase";
    private static final int SIZE = 92;
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
        check(across == ShowcaseFrame.DEPTH + 2 && along == ShowcaseFrame.WIDTH + 2 && bounds.getYSpan() == 8,
                "bounds are not 90 x 8 x 28: " + bounds);

        ShowcaseFrame frame = ShowcaseFrame.at(feet, facing);
        verifyBounds(helper, bounds);
        DoorSectionsChecks.verifyCamouflage(level, frame);
        DoorSectionsChecks.verifyRedstone(level, frame);
        WindowSectionsChecks.verifyWindows(level, frame);
        WindowSectionsChecks.verifyWindowPositions(level, frame);
        WindowSectionsChecks.verifyWindowArches(level, frame);
        WindowSectionsChecks.verifyNewWindowSections(level, frame);
        SectionChecks.verifyPieces(level, frame);
        SectionChecks.verifyMaterials(level, frame);
        SpacingChecks.verifySpacing(level, frame);
        // Integrity checks run a tick later: everything must still be whole afterwards.
        helper.runAfterDelay(3, () -> {
            WindowSectionsChecks.verifyWindowPositions(level, frame);
            WindowSectionsChecks.verifyWindowArches(level, frame);
            WindowSectionsChecks.verifyNewWindowSections(level, frame);
            SectionChecks.verifyPieces(level, frame);
            SectionChecks.verifyMaterials(level, frame);
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
}
