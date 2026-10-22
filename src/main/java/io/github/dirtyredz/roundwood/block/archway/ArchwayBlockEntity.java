package io.github.dirtyredz.roundwood.block.archway;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimNbt;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
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
 * The anchor (lower-left cell) of an archway (0.12.0): the one place its look and its two face trims
 * ({@link ArchwayTrimNbt}, through the shared {@link FaceTrimStore}) are kept, and where the client
 * draws all four cells from. Never ticks. Loading and update packets always replace both, so a change
 * back to the default (paint removed, oak again, a trim sheared off) reaches clients.
 */
public class ArchwayBlockEntity extends BlockEntity implements TrimAttachmentSource {
    /** Null: the block's default. */
    @Nullable
    private Appearance appearance;
    /** The face trims, and any payload of a version this build does not understand (saved back unchanged). */
    private final FaceTrimStore trims = new FaceTrimStore(ArchwayTrimNbt.FORMAT);

    public ArchwayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Appearance appearance() {
        return appearance != null ? appearance : AppearanceDefaults.of(getBlockState().getBlock());
    }

    /** Server: replaces the look (stored only when it differs from the default), saves and tells the clients. */
    public void setAppearance(Appearance next) {
        appearance = next.equals(AppearanceDefaults.of(getBlockState().getBlock())) ? null : next;
        changed();
    }

    /** The face trims (none while an unknown payload is held). */
    @Override
    public FaceTrims trims() {
        return trims.trims();
    }

    /** False while the stored trims are of an unknown future version: they must not be edited. */
    public boolean trimsEditable() {
        return trims.editable();
    }

    /** Server: replaces the face trims, saves and tells the clients; refused while an unknown payload is held. */
    public boolean setTrims(FaceTrims next) {
        if (!trims.set(next)) {
            return false;
        }
        changed();
        return true;
    }

    private void changed() {
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
        trims.save(tag);
    }

    /** Replaces the look and trims with whatever the tag holds (the default and none when absent or not acceptable). */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.TRIM::accepts).orElse(null);
        trims.load(tag, FaceTrimStore.Legacy.NONE);
    }

    /** The look (omitted when it is the default) and always the trims, so a removal is sent too. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        trims.writeUpdate(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A packet always carries the whole state; a packet without a tag means the default and no trims. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        load(tag == null ? new CompoundTag() : tag);
    }

    /** The four cells and the casings standing proud of them, so the archway is drawn whenever any of it is in view. */
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof ArchwayBlock ? ArchwayLayout.of(getBlockPos(), state).renderBounds() : new AABB(getBlockPos());
    }
}
