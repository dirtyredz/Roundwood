package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.dirtyredz.roundwood.data.ResolvedModels.Resolved;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.elements;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.renderType;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.resolve;
import static io.github.dirtyredz.roundwood.data.ResolvedModels.vec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every window model against the geometry the blocks collide with and target (0.8.0: all ten designs,
 * no trimmed variants: trims are block-entity parts). For every pose and connection set, the parts the checked-in
 * blockstate applies, turned as it turns them, must draw exactly the visible surfaces of
 * {@link WindowPaneGeometry}:
 * <ul>
 *     <li>every face of solid material that meets air is drawn, exactly once;</li>
 *     <li>no glass face is drawn against anything (the 0.6.1 horizontal seams were glass faces
 *     between stacked glass boxes, and arm ends inside the panel's glass);</li>
 *     <li>no opaque face is drawn against opaque material;</li>
 *     <li>a wood or beam face against glass is drawn (the reveal seen through the glazing).</li>
 * </ul>
 * Plus render types, textures, and the item models.
 */
class GeneratedWindowPaneModelsTest {
    private static final Path STATES = Path.of("src/generated/resources/assets/roundwood/blockstates");
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final byte AIR = 0;
    private static final byte SOLID = 1;
    private static final byte GLASS = 2;

    private static JsonArray multipart(String id) throws IOException {
        return JsonParser.parseString(Files.readString(STATES.resolve(id + ".json"))).getAsJsonObject().getAsJsonArray("multipart");
    }

    @Test
    void everyStateDrawsExactlyItsVisibleSurfaces() throws IOException {
        for (WindowDesign design : WindowDesign.all()) {
            JsonArray parts = multipart(design.id());
            if (!design.profile().oriented()) {
                for (WindowPosition position : WindowPosition.values()) {
                    for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
                        if (position != WindowPosition.CENTER && axis == Direction.Axis.Z) {
                            continue;
                        }
                        check(parts, Map.of("position", position.getSerializedName(), "axis", axis.getSerializedName()),
                                WindowDesign.plain(design.profile()), position.pose(axis));
                    }
                }
                continue;
            }
            for (WindowOrientation orientation : WindowOrientation.values()) {
                for (WindowDepth depth : WindowDepth.values()) {
                    check(parts, Map.of("orientation", orientation.getSerializedName(), "depth", depth.getSerializedName()),
                            WindowDesign.plain(design.profile()), orientation.pose(depth));
                }
            }
        }
    }

    private static void check(JsonArray parts, Map<String, String> pose, WindowDesign design, WindowPose windowPose) {
        // No connection selects a model (0.13.1: a window grows no connecting glass): all sixteen sets draw the one
        // model, whose surfaces are checked once, against the geometry.
        Set<String> first = null;
        for (int mask = 0; mask < 16; mask++) {
            Map<String, String> values = new HashMap<>(pose);
            values.putAll(GeneratedBlockstatesTest.flagValues("false,false"));
            Set<Direction> connected = EnumSet.noneOf(Direction.class);
            for (int i = 0; i < 4; i++) {
                boolean on = (mask & (1 << i)) != 0;
                values.put(SIDES.get(i).getSerializedName(), String.valueOf(on));
                if (on) {
                    connected.add(SIDES.get(i));
                }
            }
            String what = design.id() + " " + values;
            Set<String> applied = GeneratedBlockstatesTest.applied(parts, values);
            assertEquals(1, applied.size(), what + ": one complete model per state, got " + applied);
            // The trimmed and face_reversed properties never select a model (0.8.0: trims are block-entity parts).
            for (String flags : GeneratedBlockstatesTest.FLAG_STATES) {
                Map<String, String> flagged = new HashMap<>(values);
                flagged.putAll(GeneratedBlockstatesTest.flagValues(flags));
                if (!applied.equals(GeneratedBlockstatesTest.applied(parts, flagged))) {
                    fail(what + ": flags " + flags + " change the applied models");
                }
            }
            if (first != null) {
                assertEquals(first, applied, what + ": a connection changed the model");
                continue;
            }
            first = applied;
            Cells geometry = new Cells();
            for (Piece piece : WindowPaneGeometry.panel(design, windowPose)) {
                geometry.fill(piece.box(), piece.texture().equals(WindowPaneGeometry.GLASS) ? GLASS : SOLID);
            }
            Set<Long> drawn = new HashSet<>();
            for (String part : applied) {
                String[] split = part.split("@");
                int turns = Integer.parseInt(split[1]) / 90;
                for (Element element : ELEMENTS.computeIfAbsent(split[0], GeneratedWindowPaneModelsTest::load)) {
                    Box box = element.box().rotateY(turns);
                    if (!geometry.containsAll(box)) {
                        fail(what + ": " + split[0] + " draws outside the geometry: " + box);
                    }
                    for (Direction face : element.faces()) {
                        for (long key : Cells.faceKeys(box, turn(face, turns))) {
                            if (!drawn.add(key)) {
                                fail(what + ": face drawn twice " + Cells.describe(key));
                            }
                            byte mine = geometry.at(key);
                            byte next = geometry.neighbour(key);
                            if (!(next == AIR || (mine == SOLID && next == GLASS))) {
                                fail(what + ": hidden face drawn (" + mine + " against " + next + ") " + Cells.describe(key));
                            }
                        }
                    }
                }
            }
            for (long key : geometry.visibleFaces()) {
                if (!drawn.contains(key)) {
                    fail(what + ": visible face not drawn " + Cells.describe(key));
                }
            }
        }
    }

    /** Every window model: translucent, one texture variable per element, the right bindings, no empty element. */
    @Test
    void windowModelsAreTranslucentAndBindTheirMaterials() throws IOException {
        int checked = 0;
        for (String id : ResolvedModels.allModelIds()) {
            if (!id.startsWith("roundwood:block/round_window")) {
                continue;
            }
            assertEquals("minecraft:translucent", renderType(id), id);
            Resolved model = resolve(id);
            assertFalse(model.elements().isEmpty(), id + ": no elements");
            for (JsonObject element : elements(model)) {
                JsonObject faces = element.getAsJsonObject("faces");
                assertEquals(1, faces.size(), id + ": an element draws one surface");
                for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                    String texture = face.getValue().getAsJsonObject().get("texture").getAsString();
                    Set<String> allowed = Set.of("#frame", "#glass");
                    assertTrue(allowed.contains(texture), id + ": unexpected texture " + texture);
                }
            }
            assertEquals(ModelExpectations.OAK_PLANKS, model.texture("#frame"), id);
            assertEquals(ModelExpectations.OAK_PLANKS, model.texture("#particle"), id);
            assertEquals(ModelExpectations.GLASS, model.texture("#glass"), id);
            checked++;
        }
        assertEquals(112, checked, "window block models");
    }

    /**
     * Each window item is its piece upright and centred, in oak: glazing exactly where the design is
     * glazed, wood (frame and beams) everywhere else, sampled at every half-pixel of the front face.
     */
    @Test
    void windowItemsShowTheirUprightPieceInOak() {
        for (WindowDesign design : WindowDesign.all()) {
            Resolved item = resolve("roundwood:item/" + design.id());
            assertEquals(ModelExpectations.OAK_PLANKS, item.texture("#frame"), design.id());
            assertEquals(ModelExpectations.GLASS, item.texture("#glass"), design.id());
            ModelExpectations.assertDisplay(item, design.id(), new double[] {8, 8, 8});
            for (int hu = 0; hu < 32; hu++) {
                for (int hv = 0; hv < 32; hv++) {
                    boolean glass = expectedGlass(WindowDesign.plain(design.profile()), hu, hv);
                    String front = frontTexture(item, (hu + 0.5) / 2, (hv + 0.5) / 2);
                    assertEquals(glass ? ModelExpectations.GLASS : ModelExpectations.OAK_PLANKS, front,
                            design.id() + " at half-pixel " + hu + "," + hv);
                }
            }
        }
    }

    /** The expected glazing, written out independently of the mod's geometry classes. */
    static boolean expectedGlass(WindowDesign design, int hu, int hv) {
        int u = hu / 2;
        int v = hv / 2;
        double cu = u + 0.5;
        double cv = v + 0.5;
        boolean opening = switch (design.profile()) {
            case ROUND -> (cu - 8) * (cu - 8) + (cv - 8) * (cv - 8) <= 36;
            case ARCH_LEFT -> (cu - 16) * (cu - 16) + cv * cv <= 196;
            case ARCH_RIGHT -> cu * cu + cv * cv <= 196;
            case BOTTOM_LEFT -> u >= 2 && v >= 2;
            case BOTTOM_RIGHT -> u < 14 && v >= 2;
            case ARCH_BOTTOM_LEFT -> (cu - 16) * (cu - 16) + (16 - cv) * (16 - cv) <= 196;
            case ARCH_BOTTOM_RIGHT -> cu * cu + (16 - cv) * (16 - cv) <= 196;
            case TOP_LEFT -> u >= 2 && v < 14;
            case TOP_RIGHT -> u < 14 && v < 14;
            case EDGE_TOP -> v < 14;
            case EDGE_LEFT -> u >= 2;
            case EDGE_BOTTOM -> v >= 2;
            case EDGE_RIGHT -> u < 14;
        };
        boolean beam = design.pattern().name().equals("CROSS") && (hu == 15 || hu == 16 || hv == 15 || hv == 16);
        return opening && !beam;
    }

    /** The texture of the frontmost south-facing face over block point (x, y). */
    private static String frontTexture(Resolved model, double x, double y) {
        String best = null;
        double z = -1;
        for (JsonObject element : elements(model)) {
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            JsonObject south = element.getAsJsonObject("faces").getAsJsonObject("south");
            if (south == null || x < from[0] || x > to[0] || y < from[1] || y > to[1] || to[2] <= z) {
                continue;
            }
            z = to[2];
            best = model.texture(south.get("texture").getAsString());
        }
        return best;
    }

    private record Element(Box box, List<Direction> faces) {
    }

    private static final Map<String, List<Element>> ELEMENTS = new HashMap<>();

    private static List<Element> load(String id) {
        List<Element> out = new ArrayList<>();
        for (JsonObject element : elements(resolve(id))) {
            double[] from = vec(element, "from");
            double[] to = vec(element, "to");
            out.add(new Element(new Box(from[0], from[1], from[2], to[0], to[1], to[2]),
                    element.getAsJsonObject("faces").keySet().stream().map(Direction::byName).toList()));
        }
        return out;
    }

    private static Direction turn(Direction direction, int turns) {
        Direction out = direction;
        for (int i = 0; i < turns; i++) {
            out = out.getAxis().isHorizontal() ? out.getClockWise() : out;
        }
        return out;
    }

    /** Half-pixel material of one cell (later fills win), and voxel faces keyed as (cell index * 6 + direction). */
    private static final class Cells {
        private final byte[] cells = new byte[32 * 32 * 32];

        private static int index(int x, int y, int z) {
            return (x * 32 + y) * 32 + z;
        }

        void fill(Box box, byte material) {
            forEach(box, i -> cells[i] = material);
        }

        boolean containsAll(Box box) {
            boolean[] ok = {true};
            forEach(box, i -> ok[0] &= cells[i] != AIR);
            return ok[0];
        }

        private static void forEach(Box box, java.util.function.IntConsumer action) {
            for (int x = (int) Math.round(box.minX() * 2); x < (int) Math.round(box.maxX() * 2); x++) {
                for (int y = (int) Math.round(box.minY() * 2); y < (int) Math.round(box.maxY() * 2); y++) {
                    for (int z = (int) Math.round(box.minZ() * 2); z < (int) Math.round(box.maxZ() * 2); z++) {
                        action.accept(index(x, y, z));
                    }
                }
            }
        }

        /** The faces of a box's {@code direction} side, as the voxel cells just inside it. */
        static List<Long> faceKeys(Box box, Direction direction) {
            List<Long> out = new ArrayList<>();
            forEach(box, i -> {
                int x = i / (32 * 32);
                int y = (i / 32) % 32;
                int z = i % 32;
                boolean edge = switch (direction) {
                    case DOWN -> y == (int) Math.round(box.minY() * 2);
                    case UP -> y == (int) Math.round(box.maxY() * 2) - 1;
                    case NORTH -> z == (int) Math.round(box.minZ() * 2);
                    case SOUTH -> z == (int) Math.round(box.maxZ() * 2) - 1;
                    case WEST -> x == (int) Math.round(box.minX() * 2);
                    case EAST -> x == (int) Math.round(box.maxX() * 2) - 1;
                };
                if (edge) {
                    out.add((long) i * 6 + direction.ordinal());
                }
            });
            return out;
        }

        /** Faces that must be drawn: any material against air, and solid material against glass. */
        Set<Long> visibleFaces() {
            Set<Long> out = new HashSet<>();
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] == AIR) {
                    continue;
                }
                for (Direction direction : Direction.values()) {
                    long key = (long) i * 6 + direction.ordinal();
                    byte next = neighbour(key);
                    if (next == AIR || (cells[i] == SOLID && next == GLASS)) {
                        out.add(key);
                    }
                }
            }
            return out;
        }

        byte at(long key) {
            return cells[(int) (key / 6)];
        }

        byte neighbour(long key) {
            int i = (int) (key / 6);
            Direction direction = Direction.values()[(int) (key % 6)];
            int x = i / (32 * 32) + direction.getStepX();
            int y = (i / 32) % 32 + direction.getStepY();
            int z = i % 32 + direction.getStepZ();
            if (x < 0 || y < 0 || z < 0 || x >= 32 || y >= 32 || z >= 32) {
                return AIR;
            }
            return cells[index(x, y, z)];
        }

        static String describe(long key) {
            int i = (int) (key / 6);
            return "cell (" + (i / (32 * 32)) / 2.0 + ", " + ((i / 32) % 32) / 2.0 + ", " + (i % 32) / 2.0 + ") "
                    + Direction.values()[(int) (key % 6)];
        }
    }
}
