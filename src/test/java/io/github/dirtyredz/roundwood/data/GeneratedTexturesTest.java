package io.github.dirtyredz.roundwood.data;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated block textures: manifest, sizes, preserved files, the 0.7.0 additions, board, brick and
 * neutral-grey structure, and the clear glazing. Thin-mortar masonry is in {@link GeneratedMasonryTexturesTest}.
 */
class GeneratedTexturesTest {
    private static final Path DIR = Path.of("src/main/resources/assets/roundwood/textures/block");
    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");
    private static final List<String> DYES = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    /** Dyes whose boards and panels only appeared in 0.5.0. */
    private static final List<String> NEW_DYES = List.of("white", "orange", "magenta", "light_blue", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "brown", "black");
    private static final List<String> MASONRY = List.of("brick", "stone_brick", "deepslate_brick", "mud_brick");
    private static final double NEUTRAL_MEAN = 179.5;

    /** Every door panel: 11 woods, 16 dyes and the neutral boards used for unfamiliar woods. */
    private static List<String> doorPanels() {
        List<String> out = new ArrayList<>();
        WOODS.forEach(w -> out.add("door_panel_" + w));
        DYES.forEach(d -> out.add("door_panel_" + d));
        out.add("door_panel_neutral");
        return out;
    }

    private static BufferedImage read(String name) throws IOException {
        BufferedImage image = ImageIO.read(DIR.resolve(name + ".png").toFile());
        assertTrue(image != null, name + ".png must decode");
        return image;
    }

