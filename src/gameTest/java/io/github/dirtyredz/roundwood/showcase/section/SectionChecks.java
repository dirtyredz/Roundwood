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
 * placeable piece and the row of every look on one archway. Each is checked complete (the right
 * piece, look and sign) and its rectangle is checked to hold nothing else, which also proves it
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
            owned.add(sign(level, frame, window.r(), PiecesSection.F - 1, "Window"));
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
        owned.add(sign(level, frame, PiecesSection.ROUND_DOOR_R, f - 1, "Round door"));
        owned.add(sign(level, frame, PiecesSection.ARCHED_DOOR_R, f - 1, "Arched door"));
        owned.addAll(archway(level, frame, ArchwayDesign.ORDINARY, PiecesSection.ARCHWAY_R, f, Appearance.DEFAULT, "Archway"));
        owned.addAll(archway(level, frame, ArchwayDesign.KEYHOLE, PiecesSection.KEYHOLE_R, f, Appearance.DEFAULT, "Keyhole archway"));
        verifyOwnsOnly(level, frame, "pieces", 0, PiecesSection.R_MAX, 0, f, owned);
    }

    /** Every sample look on one ordinary archway each, in the samples' order, two columns apart. */
    public static void verifyMaterials(ServerLevel level, ShowcaseFrame frame) {
        List<Appearance> looks = ShowcaseSamples.all();
        check(woods().size() >= 11 && paints().size() == 16 && masonry().size() == 4, "expected 11+ woods, 16 paints, 4 masonry: " + looks.size());
        check(MaterialsSection.r(looks.size() - 1) + 1 <= MaterialsSection.R_MAX, "the materials row outgrew its rectangle");
        Set<BlockPos> owned = new HashSet<>();
        for (int i = 0; i < looks.size(); i++) {
            owned.addAll(archway(level, frame, ArchwayDesign.ORDINARY, MaterialsSection.r(i), MaterialsSection.F, looks.get(i),
                    ShowcaseSamples.label(looks.get(i))));
        }
        verifyOwnsOnly(level, frame, "materials", 0, MaterialsSection.R_MAX, MaterialsSection.F - 1, MaterialsSection.F, owned);
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
        owned.add(sign(level, frame, r, f - 1, label));
        return owned;
    }

    private static BlockPos sign(ServerLevel level, ShowcaseFrame frame, int pieceR, int f, String first) {
        BlockPos pos = frame.pos(ShowcaseCanvas.labelR(pieceR), 0, f);
        check(signSays(level, pos, first), "label sign '" + first + "' missing at " + pos);
        return pos;
    }
}
