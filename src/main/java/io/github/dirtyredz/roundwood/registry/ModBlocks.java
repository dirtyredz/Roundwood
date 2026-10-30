package io.github.dirtyredz.roundwood.registry;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.adapter.ThinArchPieceBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.migration.block.RetiredArchwayBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.door.adapter.ArchedDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.window.pane.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.migration.block.RetiredRoundWindowBlock;
import io.github.dirtyredz.roundwood.migration.block.RetiredOrientedWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 20 current base blocks (including the compatibility frame), two retired archways and 13 retained
 * crossed windows. Canonical window lists expose only plain profiles; CrossWindowCompatibility
 * owns retired discovery and hidden-content aggregation.
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Roundwood.MOD_ID);

    public static final DeferredHolder<Block, RoundDoorBlock> ROUND_DOOR = BLOCKS.register(DoorDesign.ROUND.id(),
            () -> new RoundDoorBlock(doorProperties(), BlockSetType.OAK, ModBlockEntities::roundDoor));
    /** The 1 x 2 arched door (0.11.0); its anchor entity is the round door's type. */
    public static final DeferredHolder<Block, ArchedDoorBlock> ARCHED_DOOR = BLOCKS.register(DoorDesign.ARCHED.id(),
            () -> new ArchedDoorBlock(doorProperties(), BlockSetType.OAK, ModBlockEntities::roundDoor));
    public static final DeferredHolder<Block, io.github.dirtyredz.roundwood.block.door.adapter.DoubleArchedDoorBlock> DOUBLE_ARCHED_DOOR = BLOCKS.register(DoorDesign.DOUBLE_ARCHED.id(),
            () -> new io.github.dirtyredz.roundwood.block.door.adapter.DoubleArchedDoorBlock(DoorDesign.DOUBLE_ARCHED, doorProperties(), BlockSetType.OAK, ModBlockEntities::roundDoor));
    public static final DeferredHolder<Block, io.github.dirtyredz.roundwood.block.door.adapter.DoubleArchedDoorBlock> TALL_DOUBLE_ARCHED_DOOR = BLOCKS.register(DoorDesign.TALL_DOUBLE_ARCHED.id(),
            () -> new io.github.dirtyredz.roundwood.block.door.adapter.DoubleArchedDoorBlock(DoorDesign.TALL_DOUBLE_ARCHED, doorProperties(), BlockSetType.OAK, ModBlockEntities::roundDoor));
    public static io.github.dirtyredz.roundwood.block.door.adapter.DoubleArchedDoorBlock tallDoubleArchedDoor() { return TALL_DOUBLE_ARCHED_DOOR.get(); }
    public static io.github.dirtyredz.roundwood.block.door.adapter.DoubleArchedDoorBlock doubleArchedDoor() { return DOUBLE_ARCHED_DOOR.get(); }
    /** The 13 plain windows and their 13 retained crossed variants, keyed by design. */
    private static final Map<WindowDesign, DeferredHolder<Block, WindowPaneBlock>> WINDOWS = registerWindows();
    /** The static 2 x 2 archways (0.12.0), ordinary then keyhole. */
    private static final Map<ArchwayDesign, DeferredHolder<Block, ArchwayBlock>> ARCHWAYS = registerArchways();
    private static final Map<ArchPieceDesign, DeferredHolder<Block, ArchPieceBlock>> ARCH_PIECES = registerArchPieces(ArchPieceThickness.FULL);
    private static final Map<ArchPieceDesign, DeferredHolder<Block, ArchPieceBlock>> THIN_ARCH_PIECES = registerArchPieces(ArchPieceThickness.THIN);
    public static final DeferredHolder<Block, RoundTrimBlock> ROUND_TRIM = BLOCKS.register("round_trim",
            () -> new RoundTrimBlock(trimProperties(), ModBlockEntities::roundTrim));

    private ModBlocks() {
    }

    /** Registered products: plain profiles and only the thirteen historical crossed IDs. */
    public static final class WindowProducts {
        private WindowProducts() {}
        public static List<WindowDesign> all() {
            return WindowDesign.all().stream().filter(d -> d.profile() != WindowProfile.BLANK
                    || d.pattern() == WindowPattern.PLAIN).toList();
        }
    }

    private static Map<WindowDesign, DeferredHolder<Block, WindowPaneBlock>> registerWindows() {
        Map<WindowDesign, DeferredHolder<Block, WindowPaneBlock>> out = new LinkedHashMap<>();
        for (WindowDesign design : ModBlocks.WindowProducts.all()) {
            out.put(design, BLOCKS.register(design.id(), () -> design.pattern() == WindowPattern.CROSS
                    ? (design.profile().oriented() ? new RetiredOrientedWindowBlock(windowProperties(), design, ModBlockEntities::roundWindow)
                        : new RetiredRoundWindowBlock(windowProperties(), ModBlockEntities::roundWindow))
                    : design.profile().oriented()
                    ? new OrientedWindowBlock(windowProperties(), design, ModBlockEntities::roundWindow)
                    : new RoundWindowBlock(windowProperties(), design.pattern(), ModBlockEntities::roundWindow)));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<ArchwayDesign, DeferredHolder<Block, ArchwayBlock>> registerArchways() {
        Map<ArchwayDesign, DeferredHolder<Block, ArchwayBlock>> out = new LinkedHashMap<>();
        for (ArchwayDesign design : ArchwayDesign.values()) {
            out.put(design, BLOCKS.register(design.id(), () -> new RetiredArchwayBlock(archwayProperties(), design, ModBlockEntities::archway)));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<ArchPieceDesign, DeferredHolder<Block, ArchPieceBlock>> registerArchPieces(ArchPieceThickness thickness) {
        Map<ArchPieceDesign, DeferredHolder<Block, ArchPieceBlock>> out = new LinkedHashMap<>();
        for (ArchPieceDesign design : ArchPieceDesign.values()) {
            out.put(design, BLOCKS.register(thickness.id(design), () -> thickness == ArchPieceThickness.FULL
                    ? new ArchPieceBlock(archwayProperties(), design, ModBlockEntities::archPiece)
                    : new ThinArchPieceBlock(archwayProperties(), design, ModBlockEntities::archPiece)));
        }
        return Collections.unmodifiableMap(out);
    }

    public static ArchPieceBlock archPiece(ArchPieceDesign design) { return ARCH_PIECES.get(design).get(); }
    public static ArchPieceBlock archPiece(ArchPieceDesign design, ArchPieceThickness thickness) {
        return (thickness == ArchPieceThickness.FULL ? ARCH_PIECES : THIN_ARCH_PIECES).get(design).get();
    }
    /** Full products followed by their thin counterparts. */
    public static List<ArchPieceBlock> archPieces() { return java.util.stream.Stream.concat(ARCH_PIECES.values().stream(), THIN_ARCH_PIECES.values().stream()).map(DeferredHolder::get).toList(); }

    public static RoundDoorBlock roundDoor() {
        return ROUND_DOOR.get();
    }

    public static ArchedDoorBlock archedDoor() {
        return ARCHED_DOOR.get();
    }

    /** The canonical doors, round then arched. */
    public static List<AssemblyDoorBlock> doors() {
        return List.of(roundDoor(), archedDoor(), doubleArchedDoor(), tallDoubleArchedDoor());
    }

    /** The plain round window (the block id since 0.3). */
    public static RoundWindowBlock roundWindow() {
        return (RoundWindowBlock) window(WindowDesign.plain(WindowProfile.ROUND));
    }

    public static OrientedWindowBlock archLeft() {
        return (OrientedWindowBlock) window(WindowDesign.plain(WindowProfile.ARCH_LEFT));
    }

    public static OrientedWindowBlock archRight() {
        return (OrientedWindowBlock) window(WindowDesign.plain(WindowProfile.ARCH_RIGHT));
    }

    /** The two plain arch pieces, left then right. */
    public static List<OrientedWindowBlock> arches() {
        return List.of(archLeft(), archRight());
    }

    /** The window block with {@code design}. */
    public static WindowPaneBlock window(WindowDesign design) {
        return WINDOWS.get(design).get();
    }

    /** All 13 plain windows, in creative order. */
    public static List<WindowPaneBlock> windows() {
        return WINDOWS.entrySet().stream().filter(e -> e.getKey().pattern() == WindowPattern.PLAIN).map(e -> e.getValue().get()).toList();
    }

    /** The 12 plain oriented windows (every profile but the round window). */
    public static List<OrientedWindowBlock> orientedWindows() {
        return windows().stream().filter(OrientedWindowBlock.class::isInstance).map(OrientedWindowBlock.class::cast).toList();
    }

    /** The archway block with {@code design}. */
    public static ArchwayBlock archway(ArchwayDesign design) {
        return ARCHWAYS.get(design).get();
    }

    /** The retired archways, ordinary then keyhole. */
    public static List<ArchwayBlock> archways() {
        return ARCHWAYS.values().stream().map(DeferredHolder::get).toList();
    }

    public static RoundTrimBlock roundTrim() {
        return ROUND_TRIM.get();
    }

    /** The 20 base blocks: two doors, 13 profiles, four arch pieces and the compatibility trim block. */
    public static List<Block> canonical() {
        List<Block> out = new ArrayList<>();
        out.addAll(doors());
        out.addAll(windows());
        out.addAll(archPieces());
        out.add(roundTrim());
        return List.copyOf(out);
    }

    /** The oak door's hardness, sounds and flammability; a multi-cell assembly cannot be pushed. */
    public static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).pushReaction(PushReaction.BLOCK);
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
     * Static arch-family material properties: oak planks' hardness, sound, tool and map colour.
     * Product geometry supplies collision. It never occludes, carries no
     * redstone, and a multi-cell assembly cannot be pushed.
     */
    public static BlockBehaviour.Properties archwayProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
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
        return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                .noCollission()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }
}
