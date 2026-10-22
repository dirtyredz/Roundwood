package io.github.dirtyredz.roundwood.showcase.section.archway;

import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.HashSet;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * The showcase's archway section read back from the world (0.12.0), from the same list the section
 * builds ({@link ArchwaysSection#archways}): every archway complete in its design, look and face
 * trims, with its sign; and the section's rectangle holding nothing else, which proves it overlaps no
 * other section (each cell is either one of this section's archway cells or signs, or untouched air).
 * Called right after building and again after the integrity ticks, in every facing.
 */
public final class ArchwaysChecks {
    private ArchwaysChecks() {
    }

    public static void verify(ServerLevel level, ShowcaseFrame frame) {
        Set<BlockPos> owned = new HashSet<>();
        for (ArchwaysSection.Exhibit archway : ArchwaysSection.archways()) {
            String what = archway.design() + " archway r " + archway.r() + " f " + archway.f();
            ArchwayLayout.Assembly assembly = new ArchwayLayout.Assembly(frame.pos(archway.r(), 0, archway.f()), frame.forward(),
                    archway.design());
            check(assembly.complete(level, ModBlocks.archway(archway.design())), what + ": missing or incomplete");
            check(level.getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity, what + ": no anchor entity");
            ArchwayBlockEntity entity = (ArchwayBlockEntity) level.getBlockEntity(assembly.anchor());
            check(entity.appearance().equals(archway.look()), what + ": look " + entity.appearance().key());
            check(entity.trims().equals(archway.trims()), what + ": trims " + entity.trims());
            BlockPos sign = frame.pos(archway.r(), 0, archway.f() - 1);
            check(level.getBlockEntity(sign) instanceof SignBlockEntity, what + ": sign missing");
            owned.addAll(assembly.positions());
            owned.add(sign);
        }
        for (int r = ArchwaysSection.R0; r <= ArchwaysSection.R1; r++) {
            for (int f = ArchwaysSection.F0; f <= ArchwaysSection.F1; f++) {
                for (int y = 0; y < ShowcaseFrame.HEIGHT; y++) {
                    BlockPos pos = frame.pos(r, y, f);
                    if (owned.contains(pos)) {
                        continue;
                    }
                    check(level.getBlockState(pos).isAir(), "the archway section's free area holds "
                            + level.getBlockState(pos) + " at r " + r + " y " + y + " f " + f);
                    check(!(level.getBlockState(pos).getBlock() instanceof ArchwayBlock), "a stray archway cell at r " + r + " f " + f);
                }
            }
        }
    }
}
