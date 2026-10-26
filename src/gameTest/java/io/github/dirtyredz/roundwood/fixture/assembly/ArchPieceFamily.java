package io.github.dirtyredz.roundwood.fixture.assembly;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.appearance.ArchPieceAppearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import java.util.List;
import java.util.Optional;

/** Modular adapter kept separate from the historical family fixtures and their fixed-index tests. */
public final class ArchPieceFamily extends AssemblyFamilies.Family {
    private final ArchPieceDesign design;
    private final Side side;
    public ArchPieceFamily(ArchPieceDesign design, Side side) {
        super(design.id() + "/" + side, ModBlocks.archPiece(design), null);
        this.design = design;
        this.side = side;
    }
    @Override public AssemblyFamilies.Instance at(BlockPos anchor, Direction facing) { return new PieceInstance(this, anchor, facing); }

    private static final class PieceInstance extends AssemblyFamilies.Instance {
        private final ArchPieceLayout.Assembly layout;
        private final ArchPieceFamily piece;
        PieceInstance(ArchPieceFamily family, BlockPos anchor, Direction facing) {
            super(family, anchor, facing);
            piece = family;
            layout = new ArchPieceLayout.Assembly(anchor, facing, family.design, family.side);
        }
        @Override public int size() { return layout.parts().size(); }
        @Override public int columns() { return piece.design.columns(); }
        @Override public BlockPos pos(int i) { return layout.pos(layout.parts().get(i)); }
        @Override public BlockState state(int i, Block block) { return layout.stateFor(layout.parts().get(i), block); }
        @Override public List<BlockState> foreignLayouts(int i) {
            return List.of(new ArchPieceLayout.Assembly(anchor(), facing().getOpposite(), piece.design, piece.side)
                    .stateFor(layout.parts().get(i), piece.block()),
                    new ArchPieceLayout.Assembly(anchor(), facing(), piece.design, piece.side.mirrored())
                            .stateFor(layout.parts().get(i), piece.block()));
        }
        @Override public AssemblyFamilies.Instance atCell(int i, BlockPos cell) {
            return piece.at(ArchPieceLayout.anchorFor(cell, facing(), layout.parts().get(i)), facing());
        }
        @Override public boolean isPart(BlockState state, int i) { return layout.isPart(state, piece.block(), layout.parts().get(i)); }
        @Override public boolean loaded(Level level) { return layout.loaded(level); }
        @Override public int present(Level level) { return layout.presentParts(level, piece.block()).size(); }
        @Override public boolean complete(Level level) { return layout.complete(level, piece.block()); }
        @Override public boolean place(Level level, Appearance look) {
            return ArchPiecePlacement.placeAt(level, (ArchPieceBlock) piece.block(), layout, look);
        }
        @Override public Optional<Appearance> anchorLook(BlockGetter level, BlockPos pos, BlockState state) {
            return ArchPieceAppearance.ANCHORS.anchor(level, pos, state).map(ArchPieceBlockEntity::appearance);
        }
        @Override public Appearance look(BlockGetter level, BlockPos pos, BlockState state) { return ArchPieceAppearance.of(level, pos, state); }
        @Override public Appearance dropsLook(LootParams.Builder builder, BlockState state) { return ArchPieceAppearance.forDrops(builder, state); }
        @Override public Optional<AppearanceActions.Target> target(Level level, BlockPos pos, BlockState state) {
            return ArchPieceAppearance.target((ArchPieceBlock) piece.block(), state, level, pos);
        }
        @Override public boolean setLook(Level level, Appearance look) {
            if (!(level.getBlockEntity(anchor()) instanceof ArchPieceBlockEntity entity)) return false;
            entity.setAppearance(look);
            return true;
        }
        @Override public AssemblyFamilies.Instance upperNeighbour() { return shifted(0, piece.design.rows()); }
    }
}
