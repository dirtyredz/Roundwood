package io.github.dirtyredz.roundwood.showcase.section;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.doorLook;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.masonry;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.paints;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.signSays;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.trimLook;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.windowLook;

/**
 * The sections that belong to neither the door nor the window family, read back from the world: the
 * freestanding brick arches and the sixteen paints (door, trim, tinted glass and window each). Called
 * right after building and again after the integrity ticks, for every facing the showcase test builds.
 */
public final class SectionChecks {
    private SectionChecks() {
    }

    public static void verifyArches(ServerLevel level, ShowcaseFrame frame) {
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

    public static void verifyPaints(ServerLevel level, ShowcaseFrame frame) {
        List<Appearance> paints = paints();
        check(paints.size() == 16, "expected all sixteen paints, have " + paints.size());
        for (int i = 0; i < paints.size(); i++) {
            Appearance paint = paints.get(i);
            int r = PaintsSection.r(i);
            int f = PaintsSection.f(i);
            DoorLayout.Assembly door = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), DoorHingeSide.LEFT, DoorDesign.ROUND);
            check(door.complete(level, ModBlocks.roundDoor()) && paint.equals(doorLook(level, door.anchor())), paint.key() + ": door");
            check(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity
                    && entity.trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, paint)), paint.key() + ": door trim");
            BlockPos glass = frame.pos(r + PaintsSection.GLASS_OFFSET, 0, f);
            check(level.getBlockEntity(glass) instanceof RoundWindowBlockEntity tinted && tinted.glass().equals(paint.paint())
                    && tinted.appearance().equals(Appearance.DEFAULT), paint.key() + ": tinted glass");
            BlockPos window = frame.pos(r + PaintsSection.WINDOW_OFFSET, 0, f);
            check(level.getBlockState(window).is(ModBlocks.roundWindow()) && paint.equals(windowLook(level, window)), paint.key() + ": window");
            check(signSays(level, frame.pos(r, 0, f - 1), ShowcaseSamples.label(paint)), paint.key() + ": label sign missing");
        }
    }
}
