package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The three canonical blocks: one round door, one round window and one round trim, whose look comes
 * from their appearance (block entity), not from the block. Their behaviour is the oak counterparts'
 * whatever the appearance. The hidden 0.4.0 aliases are registered by {@code migration/LegacyContent}
 * with the same properties.
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Roundwood.MOD_ID);

    public static final RegistryObject<RoundDoorBlock> ROUND_DOOR = BLOCKS.register("round_door",
            () -> new RoundDoorBlock(doorProperties(), BlockSetType.OAK, ModBlockEntities::roundDoor));
    public static final RegistryObject<RoundWindowBlock> ROUND_WINDOW = BLOCKS.register("round_window",
            () -> new RoundWindowBlock(windowProperties(), ModBlockEntities::roundWindow));
    public static final RegistryObject<RoundTrimBlock> ROUND_TRIM = BLOCKS.register("round_trim",
            () -> new RoundTrimBlock(trimProperties(), ModBlockEntities::roundTrim));

    private ModBlocks() {
    }

    public static RoundDoorBlock roundDoor() {
        return ROUND_DOOR.get();
    }

    public static RoundWindowBlock roundWindow() {
        return ROUND_WINDOW.get();
    }

    public static RoundTrimBlock roundTrim() {
        return ROUND_TRIM.get();
    }

    /** The three canonical blocks: door, window, trim. */
    public static List<Block> canonical() {
        return List.of(roundDoor(), roundWindow(), roundTrim());
    }

    /** The oak door's hardness, sounds and flammability; a multi-cell assembly cannot be pushed. */
    public static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.copy(Blocks.OAK_DOOR).pushReaction(PushReaction.BLOCK);
    }

    /**
     * A thin wooden pane with round glazing: plank-like hardness and sound, but glass-like for light,
     * vision and spawning, and never occluding its neighbours (they show through the glass).
     */
    public static BlockBehaviour.Properties windowProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(Blocks.OAK_DOOR.defaultMapColor())
                .instrument(NoteBlockInstrument.BASS)
                .strength(1.5F, 3.0F)
                .sound(BlockSetType.OAK.soundType())
                .noOcclusion()
                .ignitedByLava()
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    /**
     * A decorative ring: oak planks' hardness, sound, tool and map colour, but no collision, no
     * occlusion (light and neighbours show through the open centre), no redstone, and a multi-cell
     * assembly cannot be pushed.
     */
    public static BlockBehaviour.Properties trimProperties() {
        return BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
                .noCollission()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }
}
