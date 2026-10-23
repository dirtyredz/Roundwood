package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.masonry;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.paints;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.signSays;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.verifyOwnsOnly;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.woods;

/**
 * The two sections that belong to no single family, read back from the world: the row of one of each
 * placeable piece and the wall of the restyling source blocks. Each is checked complete (the right
 * piece or block, look and sign) and its rectangle is checked to hold nothing else, which also proves it
 * overlaps no other section. Called right after building and again after the integrity ticks, for
 * every facing the showcase test builds. (The pieces' windows are read back with the other listed
 * windows, in {@code WindowSectionsChecks}.)
 */
public final class SectionChecks {
    private SectionChecks() {
    }

    public static void verifyPieces(ServerLevel level, ShowcaseFrame frame) {
        Set<BlockPos> owned = new HashSet<>();
        List<ShowcaseWindow> windows = PiecesSection.windows(frame);
        check(windows.size() == 13, "expected the thirteen windows, have " + windows.size());
        check(windows.stream().map(w -> w.design().id()).distinct().count() == 13, "the pieces row repeats a window");
        for (ShowcaseWindow window : windows) {
            owned.add(frame.pos(window.r(), window.y(), window.f()));
            owned.add(sign(level, frame, window.r(), PiecesSection.F, "Window"));
        }
        int f = PiecesSection.F;
        DoorLayout.Assembly round = new DoorLayout.Assembly(frame.pos(PiecesSection.ROUND_DOOR_R, 0, f), frame.forward(), DoorHingeSide.LEFT,
                DoorDesign.ROUND);
        DoorLayout.Assembly arched = new DoorLayout.Assembly(frame.pos(PiecesSection.ARCHED_DOOR_R, 0, f), frame.forward(), DoorHingeSide.LEFT,
                DoorDesign.ARCHED);
        for (DoorLayout.Assembly door : List.of(round, arched)) {
            boolean isRound = door == round;
            String what = (isRound ? "round" : "arched") + " door";
            check(door.complete(level, isRound ? ModBlocks.roundDoor() : ModBlocks.archedDoor()), what + ": missing or incomplete");
            check(!level.getBlockState(door.anchor()).getValue(DoorBlock.OPEN), what + ": not closed");
            check(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity && entity.appearance().equals(Appearance.DEFAULT)
                    && entity.trims().equals(FaceTrims.NONE) && entity.camouflage().isEmpty(), what + ": not oak and plain");
            owned.addAll(door.positions());
        }
        owned.add(sign(level, frame, PiecesSection.ROUND_DOOR_R, f, "Round door"));
        owned.add(sign(level, frame, PiecesSection.ARCHED_DOOR_R, f, "Arched door"));
        owned.addAll(archway(level, frame, ArchwayDesign.ORDINARY, PiecesSection.ARCHWAY_R, f, Appearance.DEFAULT, "Archway"));
        owned.addAll(archway(level, frame, ArchwayDesign.KEYHOLE, PiecesSection.KEYHOLE_R, f, Appearance.DEFAULT, "Keyhole archway"));
        verifyOwnsOnly(level, frame, "pieces", 0, PiecesSection.R_MAX, f, f, owned);
    }

