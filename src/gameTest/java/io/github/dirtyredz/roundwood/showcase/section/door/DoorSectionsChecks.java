package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSamples;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.doorLook;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.masonry;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.signSays;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.woods;

/**
 * The door sections of the showcase read back from the world: the wood doors in every hinge and open
 * variant with their label signs, the camouflaged doors, the lever-driven redstone door and the trim
 * combinations. Called right after building and again after the integrity ticks (doors and trims), for
 * every facing the showcase test builds.
 */
public final class DoorSectionsChecks {
    private DoorSectionsChecks() {
    }

    public static void verifyDoors(ServerLevel level, ShowcaseFrame frame) {
        var block = ModBlocks.roundDoor();
        List<Appearance> woods = woods();
        check(woods.size() >= 11, "fewer than the eleven vanilla woods: " + woods.size());
        for (int i = 0; i < woods.size(); i++) {
            Appearance wood = woods.get(i);
            for (int variant = 0; variant < 4; variant++) {
                int r = DoorsSection.r(i, variant);
                int f = DoorsSection.f(i);
                DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(), DoorsSection.hinge(variant), DoorDesign.ROUND);
                String what = wood.key() + " variant " + variant;
                check(assembly.complete(level, block), what + ": door missing or incomplete");
                check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN) == DoorsSection.open(variant), what + ": wrong open state");
                check(wood.equals(doorLook(level, assembly.anchor())), what + ": anchor shows " + doorLook(level, assembly.anchor()));
                check(signSays(level, frame.pos(r, 0, f - 1), ShowcaseSamples.label(wood)), what + ": label sign missing");
            }
        }
    }

    public static void verifyCamouflage(ServerLevel level, ShowcaseFrame frame) {
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

    public static void verifyRedstone(ServerLevel level, ShowcaseFrame frame) {
        BlockPos door = frame.pos(RedstoneSection.DOOR_R, 0, RedstoneSection.DOOR_F);
        BlockPos leverPos = frame.pos(RedstoneSection.LEVER_R, 0, RedstoneSection.DOOR_F);
        BlockState lever = level.getBlockState(leverPos);
        check(lever.is(Blocks.LEVER) && !lever.getValue(LeverBlock.POWERED), "lever missing or already on");
        check(!level.getBlockState(door).getValue(DoorBlock.OPEN), "redstone door starts open");
        ((LeverBlock) Blocks.LEVER).pull(lever, level, leverPos);
        check(level.getBlockState(door).getValue(DoorBlock.OPEN), "flipping the lever did not open the door");
    }

    public static void verifyTrims(ServerLevel level, ShowcaseFrame frame) {
        List<Appearance> materials = new ArrayList<>(woods());
        materials.addAll(masonry());
        for (int i = 0; i < materials.size(); i++) {
            Appearance material = materials.get(i);
            for (int j = 0; j < TrimCombinationsSection.SAMPLES; j++) {
                int r = TrimCombinationsSection.r(i, j);
                int f = TrimCombinationsSection.f(i);
                String what = material.key() + " sample " + j;
                DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(r, 0, f), frame.forward(),
                        TrimCombinationsSection.hinge(i, j), DoorDesign.ROUND);
                check(assembly.complete(level, ModBlocks.roundDoor()), what + ": door missing or incomplete");
                check(TrimCombinationsSection.door(j).equals(doorLook(level, assembly.anchor())), what + ": door look");
                check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN) == TrimCombinationsSection.open(j), what + ": open state");
                boolean camouflaged = level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity
                        && entity.camouflage().map(c -> c.material().is(Blocks.STONE_BRICKS)).orElse(false);
                check(camouflaged == TrimCombinationsSection.camouflaged(j), what + ": camouflage");
                FaceTrims expected = new FaceTrims(Optional.of(material), TrimCombinationsSection.back(materials, i, j));
                FaceTrims shown = level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity entity ? entity.trims() : FaceTrims.NONE;
                check(expected.equals(shown), what + ": door trims " + shown);
                check(!(level.getBlockState(frame.pos(r, 0, f - 1)).getBlock() instanceof RoundTrimBlock),
                        what + ": a frame block stands in front of the door");
                check(signSays(level, frame.pos(r, 0, f - 2), ShowcaseSamples.label(material)), what + ": label sign missing");
            }
        }
    }
}
