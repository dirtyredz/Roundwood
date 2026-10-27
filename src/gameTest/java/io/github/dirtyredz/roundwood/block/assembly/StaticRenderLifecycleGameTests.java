package io.github.dirtyredz.roundwood.block.assembly;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.assembly.render.*;
import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.Set;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StaticRenderLifecycleGameTests {
    @GameTest(template = "empty")
    public static void sectionCrossingEditsClearAndRemovalPublishCompleteFootprints(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(BlockPos.ZERO);
        var pos = origin.offset(15 - (origin.getX() & 15), 15 - (origin.getY() & 15), 3);
        var block = ModBlocks.archPiece(ArchPieceDesign.TOP_2X3);
        var assembly = new ArchPieceLayout.Assembly(pos, Direction.NORTH, block.design(), KeyholeShoulderProfile.Side.LEFT);
        for (var cell : assembly.positions()) level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
        check(ArchPiecePlacement.placeAt(level, block, assembly, AppearanceFixtures.BIRCH), "place crossing arch");
        var entity = (ArchPieceBlockEntity) level.getBlockEntity(pos);
        var first = entity.staticRenderPublication().current();
        check(first != null && first.available(), "complete snapshot after commit");
        check(first.members().keySet().equals(Set.copyOf(assembly.positions())), "every owned cell published");
        check(first.members().keySet().stream().map(SectionPos::of).distinct().count() == 4, "fixture crosses x and y sections");
        entity.setTrims(FaceTrims.both(AppearanceFixtures.SPRUCE));
        var edited = entity.staticRenderPublication().current();
        check(edited.revision() > first.revision() && edited.trims().any() && !first.trims().any(), "immutable retained revision");
        entity.handleUpdateTag(new net.minecraft.nbt.CompoundTag());
        check(!entity.staticRenderPublication().current().trims().any(), "tagless clearing publishes");
        level.destroyBlock(assembly.pos(ArchPieceCell.UPPER_RIGHT), false);
        check(!entity.staticRenderPublication().current().available(), "removal retires published data");
        check(entity.staticRenderPublication().footprint().equals(Set.copyOf(assembly.positions())), "removal retains invalidation footprint");
        helper.succeed();
    }
}
