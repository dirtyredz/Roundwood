// Generates Roundwood's original block textures (deterministic, no dependencies).
//
//   node tools/generate-textures.mjs
//
// Writes into src/main/resources/assets/roundwood/textures/block/:
//   panel_{green,yellow,red,blue}.png, brass.png   the five original 16x16 textures (byte-identical)
//   panel_<dye>.png                                painted window boards for the twelve newer dyes
//   window_glass.png                               visible translucent glazing for the round windows
//   door_panel_<wood|dye|neutral>.png              32x32 vertical boards, one image across a whole door
//                                                  (neutral: grey, tinted per unfamiliar wood at runtime)
//   trim_radial_<material>.png                     64x64 radial brick face for a masonry frame
//   trim_edge_<material>.png                       16x16 granular sides for a masonry frame
//   window_glass_clear.png                         0.7.0 glazing: window_glass, clearer between the accents
//   trim_radial_thin_<material>.png                0.7.0 door ring face with thin mortar
//   window_trim_round_<material>.png               0.7.0 32x32 round window trim face, thin mortar
//   window_trim_arch_<material>.png                0.7.0 64x64 arched window trim face (radial top, straight courses)
//   window_trim_face.png, window_trim_edge.png     0.7.0 window trim source markers (always replaced)
//   window_glass_<dye>.png                         0.8.0 dyed window glass, the clear glass marks drawn darker
//   universal_trim_<material>.png                  0.9.0 32x32 Universal Trim icon face (square frame, straight courses)
//   window_trim_frame_<material>.png               0.10.0 64x64 top-corner trim face (head and jambs)
//   window_trim_single_<material>.png              0.10.0 32x32 single-edge trim face (one straight run per side)
//   window_sill_<material>.png                     0.10.0 64x64 arch sill face (one course per sill)
//   door_casing_arched_<material>.png              0.11.0 64x64 arched door casing face (radial crown, straight jambs; casing in the left half)
//   archway_<material>.png                         0.12.0 64x64 archway face (radial crown, straight jamb courses)
//   keyhole_archway_<material>.png                 0.12.0 64x64 keyhole archway face (shouldered, widened jambs)
// Re-running produces identical bytes. The artwork lives in tools/textures/ (the contour-following
// masonry faces in tools/textures/masonry/); this file only orchestrates.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { png } from './textures/png.mjs';
import { legacyTextures } from './textures/legacy.mjs';
import { newWindowPanels } from './textures/paint-panels.mjs';
import { DOOR_WOOD, MASONRY, STAINED_GLASS } from './textures/palettes.mjs';
import { DOOR_PANEL_SIZE, doorPanel } from './textures/door-panels.mjs';
import { EDGE_SIZE, RADIAL_SIZE, radialEdge, radialFace } from './textures/radial-bricks.mjs';
import { GLASS_SIZE, windowGlass } from './textures/window-glass.mjs';
import { stainedWindowGlass } from './textures/stained-glass.mjs';
import { SQUARE_FACE_SIZE, squareFrameFace } from './textures/masonry/square-trims.mjs';
import { SILL_FACE_SIZE, SINGLE_FACE_SIZE, frameWindowFace, sillFace, singleEdgeFace } from './textures/masonry/window-shape-trims.mjs';
import { DOOR_CASING_FACE_SIZE, archedCasingFace } from './textures/masonry/door-casings.mjs';
import { ARCHWAY_FACE_SIZE, archwayFace } from './textures/masonry/archways.mjs';
import { ARCH_FACE_SIZE, ROUND_BRICKS, ROUND_FACE_SIZE, archWindowFace, clearWindowGlass, thinRadialFace, trimMarker }
  from './textures/window-trims.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'src/main/resources/assets/roundwood/textures/block');

const textures = {}; // name -> PNG bytes
for (const [name, { pixels, size }] of Object.entries(legacyTextures())) {
  textures[name] = png(pixels, size);
}
for (const [name, pixels] of Object.entries(newWindowPanels())) {
  textures[name] = png(pixels, 16);
}
textures.window_glass = png(windowGlass(5), GLASS_SIZE, GLASS_SIZE, { alpha: true });
Object.entries(DOOR_WOOD).forEach(([finish, palette], i) => {
  textures[`door_panel_${finish}`] = png(doorPanel(palette, 101 + i * 17), DOOR_PANEL_SIZE);
});
Object.entries(MASONRY).forEach(([material, palette], i) => {
  textures[`trim_radial_${material}`] = png(radialFace(palette, 211 + i * 19), RADIAL_SIZE);
  textures[`trim_edge_${material}`] = png(radialEdge(palette, 307 + i * 23), EDGE_SIZE);
});

// 0.7.0 additions (new names only; nothing above changes)
textures.window_glass_clear = png(clearWindowGlass(5), GLASS_SIZE, GLASS_SIZE, { alpha: true });
Object.entries(MASONRY).forEach(([material, palette], i) => {
  textures[`trim_radial_thin_${material}`] = png(thinRadialFace(palette, 211 + i * 19), RADIAL_SIZE);
  textures[`window_trim_round_${material}`] = png(thinRadialFace(palette, 401 + i * 29, ROUND_FACE_SIZE, ROUND_BRICKS), ROUND_FACE_SIZE);
  textures[`window_trim_arch_${material}`] = png(archWindowFace(palette, 211 + i * 19), ARCH_FACE_SIZE);
});
textures.window_trim_face = png(trimMarker(DOOR_WOOD.oak, 503, false), 16);
textures.window_trim_edge = png(trimMarker(DOOR_WOOD.oak, 509, true), 16);

// 0.8.0 additions (new names only; nothing above changes)
Object.entries(STAINED_GLASS).forEach(([dye, rgb], i) => {
  textures[`window_glass_${dye}`] = png(stainedWindowGlass(rgb, 601 + i * 7), GLASS_SIZE, GLASS_SIZE, { alpha: true });
});

// 0.9.0 additions (new names only; nothing above changes)
Object.entries(MASONRY).forEach(([material, palette], i) => {
  textures[`universal_trim_${material}`] = png(squareFrameFace(palette, 701 + i * 31), SQUARE_FACE_SIZE);
});

// 0.10.0 additions (new names only; nothing above changes)
Object.entries(MASONRY).forEach(([material, palette], i) => {
  textures[`window_trim_frame_${material}`] = png(frameWindowFace(palette, 211 + i * 19), ARCH_FACE_SIZE);
  textures[`window_trim_single_${material}`] = png(singleEdgeFace(palette, 801 + i * 37), SINGLE_FACE_SIZE);
  textures[`window_sill_${material}`] = png(sillFace(palette, 907 + i * 41), SILL_FACE_SIZE);
});

// 0.11.0 additions (new names only; nothing above changes)
Object.entries(MASONRY).forEach(([material, palette], i) => {
  textures[`door_casing_arched_${material}`] = png(archedCasingFace(palette, 1009 + i * 43), DOOR_CASING_FACE_SIZE);
});

// 0.12.0 additions (new names only; nothing above changes)
Object.entries(MASONRY).forEach(([material, palette], i) => {
  textures[`archway_${material}`] = png(archwayFace(palette, 1103 + i * 47, false), ARCHWAY_FACE_SIZE);
  textures[`keyhole_archway_${material}`] = png(archwayFace(palette, 1201 + i * 53, true), ARCHWAY_FACE_SIZE);
});

mkdirSync(outDir, { recursive: true });
for (const [name, bytes] of Object.entries(textures)) {
  writeFileSync(join(outDir, `${name}.png`), bytes);
}
console.log(`wrote ${Object.keys(textures).length} textures`);
