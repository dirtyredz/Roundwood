package io.github.dirtyredz.roundwood.data.client.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.geometry.RingGeometry;
import io.github.dirtyredz.roundwood.client.trim.RingModelNames;
import io.github.dirtyredz.roundwood.data.client.door.DoorModelProvider;
import io.github.dirtyredz.roundwood.data.client.GeometryModelWriter;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.List;

/**
 * The compatibility frame's blockstate and models (the Universal Trim's icon is
 * {@link AttachmentItemModelProvider}'s). The frame is drawn by its anchor's block-entity renderer,
 * so every trim state uses one empty world model (break particle only). Each cell-local quarter
 * template holds its piece of the ring once, authored facing north, in two mapping families:
 * <b>tiled</b> (wood: the texture repeats per block like any block) and <b>whole</b> (paint and
 * masonry: one image across the whole ring, so radial brick joints and painted boards continue
 * across the quarters, {@link AssemblyFaceUv}). Front and back use {@code #face}, the ring's sides
 * {@code #edge}; faces where two quarters meet are buried and omitted. The source quarters
 * ({@link RingModelNames}) bind oak; the client retextures and turns them per appearance and facing.
 */
public class TrimModelProvider extends BlockStateProvider {
    public static final String TEMPLATE = "template_round_trim_";
    public static final String WHOLE_TEMPLATE = "template_round_trim_whole_";

    public TrimModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood frame blockstates and models";
    }

    /** Wood sources bind the stripped oak log; whole-ring sources the oak board panel. */
    static final String OAK_LOG = "minecraft:block/stripped_oak_log";
    private static final String OAK_PANEL = "roundwood:" + DoorModelProvider.OAK_PANEL;

    /**
     * How a cell-local quarter's faces are written: front and back with the face texture, sides with
     * the edge texture; with {@code whole}, front and back spread one image across the ring.
     * {@code column} and {@code row} place the quarter in the assembly; buried faces between quarters
     * are skipped.
     */
    private static GeometryModelWriter.FaceStyle style(boolean whole, int column, int row) {
        return new GeometryModelWriter.FaceStyle() {
            @Override
            public boolean draw(Box part, Direction face) {
                return switch (face) {
                    case EAST -> !(column == 0 && part.maxX() == 16);
                    case WEST -> !(column == 1 && part.minX() == 0);
                    case UP -> !(row == 0 && part.maxY() == 16);
                    case DOWN -> !(row == 1 && part.minY() == 0);
                    default -> true;
                };
            }

            @Override
            public String texture(Piece piece, Direction face) {
                return face.getAxis() == Direction.Axis.Z ? RingGeometry.FACE : RingGeometry.EDGE;
            }

            @Override
            public float[] uv(Piece piece, Box part, Direction face) {
                return whole ? AssemblyFaceUv.of(part.translate(16 * column, 16 * row, 0), face, RingGeometry.OUTER) : null;
            }
        };
    }

    @Override
    protected void registerStatesAndModels() {
        for (Quarter part : Quarter.values()) {
            source(part, template(TEMPLATE, part, false), false);
            source(part, template(WHOLE_TEMPLATE, part, true), true);
        }
        getVariantBuilder(ModBlocks.roundTrim()).partialState().setModels(new ConfiguredModel(worldModel()));
    }

    /** The empty world model every trim state (canonical and alias) uses. */
    private ModelFile worldModel() {
        return models().getBuilder(RingModelNames.TRIM_ID).texture("particle", OAK_LOG);
    }

    private void source(Quarter part, BlockModelBuilder template, boolean whole) {
        String texture = whole ? OAK_PANEL : OAK_LOG;
        models().getBuilder(RingModelNames.quarter(part, whole)).parent(template)
                .texture(RingGeometry.FACE, texture)
                .texture(RingGeometry.EDGE, texture)
                .texture("particle", texture);
    }

    private BlockModelBuilder template(String prefix, Quarter part, boolean whole) {
        BlockModelBuilder builder = models().getBuilder(prefix + part.getSerializedName());
        builder.texture("particle", "#" + RingGeometry.EDGE);
        List<Piece> pieces = RingGeometry.quarter(part.column, part.row).stream()
                .map(box -> new Piece(box, RingGeometry.FACE)).toList();
        return GeometryModelWriter.addPieces(builder, pieces, true, style(whole, part.column, part.row));
    }
}
