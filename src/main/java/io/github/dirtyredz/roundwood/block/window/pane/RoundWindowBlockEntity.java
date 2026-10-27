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
import io.github.dirtyredz.roundwood.block.window.trim.WindowBeamNbt;
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
 * Shared-anchor window contents: frame, tint, perimeter faces, sill faces and whole-pane beams.
 * WindowTrim always saves an explicit empty payload, so a beam-only summary cannot invent legacy
 * oak trims. Unknown stores retain disk custody independently. Client model data is an immutable
 * level-thread snapshot; load/state/packet changes refresh neighbouring panes as well.
 */
public class RoundWindowBlockEntity extends AppearanceAnchorBlockEntity implements AppearanceActions.Target, TrimAttachmentSource {
    /** How far a projecting trim reaches past the cell, in blocks. */
    private static final double TRIM_REACH = AttachmentFace.PROUD / 16.0;

    @Nullable
    private DyeColor glass;
    private volatile ModelData renderData = ModelData.EMPTY;
    private boolean renderAvailable;

    boolean renderAvailable() { return renderAvailable && !isRemoved(); }

    public RoundWindowBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, AppearanceFamily.WINDOW,
                Map.of(TrimSlot.PERIMETER, WindowTrimNbt.FORMAT, TrimSlot.SILL, WindowSillNbt.FORMAT, TrimSlot.CROSS_BEAMS, WindowBeamNbt.FORMAT));
    }

    /** True when any declared slot has an attachment (the {@code trimmed} summary's meaning). */
    public boolean anyAttachment() {
        return trims(TrimSlot.PERIMETER).any() || trims(TrimSlot.SILL).any() || trims(TrimSlot.CROSS_BEAMS).any();
    }

    /** True when no slot holds an unknown payload, so the {@code trimmed} summary may be reconciled. */
    public boolean attachmentsEditable() {
        return trimsEditable(TrimSlot.PERIMETER) && trimsEditable(TrimSlot.SILL) && trimsEditable(TrimSlot.CROSS_BEAMS);
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
        renderAvailable = true;
        if (level != null && level.isClientSide) WindowPaneState.refreshAround(level, worldPosition);
        if (level != null && !level.isClientSide && attachmentsEditable()
                && WindowPaneBlock.trimmed(getBlockState()) != anyAttachment()) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        renderAvailable = false;
        if (level != null) WindowPaneState.refreshAround(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        renderAvailable = false;
        if (level != null) WindowPaneState.refreshAround(level, worldPosition);
    }

    /** Client: the loaded look is handed to the chunk model and the section redrawn. */
    @Override
    protected void afterLoad() {
        if (level != null && level.isClientSide) {
            WindowPaneState.refreshAround(level, worldPosition);
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (level != null && level.isClientSide) WindowPaneState.refreshAround(level, worldPosition);
    }

    /** Level-thread snapshot publication, including neighbours across render-section boundaries. */
    void refreshRenderData() {
        if (level == null || !(getBlockState().getBlock() instanceof WindowPaneBlock)) return;
        renderData = ModelData.builder().with(WindowAppearanceData.PROPERTY,
                new WindowAppearanceData(appearance(), glass(), WindowPaneState.beams(getBlockState(), this),
                        WindowPaneState.neighbours(level, worldPosition), trims(), trims(TrimSlot.SILL))).build();
        requestModelDataUpdate();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public ModelData getModelData() {
        return renderData;
    }

    /** The cell plus the one pixel a trim may stand proud of it on any side. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(TRIM_REACH);
    }
}
