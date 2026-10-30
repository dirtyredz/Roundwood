package io.github.dirtyredz.roundwood.fixture.assembly;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * A fake survival player who may not use items at a chosen set of cells, standing in for a protection
 * mod that refuses one cell of an assembly ({@link #mayUseItemAt}). Everything else is an ordinary
 * fake player, set up like {@code DoorFixtures.player}.
 */
public final class DenyingPlayer extends FakePlayer {
    private final Set<BlockPos> denied;

    private DenyingPlayer(GameTestHelper helper, String name, Set<BlockPos> denied) {
        super(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(("roundwood:deny_" + name).getBytes(StandardCharsets.UTF_8)),
                "rw_deny_" + name));
        this.denied = Set.copyOf(denied);
    }

    /** A survival player facing {@code facing}, refused at the absolute cells {@code denied}. */
    public static DenyingPlayer create(GameTestHelper helper, String name, Direction facing, Set<BlockPos> denied) {
        DenyingPlayer player = new DenyingPlayer(helper, name, denied);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos stand = helper.absolutePos(new BlockPos(0, 4, 0));
        player.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, facing.toYRot(), 0);
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
        return player;
    }

    @Override
    public boolean mayUseItemAt(BlockPos pos, Direction facing, ItemStack stack) {
        return !denied.contains(pos) && super.mayUseItemAt(pos, facing, stack);
    }
}