    private static double luminance(BufferedImage image, int x, int y) {
        int argb = image.getRGB(x, y);
        return 0.3 * ((argb >> 16) & 0xFF) + 0.59 * ((argb >> 8) & 0xFF) + 0.11 * (argb & 0xFF);
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    @Test
    void theTextureManifestIsExact() throws IOException {
        TreeSet<String> expected = new TreeSet<>(List.of("brass", "window_glass", "window_glass_clear",
                "window_trim_face", "window_trim_edge"));
        expected.addAll(doorPanels());
        DYES.forEach(d -> expected.add("panel_" + d));
        for (String masonry : MASONRY) {
            expected.add("trim_radial_" + masonry);
            expected.add("trim_edge_" + masonry);
            expected.add("trim_radial_thin_" + masonry);
            expected.add("window_trim_round_" + masonry);
            expected.add("window_trim_arch_" + masonry);
        }
        assertEquals(69, expected.size());
        TreeSet<String> actual = new TreeSet<>();
        try (Stream<Path> files = Files.list(DIR)) {
            files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png"))
                    .forEach(n -> actual.add(n.substring(0, n.length() - 4)));
        }
        assertEquals(expected, actual);
    }

    @Test
    void textureDimensionsMatchTheirRole() throws IOException {
        assertEquals(28, doorPanels().size());
        for (String panel : doorPanels()) {
            assertSize(panel, 32);
        }
        for (String dye : DYES) {
            assertSize("panel_" + dye, 16);
        }
        for (String masonry : MASONRY) {
            assertSize("trim_radial_" + masonry, 64);
            assertSize("trim_edge_" + masonry, 16);
            assertSize("trim_radial_thin_" + masonry, 64);
            assertSize("window_trim_round_" + masonry, 32);
            assertSize("window_trim_arch_" + masonry, 64);
        }
        assertSize("window_glass", 16);
        assertSize("window_glass_clear", 16);
        assertSize("window_trim_face", 16);
        assertSize("window_trim_edge", 16);
        assertSize("brass", 16);
    }

    private static void assertSize(String name, int size) throws IOException {
        BufferedImage image = read(name);
        assertEquals(size, image.getWidth(), name + " width");
        assertEquals(size, image.getHeight(), name + " height");
    }

    @Test
    void the29PreservedTexturesAreByteForByteUnchanged() throws Exception {
        Map<String, String> hashes = Map.ofEntries(
                entry("brass", "3ec5bfd9eeb11e6a5ad2f0a1be62ab873cee072d662b12e4e2f2b9185fa3bc16"),
                entry("door_panel_acacia", "4f2d9649d123d0a27eb80d3cd3849c70ad340766d96e4761f428e01ecd874e02"),
                entry("door_panel_bamboo", "a97487691d64c02659bc5d69cf73d333f91342e9861f2578a2e3edf05fc18055"),
                entry("door_panel_birch", "3d064d3a9cfdb6c998cb8af80de57283c57fbbb7df747588065bf59cde4cdbd0"),
                entry("door_panel_blue", "6fa5cda337fdfd7e0234e0ae16be9c5032949b51ca1543a6d499f010a1014358"),
                entry("door_panel_cherry", "e38496398244669ed70406afdc556a4569cc075486bf2227de6260bfdf54fbcc"),
                entry("door_panel_crimson", "ae3e016d669fdaf8f6ccbdd65422376308429d7991444185bbda81079d29a228"),
                entry("door_panel_dark_oak", "e674731c59d00da1ba046009055aa62d2fba9f3d27c3d03010f74efa0cf4e088"),
                entry("door_panel_green", "615b0ab30b83c47f0ab24e07afdd4bd6ced877b87cd29b365444cb31ec257ec2"),
                entry("door_panel_jungle", "06440b80142c1a188775998c01faedf6c65a32a3789cdc8ce748cb2bd8a34c3a"),
                entry("door_panel_mangrove", "523bd7ecf99a58452574278a42f8b8bdf02bcace2541661d5dc95d976746d0c5"),
                entry("door_panel_oak", "9556ce700c3fcac1f819afbec5fc820361c0b9bcacc50211a6d1b18fe23fd9f2"),
                entry("door_panel_red", "690938e6ae5d3ec1b7b23aceb2cd2226f931315f142ce6435282c1ff55eb84db"),
                entry("door_panel_spruce", "c79ef30fdfe654d8faedba35c1675c4a96d78c08ff9157b212cdd78e99718bf2"),
                entry("door_panel_warped", "764933826f9303aa1fe3802a6e9a26d2d186dc73035d95311b3843b73ade9230"),
                entry("door_panel_yellow", "93423cc31d691acc184e7f734d69a81631fb9a6435e3d2e0aca261624be58ddd"),
                entry("panel_blue", "20bcd9cc9889e7c38d484a5354c646333ef1600088fb270d3a84d14af395174f"),
                entry("panel_green", "e38b41670723c40870421b1e40070a268c1eddd1ce05bac0151ecd826863ab92"),
                entry("panel_red", "b0f90c21cc399b804a5565d869f4bad20fa733a5c0607df7ca1452779ec1d496"),
                entry("panel_yellow", "9a767211b659a3147910c9d6eb2c1e9d71e792f70f73571c291140e35a8be249"),
                entry("trim_edge_brick", "66cfa72bef4f95b6f869adbda449b087b096a04da19f406cd2906faec33eec3f"),
                entry("trim_edge_deepslate_brick", "3e54d616a762e26f24d14110534439fe5e3ea240a908b222de7b42bf4b447e27"),
                entry("trim_edge_mud_brick", "d07ac8e7f944cda42d9bb38da6dfd7c305b0257b6a5681060636dc63fe715d9d"),
                entry("trim_edge_stone_brick", "8759c22f68cf2347e17ff771ff76a57df3e1ff8d1e864df9180626d98f9739c7"),
                entry("trim_radial_brick", "c0cdec65a33671ec542abbb77df224251f93a0e23d7c3917641f2cf11f29183f"),
                entry("trim_radial_deepslate_brick", "a1f1033a8d78bc2cd1d7ceced47a3bbe810f70c444c816547353a4c76d407326"),
                entry("trim_radial_mud_brick", "c30bdc6ddd2db9338da77ca3ae364a0c73c85f989f81a1c6327c644a9c57b52a"),
                entry("trim_radial_stone_brick", "afc402105feffdd9ac9167afdf49fc587c687649384a11bde4ca5272896afbf7"),
                entry("window_glass", "44d3d4526d1551bf4e26be13c35d350986d9a39f430a8f93cd54ace93a752236"));
        assertEquals(29, hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(DIR.resolve(entry.getKey() + ".png")));
            assertEquals(entry.getValue(), HexFormat.of().formatHex(digest), entry.getKey() + ".png changed");
        }
        // The 25 new files are exactly the 12 new dyes' boards and panels plus the neutral boards.
        assertEquals(25, NEW_DYES.size() * 2 + 1);
    }

