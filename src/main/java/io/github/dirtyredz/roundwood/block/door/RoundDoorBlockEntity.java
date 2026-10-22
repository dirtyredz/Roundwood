package io.github.dirtyredz.roundwood.block.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimNbt;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorSwing;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The anchor (lower-left part) of every assembly carries this entity, so the client has one place
 * to draw the whole leaf from, and the assembly has one place to keep its appearance (wood and
 * paint, {@link Appearance}), its camouflage and its face trims (0.8.0, {@link DoorTrimNbt}). It never
 * ticks: the blockstate stays authoritative for open/closed, and the client-side {@link DoorSwing}
 * only animates between states it is told about through {@link #setBlockState}.
 *
 * <p>Appearance and camouflage travel with the vanilla block-entity update packet: clients get the
 * camouflage material, never the refund. Loading always replaces both previous values (a clear or
 * a paint removal reaches clients too) and never touches the swing.
 */
public class RoundDoorBlockEntity extends BlockEntity implements TrimAttachmentSource {
    private final DoorSwing swing;
    @Nullable
    private CamouflageData camouflage;
    /** Null: the block's default (oak, or a hidden alias's historical finish). */
    @Nullable
    private Appearance appearance;
    /** The face trims, and any payload of a version this build does not understand (saved back unchanged). */
    private final FaceTrimStore trims = new FaceTrimStore(DoorTrimNbt.FORMAT);

    public RoundDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Whatever state the entity is created in (placement, chunk load) is drawn at rest.
        this.swing = new DoorSwing(open(state));
    }

    public DoorSwing swing() {
        return swing;
    }

    /** The door's look: what is stored, else the block's default. */
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

    /** The door's face trims (0.8.0; none while an unknown payload is held). */
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
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public Optional<CamouflageData> camouflage() {
        return Optional.ofNullable(camouflage);
    }

    /** Server: replaces (or with null clears) the camouflage, saves, and tells the clients. */
    public void setCamouflage(@Nullable CamouflageData data) {
        camouflage = data;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Removes and returns the camouflage, so whoever takes it is the only one who can refund it. */
    public Optional<CamouflageData> takeCamouflage() {
        Optional<CamouflageData> taken = camouflage();
        if (taken.isPresent()) {
            camouflage = null;
            setChanged();
        }
        return taken;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (camouflage != null) {
            tag.put(CamouflageData.TAG, camouflage.write(true));
        }
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        trims.save(tag);
    }

    /** Replaces camouflage, appearance and trims with whatever the tag holds (nothing when absent or not acceptable). */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        camouflage = CamouflageData.read(tag).orElse(null);
        appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.DOOR::accepts).orElse(null);
        trims.load(tag, FaceTrimStore.Legacy.NONE);
    }

    /**
     * Looks only, always carrying the camouflage and trim keys (possibly empty) so that a clear is sent
     * too; the appearance is omitted when it is the default, which the client then reads as the default.
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(CamouflageData.TAG, camouflage == null ? new CompoundTag() : camouflage.write(false));
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        trims.writeUpdate(tag); // always: a removal must reach clients
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A packet always carries the whole appearance; a packet without a tag means none, never the old value. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        load(tag == null ? new CompoundTag() : tag);
    }

    /** Called by the chunk whenever this cell's state changes; on the client an open change starts a swing. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        boolean was = open(getBlockState());
        super.setBlockState(state);
        boolean now = open(state);
        if (level != null && level.isClientSide && now != was) {
            swing.retarget(now, level.getGameTime());
        }
    }

    /** Both resting poses and the sweep between them, so the leaf is not culled while only its projection is in view. */
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof RoundDoorBlock) {
            return DoorLayout.of(getBlockPos(), state).sweepBounds();
        }
        return new AABB(getBlockPos());
    }

    private static boolean open(BlockState state) {
        return state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN);
    }
}
