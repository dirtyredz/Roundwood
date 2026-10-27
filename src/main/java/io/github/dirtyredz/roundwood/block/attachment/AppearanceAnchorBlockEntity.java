package io.github.dirtyredz.roundwood.block.attachment;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceAnchor;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The persistence and synchronisation every appearance anchor shares (a door's or archway's anchor
 * cell, a compatibility frame's anchor quarter, a window): one appearance of a family and the
 * face-attachment stores the concrete entity declares, one {@link FaceTrimStore} per supported
 * {@link TrimSlot} (none for the frame). The concrete entity names its family and stores through the
 * constructor (no overridable call during construction) and adds only its own state through the narrow
 * hooks below.
 * <ul>
 *     <li>Setters normalise against the current block's default (aliases included): the default is
 *     never stored. A change saves and tells the clients; a refused trim write does neither.</li>
 *     <li>Disk saves keep an unknown attachment payload unchanged; update tags always carry every
 *     store, explicit-empty while one is unknown, so a removal reaches clients. Slots are independent.</li>
 *     <li>Loading replaces everything with what the tag holds: an absent or unacceptable look is the
 *     default, each store reads through its slot's {@link #legacy} rule. A packet without a tag means
 *     the default and every store explicitly empty (never a legacy reading).</li>
 * </ul>
 */
public abstract class AppearanceAnchorBlockEntity extends BlockEntity implements AppearanceAnchor {
    private final io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderPublication<io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot, BlockPos> staticRenderPublication = new io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderPublication<>();
    public final io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderPublication<io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderSnapshot, BlockPos> staticRenderPublication() { return staticRenderPublication; }

    @Override public void onLoad() {
        super.onLoad();
        io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates.loaded(this);
    }
    @Override public void setRemoved() {
        super.setRemoved();
        io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates.removed(this);
    }
    @Override public void onChunkUnloaded() {
        io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates.removed(this);
        super.onChunkUnloaded();
    }
    private final AppearanceFamily family;
    private final Map<TrimSlot, FaceTrimStore> stores;
    /** Null: the block's default. */
    @Nullable
    private Appearance appearance;

    protected AppearanceAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, AppearanceFamily family,
                                          Map<TrimSlot, FaceTrimStore.Format> formats) {
        super(type, pos, state);
        this.family = family;
        Map<TrimSlot, FaceTrimStore> declared = new EnumMap<>(TrimSlot.class);
        formats.forEach((slot, format) -> declared.put(slot, new FaceTrimStore(format)));
        this.stores = Collections.unmodifiableMap(declared);
    }

    /** The look: what is stored, else the block's default. */
    public Appearance appearance() {
        return appearance != null ? appearance : AppearanceDefaults.of(getBlockState().getBlock());
    }

    /** Server: replaces the look (stored only when it differs from the default), saves and tells the clients. */
    public void setAppearance(Appearance next) {
        appearance = next.equals(AppearanceDefaults.of(getBlockState().getBlock())) ? null : next;
        changed();
    }

    /** The perimeter attachments (none while an unknown payload is held, or when there is no such store). */
    public FaceTrims trims() {
        return trims(TrimSlot.PERIMETER);
    }

    /** The attachments in {@code slot}; none for a slot this entity does not store. */
    public FaceTrims trims(TrimSlot slot) {
        FaceTrimStore store = stores.get(slot);
        return store == null ? FaceTrims.NONE : store.trims();
    }

    /** False while the stored perimeter attachments are of an unknown future version: they must not be edited. */
    public boolean trimsEditable() {
        return trimsEditable(TrimSlot.PERIMETER);
    }

    /** False while {@code slot}'s payload is of an unknown version, or when this entity has no such store. */
    public boolean trimsEditable(TrimSlot slot) {
        FaceTrimStore store = stores.get(slot);
        return store != null && store.editable();
    }

    /** Server: replaces the perimeter attachments ({@link #setTrims(TrimSlot, FaceTrims)}). */
    public boolean setTrims(FaceTrims next) {
        return setTrims(TrimSlot.PERIMETER, next);
    }

    /**
     * Server: replaces the attachments in {@code slot}, saves and tells the clients. Refused (nothing
     * saved or sent) while that slot holds an unknown payload, or when this entity has no such store.
     */
    public boolean setTrims(TrimSlot slot, FaceTrims next) {
        FaceTrimStore store = stores.get(slot);
        if (store == null || !store.set(next)) {
            return false;
        }
        changed();
        return true;
    }

    /** Saves, and on the server tells the clients (a block update for this cell). */
    protected final void changed() {
        setChanged();
        io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates.refresh(this, false);
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** How a missing or older payload of {@code slot} reads (the window's 0.7.0 perimeter); none by default. */
    protected FaceTrimStore.Legacy legacy(TrimSlot slot) {
        return FaceTrimStore.Legacy.NONE;
    }

    /** The family's own saved state (door camouflage with its refund, window glass). */
    protected void saveFamily(CompoundTag tag) {
    }

    /** The family's own state for clients (door camouflage without its refund, explicit when cleared; window glass). */
    protected void writeFamilyUpdate(CompoundTag tag) {
    }

    /** Replaces the family's own state with what {@code tag} holds (a save or an update). */
    protected void loadFamily(CompoundTag tag) {
    }

    /** Client, after a packet or update tag was loaded (the window refreshes its chunk model). */
    protected void afterClientUpdate() {
    }

    /** Complete direct loads also need to refresh derived window data. */
    protected void afterLoad() {
    }

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveFamily(tag);
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        stores.values().forEach(store -> store.save(tag));
    }

    /** Replaces the look, every store and the family's state with whatever the tag holds. */
    @Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        appearance = AppearanceNbt.read(tag).filter(family::accepts).orElse(null);
        stores.forEach((slot, store) -> store.load(tag, legacy(slot)));
        loadFamily(tag);
        io.github.dirtyredz.roundwood.block.assembly.render.AssemblyRenderUpdates.refresh(this, false);
        afterLoad();
    }

    /** The look (omitted when it is the default), always every store, and the family's update state. */
    @Override
    public final CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeFamilyUpdate(tag);
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        stores.values().forEach(store -> store.writeUpdate(tag));
        return tag;
    }

    @Override
    public final Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A packet always carries the whole state; one without a tag means the default and every store explicitly empty. */
    @Override
    public final void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        load(tag == null ? emptyUpdate() : tag);
        afterClientUpdate();
    }

    @Override
    public final void handleUpdateTag(CompoundTag tag) {
        load(tag);
        afterClientUpdate();
    }

    /** Every store's explicit empty payload: never read through a legacy rule (a window's 0.7.0 summary). */
    private CompoundTag emptyUpdate() {
        CompoundTag tag = new CompoundTag();
        stores.values().forEach(store -> FaceTrimStore.write(store.format(), tag, FaceTrims.NONE));
        return tag;
    }
}
