package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.block.attachment.TrimAttachmentSource;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.attachment.AppearanceAnchorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchStore;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorSwing;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * The anchor (lower-left part) of every door assembly (round, and arched since 0.11.0; the entity
 * type keeps its {@code round_door} id) carries this entity, so the client has one place to draw
 * the whole leaf from, and the assembly has one place to keep its appearance (wood and
 * paint, {@link Appearance}), its camouflage and its face trims (0.8.0, {@link DoorTrimNbt}). It never
 * ticks: the blockstate stays authoritative for open/closed, and the client-side {@link DoorSwing}
 * only animates between states it is told about through {@link #setBlockState}.
 *
 * <p>The look and trims persist and synchronise as every anchor's do ({@link AppearanceAnchorBlockEntity});
 * the camouflage is the door's own: saved with its refund, sent to clients without it (always, so a
 * clear reaches them). Loading always replaces it and never touches the swing.
 */
public class RoundDoorBlockEntity extends AppearanceAnchorBlockEntity implements TrimAttachmentSource {
    private final DoorSwing swing;
    private BlockState renderState;
    private DoorLayout.Assembly renderAssembly;
    private AABB renderBounds;

    /** Immutable block states are a cheap invalidation key; only changed poses rebuild layout/bounds. */
    public DoorLayout.Assembly renderAssembly() {
        if (renderState != getBlockState()) {
            renderState = getBlockState();
            renderAssembly = DoorLayout.of(getBlockPos(), renderState);
            renderBounds = renderAssembly.sweepBounds();
        }
        return renderAssembly;
    }
    private final DoorArchStore arch = new DoorArchStore();
    private final io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStore hardware = new io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStore();
    public io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware hardware() { return hardware.value(); }
    public boolean hardwareEditable() { return hardware.editable(); }
    public boolean setHardware(io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware value) {
        if (!hardware.set(value)) return false;
        changed(); return true;
    }
    public void writeHardware(CompoundTag parent) { hardware.save(parent); }
    public void loadHardware(CompoundTag parent) { hardware.load(parent); changed(); }

    public Optional<DoorArchAttachment> arch() { return arch.attachment(); }
    @Override public DoorRenderExtras renderExtras() {
        return new DoorRenderExtras(arch(), camouflage().map(CamouflageData::material));
    }
    public boolean archEditable() { return arch.editable(); }
    public boolean setArch(@Nullable DoorArchAttachment attachment) {
        if (!arch.set(attachment)) return false;
        changed();
        return true;
    }
    @Nullable
    private CamouflageData camouflage;

    public RoundDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, AppearanceFamily.DOOR, Map.of(TrimSlot.PERIMETER, DoorTrimNbt.FORMAT));
        // Whatever state the entity is created in (placement, chunk load) is drawn at rest.
        this.swing = new DoorSwing(open(state));
    }

    public DoorSwing swing() {
        return swing;
    }

    public Optional<CamouflageData> camouflage() {
        return Optional.ofNullable(camouflage);
    }

    /** Server: replaces (or with null clears) the camouflage, saves, and tells the clients. */
    public void setCamouflage(@Nullable CamouflageData data) {
        camouflage = data;
        changed();
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

    /** On disk the camouflage keeps its refund. */
    @Override
    protected void saveFamily(CompoundTag tag) {
        arch.save(tag);
        hardware.save(tag);
        if (camouflage != null) {
            tag.put(CamouflageData.TAG, camouflage.write(true));
        }
    }

    /** Clients get the material, never the refund, and an explicit empty compound once it is cleared. */
    @Override
    protected void writeFamilyUpdate(CompoundTag tag) {
        arch.writeUpdate(tag);
        hardware.writeUpdate(tag);
        tag.put(CamouflageData.TAG, camouflage == null ? new CompoundTag() : camouflage.write(false));
    }

    @Override
    protected void loadFamily(CompoundTag tag) {
        arch.load(tag);
        hardware.load(tag);
        camouflage = CamouflageData.read(tag).orElse(null);
    }

    /** Called by the chunk whenever this cell's state changes; on the client an open change starts a swing. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        boolean was = open(getBlockState());
        super.setBlockState(state);
        boolean now = open(state);
        if (level != null && now != was) {
            swing.retarget(now, level.getGameTime());
        }
    }

    /** Both resting poses and the sweep between them, so the leaf is not culled while only its projection is in view. */
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof AssemblyDoorBlock) {
            renderAssembly();
            return renderBounds;
        }
        return new AABB(getBlockPos());
    }

    private static boolean open(BlockState state) {
        return state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN);
    }
}
