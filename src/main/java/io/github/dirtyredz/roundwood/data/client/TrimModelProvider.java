package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPart;
import io.github.dirtyredz.roundwood.block.trim.geometry.TrimGeometry;
import io.github.dirtyredz.roundwood.geometry.AssemblyFaceUv;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import io.github.dirtyredz.roundwood.variant.TrimMaterial;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Round frame blockstates and models: static, cell-local quarter models (no block entity). Each
 * quarter template holds its piece of the ring once; each material binds a face and an edge texture.
 * Two template families: <b>tiled</b> (natural wood: the stripped log repeats per block like any
 * block) and <b>whole</b> (painted wood and masonry: one image across the whole ring, so radial brick
 * joints and painted boards continue across the quarters, {@link AssemblyFaceUv}). Front and back
 * use {@code #face}, the ring's sides {@code #edge}. Faces where two quarters meet are buried and
 * omitted. Every state (facing x part) picks its quarter, turned for the facing.
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

    /** The model path of a material's quarter. */
    public static String quarterModel(TrimMaterial material, TrimPart part) {
        return material.trimId() + "_" + part.getSerializedName();
    }

    /**
     * How the frame's faces are written: front and back with the face texture, sides with the edge
     * texture; with {@code whole}, front and back spread one image across the ring. {@code column} and
     * {@code row} place a cell-local quarter in the assembly (0, 0 for whole-ring models); buried
     * faces between quarters are skipped for cell-local quarters.
     */
    static GeometryModelWriter.FaceStyle style(boolean whole, int column, int row, boolean cellLocal) {
        return new GeometryModelWriter.FaceStyle() {
            @Override
            public boolean draw(Box part, Direction face) {
                if (!cellLocal) {
                    return true;
                }
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
                return face.getAxis() == Direction.Axis.Z ? TrimGeometry.FACE : TrimGeometry.EDGE;
            }

            @Override
            public float[] uv(Piece piece, Box part, Direction face) {
                return whole ? AssemblyFaceUv.of(part.translate(16 * column, 16 * row, 0), face, TrimGeometry.OUTER) : null;
            }
        };
    }

    @Override
    protected void registerStatesAndModels() {
        Map<TrimPart, BlockModelBuilder> tiled = templates(TEMPLATE, false);
        Map<TrimPart, BlockModelBuilder> whole = templates(WHOLE_TEMPLATE, true);
        for (TrimMaterial material : FinishCatalog.TRIMS) {
            Map<TrimPart, BlockModelBuilder> models = new EnumMap<>(TrimPart.class);
            for (TrimPart part : TrimPart.values()) {
                models.put(part, models().getBuilder(quarterModel(material, part))
                        .parent((material.wholeFace() ? whole : tiled).get(part))
                        .texture(TrimGeometry.FACE, material.faceTexture())
                        .texture(TrimGeometry.EDGE, material.edgeTexture())
                        .texture("particle", material.edgeTexture()));
            }
            RoundTrimBlock block = ModBlocks.trim(material);
            getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(models.get(state.getValue(RoundTrimBlock.PART)))
                    .rotationY(90 * VoxelShapeFactory.quarterTurns(state.getValue(RoundTrimBlock.FACING)))
                    .build());
        }
    }

    private Map<TrimPart, BlockModelBuilder> templates(String prefix, boolean whole) {
        Map<TrimPart, BlockModelBuilder> out = new EnumMap<>(TrimPart.class);
        for (TrimPart part : TrimPart.values()) {
            BlockModelBuilder builder = models().getBuilder(prefix + part.getSerializedName());
            builder.texture("particle", "#" + TrimGeometry.EDGE);
            List<Piece> pieces = TrimGeometry.quarter(part.column, part.row).stream()
                    .map(box -> new Piece(box, TrimGeometry.FACE)).toList();
            out.put(part, GeometryModelWriter.addPieces(builder, pieces, true, style(whole, part.column, part.row, true)));
        }
        return out;
    }
}
