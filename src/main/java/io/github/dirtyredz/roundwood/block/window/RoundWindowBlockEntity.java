package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
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
 * Every window carries one of these (never ticking) for its appearance. The window stays
 * chunk-baked: the entity hands its appearance to the model as {@link WindowAppearanceData}, and an
 * update packet asks the client to refresh that data and re-render the section. Loading always
 * replaces the whole appearance, so paint removal and a return to oak reach clients.
 */
public class RoundWindowBlockEntity extends BlockEntity implements AppearanceActions.Target {
    /** Null: the block's default. */
    @Nullable
    private Appearance appearance;

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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        appearance = AppearanceNbt.read(tag).filter(AppearanceFamily.WINDOW::accepts).orElse(null);
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
        return ModelData.builder().with(WindowAppearanceData.PROPERTY, new WindowAppearanceData(appearance())).build();
    }

    private void refreshModel() {
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }
}
