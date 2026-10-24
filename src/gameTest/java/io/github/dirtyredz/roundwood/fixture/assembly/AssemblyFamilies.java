package io.github.dirtyredz.roundwood.fixture.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceDefaults;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.appearance.ArchwayAppearance;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPlacement;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.appearance.DoorAppearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.appearance.TrimAppearance;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The five multi-cell objects (round door, arched door, ordinary and keyhole archway, compatibility
 * frame) behind one test-side vocabulary, so one lifecycle or anchor test can loop over all of them.
 * Every method forwards to the family's own production method (its layout, placement, anchor lookup,
 * appearance and target); nothing here reimplements them. Cells are indexed in the family's part
 * order; index 0 is always the anchor (lower-left). All positions are absolute.
 */
public final class AssemblyFamilies {
    /** A look nothing defaults to, for the anchor of the object under test. */
    public static final Appearance LOOK = wood("jungle_planks");
    /** A second look, for a decoy anchor. */
    public static final Appearance OTHER_LOOK = wood("acacia_planks");

    private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private AssemblyFamilies() {
    }

    public static Appearance wood(String block) {
        return Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", block));
    }

    /** Round door, arched door, ordinary archway, keyhole archway, frame. */
    public static List<Family> all() {
        List<Family> out = new ArrayList<>();
        out.add(new DoorFamily("round door", ModBlocks.roundDoor(), LegacyContent.doors().get(0), DoorDesign.ARCHED, ModBlocks.archedDoor()));
        out.add(new DoorFamily("arched door", ModBlocks.archedDoor(), null, DoorDesign.ROUND, ModBlocks.roundDoor()));
        out.add(new ArchwayFamily("ordinary archway", ArchwayDesign.ORDINARY, ArchwayDesign.KEYHOLE));
        out.add(new ArchwayFamily("keyhole archway", ArchwayDesign.KEYHOLE, ArchwayDesign.ORDINARY));
        out.add(new TrimFamily("frame", ModBlocks.roundTrim(), LegacyContent.trims().get(0)));
        return out;
    }

    // ---- raw world access (no callbacks, no scheduled ticks, no block entity) ----

    /** Writes {@code state} straight into the chunk section, as a chunk load does. The chunk must be loaded. */
    public static void writeRaw(ServerLevel level, BlockPos pos, BlockState state) {
        LevelChunk chunk = level.getChunkAt(pos);
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
        level.getChunkSource().blockChanged(pos);
    }

