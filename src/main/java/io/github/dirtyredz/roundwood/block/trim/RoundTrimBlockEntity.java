package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The anchor (lower-left quarter) of a frame: the one place its appearance is kept, and where the
 * client draws all four quarters from. Never ticks. Loading and update packets always replace the
 * whole appearance, so a change back to the default (paint removed, oak again) reaches clients.
 */
public class RoundTrimBlockEntity extends BlockEntity {
    /** Null: the block's default. */
    @Nullable
    private Appearance appearance;

    public RoundTrimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Appearance appearance() {
        return appearance != null ? appearance : AppearanceDefaults.of(getBlockState().getBlock());
    }

    /** Server: replaces the appearance (stored only when it differs from the default), saves and tells the clients. */
    public void setAppearance(Appearance next) {
        appearance = next.equals(AppearanceDefaults.of(getBlockState().getBlock())) ? null : next;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.TRIM::accepts).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A packet always carries the whole appearance; a packet without a tag means the default. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        load(tag == null ? new CompoundTag() : tag);
    }

    /** The four cells, so the frame is drawn whenever any of it is in view. */
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof RoundTrimBlock) {
            TrimLayout.Assembly assembly = TrimLayout.of(getBlockPos(), state);
            AABB box = new AABB(assembly.anchor());
            for (BlockPos pos : assembly.positions()) {
                box = box.minmax(new AABB(pos));
            }
            return box;
        }
        return new AABB(getBlockPos());
    }
}
