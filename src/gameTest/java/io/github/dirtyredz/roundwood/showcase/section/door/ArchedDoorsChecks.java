package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * The showcase's arched-door section read back from the world (0.11.0), from the same list the
 * section builds ({@link ArchedDoorsSection#doors}): every door complete in the listed hinge, open
 * state, look, face trims and camouflage, with its sign; nothing in the cell beside each door; and
 * the lever opening the redstone door. Called right after building and again after the integrity
 * ticks, in every facing.
 */
public final class ArchedDoorsChecks {
    private ArchedDoorsChecks() {
    }

    public static void verify(ServerLevel level, ShowcaseFrame frame) {
        for (ArchedDoorsSection.Exhibit door : ArchedDoorsSection.doors()) {
            String what = "arched door r " + door.r() + " f " + door.f();
            DoorLayout.Assembly assembly = new DoorLayout.Assembly(frame.pos(door.r(), 0, door.f()), frame.forward(), door.hinge(),
                    DoorDesign.ARCHED);
            check(assembly.complete(level, ModBlocks.archedDoor()), what + ": missing or incomplete");
            check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN) == door.open(), what + ": open state");
            check(level.getBlockEntity(assembly.anchor()) instanceof RoundDoorBlockEntity, what + ": no anchor entity");
            RoundDoorBlockEntity entity = (RoundDoorBlockEntity) level.getBlockEntity(assembly.anchor());
            check(entity.appearance().equals(door.look()), what + ": look " + entity.appearance().key());
            check(entity.trims().equals(door.trims()), what + ": trims " + entity.trims());
            check(entity.camouflage().map(c -> door.camouflage().filter(c.material()::is).isPresent())
                    .orElse(door.camouflage().isEmpty()), what + ": camouflage " + entity.camouflage());
            BlockPos besides = frame.pos(door.r() + 1, 0, door.f());
            check(!level.getBlockState(besides).is(ModBlocks.archedDoor()) && !level.getBlockState(besides.above()).is(ModBlocks.archedDoor()),
                    what + ": a door part stands beside the one-column door");
            check(level.getBlockEntity(frame.pos(ShowcaseCanvas.labelR(door.r()), 0, door.f() - 1)) instanceof SignBlockEntity, what + ": sign missing");
        }
    }

    /** Flips the section's lever: the redstone door opens (both halves). Run once; it changes the world. */
    public static void verifyRedstone(ServerLevel level, ShowcaseFrame frame) {
        ArchedDoorsSection.Exhibit door = ArchedDoorsSection.redstoneDoor();
        BlockPos leverPos = frame.pos(ArchedDoorsSection.LEVER_R, 0, door.f());
        BlockState lever = level.getBlockState(leverPos);
        check(lever.is(Blocks.LEVER) && !lever.getValue(LeverBlock.POWERED), "arched redstone lever missing or already on");
        ((LeverBlock) Blocks.LEVER).pull(lever, level, leverPos);
        BlockPos anchor = frame.pos(door.r(), 0, door.f());
        check(level.getBlockState(anchor).getValue(DoorBlock.OPEN) && level.getBlockState(anchor.above()).getValue(DoorBlock.OPEN),
                "the lever did not open the arched door");
    }
}
