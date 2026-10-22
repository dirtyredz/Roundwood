package io.github.dirtyredz.roundwood.block.window.pane;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowSillNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Every window carries one of these (never ticking) for four independent things: the frame's
 * appearance (top-level {@code RoundwoodAppearance}), the perimeter face trims (0.8.0,
 * {@link WindowTrimNbt}: a FRONT and a BACK slot, each absent or present with its own look), the arch
 * sills (0.10.0, {@link WindowSillNbt}, the same two faces in their own store) and the glass colour
 * ({@link WindowGlassNbt}). Frame and glass stay chunk-baked: the entity hands them to the model as
 * one {@link WindowAppearanceData} snapshot, refreshed whenever a packet or update tag is loaded.
 * Trims and sills are drawn by a block-entity renderer, since they may project beyond the cell.
 * Persistence and synchronisation are the shared anchor's ({@link AppearanceAnchorBlockEntity}): the
 * update tag always carries both attachment payloads, so removals reach clients, and a packet without
 * a tag clears everything.
 *
 * <p>The {@code trimmed} blockstate is only a summary of "any attachment present" (either slot, either
 * face); it is consulted when reading 0.7.0 data, which is why the perimeter payload is always written,
 * empty included. Callers keep it in step after a successful write ({@code trim/WindowAttachmentState});
 * a loaded summary that disagrees is corrected by a scheduled block tick, never from the decoder.
 */
public class RoundWindowBlockEntity extends AppearanceAnchorBlockEntity implements AppearanceActions.Target, TrimAttachmentSource {
    /** How far a projecting trim reaches past the cell, in blocks. */
    private static final double TRIM_REACH = AttachmentFace.PROUD / 16.0;

    @Nullable
    private DyeColor glass;

    public RoundWindowBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, AppearanceFamily.WINDOW,
                Map.of(TrimSlot.PERIMETER, WindowTrimNbt.FORMAT, TrimSlot.SILL, WindowSillNbt.FORMAT));
    }

    /** True when either slot has anything fitted on either face (the {@code trimmed} summary's meaning). */
    public boolean anyAttachment() {
        return trims(TrimSlot.PERIMETER).any() || trims(TrimSlot.SILL).any();
    }

    /** True when neither slot holds an unknown payload, so the {@code trimmed} summary may be reconciled. */
    public boolean attachmentsEditable() {
        return trimsEditable(TrimSlot.PERIMETER) && trimsEditable(TrimSlot.SILL);
    }

    public Optional<DyeColor> glass() {
        return Optional.ofNullable(glass);
    }

    /** Server: replaces the glass colour (empty: clear), saves and tells the clients. */
    public void setGlass(Optional<DyeColor> next) {
        glass = next.orElse(null);
        changed();
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

    /** A missing perimeter payload is read through the 0.7.0 {@code trimmed} summary. */
    @Override
    protected FaceTrimStore.Legacy legacy(TrimSlot slot) {
        return slot == TrimSlot.PERIMETER ? WindowTrimNbt.legacy(WindowPaneBlock.trimmed(getBlockState())) : FaceTrimStore.Legacy.NONE;
    }

    @Override
    protected void saveFamily(CompoundTag tag) {
        WindowGlassNbt.write(tag, glass());
    }

    @Override
    protected void writeFamilyUpdate(CompoundTag tag) {
        WindowGlassNbt.write(tag, glass());
    }

    @Override
    protected void loadFamily(CompoundTag tag) {
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

    /** Client: the loaded look is handed to the chunk model and the section redrawn. */
    @Override
    protected void afterClientUpdate() {
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
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
}