    /**
     * Clears the loaded cells silently: inside an {@link AssemblyMutationBatch} (the parts' own removal
     * callbacks stand down), drops suppressed, clients only. Never touches an unloaded cell.
     */
    public static void silentClear(Level level, List<BlockPos> cells) {
        AssemblyMutationBatch.run(level, cells, () -> {
            for (BlockPos pos : cells) {
                if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), SILENT);
                }
            }
        });
    }

    /** Replaces one cell silently (batch, clients only), as a test double for a foreign or aliased cell. */
    public static void swap(Level level, List<BlockPos> cells, BlockPos pos, BlockState state) {
        AssemblyMutationBatch.run(level, cells, () -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
    }

    /** The loot of {@code state} broken at {@code origin}, with {@code captured} as the broken block's entity (may be null). */
    public static List<ItemStack> drops(ServerLevel level, BlockState state, BlockPos origin, @Nullable BlockEntity captured) {
        return state.getDrops(lootBuilder(level, origin, captured));
    }

    /** The loot parameters a block break supplies (origin, tool, and the captured entity when there is one). */
    public static LootParams.Builder lootBuilder(ServerLevel level, BlockPos origin, @Nullable BlockEntity captured) {
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, captured);
    }

    // ---- the vocabulary ----

    /** One kind of multi-cell object. */
    public abstract static class Family {
        private final String name;
        private final Block block;
        @Nullable
        private final Block alias;

        Family(String name, Block block, @Nullable Block alias) {
            this.name = name;
            this.block = block;
            this.alias = alias;
        }

        public String name() {
            return name;
        }

        /** The canonical block of this family. */
        public Block block() {
            return block;
        }

        /** A hidden 0.4.0 alias block of the same family class, or null when the family has none. */
        @Nullable
        public Block alias() {
            return alias;
        }

        /** The item that carries the object's look. */
        public Item item() {
            return block.asItem();
        }

        /** The object whose anchor (lower-left) cell is {@code anchor}, facing {@code facing}. */
        public abstract Instance at(BlockPos anchor, Direction facing);

        @Override
        public String toString() {
            return name;
        }
    }

    /** One object of a family at a place. */
    public abstract static class Instance {
        private final Family family;
        private final BlockPos anchor;
        private final Direction facing;

        Instance(Family family, BlockPos anchor, Direction facing) {
            this.family = family;
            this.anchor = anchor.immutable();
            this.facing = facing;
        }

        public Family family() {
            return family;
        }

        public BlockPos anchor() {
            return anchor;
        }

        public Direction facing() {
            return facing;
        }

        /** Number of cells (4, or 2 for the arched door). */
        public abstract int size();

        /** Columns the object is wide (2, or 1 for the arched door). */
        public abstract int columns();

        public abstract BlockPos pos(int i);

        /** The state of cell {@code i} as the family's layout writes it, in block {@code as} (canonical or alias). */
        public abstract BlockState state(int i, Block as);

        /**
         * States that sit at cell {@code i} but belong to a different layout (another facing, hinge or
         * design), so the anchor lookup must not take that cell for this object's anchor.
         */
        public abstract List<BlockState> foreignLayouts(int i);

        /** The same object moved so that its cell {@code i} is at {@code cell} (same facing, hinge and design). */
        public abstract Instance atCell(int i, BlockPos cell);

        /** The layout's own "is this state cell {@code i} of this object in the canonical block". */
        public abstract boolean isPart(BlockState state, int i);

        /** The layout's {@code loaded}: every cell in a loaded chunk. */
        public abstract boolean loaded(Level level);

        /** The layout's {@code presentParts} count: loaded cells holding this object's canonical part. */
        public abstract int present(Level level);

        /** The layout's {@code complete}. */
        public abstract boolean complete(Level level);

        /** Production placement without a player; false when refused. Installs {@code look} on the anchor. */
        public abstract boolean place(Level level, Appearance look);

        /**
         * The look found by the family's anchor lookup from {@code pos}/{@code state}; empty when it finds
         * no anchor. (The frame has no separate lookup: its appearance resolver is used and a result equal
         * to the queried block's default counts as "none", so tests give the anchor a non-default look.)
         */
        public abstract Optional<Appearance> anchorLook(BlockGetter level, BlockPos pos, BlockState state);

        /** The family's appearance resolver for picks and rendering (the block's default when no anchor). */
        public abstract Appearance look(BlockGetter level, BlockPos pos, BlockState state);

        /** The family's loot appearance resolver. */
        public abstract Appearance dropsLook(LootParams.Builder builder, BlockState state);

        /** The family's whole-object restyling target as seen from the clicked cell. */
        public abstract Optional<AppearanceActions.Target> target(Level level, BlockPos pos, BlockState state);

        /** Sets the look on the anchor cell's block entity, whatever block is there; false when there is none. */
        public boolean setLook(Level level, Appearance look) {
            BlockEntity entity = level.getBlockEntity(anchor);
            if (entity instanceof RoundDoorBlockEntity door) {
                door.setAppearance(look);
            } else if (entity instanceof ArchwayBlockEntity archway) {
                archway.setAppearance(look);
            } else if (entity instanceof RoundTrimBlockEntity trim) {
                trim.setAppearance(look);
            } else {
                return false;
            }
            return true;
        }

        public List<BlockPos> positions() {
            List<BlockPos> out = new ArrayList<>(size());
            for (int i = 0; i < size(); i++) {
                out.add(pos(i));
            }
            return out;
        }

        public BlockState state(int i) {
            return state(i, family.block());
        }

        /** The same object shifted by whole columns across (to its right) and rows up. */
        public Instance shifted(int columnsRight, int up) {
            BlockPos next = anchor.relative(facing.getClockWise(), columnsRight).above(up);
            return family.at(next, facing);
        }

        /** The object just to the right of this one, touching it. */
        public Instance rightNeighbour() {
            return shifted(columns(), 0);
        }

        /** The object stacked directly on top of this one. */
        public Instance upperNeighbour() {
            return shifted(0, 2);
        }

        /** Writes every cell silently (batch, clients only; no placement checks) and sets {@code look} on the anchor. */
        public void writeSilently(Level level, Appearance look) {
            List<BlockPos> cells = positions();
            AssemblyMutationBatch.run(level, cells, () -> {
                for (int i = 0; i < size(); i++) {
                    level.setBlock(pos(i), state(i), Block.UPDATE_CLIENTS);
                }
            });
            setLook(level, look);
        }

        /** True when every cell is the canonical block's matching part. */
        public boolean allParts(Level level) {
            for (int i = 0; i < size(); i++) {
                if (!isPart(level.getBlockState(pos(i)), i)) {
                    return false;
                }
            }
            return true;
        }

        /** True when every cell is air. */
        public boolean allAir(Level level) {
            for (BlockPos pos : positions()) {
                if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
            return true;
        }

        /** Clears every loaded cell silently. */
        public void clear(Level level) {
            silentClear(level, positions());
        }

        BlockPos anchorForCell(int i, BlockPos cell) {
            return cell.subtract(pos(i).subtract(anchor));
        }
    }

    // ---- doors ----

    private static final class DoorFamily extends Family {
        private final DoorDesign other;
        private final AssemblyDoorBlock otherBlock;

        DoorFamily(String name, AssemblyDoorBlock block, @Nullable Block alias, DoorDesign other, AssemblyDoorBlock otherBlock) {
            super(name, block, alias);
            this.other = other;
            this.otherBlock = otherBlock;
        }

        AssemblyDoorBlock door() {
            return (AssemblyDoorBlock) block();
        }

        @Override
        public Instance at(BlockPos anchor, Direction facing) {
            return new DoorInstance(this, anchor, facing);
        }
    }

    private static final class DoorInstance extends Instance {
        private final DoorLayout.Assembly assembly;
        private final DoorFamily door;

        DoorInstance(DoorFamily family, BlockPos anchor, Direction facing) {
            super(family, anchor, facing);
            this.door = family;
            this.assembly = new DoorLayout.Assembly(anchor, facing, DoorHingeSide.LEFT, family.door().design());
        }

        @Override
        public int size() {
            return assembly.parts().size();
        }

        @Override
        public int columns() {
            return door.door().design().columns();
        }

        @Override
        public BlockPos pos(int i) {
            return assembly.pos(assembly.parts().get(i));
        }

        @Override
        public BlockState state(int i, Block as) {
            return assembly.stateFor(assembly.parts().get(i), as, false, false);
        }

        @Override
        public List<BlockState> foreignLayouts(int i) {
            DoorLayout.Part part = assembly.parts().get(i);
            DoorLayout.Assembly turned = new DoorLayout.Assembly(anchor(), facing().getOpposite(), DoorHingeSide.LEFT, assembly.design());
            DoorLayout.Assembly hinged = new DoorLayout.Assembly(anchor(), facing(), DoorHingeSide.RIGHT, assembly.design());
            DoorLayout.Assembly redesigned = new DoorLayout.Assembly(anchor(), facing(), DoorHingeSide.LEFT, door.other);
            return List.of(turned.stateFor(part, door.door(), false, false), hinged.stateFor(part, door.door(), false, false),
                    redesigned.stateFor(part, door.otherBlock, false, false));
        }

        @Override
        public Instance atCell(int i, BlockPos cell) {
            return family().at(anchorForCell(i, cell), facing());
        }

        @Override
        public boolean isPart(BlockState state, int i) {
            return assembly.isPart(state, door.door(), assembly.parts().get(i));
        }

        @Override
        public boolean loaded(Level level) {
            return assembly.loaded(level);
        }

        @Override
        public int present(Level level) {
            return assembly.presentParts(level, door.door()).size();
        }

        @Override
        public boolean complete(Level level) {
            return assembly.complete(level, door.door());
        }

        @Override
        public boolean place(Level level, Appearance look) {
            return DoorPlacement.placeAt(level, door.door(), assembly, false, look);
        }

        @Override
        public Optional<Appearance> anchorLook(BlockGetter level, BlockPos pos, BlockState state) {
            return DoorAppearance.ANCHORS.anchor(level, pos, state).map(RoundDoorBlockEntity::appearance);
        }

        @Override
        public Appearance look(BlockGetter level, BlockPos pos, BlockState state) {
            return DoorAppearance.of(level, pos, state);
        }

        @Override
        public Appearance dropsLook(LootParams.Builder builder, BlockState state) {
            return DoorAppearance.forDrops(builder, state);
        }

        @Override
        public Optional<AppearanceActions.Target> target(Level level, BlockPos pos, BlockState state) {
            return DoorAppearance.target((AssemblyDoorBlock) state.getBlock(), state, level, pos);
        }
    }

    // ---- archways ----

    private static final class ArchwayFamily extends Family {
        private final ArchwayDesign design;
        private final ArchwayDesign other;

        ArchwayFamily(String name, ArchwayDesign design, ArchwayDesign other) {
            super(name, ModBlocks.archway(design), null);
            this.design = design;
            this.other = other;
        }

        @Override
        public Instance at(BlockPos anchor, Direction facing) {
            return new ArchwayInstance(this, anchor, facing);
        }
    }

    private static final class ArchwayInstance extends Instance {
        private final ArchwayLayout.Assembly assembly;
        private final ArchwayFamily archway;

        ArchwayInstance(ArchwayFamily family, BlockPos anchor, Direction facing) {
            super(family, anchor, facing);
            this.archway = family;
            this.assembly = new ArchwayLayout.Assembly(anchor, facing, family.design);
        }

        @Override
        public int size() {
            return ArchwayPart.values().length;
        }

        @Override
        public int columns() {
            return 2;
        }

        @Override
        public BlockPos pos(int i) {
            return assembly.pos(ArchwayPart.values()[i]);
        }

        @Override
        public BlockState state(int i, Block as) {
            return assembly.stateFor(ArchwayPart.values()[i], as);
        }

        @Override
        public List<BlockState> foreignLayouts(int i) {
            ArchwayPart part = ArchwayPart.values()[i];
            ArchwayLayout.Assembly turned = new ArchwayLayout.Assembly(anchor(), facing().getOpposite(), archway.design);
            return List.of(turned.stateFor(part, archway.block()),
                    assembly.stateFor(part, ModBlocks.archway(archway.other)));
        }

        @Override
        public Instance atCell(int i, BlockPos cell) {
            return family().at(anchorForCell(i, cell), facing());
        }

        @Override
        public boolean isPart(BlockState state, int i) {
            return assembly.isPart(state, archway.block(), ArchwayPart.values()[i]);
        }

        @Override
        public boolean loaded(Level level) {
            return assembly.loaded(level);
        }

        @Override
        public int present(Level level) {
            return assembly.presentParts(level, archway.block()).size();
        }

        @Override
        public boolean complete(Level level) {
            return assembly.complete(level, archway.block());
        }

        @Override
        public boolean place(Level level, Appearance look) {
            return ArchwayPlacement.placeAt(level, (ArchwayBlock) archway.block(), assembly, look);
        }

        @Override
        public Optional<Appearance> anchorLook(BlockGetter level, BlockPos pos, BlockState state) {
            return ArchwayAppearance.ANCHORS.anchor(level, pos, state).map(ArchwayBlockEntity::appearance);
        }

        @Override
        public Appearance look(BlockGetter level, BlockPos pos, BlockState state) {
            return ArchwayAppearance.of(level, pos, state);
        }

        @Override
        public Appearance dropsLook(LootParams.Builder builder, BlockState state) {
            return ArchwayAppearance.forDrops(builder, state);
        }

        @Override
        public Optional<AppearanceActions.Target> target(Level level, BlockPos pos, BlockState state) {
            return ArchwayAppearance.target((ArchwayBlock) state.getBlock(), state, level, pos);
        }
    }

    // ---- the compatibility frame ----

    private static final class TrimFamily extends Family {
        TrimFamily(String name, RoundTrimBlock block, RoundTrimBlock alias) {
            super(name, block, alias);
        }

        @Override
        public Instance at(BlockPos anchor, Direction facing) {
            return new TrimInstance(this, anchor, facing);
        }
    }

    private static final class TrimInstance extends Instance {
        private final TrimLayout.Assembly assembly;

        TrimInstance(TrimFamily family, BlockPos anchor, Direction facing) {
            super(family, anchor, facing);
            this.assembly = new TrimLayout.Assembly(anchor, facing);
        }

        private RoundTrimBlock trim() {
            return (RoundTrimBlock) family().block();
        }

        @Override
        public int size() {
            return Quarter.values().length;
        }

        @Override
        public int columns() {
            return 2;
        }

        @Override
        public BlockPos pos(int i) {
            return assembly.pos(Quarter.values()[i]);
        }

        @Override
        public BlockState state(int i, Block as) {
            return assembly.stateFor(Quarter.values()[i], as);
        }

        @Override
        public List<BlockState> foreignLayouts(int i) {
            TrimLayout.Assembly turned = new TrimLayout.Assembly(anchor(), facing().getOpposite());
            return List.of(turned.stateFor(Quarter.values()[i], trim()));
        }

        @Override
        public Instance atCell(int i, BlockPos cell) {
            return family().at(anchorForCell(i, cell), facing());
        }

        @Override
        public boolean isPart(BlockState state, int i) {
            return assembly.isPart(state, trim(), Quarter.values()[i]);
        }

        @Override
        public boolean loaded(Level level) {
            return assembly.loaded(level);
        }

        @Override
        public int present(Level level) {
            return assembly.presentParts(level, trim()).size();
        }

        @Override
        public boolean complete(Level level) {
            return assembly.complete(level, trim());
        }

        @Override
        public boolean place(Level level, Appearance look) {
            return TrimPlacement.placeAt(level, trim(), assembly, look);
        }

        @Override
        public Optional<Appearance> anchorLook(BlockGetter level, BlockPos pos, BlockState state) {
            Appearance look = TrimAppearance.of(level, pos, state);
            return look.equals(AppearanceDefaults.of(state.getBlock())) ? Optional.empty() : Optional.of(look);
        }

        @Override
        public Appearance look(BlockGetter level, BlockPos pos, BlockState state) {
            return TrimAppearance.of(level, pos, state);
        }

        @Override
        public Appearance dropsLook(LootParams.Builder builder, BlockState state) {
            return TrimAppearance.forDrops(builder, state);
        }

        @Override
        public Optional<AppearanceActions.Target> target(Level level, BlockPos pos, BlockState state) {
            return TrimAppearance.target((RoundTrimBlock) state.getBlock(), state, level, pos);
        }
    }
}
