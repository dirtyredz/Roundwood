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
// Re-running produces identical bytes. The artwork lives in tools/textures/; this file only orchestrates.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { png } from './textures/png.mjs';
import { legacyTextures } from './textures/legacy.mjs';
import { newWindowPanels } from './textures/paint-panels.mjs';
import { DOOR_WOOD, MASONRY } from './textures/palettes.mjs';
import { DOOR_PANEL_SIZE, doorPanel } from './textures/door-panels.mjs';
import { EDGE_SIZE, RADIAL_SIZE, radialEdge, radialFace } from './textures/radial-bricks.mjs';
import { GLASS_SIZE, windowGlass } from './textures/window-glass.mjs';

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

mkdirSync(outDir, { recursive: true });
for (const [name, bytes] of Object.entries(textures)) {
  writeFileSync(join(outDir, `${name}.png`), bytes);
}
console.log(`wrote ${Object.keys(textures).length} textures`);
