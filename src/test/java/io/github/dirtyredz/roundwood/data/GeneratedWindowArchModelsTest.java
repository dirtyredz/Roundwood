package io.github.dirtyredz.roundwood.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPaneGeometry;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowOrientation;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.Piece;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

/**
 * The posed window models (flush round windows and both arch pieces) against the geometry the
 * blocks collide with: for every pose and connection set, the parts the checked-in blockstate
 * applies, turned as it turns them, draw every visible face of {@link WindowPaneGeometry} and no
 * glass face buried against other glass. Plus the arch items, render types and textures.
 */
class GeneratedWindowArchModelsTest {
    private static final Path STATES = Path.of("src/generated/resources/assets/roundwood/blockstates");
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private static JsonArray multipart(String id) throws IOException {
        return JsonParser.parseString(Files.readString(STATES.resolve(id + ".json"))).getAsJsonObject().getAsJsonArray("multipart");
    }

    @Test
    void everyPosedStateDrawsItsPaneGeometry() throws IOException {
        JsonArray round = multipart("round_window");
        for (WindowPosition position : WindowPosition.values()) {
            if (position == WindowPosition.CENTER) {
                continue;
            }
            Map<String, String> values = new HashMap<>(Map.of("position", position.getSerializedName(), "axis", "x"));
            check(round, values, WindowProfile.ROUND, position.pose(Direction.Axis.X));
        }
        for (WindowProfile profile : List.of(WindowProfile.ARCH_LEFT, WindowProfile.ARCH_RIGHT)) {
            JsonArray parts = multipart(profile.id());
            for (WindowOrientation orientation : WindowOrientation.values()) {
                for (WindowDepth depth : WindowDepth.values()) {
                    Map<String, String> values = new HashMap<>(Map.of("orientation", orientation.getSerializedName(),
                            "depth", depth.getSerializedName()));
                    check(parts, values, profile, orientation.pose(depth));
                }
            }
        }
    }

    private static void check(JsonArray parts, Map<String, String> pose, WindowProfile profile, WindowPose windowPose) {
        for (int mask = 0; mask < 16; mask++) {
            Map<String, String> values = new HashMap<>(pose);
            Set<Direction> connected = EnumSet.noneOf(Direction.class);
            for (int i = 0; i < 4; i++) {
                boolean on = (mask & (1 << i)) != 0;
                values.put(SIDES.get(i).getSerializedName(), String.valueOf(on));
                if (on) {
                    connected.add(SIDES.get(i));
                }
            }
            String what = profile + " " + values;
            Cells geometry = new Cells();
            for (Piece piece : WindowPaneGeometry.pieces(profile, windowPose, connected)) {
                geometry.fill(piece.box());
            }
            Set<Long> faces = new HashSet<>();
            List<List<Long>> junctionFaces = new java.util.ArrayList<>();
            for (String applied : GeneratedBlockstatesTest.applied(parts, values)) {
                String[] split = applied.split("@");
                int turns = Integer.parseInt(split[1]) / 90;
                boolean junction = split[0].contains("_arm_");
                for (Element element : ELEMENTS.computeIfAbsent(split[0], GeneratedWindowArchModelsTest::load)) {
                    Box box = element.box().rotateY(turns);
                    assertTrue(geometry.containsAll(box), what + ": " + split[0] + " draws outside the geometry: " + box);
                    for (Direction face : element.faces()) {
                        List<Long> keys = Cells.faceKeys(box, turn(face, turns));
                        faces.addAll(keys);
                        if (junction) {
                            junctionFaces.add(keys);
                        }
                    }
                }
            }
            for (long key : geometry.exposedFaces()) {
                assertTrue(faces.contains(key), what + ": visible face not drawn " + Cells.describe(key));
            }
            // A junction face may touch other geometry in part (an arm end beside wood and recess), never wholly.
            for (List<Long> face : junctionFaces) {
                assertFalse(face.stream().allMatch(geometry::neighbourFilled),
                        what + ": buried junction face drawn at " + Cells.describe(face.get(0)));
            }
        }
    }

