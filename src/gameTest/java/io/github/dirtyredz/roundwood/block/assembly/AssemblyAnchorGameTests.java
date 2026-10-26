package io.github.dirtyredz.roundwood.block.assembly;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.Family;
import io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.Instance;
import io.github.dirtyredz.roundwood.fixture.assembly.SingleChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.LOOK;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.OTHER_LOOK;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.all;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.drops;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.lootBuilder;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.silentClear;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.swap;
import static io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies.writeRaw;

/**
 * Characterisation of how a part finds its assembly's anchor (where the look lives) in the round door,
 * arched door, both archways and the compatibility frame, which are about to share one implementation:
 * the lookup accepts a cell of the same block class with the same reconstructed layout (so a canonical
 * block and a 0.4.0 alias block of one object resolve to one anchor) and nothing else; it never loads a
 * chunk; the loot falls back to the entity captured with a broken anchor; and a restyling target reports
 * the clicked cell and exists only for a complete object.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblyAnchorGameTests {
    private static final int FAR_CHUNK_X = 2000;
    private static final int FAR_CHUNK_Z = 2100;

    private AssemblyAnchorGameTests() {
    }

    private static BlockPos slot(GameTestHelper helper, int row, int column) {
        return helper.absolutePos(new BlockPos(1 + column * 4, 1, row * 2));
    }

    private static void resolvesFromEveryPart(ServerLevel level, Instance object, Appearance expected, String what) {
        for (int i = 0; i < object.size(); i++) {
            BlockPos pos = object.pos(i);
            BlockState state = level.getBlockState(pos);
            check(object.anchorLook(level, pos, state).equals(Optional.of(expected)),
                    what + ": no anchor from part " + i + " (" + object.anchorLook(level, pos, state) + ")");
            check(object.look(level, pos, state).equals(expected), what + ": wrong look from part " + i + ": " + object.look(level, pos, state));
        }
    }

    /** As the overload below, reading the cell from the world (it must be loaded). */
    private static void fallsBackToDefault(ServerLevel level, Instance object, int from, String what) {
        fallsBackToDefault(level, object, from, level.getBlockState(object.pos(from)), what);
    }

    /** The cell {@code from} is queried with {@code state}, never read from the world (it may be unloaded). */
    private static void fallsBackToDefault(ServerLevel level, Instance object, int from, BlockState state, String what) {
        BlockPos pos = object.pos(from);
        check(object.anchorLook(level, pos, state).isEmpty(), what + ": found an anchor from part " + from);
        check(object.look(level, pos, state).equals(AppearanceDefaults.of(state.getBlock())),
                what + ": part " + from + " did not fall back to its block's default look: " + object.look(level, pos, state));
    }

    /** Every part of a canonical object finds the anchor's look, in every family and facing. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void everyPartOfACanonicalObjectResolvesItsAnchor(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        for (int row = 0; row < families.size(); row++) {
            for (Direction facing : List.of(Direction.NORTH, Direction.SOUTH)) {
                Instance object = families.get(row).at(slot(helper, row, 0), facing);
                check(object.place(level, LOOK), object.family() + " " + facing + ": placement refused");
                resolvesFromEveryPart(level, object, LOOK, object.family() + " " + facing);
                object.clear(level);
            }
        }
        helper.succeed();
    }

    /**
     * A 0.4.0 alias block of the same class and layout is the same object: with a canonical anchor and alias
     * parts, or an alias anchor and canonical parts, every cell still finds the anchor. (The two families that
     * have aliases: the round door and the frame.)
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void canonicalAndAliasCellsOfOneObjectShareTheirAnchor(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        int mixed = 0;
        for (int row = 0; row < families.size(); row++) {
            Family family = families.get(row);
            if (family.alias() == null) {
                continue;
            }
            Instance object = family.at(slot(helper, row, 0), Direction.NORTH);
            List<BlockPos> cells = object.positions();

            check(object.place(level, LOOK), family + ": placement refused");
            for (int i = 1; i < object.size(); i++) {
                swap(level, cells, object.pos(i), object.state(i, family.alias()));
            }
            check(level.getBlockState(object.pos(1)).getBlock() == family.alias(), family + ": the parts were not swapped to the alias");
            resolvesFromEveryPart(level, object, LOOK, family + " canonical anchor, alias parts");
            object.clear(level);

            check(object.place(level, LOOK), family + ": placement refused");
            swap(level, cells, object.pos(0), object.state(0, family.alias()));
            check(level.getBlockState(object.pos(0)).getBlock() == family.alias(), family + ": the anchor was not swapped to the alias");
            check(object.setLook(level, LOOK), family + ": the alias anchor has no block entity");
            resolvesFromEveryPart(level, object, LOOK, family + " alias anchor, canonical parts");
            object.clear(level);
            mixed++;
        }
        check(mixed == 2, "expected the round door and the frame to have aliases, found " + mixed);
        helper.succeed();
    }

    /**
     * A cell at the anchor position that reconstructs to a different layout (another facing, another hinge,
     * another design or archway profile) is not this object's anchor, whatever look it carries.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aDifferentLayoutAtTheAnchorIsNoAnchor(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        int tried = 0;
        for (int row = 0; row < families.size(); row++) {
            Instance object = families.get(row).at(slot(helper, row, 0), Direction.NORTH);
            List<BlockState> foreigners = object.foreignLayouts(0);
            for (int n = 0; n < foreigners.size(); n++) {
                String what = object.family() + " foreign anchor #" + n;
                check(object.place(level, LOOK), what + ": placement refused");
                swap(level, object.positions(), object.pos(0), foreigners.get(n));
                object.setLook(level, OTHER_LOOK); // a decoy look, if the foreign cell has an entity
                for (int from = 1; from < object.size(); from++) {
                    fallsBackToDefault(level, object, from, what);
                }
                object.clear(level);
                tried++;
            }
        }
        check(tried == 7, "expected 3 + 3 + 1 foreign layouts, tried " + tried);
        helper.succeed();
    }

    /** An anchor cell that is air, or holds another family's block, is no anchor. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aMissingOrForeignAnchorFallsBackToTheDefault(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        for (int row = 0; row < families.size(); row++) {
            Family family = families.get(row);
            Instance object = family.at(slot(helper, row, 0), Direction.NORTH);

            check(object.place(level, LOOK), family + ": placement refused");
            swap(level, object.positions(), object.pos(0), Blocks.AIR.defaultBlockState());
            for (int from = 1; from < object.size(); from++) {
                fallsBackToDefault(level, object, from, family + " anchor air");
            }
            object.clear(level);

            Family other = families.get((row + 2) % families.size());
            Instance intruder = other.at(object.anchor(), Direction.NORTH);
            check(object.place(level, LOOK), family + ": placement refused");
            swap(level, object.positions(), object.pos(0), intruder.state(0));
            for (int from = 1; from < object.size(); from++) {
                fallsBackToDefault(level, object, from, family + " anchor holding " + other);
            }
            object.clear(level);
        }
        helper.succeed();
    }

    /**
     * An anchor in an unloaded chunk is no anchor, and asking never loads it: once through coordinates
     * nothing has ever loaded, once with a part at the edge of the only loaded chunk whose anchor lies in
     * the unloaded neighbour (the single-column arched door can only take the first).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void anAnchorInAnUnloadedChunkIsNoAnchorAndStaysUnloaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int y = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        for (Family family : all()) {
            Instance far = family.at(new BlockPos(1_000_000, y, 1_000_000), Direction.NORTH);
            check(!level.hasChunk(1_000_000 >> 4, 1_000_000 >> 4), family + ": setup, the far chunk is loaded");
            for (int from = 0; from < far.size(); from++) {
                fallsBackToDefault(level, far, from, far.state(from), family + " far away");
            }
            check(!level.hasChunk(1_000_000 >> 4, 1_000_000 >> 4), family + ": the far chunk was loaded by the lookup");
        }
        int edge = 0;
        try (SingleChunk chunk = SingleChunk.open(level, FAR_CHUNK_X, FAR_CHUNK_Z)) {
            for (Family family : all()) {
                Instance probe = family.at(new BlockPos(chunk.minX(), y, chunk.midZ()), Direction.NORTH);
                if (probe.columns() < 2) {
                    continue;
                }
                BlockPos cell = new BlockPos(chunk.minX(), y, chunk.midZ());
                Instance object = probe.atCell(2, cell);
                writeRaw(level, cell, object.state(2));
                edge++;
                try {
                    fallsBackToDefault(level, object, 2, object.state(2), family + " anchor in the west chunk");
                    chunk.assertIsolated(family + ": after the lookup");
                } finally {
                    writeRaw(level, cell, Blocks.AIR.defaultBlockState());
                }
            }
        }
        check(edge == 2, "expected 2 multi-column families, visited " + edge);
        helper.succeed();
    }

    /**
     * Loot is evaluated after the anchor may be gone: the look then comes from the entity captured with the
     * broken part when that is the family's anchor entity, not from a captured entity of another type. A live
     * anchor still wins over a captured entity of its own class. The full loot carries the same look.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void lootFallsBackToTheCapturedAnchorEntity(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        for (int row = 0; row < families.size(); row++) {
            Family family = families.get(row);
            Instance object = family.at(slot(helper, row, 0), Direction.NORTH);
            Instance decoy = family.at(slot(helper, row, 1), Direction.NORTH);
            check(object.place(level, LOOK) && decoy.place(level, OTHER_LOOK), family + ": placement refused");
            BlockPos anchor = object.anchor();
            BlockState anchorState = level.getBlockState(anchor);
            BlockEntity captured = level.getBlockEntity(anchor);
            BlockEntity decoyEntity = level.getBlockEntity(decoy.anchor());
            check(captured != null && decoyEntity != null, family + ": no anchor entities");

            check(object.dropsLook(lootBuilder(level, anchor, decoyEntity), anchorState).equals(LOOK),
                    family + ": a captured entity beat the live anchor");

            silentClear(level, object.positions());
            BlockEntity wrong = new SignBlockEntity(anchor, Blocks.OAK_SIGN.defaultBlockState());
            Appearance fallback = AppearanceDefaults.of(anchorState.getBlock());

            check(object.dropsLook(lootBuilder(level, anchor, captured), anchorState).equals(LOOK),
                    family + ": the captured anchor entity's look was lost");
            check(object.dropsLook(lootBuilder(level, anchor, null), anchorState).equals(fallback),
                    family + ": no anchor and no entity did not give the default look");
            check(object.dropsLook(lootBuilder(level, anchor, wrong), anchorState).equals(fallback),
                    family + ": a captured entity of another type was not ignored");

            List<ItemStack> withCaptured = drops(level, anchorState, anchor, captured);
            check(lookOfDrops(family, withCaptured).equals(Optional.of(LOOK)),
                    family + ": the dropped item lost the captured look: " + withCaptured);
            List<ItemStack> withWrong = drops(level, anchorState, anchor, wrong);
            check(lookOfDrops(family, withWrong).equals(Optional.of(fallback)),
                    family + ": the dropped item took a look from a foreign entity: " + withWrong);
            decoy.clear(level);
            clearDrops(helper);
        }
        helper.succeed();
    }

    private static Optional<Appearance> lookOfDrops(Family family, List<ItemStack> drops) {
        return drops.stream().filter(stack -> stack.is(family.item())).map(AppearanceStacks::read).findFirst();
    }

    /**
     * Restyling reports the cell that was clicked as its origin (where the sound plays), not the anchor, from
     * every part; and exists only while the object is complete: a missing part, the anchor included, leaves
     * no target from any other part.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aRestylingTargetReportsTheClickedCellAndNeedsACompleteObject(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        List<Family> families = all();
        for (int row = 0; row < families.size(); row++) {
            Instance object = families.get(row).at(slot(helper, row, 0), Direction.NORTH);
            check(object.place(level, LOOK), object.family() + ": placement refused");
            for (int i = 0; i < object.size(); i++) {
                BlockPos pos = object.pos(i);
                Optional<AppearanceActions.Target> target = object.target(level, pos, level.getBlockState(pos));
                check(target.isPresent(), object.family() + ": no target from part " + i);
                check(target.get().origin().equals(pos), object.family() + ": target origin " + target.get().origin()
                        + " is not the clicked cell " + pos);
                check(target.get().current().equals(Optional.of(LOOK)), object.family() + ": target shows the wrong look");
            }
            object.clear(level);

            for (int missing : new int[] {0, object.size() - 1}) {
                check(object.place(level, LOOK), object.family() + ": placement refused");
                BlockState[] states = new BlockState[object.size()];
                for (int i = 0; i < object.size(); i++) {
                    states[i] = level.getBlockState(object.pos(i));
                }
                silentClear(level, List.of(object.pos(missing)));
                for (int i = 0; i < object.size(); i++) {
                    if (i != missing) {
                        check(object.target(level, object.pos(i), states[i]).isEmpty(),
                                object.family() + ": a target from part " + i + " with part " + missing + " missing");
                    }
                }
                object.clear(level);
            }
        }
        clearDrops(helper);
        helper.succeed();
    }
}
