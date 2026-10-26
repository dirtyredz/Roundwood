package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The anchors: the doors' (id {@code round_door}, unchanged since 0.2.0 so saved camouflage loads; it also
 * anchors the arched door since 0.11.0),
 * the window's and the trim's. Each is valid for its canonical block(s) and that family's hidden
 * 0.4.0 aliases, so saved entities load into unconverted blocks. The window entity (id
 * {@code round_window}) serves all 26 canonical windows and the fifteen window aliases.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Roundwood.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // the data fixer type is unused for mod block entities
    public static final RegistryObject<BlockEntityType<RoundDoorBlockEntity>> ROUND_DOOR = BLOCK_ENTITIES.register("round_door",
            () -> BlockEntityType.Builder.of((pos, state) -> new RoundDoorBlockEntity(roundDoor(), pos, state),
                    blocksOf(AssemblyDoorBlock.class)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<RoundWindowBlockEntity>> ROUND_WINDOW = BLOCK_ENTITIES.register("round_window",
            () -> BlockEntityType.Builder.of((pos, state) -> new RoundWindowBlockEntity(roundWindow(), pos, state),
                    blocksOf(WindowPaneBlock.class)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<RoundTrimBlockEntity>> ROUND_TRIM = BLOCK_ENTITIES.register("round_trim",
            () -> BlockEntityType.Builder.of((pos, state) -> new RoundTrimBlockEntity(roundTrim(), pos, state),
                    blocksOf(RoundTrimBlock.class)).build(null));

    /** The archways' anchor (0.12.0), for both designs; archways have no aliases. */
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ArchwayBlockEntity>> ARCHWAY = BLOCK_ENTITIES.register("archway",
            () -> BlockEntityType.Builder.of((pos, state) -> new ArchwayBlockEntity(archway(), pos, state),
                    ModBlocks.archways().toArray(ArchwayBlock[]::new)).build(null));

    private ModBlockEntities() {
    }

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ArchPieceBlockEntity>> ARCH_PIECE = BLOCK_ENTITIES.register("arch_piece",
            () -> BlockEntityType.Builder.of((pos, state) -> new ArchPieceBlockEntity(archPiece(), pos, state),
                    blocksOf(ArchPieceBlock.class)).build(null));

    public static BlockEntityType<ArchPieceBlockEntity> archPiece() { return ARCH_PIECE.get(); }

    public static BlockEntityType<ArchwayBlockEntity> archway() {
        return ARCHWAY.get();
    }

    public static BlockEntityType<RoundDoorBlockEntity> roundDoor() {
        return ROUND_DOOR.get();
    }

    public static BlockEntityType<RoundWindowBlockEntity> roundWindow() {
        return ROUND_WINDOW.get();
    }

    public static BlockEntityType<RoundTrimBlockEntity> roundTrim() {
        return ROUND_TRIM.get();
    }

    /** All entries share this deferred register, including aliases added by bootstrap before registration fires. */
    private static <T extends Block> Block[] blocksOf(Class<T> type) {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get).filter(type::isInstance).toArray(Block[]::new);
    }
}
