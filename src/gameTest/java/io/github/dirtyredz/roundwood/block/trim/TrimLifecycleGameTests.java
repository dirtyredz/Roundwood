package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.STRIP;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertNoTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/**
 * The legacy frame's life (frames are placed by the fixture writer, not the item): structure transforms (rotate / mirror), neighbouring frames staying independent,
 * the integrity tick for incomplete frames, and placement across a chunk boundary. The frame has no
 * block entity, so there is no save/load round trip to mirror from the door suite.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimLifecycleGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final Rotation[] ROTATIONS = Rotation.values();
    private static final Mirror[] MIRRORS = Mirror.values();

    private TrimLifecycleGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void rotatedAndMirroredStatesStayOneConsistentFrame(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, Direction.NORTH);
        check(TrimPlacement.placeAt(helper.getLevel(), oakTrim(), assembly),
                "setup placement failed");
        BlockPos pivot = assembly.anchor();
        for (Mirror mirror : MIRRORS) {
            for (Rotation rotation : ROTATIONS) {
                String name = mirror + "/" + rotation;
                Set<TrimPart> parts = EnumSet.noneOf(TrimPart.class);
                Set<Direction> facings = new HashSet<>();
                Set<BlockPos> anchors = new HashSet<>();
                for (TrimPart part : TrimPart.values()) {
                    BlockState state = helper.getLevel().getBlockState(assembly.pos(part)).mirror(mirror).rotate(rotation);
                    BlockPos moved = StructureTemplate.transform(assembly.pos(part), mirror, rotation, pivot);
                    parts.add(state.getValue(RoundTrimBlock.PART));
                    facings.add(state.getValue(RoundTrimBlock.FACING));
                    anchors.add(TrimLayout.of(moved, state).anchor());
                    if (mirror == Mirror.NONE) {
                        check(state.getValue(RoundTrimBlock.PART) == part, name + ": rotation changed the part " + part);
                    } else {
                        check(state.getValue(RoundTrimBlock.PART) == part.mirrored(), name + ": mirror did not swap the column of " + part);
                    }
                }
                check(parts.size() == 4, name + ": quarters collapsed to " + parts);
                check(facings.size() == 1, name + ": quarters disagree on facing " + facings);
                check(anchors.size() == 1, name + ": quarters disagree on the anchor " + anchors);
                // The transformed anchor and facing must put every quarter where the transform put it.
                TrimLayout.Assembly moved = new TrimLayout.Assembly(anchors.iterator().next(), facings.iterator().next());
                for (TrimPart part : TrimPart.values()) {
                    BlockPos expected = StructureTemplate.transform(assembly.pos(part), mirror, rotation, pivot);
                    BlockState state = helper.getLevel().getBlockState(assembly.pos(part)).mirror(mirror).rotate(rotation);
                    check(moved.pos(state.getValue(RoundTrimBlock.PART)).equals(expected),
                            name + ": " + part + " lands at " + moved.pos(state.getValue(RoundTrimBlock.PART)) + ", expected " + expected);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void frameSurvivesStructureTransforms(GameTestHelper helper) {
        transformed(helper, 0);
    }

    /**
     * Builds a frame in the source corner, copies it to a template and places it at the centre under
     * rotation / mirror combination {@code index}, then checks the result and moves to the next combination.
     */
    private static void transformed(GameTestHelper helper, int index) {
        if (index == 0) {
            floor(helper);
            check(TrimPlacement.placeAt(helper.getLevel(), oakTrim(),
                    trimAssembly(helper, new BlockPos(1, 1, 1), Direction.NORTH)), "source placement failed");
        }
        Rotation rotation = ROTATIONS[index / MIRRORS.length];
        Mirror mirror = MIRRORS[index % MIRRORS.length];
        String name = rotation + "/" + mirror;
        ServerLevel level = helper.getLevel();
        BlockPos from = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos dest = helper.absolutePos(new BlockPos(5, 1, 5));
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, from, new Vec3i(3, 2, 3), false, Blocks.STRUCTURE_VOID);
        template.placeInWorld(level, dest, dest, new StructurePlaceSettings().setRotation(rotation).setMirror(mirror),
                level.getRandom(), Block.UPDATE_CLIENTS);

        TrimLayout.Assembly placed = findPlaced(helper, dest);
        check(placed != null, name + ": no frame quarter found at the destination");
        check(placed.complete(level, oakTrim()), name + ": placed frame is incomplete");
        helper.runAfterDelay(3, () -> {
            check(placed.complete(level, oakTrim()), name + ": integrity check removed or altered the placed frame");
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = 0; y <= 1; y++) {
                        level.removeBlock(dest.offset(x, y, z), false);
                    }
                }
            }
            if (index + 1 < ROTATIONS.length * MIRRORS.length) {
                transformed(helper, index + 1);
            } else {
                helper.succeed();
            }
        });
    }

    /** The frame any quarter in the 5 x 5 box around {@code dest} belongs to. */
    private static TrimLayout.Assembly findPlaced(GameTestHelper helper, BlockPos dest) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = dest.offset(x, 0, z);
                BlockState state = helper.getLevel().getBlockState(pos);
                if (state.is(oakTrim())) {
                    return TrimLayout.of(pos, state);
                }
            }
        }
        return null;
    }

    @GameTest(template = TEMPLATE)
    public static void adjacentFramesAreIndependent(GameTestHelper helper) {
        floor(helper);
        BlockPos second = ANCHOR.east(2);
        TrimLayout.Assembly left = trimAssembly(helper, ANCHOR, Direction.NORTH);
        TrimLayout.Assembly right = trimAssembly(helper, second, Direction.NORTH);
        check(TrimPlacement.placeAt(helper.getLevel(), oakTrim(), left), "left placement failed");
        check(TrimPlacement.placeAt(helper.getLevel(), oakTrim(), right), "right placement failed");
        for (TrimPart part : TrimPart.values()) {
            clearDrops(helper);
            ServerPlayer breaker = player(helper, "adjacent_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(left.pos(part)), part + ": break refused");
            assertNoTrim(helper, left);
            assertTrim(helper, right, oakTrim());
            check(dropped(helper, oakTrim().asItem()) == 1, part + ": expected one frame, got " + dropped(helper, oakTrim().asItem()));
            check(TrimPlacement.placeAt(helper.getLevel(), oakTrim(), left),
                    part + ": re-placement failed");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aLoneLowerQuarterIsRemovedByTheIntegrityTick(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, Direction.NORTH);
        helper.setBlock(ANCHOR, assembly.stateFor(TrimPart.LOWER_LEFT, oakTrim()));
        lookOnAnchor(helper, assembly, BIRCH);
        clearDrops(helper);
        helper.runAfterDelay(3, () -> {
            assertNoTrim(helper, assembly);
            for (BlockPos pos : assembly.positions()) {
                check(helper.getLevel().getBlockState(pos).isAir(), "a quarter was created or left at " + pos);
            }
            check(dropped(helper, oakTrim().asItem(), BIRCH) <= 1, "incomplete cleanup dropped " + io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropReport(helper, oakTrim().asItem(), BIRCH));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void threeQuartersAreRemovedAndNeverCompleted(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, Direction.NORTH);
        for (TrimPart part : new TrimPart[] {TrimPart.LOWER_LEFT, TrimPart.UPPER_LEFT, TrimPart.LOWER_RIGHT}) {
            helper.setBlock(ANCHOR.relative(Direction.EAST, part.column).above(part.row), assembly.stateFor(part, oakTrim()));
        }
        lookOnAnchor(helper, assembly, SPRUCE);
        clearDrops(helper);
        helper.runAfterDelay(3, () -> {
            assertNoTrim(helper, assembly);
            check(helper.getLevel().getBlockState(assembly.pos(TrimPart.UPPER_RIGHT)).isAir(), "the missing quarter was created");
            check(dropped(helper, oakTrim().asItem(), SPRUCE) <= 1, "incomplete cleanup dropped " + io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropReport(helper, oakTrim().asItem(), SPRUCE));
            helper.succeed();
        });
    }

    @GameTest(template = STRIP)
    public static void placesAndBreaksAcrossAChunkBoundary(GameTestHelper helper) {
        floor(helper, 17, 4);
        // The relative x whose absolute x is the last column of a chunk; the right column is in the next chunk.
        int x = 0;
        while ((helper.absolutePos(new BlockPos(x, 0, 0)).getX() & 15) != 15) {
            x++;
        }
        BlockPos anchor = new BlockPos(x, 1, 1);
        TrimLayout.Assembly assembly = trimAssembly(helper, anchor, Direction.NORTH);
        check((assembly.pos(TrimPart.LOWER_LEFT).getX() >> 4) != (assembly.pos(TrimPart.LOWER_RIGHT).getX() >> 4),
                "test setup does not straddle chunks");
        for (TrimPart broken : new TrimPart[] {TrimPart.LOWER_LEFT, TrimPart.UPPER_RIGHT}) {
            ServerPlayer player = player(helper, "chunks_" + broken, GameType.SURVIVAL, Direction.NORTH);
            check(placeFreestanding(helper, player, new ItemStack(oakTrim()), anchor).consumesAction(), broken + ": placement failed");
            assertTrim(helper, assembly, oakTrim());
            clearDrops(helper);
            ServerPlayer breaker = player(helper, "chunks_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(assembly.pos(broken)), broken + ": break refused");
            assertNoTrim(helper, assembly);
            check(dropped(helper, oakTrim().asItem()) == 1, broken + ": expected one frame, got " + dropped(helper, oakTrim().asItem()));
        }
        helper.succeed();
    }

    /**
     * Looks no other test drops (birch: the lone quarter, spruce: three quarters): the drop count
     * reaches past a 9-wide template, so a neighbouring test's oak drops are never counted.
     */
    private static final io.github.dirtyredz.roundwood.appearance.Appearance BIRCH = io.github.dirtyredz.roundwood.appearance.Appearance
            .wood(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "birch_planks"));
    private static final io.github.dirtyredz.roundwood.appearance.Appearance SPRUCE = io.github.dirtyredz.roundwood.appearance.Appearance
            .wood(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    /** Gives the anchor quarter's block entity {@code look}, as a placed frame in that look would have. */
    private static void lookOnAnchor(GameTestHelper helper, TrimLayout.Assembly assembly,
                                     io.github.dirtyredz.roundwood.appearance.Appearance look) {
        if (helper.getLevel().getBlockEntity(assembly.anchor()) instanceof RoundTrimBlockEntity entity) {
            entity.setAppearance(look);
        } else {
            throw new net.minecraft.gametest.framework.GameTestAssertException("no trim block entity at the anchor");
        }
    }
}