    /**
     * The 15 files added in 0.7.0 are new artwork, so these pins are new: they guard against
     * accidental regeneration drift from here on, not against an earlier release.
     */
    @Test
    void the15TexturesAddedIn070AreTheirPinnedArtwork() throws Exception {
        Map<String, String> hashes = Map.ofEntries(
                entry("window_glass_clear", "6ba76d3c9f2888bdfb6013b60f34ced302c246c5a44314aadbf612412f7a7249"),
                entry("trim_radial_thin_brick", "922288bd0fdb9e109428746ebc4e08d7fa4f5d7048a3b62f0d8a220c7a06c7c9"),
                entry("trim_radial_thin_deepslate_brick", "ac2a8fe29c366494e1bb767eaa3ac671008c686138553812a93ad68628238882"),
                entry("trim_radial_thin_mud_brick", "c3a49b4f8bc8d5ce795c81eed8849576cfd19d07767833570a1b0c89cfdfb2ff"),
                entry("trim_radial_thin_stone_brick", "17b03f5b49f35745805d68b040f053821583326843da720fd3bdc97bea242705"),
                entry("window_trim_round_brick", "bfcfe9ebac334a163584271aa820e6f67ba009a973ef04a3b9364a3e2b6bdad3"),
                entry("window_trim_round_deepslate_brick", "e6ae600330285c1ff1beb0a3ec05ff538130b8a311da4f8d73c0f7eb2f998b59"),
                entry("window_trim_round_mud_brick", "fd0bea0d13cf290d7636b6d10fae7124194daff22a622caebd16793342e3170a"),
                entry("window_trim_round_stone_brick", "ca99280dd8f9b4fe3a330bf860fc6cfc862a3309d5d7f90fa33a8ab37481ae17"),
                entry("window_trim_arch_brick", "20cde54d212869476172599698309b5cdc70e57c8076760914dc098d666fe570"),
                entry("window_trim_arch_deepslate_brick", "925f96f9c078b4e9b199e10f530fe26fef7c4d2426a005e85b19f5e01aaa5a85"),
                entry("window_trim_arch_mud_brick", "b4d97c6e536bfa450a53a093bc443dde176787dc1e887967f2274e68e2724966"),
                entry("window_trim_arch_stone_brick", "271e64363ea622f8492a1669b122403a75e5be2c7f96a8b5243d9ca7ec9628c2"),
                entry("window_trim_face", "557ffd5b8d9e741ee20842289d7b63fdcd2a56ba667e885f63a06bdd3b9d4633"),
                entry("window_trim_edge", "ca68630bdc82d76ec9cd94ac562618eb0a36865327ae29f124f378face8379d6"));
        assertEquals(15, hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(DIR.resolve(entry.getKey() + ".png")));
            assertEquals(entry.getValue(), HexFormat.of().formatHex(digest), entry.getKey() + ".png changed");
        }
    }

    @Test
    void doorBoardsRunTheFullHeightWithoutRestartingOrCourses() throws IOException {
        for (String name : doorPanels()) {
            BufferedImage image = read(name);
            int seams = 0;
            for (int x = 0; x < 32; x += 4) {
                seams++;
                for (int y = 0; y < 32; y++) {
                    double seam = luminance(image, x, y);
                    assertTrue(seam < luminance(image, x + 2, y) && seam < luminance(image, x + 3, y),
                            name + ": seam column " + x + " not darker at row " + y + " (rows 15/16 mean a restart)");
                }
            }
            assertEquals(8, seams, name + ": board count");
            int columns = 0;
            for (int x = 0; x < 32; x++) {
                if (x % 4 != 0) {
                    columns++;
                }
            }
            int joints = 0;
            for (int y = 1; y < 31; y++) {
                for (int x = 0; x < 32; x++) {
                    if (x % 4 != 0 && luminance(image, x, y) < luminance(image, x, y - 1)
                            && luminance(image, x, y) < luminance(image, x, y + 1)) {
                        joints++;
                    }
                }
                // A real joint line darkens nearly every column; grain noise reaches about half on a few rows.
                assertTrue(joints <= columns * 3 / 4, name + ": row " + y + " looks like a horizontal joint line");
                joints = 0;
            }
        }
    }

    /** The neutral boards are tinted at runtime, so they must be pure grey with the documented mean. */
    @Test
    void neutralBoardsArePureGreyWithTheDocumentedMean() throws IOException {
        BufferedImage image = read("door_panel_neutral");
        double sum = 0;
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                assertTrue(r == g && g == b, "texel " + x + "," + y + " is not grey: " + r + "," + g + "," + b);
                sum += r;
            }
        }
        double mean = sum / (32 * 32);
        assertEquals(NEUTRAL_MEAN, mean, 1.0, "mean grey level must match DonorColors.NEUTRAL_MEAN");
    }

    @Test
    void paintedDoorPanelsAreOpaqueAndLegible() throws IOException {
        List<String> painted = new ArrayList<>(doorPanels());
        painted.remove("door_panel_neutral");
        assertEquals(27, painted.size());
        for (String name : painted) {
            BufferedImage image = read(name);
            double sum = 0;
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    assertEquals(255, alpha(image, x, y), name + ": translucent texel at " + x + "," + y);
                    sum += luminance(image, x, y);
                }
            }
            double mean = sum / (32 * 32);
            assertTrue(mean > 25 && mean < 235, name + ": average luminance " + mean + " is not legible");
        }
    }

    private static int[] sample(double radius, double degrees) {
        double theta = Math.toRadians(degrees);
        long x = Math.round(32 + radius * Math.sin(theta) - 0.5);
        long y = Math.round(32 - radius * Math.cos(theta) - 0.5);
        return new int[] {(int) x, (int) y};
    }

    @Test
    void radialFacesHaveMortarJointsBetweenEveryBrickAndSolidBricksOnTheMidlines() throws IOException {
        for (String masonry : MASONRY) {
            String name = "trim_radial_" + masonry;
            BufferedImage image = read(name);
            double jointSum = 0;
            double brickSum = 0;
            int direction = 0;
            for (int k = 0; k < 24; k++) {
                int[] joint = sample(30, (k + 0.5) * 15);
                int[] centre = sample(30, k * 15.0);
                double jointLum = luminance(image, joint[0], joint[1]);
                double centreLum = luminance(image, centre[0], centre[1]);
                double diff = jointLum - centreLum;
                assertTrue(Math.abs(diff) > 12, name + ": joint " + k + " indistinct from brick centre (" + diff + ")");
                if (direction == 0) {
                    direction = diff > 0 ? 1 : -1;
                }
                assertTrue(diff * direction > 0, name + ": joint " + k + " differs in the opposite direction");
                jointSum += jointLum;
                brickSum += centreLum;
            }
            double mortar = jointSum / 24;
            double brick = brickSum / 24;
            // The crown brick straddles the vertical midline, the quadrant boundaries are brick centres.
            for (int[] texel : new int[][] {{31, 2}, {32, 2}, {61, 31}, {61, 32}, {31, 61}, {32, 61}, {2, 31}, {2, 32}}) {
                double lum = luminance(image, texel[0], texel[1]);
                assertTrue(Math.abs(lum - brick) < Math.abs(lum - mortar),
                        name + ": texel " + texel[0] + "," + texel[1] + " reads as mortar, expected brick");
            }
        }
    }

    /** Texel (x, y) lies on one of the two diagonal glints, defined here by x + y and x independently of the generator. */
    private static boolean isAccent(int x, int y) {
        int d = x + y;
        return (d >= 6 && d <= 7 && x >= 2 && x <= 6) || (d >= 19 && d <= 21 && x >= 8 && x <= 13);
    }

    private static int[] rgba(BufferedImage image, int x, int y) {
        int argb = image.getRGB(x, y);
        return new int[] {(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24};
    }

    @Test
    void clearGlassSharesTheColoursOfWindowGlassAndOnlyThinsTheBody() throws IOException {
        BufferedImage glass = read("window_glass");
        BufferedImage clear = read("window_glass_clear");
        int accents = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int[] old = rgba(glass, x, y);
                int[] now = rgba(clear, x, y);
                assertEquals(old[0], now[0], "red at " + x + "," + y);
                assertEquals(old[1], now[1], "green at " + x + "," + y);
                assertEquals(old[2], now[2], "blue at " + x + "," + y);
                if (isAccent(x, y)) {
                    accents++;
                    assertArrayEquals(new int[] {246, 252, 255, 176}, old, "window_glass accent at " + x + "," + y);
                    assertArrayEquals(new int[] {246, 252, 255, 176}, now, "window_glass_clear accent at " + x + "," + y);
                } else {
                    assertTrue(old[3] >= 92 && old[3] <= 104, "window_glass alpha " + old[3] + " at " + x + "," + y);
                    assertTrue(now[3] >= 24 && now[3] <= 36, "window_glass_clear alpha " + now[3] + " at " + x + "," + y);
                }
            }
        }
        assertTrue(accents > 0, "the glints must exist");
    }
}
