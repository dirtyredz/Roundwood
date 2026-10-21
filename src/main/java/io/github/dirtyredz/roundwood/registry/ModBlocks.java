package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.variant.Finish;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.window.RoundWindowBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** One round door and one round window per finish. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Roundwood.MOD_ID);

    private static final Map<Finish, RegistryObject<RoundDoorBlock>> DOORS = new LinkedHashMap<>();
    private static final Map<Finish, RegistryObject<RoundWindowBlock>> WINDOWS = new LinkedHashMap<>();

    static {
        for (Finish finish : FinishCatalog.ALL) {
            DOORS.put(finish, BLOCKS.register(finish.doorId(),
                    () -> new RoundDoorBlock(doorProperties(finish), finish.setType(), ModBlockEntities::roundDoor)));
        }
        for (Finish finish : FinishCatalog.ALL) {
            WINDOWS.put(finish, BLOCKS.register(finish.windowId(),
                    () -> new RoundWindowBlock(windowProperties(finish))));
        }
    }

    private ModBlocks() {
    }

    public static RoundDoorBlock door(Finish finish) {
        return DOORS.get(finish).get();
    }

    public static RoundWindowBlock window(Finish finish) {
        return WINDOWS.get(finish).get();
    }

    public static List<RoundDoorBlock> doors() {
        return DOORS.values().stream().map(RegistryObject::get).toList();
    }

    public static List<RoundWindowBlock> windows() {
        return WINDOWS.values().stream().map(RegistryObject::get).toList();
    }

    public static List<Block> all() {
        return Stream.concat(doors().stream(), windows().stream()).map(Block.class::cast).toList();
    }

    private static BlockBehaviour.Properties doorProperties(Finish finish) {
        // Same hardness, sounds and flammability as the vanilla door; a multi-cell assembly cannot be pushed.
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.copy(finish.baseDoor())
                .pushReaction(PushReaction.BLOCK);
        finish.paint().ifPresent(dye -> properties.mapColor(dye));
        return properties;
    }

    /**
     * A thin wooden pane with round glazing: plank-like hardness and sound, but glass-like for light,
     * vision and spawning, and never occluding its neighbours (they show through the glass).
     */
    private static BlockBehaviour.Properties windowProperties(Finish finish) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(finish.baseDoor().defaultMapColor())
                .instrument(NoteBlockInstrument.BASS)
                .strength(1.5F, 3.0F)
                .sound(finish.setType().soundType())
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
        if (finish.baseDoor().defaultBlockState().ignitedByLava()) {
            properties.ignitedByLava(); // nether woods do not burn, like their doors
        }
        finish.paint().ifPresent(dye -> properties.mapColor(dye));
        return properties;
    }
}
