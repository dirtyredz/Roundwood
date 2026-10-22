package io.github.dirtyredz.roundwood.block.assembly;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.Family;
import io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.Instance;
import io.github.dirtyredz.roundwood.fixture.assembly.SingleChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.LOOK;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.all;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.writeRaw;

/**
 * Characterisation of the lifecycle the round door, arched door, both archways and the compatibility
 * frame each implement for themselves today (and are about to share): an unloaded partner defers the
 * integrity check without any read or forced load; a lone part is removed one tick later while a
 * complete assembly survives its own scheduled check; identical neighbours never touch each other;
 * and Forge's snapshot restoration, like Roundwood's own mutation batch, never tears partners down or
 * schedules a check. Every test loops over all five families.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblyLifecycleGameTests {
    /** Far from anything else; the chunk is loaded alone, so its neighbours are not. */
    private static final int FAR_CHUNK_X = 2000;
    private static final int FAR_CHUNK_Z = 2000;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private AssemblyLifecycleGameTests() {
    }

    /** Absolute anchor of family {@code row}'s object in column {@code column} (0 or 1) of the 9 x 9 floor. */
    private static BlockPos slot(GameTestHelper helper, int row, int column) {
        return helper.absolutePos(new BlockPos(1 + column * 4, 1, row * 2));
    }

    /**
     * The check's lone part stands at the edge of a chunk whose neighbour is not loaded, so a partner
     * falls into unloaded space (east of the anchor, or west of the right column's cell). The scheduled
     * check must leave the part alone, and neither it nor the layout's queries may read or load the
     * neighbour. The arched door is a single column: every one of its cells shares the anchor's chunk, so
     * it can never have an unloaded partner and is skipped here (its retry path is the loaded one below).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void anUnloadedPartnerDefersTheCheckWithoutReadingOrLoadingIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int y = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        int visited = 0;
        try (SingleChunk chunk = SingleChunk.open(level, FAR_CHUNK_X, FAR_CHUNK_Z)) {
            for (Family family : all()) {
                for (boolean east : new boolean[] {true, false}) {
                    Instance probe = family.at(new BlockPos(chunk.minX(), y, chunk.midZ()), Direction.NORTH);
                    int part = east ? 0 : 2;
                    if (probe.columns() < 2) {
                        continue;
                    }
                    BlockPos cell = new BlockPos(east ? chunk.maxX() : chunk.minX(), y, chunk.midZ());
                    Instance object = probe.atCell(part, cell);
                    String what = family + (east ? " (partner east)" : " (anchor west)");
                    BlockState state = object.state(part);
                    writeRaw(level, cell, state);
                    visited++;
                    try {
                        chunk.assertIsolated(what + ": after writing the lone part");
                        check(!object.loaded(level), what + ": the layout reports the unloaded cell as loaded");
                        check(object.present(level) == 1, what + ": presentParts read past the loaded chunk");
                        check(!object.complete(level), what + ": complete() with an unloaded cell");
                        chunk.assertIsolated(what + ": after the layout queries");

                        level.getBlockState(cell).tick(level, cell, level.random);
                        check(level.getBlockState(cell).equals(state), what + ": the scheduled check removed a part whose partner is unloaded");
                        chunk.assertIsolated(what + ": after the scheduled check");
                    } finally {
                        writeRaw(level, cell, AIR);
                    }
                }
            }
        }
        check(visited == 8, "expected 4 multi-column families x 2 edges, visited " + visited);
        helper.succeed();
    }

    /**
     * A lone part whose cells are all loaded is removed by the scheduled check one tick after it appears,
     * with no other cell created; a complete assembly written by the production placement in that same tick
     * survives the same check.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aLonePartIsRemovedAndACompleteAssemblySurvivesTheScheduledCheck(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        List<Instance> lone = new ArrayList<>();
        List<Instance> whole = new ArrayList<>();
        for (int i = 0; i < families.size(); i++) {
            Instance loner = families.get(i).at(slot(helper, i, 0), Direction.NORTH);
            level.setBlock(loner.pos(0), loner.state(0), Block.UPDATE_CLIENTS);
            lone.add(loner);
            Instance complete = families.get(i).at(slot(helper, i, 1), Direction.NORTH);
            check(complete.place(level, LOOK), families.get(i) + ": the production placement refused");
            whole.add(complete);
        }
        for (Instance loner : lone) {
            check(loner.isPart(level.getBlockState(loner.pos(0)), 0), loner.family() + ": the lone part was gone before the scheduled check");
        }
        helper.runAfterDelay(3, () -> {
            for (Instance loner : lone) {
                check(loner.allAir(level), loner.family() + ": the lone part was not removed, or another cell was made");
            }
            for (Instance complete : whole) {
                check(complete.allParts(level), complete.family() + ": the scheduled check removed part of a complete assembly");
                check(complete.anchorLook(level, complete.pos(0), complete.state(0)).equals(Optional.of(LOOK)),
                        complete.family() + ": the complete assembly lost its anchor");
                complete.clear(level);
            }
            clearDrops(helper);
            helper.succeed();
        });
    }

    /**
     * Breaking any part of an assembly takes only that assembly: an identical neighbour (same family, facing,
     * hinge and look) beside it or stacked on it keeps every part and its anchor. Both orders, every part.
     * The stacked pair breaks only the upper object: removing the lower one would leave the upper without a
     * floor, which legitimately breaks it too.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void identicalNeighboursStayIndependent(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 4));
        for (Family family : all()) {
            Instance left = family.at(base, Direction.NORTH);
            Instance right = left.rightNeighbour();
            for (int i = 0; i < left.size(); i++) {
                for (int order = 0; order < 2; order++) {
                    Instance broken = order == 0 ? left : right;
                    Instance kept = order == 0 ? right : left;
                    String what = family + " side by side, part " + i + " of the " + (order == 0 ? "left" : "right") + " one";
                    check(left.place(level, LOOK) && right.place(level, LOOK), what + ": placement refused");
                    level.destroyBlock(broken.pos(i), true);
                    check(broken.allAir(level), what + ": the broken assembly is not gone");
                    check(kept.allParts(level) && kept.complete(level), what + ": the neighbour lost a part");
                    check(kept.anchorLook(level, kept.pos(0), kept.state(0)).equals(Optional.of(LOOK)),
                            what + ": the neighbour lost its anchor");
                    left.clear(level);
                    right.clear(level);
                    clearDrops(helper);
                }
            }
            Instance upper = left.upperNeighbour();
            for (int i = 0; i < upper.size(); i++) {
                String what = family + " stacked, part " + i + " of the upper one";
                check(left.place(level, LOOK), what + ": placement refused");
                upper.writeSilently(level, LOOK);
                level.destroyBlock(upper.pos(i), true);
                check(upper.allAir(level), what + ": the broken assembly is not gone");
                check(left.allParts(level) && left.complete(level), what + ": the assembly below lost a part");
                left.clear(level);
                clearDrops(helper);
            }
        }
        // And once more with time passing: nothing about the neighbour is rescheduled away.
        List<Instance> kept = new ArrayList<>();
        List<Family> families = all();
        for (int i = 0; i < families.size(); i++) {
            Instance left = families.get(i).at(slot(helper, i, 0), Direction.NORTH);
            Instance right = left.rightNeighbour();
            check(left.place(level, LOOK) && right.place(level, LOOK), families.get(i) + ": placement refused");
            level.destroyBlock(left.pos(0), true);
            kept.add(right);
        }
        helper.runAfterDelay(3, () -> {
            for (Instance neighbour : kept) {
                check(neighbour.allParts(level), neighbour.family() + ": the neighbour changed after the break");
                neighbour.clear(level);
            }
            clearDrops(helper);
            helper.succeed();
        });
    }

    /**
     * Forge restores a cancelled placement's block snapshots with {@code restoringBlockSnapshots} set.
     * Replacing one part with air then must not tear its partners down (every part, every family), and a
     * part written while restoring must get no integrity check, so a lone one stays where a normally written
     * lone part is removed.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void snapshotRestorationNeitherTearsDownNorSchedulesChecks(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        for (int row = 0; row < families.size(); row++) {
            Instance object = families.get(row).at(slot(helper, row, 0), Direction.NORTH);
            for (int i = 0; i < object.size(); i++) {
                String what = object.family() + " restoring over part " + i;
                check(object.place(level, LOOK), what + ": placement refused");
                level.restoringBlockSnapshots = true;
                try {
                    // No neighbour shape updates: a door's own vertical-partner rule is a separate mechanism.
                    level.setBlock(object.pos(i), AIR, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                } finally {
                    level.restoringBlockSnapshots = false;
                }
                for (int j = 0; j < object.size(); j++) {
                    if (j != i) {
                        check(object.isPart(level.getBlockState(object.pos(j)), j), what + ": partner " + j + " was torn down");
                    }
                }
                object.clear(level);
                clearDrops(helper);
            }
        }

        List<Instance> restored = new ArrayList<>();
        List<Instance> control = new ArrayList<>();
        for (int row = 0; row < families.size(); row++) {
            Instance quiet = families.get(row).at(slot(helper, row, 0), Direction.NORTH);
            level.restoringBlockSnapshots = true;
            try {
                level.setBlock(quiet.pos(0), quiet.state(0), Block.UPDATE_CLIENTS);
            } finally {
                level.restoringBlockSnapshots = false;
            }
            check(!level.getBlockTicks().hasScheduledTick(quiet.pos(0), quiet.family().block()),
                    quiet.family() + ": a part written while restoring got an integrity check");
            restored.add(quiet);
            Instance normal = families.get(row).at(slot(helper, row, 1), Direction.NORTH);
            level.setBlock(normal.pos(0), normal.state(0), Block.UPDATE_CLIENTS);
            check(level.getBlockTicks().hasScheduledTick(normal.pos(0), normal.family().block()),
                    normal.family() + ": a normally written part got no integrity check (the control)");
            control.add(normal);
        }
        helper.runAfterDelay(3, () -> {
            for (Instance quiet : restored) {
                check(quiet.isPart(level.getBlockState(quiet.pos(0)), 0), quiet.family() + ": the restored lone part was removed");
                quiet.clear(level);
            }
            for (Instance normal : control) {
                check(normal.allAir(level), normal.family() + ": the control lone part was not removed");
            }
            clearDrops(helper);
            helper.succeed();
        });
    }

    /**
     * Roundwood's own rewrites run inside an {@link AssemblyMutationBatch}: removing a part there, with
     * the full neighbour cascade, leaves every partner in place (the callbacks stand down).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aMutationBatchLeavesPartnersAlone(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        for (int row = 0; row < families.size(); row++) {
            Instance object = families.get(row).at(slot(helper, row, 0), Direction.NORTH);
            for (int i = 0; i < object.size(); i++) {
                String what = object.family() + " batch over part " + i;
                check(object.place(level, LOOK), what + ": placement refused");
                int removed = i;
                boolean ran = AssemblyMutationBatch.run(level, object.positions(),
                        () -> level.setBlock(object.pos(removed), AIR, Block.UPDATE_ALL));
                check(ran, what + ": the batch was refused");
                for (int j = 0; j < object.size(); j++) {
                    if (j != i) {
                        check(object.isPart(level.getBlockState(object.pos(j)), j), what + ": partner " + j + " was touched");
                    }
                }
                object.clear(level);
                clearDrops(helper);
            }
        }
        helper.succeed();
    }
}
