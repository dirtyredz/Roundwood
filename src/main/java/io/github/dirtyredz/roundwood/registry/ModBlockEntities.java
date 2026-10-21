package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.door.RoundDoorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The one block-entity type: the anchor of a round door, valid for every finish. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Roundwood.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // the data fixer type is unused for mod block entities
    public static final RegistryObject<BlockEntityType<RoundDoorBlockEntity>> ROUND_DOOR = BLOCK_ENTITIES.register("round_door",
            () -> BlockEntityType.Builder.of((pos, state) -> new RoundDoorBlockEntity(roundDoor(), pos, state),
                    ModBlocks.doors().toArray(RoundDoorBlock[]::new)).build(null));

    private ModBlockEntities() {
    }

    public static BlockEntityType<RoundDoorBlockEntity> roundDoor() {
        return ROUND_DOOR.get();
    }
}
