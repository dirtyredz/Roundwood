package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorArchPersistenceGameTests {
    @GameTest(template = TEMPLATE)
    public static void fittedStoreKeepsIndependentLooksAndClearsClientCopies(GameTestHelper helper) {
        for (var block : ModBlocks.doors()) {
            var entity = new RoundDoorBlockEntity(ModBlockEntities.roundDoor(), BlockPos.ZERO, block.defaultBlockState());
            var leaf = Appearance.DEFAULT.painted(DyeColor.RED);
            var frame = Appearance.DEFAULT.painted(DyeColor.BLUE);
            var casing = Appearance.DEFAULT.painted(DyeColor.GREEN);
            entity.setAppearance(leaf);
            entity.setTrims(FaceTrims.both(casing));
            var attachment = new DoorArchAttachment(DoorArchFitPlan.topFor(block.design()), ArchPieceThickness.FULL,
                    ArchPieceDepth.FRONT, frame, FaceTrims.both(frame));
            check(entity.setArch(attachment), "known attachment refused");
            var copy = new RoundDoorBlockEntity(ModBlockEntities.roundDoor(), BlockPos.ZERO, block.defaultBlockState());
            copy.load(entity.saveWithoutMetadata());
            check(copy.arch().orElseThrow().equals(attachment) && copy.appearance().equals(leaf)
                    && copy.trims().equals(FaceTrims.both(casing)), "independent custody lost on reload");
            entity.setArch(null);
            copy.load(entity.getUpdateTag());
            check(copy.arch().isEmpty() && copy.archEditable(), "explicit clear did not reach client");
            var unknown = new CompoundTag(); var payload = DoorArchStore.encode(attachment);
            payload.putInt("Version", 99); unknown.put(DoorArchStore.TAG, payload);
            copy.load(unknown);
            check(!copy.setArch(attachment) && !copy.setArch(null), "unknown frame accepted edit");
            check(copy.saveWithoutMetadata().get(DoorArchStore.TAG).equals(payload), "unknown frame lost custody");
            entity.load(copy.getUpdateTag());
            check(!entity.archEditable() && entity.arch().isEmpty(), "client did not receive refusal marker");
            copy.load(new CompoundTag());
            check(copy.arch().isEmpty() && copy.archEditable(), "legacy empty store changed");
        }
        helper.succeed();
    }
}
