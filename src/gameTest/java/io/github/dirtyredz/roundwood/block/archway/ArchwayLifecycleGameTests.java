package io.github.dirtyredz.roundwood.block.archway;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimNbt;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.assertArchway;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.assertNoArchway;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.block;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.place;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.point;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.write;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.STRIP;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropReport;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * An archway's lifecycle (0.12.0): breaking any cell removes the whole archway and drops it once in its
 * look; a neighbouring archway is untouched; losing the floor breaks it once; lone or partial cells are
 * removed, never completed; it works across a chunk boundary; the passage is open while the jambs
 * collide; it is no door and has no redstone behaviour; its look and trims survive a save, and a trim
 * payload of an unknown version is kept and refuses edits. Every test gives its archways a look no
 * other test drops, so neighbouring tests' items are never counted.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchwayLifecycleGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final Appearance JUNGLE = wood("jungle_planks");
    private static final Appearance ACACIA = wood("acacia_planks");
    private static final Appearance MANGROVE = wood("mangrove_planks");
    private static final Appearance CRIMSON = wood("crimson_planks");
    private static final Appearance BAMBOO = wood("bamboo_planks");

    private ArchwayLifecycleGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void breakingAnyCellDropsTheArchwayOnceInItsLook(GameTestHelper helper) {
        floor(helper);
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (ArchwayPart part : ArchwayPart.values()) {
                String what = design + " " + part;
                ArchwayLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, design, JUNGLE);
                clearDrops(helper);
                ServerPlayer breaker = player(helper, "break_" + design + "_" + part, GameType.SURVIVAL, Direction.NORTH);
                check(breaker.gameMode.destroyBlock(archway.pos(part)), what + ": break refused");
                assertNoArchway(helper, archway);
                check(dropped(helper, block(design).asItem(), JUNGLE) == 1,
                        what + ": expected one jungle archway, got " + dropReport(helper, block(design).asItem(), JUNGLE));
                clearDrops(helper);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void adjacentArchwaysAreIndependent(GameTestHelper helper) {
        floor(helper);
        ArchwayLayout.Assembly left = write(helper, ANCHOR.west(2), Direction.NORTH, ArchwayDesign.ORDINARY, ACACIA);
        ArchwayLayout.Assembly right = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.KEYHOLE, ACACIA);
        for (ArchwayPart part : ArchwayPart.values()) {
            clearDrops(helper);
            ServerPlayer breaker = player(helper, "adjacent_" + part, GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(left.pos(part)), part + ": break refused");
            assertNoArchway(helper, left);
            assertArchway(helper, right);
            check(dropped(helper, block(ArchwayDesign.ORDINARY).asItem(), ACACIA) == 1, part + ": expected one archway");
            check(dropped(helper, block(ArchwayDesign.KEYHOLE).asItem(), ACACIA) == 0, part + ": the neighbour dropped");
            left = write(helper, ANCHOR.west(2), Direction.NORTH, ArchwayDesign.ORDINARY, ACACIA);
        }
        clearDrops(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void losingTheFloorBreaksTheArchwayOnce(GameTestHelper helper) {
        floor(helper);
        ArchwayLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.KEYHOLE, MANGROVE);
        anchorEntity(helper, archway).setTrims(new FaceTrims(Optional.of(MANGROVE), Optional.empty()));
        clearDrops(helper);
        helper.setBlock(ANCHOR.east().below(), Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            assertNoArchway(helper, archway);
            check(dropped(helper, block(ArchwayDesign.KEYHOLE).asItem(), MANGROVE) == 1,
                    "expected one archway, got " + dropReport(helper, block(ArchwayDesign.KEYHOLE).asItem(), MANGROVE));
            check(dropped(helper, ModItems.ROUND_TRIM.get(), MANGROVE) == 1,
                    "expected the front trim once");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void partialArchwaysAreRemovedNeverCompleted(GameTestHelper helper) {
        floor(helper);
        ArchwayLayout.Assembly lone = assembly(helper, ANCHOR.west(2), Direction.NORTH, ArchwayDesign.ORDINARY);
        helper.getLevel().setBlock(lone.anchor(), lone.stateFor(ArchwayPart.LOWER_LEFT, block(ArchwayDesign.ORDINARY)), 3);
        ArchwayLayout.Assembly three = assembly(helper, ANCHOR.east(), Direction.NORTH, ArchwayDesign.KEYHOLE);
        for (ArchwayPart part : new ArchwayPart[] {ArchwayPart.LOWER_LEFT, ArchwayPart.UPPER_LEFT, ArchwayPart.LOWER_RIGHT}) {
            helper.getLevel().setBlock(three.pos(part), three.stateFor(part, block(ArchwayDesign.KEYHOLE)), 3);
        }
        helper.runAfterDelay(3, () -> {
            assertNoArchway(helper, lone);
            assertNoArchway(helper, three);
            for (BlockPos pos : lone.positions()) {
                check(helper.getLevel().getBlockState(pos).isAir(), "a lone cell was completed or left at " + pos);
            }
            check(helper.getLevel().getBlockState(three.pos(ArchwayPart.UPPER_RIGHT)).isAir(), "the missing cell was created");
            helper.succeed();
        });
    }

    @GameTest(template = STRIP)
    public static void placesAndBreaksAcrossAChunkBoundary(GameTestHelper helper) {
        floor(helper, 17, 4);
        int x = 0;
        while ((helper.absolutePos(new BlockPos(x, 0, 0)).getX() & 15) != 15) {
            x++;
        }
        BlockPos anchor = new BlockPos(x, 1, 1);
        for (ArchwayPart broken : new ArchwayPart[] {ArchwayPart.LOWER_LEFT, ArchwayPart.UPPER_RIGHT}) {
            ServerPlayer player = player(helper, "chunks_" + broken, GameType.SURVIVAL, Direction.NORTH);
            check(place(helper, player,
                    styled(block(ArchwayDesign.ORDINARY), CRIMSON), anchor).consumesAction(),
                    broken + ": placement failed");
            ArchwayLayout.Assembly archway = assembly(helper, anchor, Direction.NORTH, ArchwayDesign.ORDINARY);
            check((archway.pos(ArchwayPart.LOWER_LEFT).getX() >> 4) != (archway.pos(ArchwayPart.LOWER_RIGHT).getX() >> 4),
                    "test setup does not straddle chunks");
            assertArchway(helper, archway);
            check(anchorEntity(helper, archway).appearance().equals(CRIMSON), broken + ": the stack's look was not installed");
            clearDrops(helper);
            ServerPlayer breaker = player(helper, "chunks_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(archway.pos(broken)), broken + ": break refused");
            assertNoArchway(helper, archway);
            check(dropped(helper, block(ArchwayDesign.ORDINARY).asItem(), CRIMSON) == 1, broken + ": expected one archway");
            clearDrops(helper);
        }
        helper.succeed();
    }

    /** The passage is open to a player-sized box; the jambs and crown collide; the cells never occlude. */
    @GameTest(template = TEMPLATE)
    public static void playersWalkThroughThePassageNotTheJambs(GameTestHelper helper) {
        floor(helper);
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
                String what = design + " " + facing;
                ArchwayLayout.Assembly archway = write(helper, ANCHOR, facing, design, Appearance.DEFAULT);
                AABB passage = new AABB(point(archway, 0.7, 0.01, 0.05), point(archway, 1.3, 1.8, 0.95));
                check(helper.getLevel().noCollision(passage), what + ": the passage collides");
                AABB jamb = new AABB(point(archway, 0.02, 0.2, 0.2), point(archway, 0.1, 0.8, 0.8));
                check(!helper.getLevel().noCollision(jamb), what + ": the left jamb does not collide");
                AABB crown = new AABB(point(archway, 0.95, 1.9, 0.2), point(archway, 1.05, 1.98, 0.8));
                check(!helper.getLevel().noCollision(crown), what + ": the crown does not collide");
                for (BlockPos cell : archway.positions()) {
                    BlockState state = helper.getLevel().getBlockState(cell);
                    check(!state.canOcclude(), what + ": a cell occludes");
                }
                helper.setBlock(ANCHOR, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    /** No door tags, its own family tag, no redstone: power beside it changes nothing. */
    @GameTest(template = TEMPLATE)
    public static void anArchwayIsNoDoorAndIgnoresRedstone(GameTestHelper helper) {
        floor(helper);
        ArchwayLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.ORDINARY, BAMBOO);
        List<BlockState> before = new ArrayList<>();
        for (BlockPos cell : archway.positions()) {
            BlockState state = helper.getLevel().getBlockState(cell);
            check(state.is(ModTags.ARCHWAYS) && !state.is(BlockTags.DOORS) && !state.is(BlockTags.WOODEN_DOORS), "tags at " + cell);
            check(!state.isSignalSource(), "a cell is a signal source");
            before.add(state);
        }
        helper.setBlock(ANCHOR.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(ANCHOR.east(2).above(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < archway.positions().size(); i++) {
                check(helper.getLevel().getBlockState(archway.positions().get(i)).equals(before.get(i)), "redstone changed a cell");
            }
            assertArchway(helper, archway);
            helper.setBlock(ANCHOR.west(), Blocks.AIR);
            helper.setBlock(ANCHOR.east(2).above(), Blocks.AIR);
            helper.succeed();
        });
    }

    /** Look and both trims survive a save; a trim payload of an unknown version is kept byte for byte and refuses edits. */
    @GameTest(template = TEMPLATE)
    public static void looksAndTrimsSaveAndUnknownTrimsAreKept(GameTestHelper helper) {
        floor(helper);
        Appearance blue = Appearance.DEFAULT.painted(DyeColor.BLUE);
        ArchwayLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.KEYHOLE, blue);
        ArchwayBlockEntity entity = anchorEntity(helper, archway);
        FaceTrims trims = new FaceTrims(Optional.of(Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"))),
                Optional.of(JUNGLE));
        check(entity.setTrims(trims), "trims refused");
        CompoundTag saved = entity.saveWithoutMetadata();
        ArchwayBlockEntity loaded = new ArchwayBlockEntity(ModBlockEntities.archway(), archway.anchor(), entity.getBlockState());
        loaded.load(saved);
        check(loaded.appearance().equals(blue), "look after load " + loaded.appearance());
        check(loaded.trims().equals(trims), "trims after load " + loaded.trims());

        CompoundTag future = new CompoundTag();
        CompoundTag payload = new CompoundTag();
        payload.putInt("Version", 99);
        payload.putString("Mystery", "kept");
        future.put(ArchwayTrimNbt.TAG, payload);
        ArchwayBlockEntity unknown = new ArchwayBlockEntity(ModBlockEntities.archway(), archway.anchor(), entity.getBlockState());
        unknown.load(future);
        check(unknown.trims().equals(FaceTrims.NONE) && !unknown.trimsEditable(), "an unknown payload was read or is editable");
        check(!unknown.setTrims(FaceTrims.NONE.with(AttachmentFace.FRONT, JUNGLE)), "an unknown payload accepted an edit");
        check(unknown.saveWithoutMetadata().getCompound(ArchwayTrimNbt.TAG).equals(payload), "the unknown payload was not kept");
        CompoundTag update = unknown.getUpdateTag();
        check(update.getCompound(ArchwayTrimNbt.TAG).getInt("Version") == ArchwayTrimNbt.VERSION
                && !update.getCompound(ArchwayTrimNbt.TAG).contains("Front"), "clients were sent an unknown payload");
        helper.succeed();
    }

    private static Appearance wood(String block) {
        return Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", block));
    }
}
