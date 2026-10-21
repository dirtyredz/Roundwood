package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Every window carries one of these (never ticking) for its appearance and, when a window trim is
 * fitted ({@link WindowPaneBlock#TRIMMED}), the trim's own look (0.7.0, saved under
 * {@link WindowTrimNbt#TAG}; the pane's top-level appearance tag is unchanged). The window stays
 * chunk-baked: the entity hands both looks to the model as one {@link WindowAppearanceData}
 * snapshot, and an update packet asks the client to refresh that data and re-render the section.
 * Loading always replaces the whole payload, so paint removal, a return to oak and a detached trim
 * reach clients. The blockstate decides whether a trim exists: trim data on an untrimmed window is
 * ignored, and a trimmed window without (valid) data shows an oak trim.
 */
public class RoundWindowBlockEntity extends BlockEntity implements AppearanceActions.Target {
    /** Null: the block's default. */
    @Nullable
    private Appearance appearance;
    /** Null: an oak trim (when the state is trimmed at all). */
    @Nullable
    private Appearance trim;

    public RoundWindowBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    /** The fitted trim's look, or empty when the window has no trim. */
    public Optional<Appearance> trim() {
        if (!WindowPaneBlock.trimmed(getBlockState())) {
            return Optional.empty();
        }
        return Optional.of(trim != null ? trim : Appearance.DEFAULT);
    }

    /** Server: replaces the trim's look (the state must already be trimmed), saves and tells the clients. */
    public void setTrim(Appearance next) {
        trim = next.equals(Appearance.DEFAULT) ? null : next;
        changed();
    }

    /** Server: forgets the trim's look (after the state lost its trim), saves and tells the clients. */
    public void clearTrim() {
        trim = null;
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
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        if (trim != null) {
            WindowTrimNbt.write(tag, trim);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.WINDOW::accepts).orElse(null);
        trim = WindowTrimNbt.read(tag).filter(look -> !look.equals(Appearance.DEFAULT)).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        if (appearance != null) {
            AppearanceNbt.write(tag, appearance);
        }
        if (trim != null) {
            WindowTrimNbt.write(tag, trim);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Client: the whole appearance replaces the old one, then the model data and the section are refreshed. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        load(tag == null ? new CompoundTag() : tag);
        refreshModel();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        refreshModel();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(WindowAppearanceData.PROPERTY, new WindowAppearanceData(appearance(), trim())).build();
    }

    private void refreshModel() {
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }
}
