package io.github.dirtyredz.roundwood.block.window.pane;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowSillNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Every window carries one of these (never ticking) for four independent things: the frame's
 * appearance (top-level {@code RoundwoodAppearance}), the perimeter face trims (0.8.0,
 * {@link WindowTrimNbt}: a FRONT and a BACK slot, each absent or present with its own look), the arch
 * sills (0.10.0, {@link WindowSillNbt}, the same two faces in their own store) and the glass colour
 * ({@link WindowGlassNbt}). Frame and glass stay chunk-baked: the entity hands them to the model as
 * one {@link WindowAppearanceData} snapshot. Trims and sills are drawn by a block-entity renderer,
 * since they may project beyond the cell. Loading and update packets always replace everything (the
 * update tag always carries both attachment payloads), so removals reach clients.
 *
 * <p>The {@code trimmed} blockstate is only a summary of "any attachment present" (either slot, either
 * face); it is consulted when reading 0.7.0 data, which is why the perimeter payload is always written,
 * empty included. A loaded summary that disagrees is corrected by a scheduled block tick, never from
 * the decoder.
 */
public class RoundWindowBlockEntity extends BlockEntity implements AppearanceActions.Target, TrimAttachmentSource {
    /** How far a projecting trim reaches past the cell, in blocks. */
    private static final double TRIM_REACH = 1 / 16.0;

    /** Null: the block's default. */
    @Nullable
    private Appearance appearance;
    /** The perimeter trims, and any payload of a version this build does not understand (saved back unchanged). */
    private final FaceTrimStore trims = new FaceTrimStore(WindowTrimNbt.FORMAT);
    /** The arch sills (0.10.0), likewise: a second store, so either payload's custody is independent. */
    private final FaceTrimStore sills = new FaceTrimStore(WindowSillNbt.FORMAT);
    @Nullable
    private DyeColor glass;

    public RoundWindowBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Appearance appearance() {
        return appearance != null ? appearance : AppearanceDefaults.of(getBlockState().getBlock());
    }

    /** Server: replaces the appearance (stored only when it differs from the default), saves and tells the clients. */
    public void setAppearance(Appearance next) {
        appearance = next.equals(AppearanceDefaults.of(getBlockState().getBlock())) ? null : next;
        changed();
    }

    /** The fitted perimeter trims (none while an unknown payload is held). */
    @Override
    public FaceTrims trims() {
        return trims.trims();
    }

    /** The fitted attachments in {@code slot}: the perimeter trims or the arch sills (0.10.0). */
    @Override
    public FaceTrims trims(TrimSlot slot) {
        return store(slot).trims();
    }

    /** False while the stored perimeter trims are of an unknown future version: they must not be edited. */
    public boolean trimsEditable() {
        return trims.editable();
    }

    /** False while the payload of {@code slot} is of an unknown future version. */
    public boolean trimsEditable(TrimSlot slot) {
        return store(slot).editable();
    }

    /** True when either slot has anything fitted on either face (the {@code trimmed} summary's meaning). */
    public boolean anyAttachment() {
        return trims.trims().any() || sills.trims().any();
    }

    /** True when neither slot holds an unknown payload, so the {@code trimmed} summary may be reconciled. */
    public boolean attachmentsEditable() {
        return trims.editable() && sills.editable();
    }

    /** Server: replaces the perimeter trims ({@link #setTrims(TrimSlot, FaceTrims)}). */
    public boolean setTrims(FaceTrims next) {
        return setTrims(TrimSlot.PERIMETER, next);
    }

    /**
     * Server: replaces the attachments in {@code slot}, saves and tells the clients. Callers keep the
     * {@code trimmed} summary in step ({@code trim/WindowTrimActions}). Refused while that slot holds
     * an unknown payload.
     */
    public boolean setTrims(TrimSlot slot, FaceTrims next) {
        if (!store(slot).set(next)) {
            return false;
        }
        changed();
        return true;
    }

    private FaceTrimStore store(TrimSlot slot) {
        return slot == TrimSlot.SILL ? sills : trims;
    }

    public Optional<DyeColor> glass() {
        return Optional.ofNullable(glass);
    }

    /** Server: replaces the glass colour (empty: clear), saves and tells the clients. */
    public void setGlass(Optional<DyeColor> next) {
        glass = next.orElse(null);
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Optional<Appearance> current() {
        return Optional.of(appearance());
    }

    @Override
    public boolean mayEdit(Player player) {
        return level != null && level.mayInteract(player, worldPosition);
    }

    @Override
    public void set(Appearance next) {
        setAppearance(next);
    }

    @Override
    public BlockPos origin() {
        return worldPosition;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeFrameAndGlass(tag);
        trims.save(tag); // always written, empty included: a sill-only window's true summary never reads as 0.7.0 oak
        sills.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.WINDOW::accepts).orElse(null);
        trims.load(tag, WindowTrimNbt.legacy(WindowPaneBlock.trimmed(getBlockState())));
        sills.load(tag, FaceTrimStore.Legacy.NONE);
        glass = WindowGlassNbt.read(tag).orElse(null);
    }

    /** Server, once in a level: a {@code trimmed} summary out of step with the attachments is corrected by the block's tick. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && attachmentsEditable()
                && WindowPaneBlock.trimmed(getBlockState()) != anyAttachment()) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    /** Looks only; always both attachment payloads (possibly empty) so a removal reaches clients. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeFrameAndGlass(tag);
        trims.writeUpdate(tag); // explicit empty while an unknown payload is held: clients could not read it either
        sills.writeUpdate(tag);
        return tag;
    }

    private void writeFrameAndGlass(CompoundTag tag) {
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        WindowGlassNbt.write(tag, glass());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Client: the whole look replaces the old one, then the model data and the section are refreshed. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        load(tag == null ? emptyUpdate() : tag);
        refreshModel();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        refreshModel();
    }

    /** A packet without a tag clears everything (never falls back to the 0.7.0 reading of the state). */
    private static CompoundTag emptyUpdate() {
        CompoundTag tag = new CompoundTag();
        WindowTrimNbt.write(tag, FaceTrims.NONE);
        return tag;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(WindowAppearanceData.PROPERTY, new WindowAppearanceData(appearance(), glass())).build();
    }

    /** The cell plus the one pixel a trim may stand proud of it on any side. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(TRIM_REACH);
    }

    private void refreshModel() {
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }
}