    /**
     * The materials wall (0.13.1): every wood's planks on the bottom row, every masonry block on the row
     * above, in the samples' order, each with a wall sign on its front face naming it, and the barrel
     * after the masonry holding one stack of each of the sixteen dyes. No archway is left behind: the
     * rectangle holds only the wall, its signs and the barrel.
     */
    public static void verifyMaterials(ServerLevel level, ShowcaseFrame frame) {
        List<Appearance> looks = ShowcaseSamples.all();
        check(woods().size() >= 11 && woods().size() <= MaterialsSection.MAX_WOODS && paints().size() == 16 && masonry().size() == 4,
                "expected 11 to 19 woods, 16 paints, 4 masonry: " + looks.size());
        Set<BlockPos> owned = new HashSet<>();
        int f = MaterialsSection.F;
        for (int i = 0; i < woods().size(); i++) {
            wallBlock(level, frame, owned, MaterialsSection.woodR(i), MaterialsSection.WOOD_Y, f, woods().get(i));
        }
        for (int i = 0; i < masonry().size(); i++) {
            wallBlock(level, frame, owned, MaterialsSection.masonryR(i), MaterialsSection.MASONRY_Y, f, masonry().get(i));
        }
        int barrelR = MaterialsSection.barrelR(masonry().size());
        BlockPos barrel = frame.pos(barrelR, MaterialsSection.MASONRY_Y, f);
        owned.add(barrel);
        owned.add(frame.pos(barrelR, MaterialsSection.MASONRY_Y, f - 1));
        check(level.getBlockState(barrel).is(Blocks.BARREL) && level.getBlockEntity(barrel) instanceof BarrelBlockEntity, "the dye barrel is missing");
        BarrelBlockEntity contents = (BarrelBlockEntity) level.getBlockEntity(barrel);
        List<ItemStack> expected = MaterialsSection.dyes(paints());
        for (int i = 0; i < expected.size(); i++) {
            check(ItemStack.matches(expected.get(i), contents.getItem(i)), "dye " + i + " is " + contents.getItem(i) + ", expected " + expected.get(i));
        }
        check(wallSignSays(level, frame.pos(barrelR, MaterialsSection.MASONRY_Y, f - 1), "Dyes"), "the dye barrel is not labelled");
        verifyOwnsOnly(level, frame, "materials", 0, MaterialsSection.R_MAX, f - 1, f, owned);
    }

    /** A planks or masonry block on the wall in its place, with a wall sign naming it on its front face. */
    private static void wallBlock(ServerLevel level, ShowcaseFrame frame, Set<BlockPos> owned, int r, int y, int f, Appearance look) {
        BlockPos block = frame.pos(r, y, f);
        BlockPos sign = frame.pos(r, y, f - 1);
        check(level.getBlockState(block).is(MaterialsSection.block(look)), ShowcaseSamples.label(look) + ": the wall block at r " + r + " y " + y
                + " is " + level.getBlockState(block));
        check(wallSignSays(level, sign, MaterialsSection.lines(ShowcaseSamples.label(look))[0]),
                ShowcaseSamples.label(look) + ": no wall sign at " + sign);
        owned.add(block);
        owned.add(sign);
    }

    private static boolean wallSignSays(ServerLevel level, BlockPos pos, String first) {
        return level.getBlockState(pos).getBlock() instanceof WallSignBlock && signSays(level, pos, first);
    }

    /** One archway read back (complete, design, look, no trims) plus its sign; returns the cells it and its sign own. */
    private static Set<BlockPos> archway(ServerLevel level, ShowcaseFrame frame, ArchwayDesign design, int r, int f, Appearance look,
                                         String label) {
        String what = design + " archway (" + look.key() + ") r " + r;
        ArchwayLayout.Assembly assembly = new ArchwayLayout.Assembly(frame.pos(r, 0, f), frame.forward(), design);
        check(assembly.complete(level, ModBlocks.archway(design)), what + ": missing or incomplete");
        check(level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity entity && entity.appearance().equals(look)
                && entity.trims().equals(FaceTrims.NONE), what + ": wrong look or trims");
        Set<BlockPos> owned = new HashSet<>(assembly.positions());
        owned.add(sign(level, frame, r, f, label));
        return owned;
    }

    private static BlockPos sign(ServerLevel level, ShowcaseFrame frame, int pieceR, int f, String first) {
        BlockPos pos = frame.pos(ShowcaseCanvas.labelR(pieceR), 0, f);
        check(signSays(level, pos, first), "label sign '" + first + "' missing at " + pos);
        return pos;
    }
}
