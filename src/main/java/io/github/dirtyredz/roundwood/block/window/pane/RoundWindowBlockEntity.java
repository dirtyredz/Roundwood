package io.github.dirtyredz.roundwood.block.window.pane;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassNbt;
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
 * Every window carries one of these (never ticking) for three independent things: the frame's
 * appearance (top-level {@code RoundwoodAppearance}), the face trims (0.8.0, {@link WindowTrimNbt}:
 * a FRONT and a BACK slot, each absent or present with its own look) and the glass colour
 * ({@link WindowGlassNbt}). Frame and glass stay chunk-baked: the entity hands them to the model as
 * one {@link WindowAppearanceData} snapshot. The trims are drawn by a block-entity renderer, since
 * they may project beyond the cell. Loading and update packets always replace all three (the update
 * tag always carries the trim payload), so removals reach clients.
 *
 * <p>The {@code trimmed} blockstate is only a summary of "either face present"; it is consulted when
 * reading 0.7.0 data. A loaded summary that disagrees with the faces is corrected by a scheduled
 * block tick, never from the decoder.
 */
public class RoundWindowBlockEntity extends BlockEntity implements AppearanceActions.Target, TrimAttachmentSource {
    /** How far a projecting trim reaches past the cell, in blocks. */
    private static final double TRIM_REACH = 1 / 16.0;

    /** Null: the block's default. */
    @Nullable
    private Appearance appearance;
    /** The face trims, and any payload of a version this build does not understand (saved back unchanged). */
    private final FaceTrimStore trims = new FaceTrimStore(WindowTrimNbt.FORMAT);
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

    /** The fitted face trims (none while an unknown payload is held). */
    @Override
    public FaceTrims trims() {
        return trims.trims();
    }

    /** False while the stored trims are of an unknown future version: they must not be edited. */
    public boolean trimsEditable() {
        return trims.editable();
    }

    /**
     * Server: replaces the face trims, saves and tells the clients. Callers keep the {@code trimmed}
     * summary in step ({@code trim/WindowTrimActions}). Refused while an unknown payload is held.
     */
    public boolean setTrims(FaceTrims next) {
        if (!trims.set(next)) {
            return false;
        }
        changed();
        return true;
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
        trims.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.WINDOW::accepts).orElse(null);
        trims.load(tag, WindowTrimNbt.legacy(WindowPaneBlock.trimmed(getBlockState())));
        glass = WindowGlassNbt.read(tag).orElse(null);
    }

    /** Server, once in a level: a {@code trimmed} summary out of step with the faces is corrected by the block's tick. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && trimsEditable()
                && WindowPaneBlock.trimmed(getBlockState()) != trims().any()) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    /** Looks only; always the trim payload (possibly empty) so a removal reaches clients. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeFrameAndGlass(tag);
        trims.writeUpdate(tag); // explicit empty while an unknown payload is held: clients could not read it either
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