    @Test
    void archItemsShowTheirUprightPieceInOak() {
        for (String piece : List.of("round_window_arch_left", "round_window_arch_right")) {
            Resolved item = resolve("roundwood:item/" + piece);
            assertEquals(ModelExpectations.OAK_PLANKS, item.texture("#frame"), piece);
            assertEquals(ModelExpectations.GLASS, item.texture("#glass"), piece);
            ModelExpectations.assertDisplay(item, piece, new double[] {8, 8, 8});
            assertEquals("minecraft:translucent", renderType("roundwood:block/" + piece), piece);
            boolean left = piece.endsWith("left");
            // Seen from the front (south), the glass is a quarter circle in the bottom inner corner.
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    int combined = left ? u : u + 16;
                    double dx = combined + 0.5 - 16;
                    double dy = v + 0.5;
                    boolean glass = dx * dx + dy * dy <= 196;
                    assertEquals(glass, ResolvedModels.covered(item, u, v, "south", ModelExpectations.GLASS), piece + " glass " + u + "," + v);
                    assertEquals(!glass, ResolvedModels.covered(item, u, v, "south", ModelExpectations.OAK_PLANKS), piece + " wood " + u + "," + v);
                }
            }
        }
    }

    /** Every posed model: translucent, wood only on the core's frame variable, glass everywhere else, no empty element. */
    @Test
    void posedModelsAreTranslucentAndKeepWoodAndGlassApart() {
        for (String name : GeneratedResourcesTest.windowPaneModels()) {
            String id = "roundwood:block/" + name;
            assertEquals("minecraft:translucent", renderType(id), id);
            Resolved model = resolve(id);
            boolean core = !name.contains("_arm_");
            assertFalse(model.elements().isEmpty(), id + ": no elements");
            for (JsonObject element : elements(model)) {
                JsonObject faces = element.getAsJsonObject("faces");
                assertTrue(faces != null && !faces.entrySet().isEmpty(), id + ": an element without faces");
                Set<String> used = new HashSet<>();
                for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                    String texture = face.getValue().getAsJsonObject().get("texture").getAsString();
                    used.add(texture);
                    if (!core) {
                        assertEquals("#glass", texture, id + ": junction glass only");
                    }
                }
                assertEquals(1, used.size(), id + ": an element mixes frame and glass " + used);
            }
            if (core) {
                assertEquals(ModelExpectations.OAK_PLANKS, model.texture("#frame"), id);
                assertEquals(ModelExpectations.OAK_PLANKS, model.texture("#particle"), id);
            }
            assertEquals(ModelExpectations.GLASS, model.texture("#glass"), id);
        }
    }

    private record Element(Box box, List<Direction> faces) {
    }

    private static final Map<String, List<Element>> ELEMENTS = new HashMap<>();

    private static List<Element> load(String id) {
        List<Element> out = new java.util.ArrayList<>();
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

    /** Half-pixel occupancy of one cell, and voxel faces keyed as (cell index * 6 + direction). */
    private static final class Cells {
        private final boolean[] cells = new boolean[32 * 32 * 32];

        private static int index(int x, int y, int z) {
            return (x * 32 + y) * 32 + z;
        }

        void fill(Box box) {
            forEach(box, i -> cells[i] = true);
        }

        boolean containsAll(Box box) {
            boolean[] ok = {true};
            forEach(box, i -> ok[0] &= cells[i]);
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
            List<Long> out = new java.util.ArrayList<>();
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

        /** Faces of filled cells whose neighbour across them is empty (or outside the cell). */
        Set<Long> exposedFaces() {
            Set<Long> out = new HashSet<>();
            for (int i = 0; i < cells.length; i++) {
                if (!cells[i]) {
                    continue;
                }
                for (Direction direction : Direction.values()) {
                    long key = (long) i * 6 + direction.ordinal();
                    if (!neighbourFilled(key)) {
                        out.add(key);
                    }
                }
            }
            return out;
        }

        boolean neighbourFilled(long key) {
            int i = (int) (key / 6);
            Direction direction = Direction.values()[(int) (key % 6)];
            int x = i / (32 * 32) + direction.getStepX();
            int y = (i / 32) % 32 + direction.getStepY();
            int z = i % 32 + direction.getStepZ();
            if (x < 0 || y < 0 || z < 0 || x >= 32 || y >= 32 || z >= 32) {
                return false;
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
