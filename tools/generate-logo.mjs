// node tools/generate-logo.mjs [--check | <artifact-directory>]
// A 32px master keeps every edge crisp at launcher size; larger exports use nearest neighbours.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { png } from './textures/png.mjs';
import { DOOR_WOOD, MASONRY } from './textures/palettes.mjs';
import { doorPanel } from './textures/door-panels.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const wood = doorPanel(DOOR_WOOD.oak, 101);
const stone = MASONRY.stone_brick;
const ink = [43, 34, 26];
const brass = [218, 178, 68];
const master = [];
for (let y = 0; y < 32; y++) {
  for (let x = 0; x < 32; x++) {
    const dx = x - 15.5, dy = y - 15.5;
    const r = Math.hypot(dx, dy);
    let c = [0, 0, 0, 0];
    if (r < 14.8) {
      c = ink;
      if (r < 13.8 && r >= 10.8) {
        // Twelve wedge stones with single-pixel mortar joints and a lit outer bevel.
        const sector = (Math.atan2(dy, dx) + Math.PI) * 6 / Math.PI;
        const joint = Math.abs(sector - Math.round(sector)) * r < 0.9;
        c = joint ? stone.mortar : r > 12.8 ? stone.light : dy > 3 ? stone.dark : stone.brick;
      }
      if (r < 9.9) c = wood[y * 32 + x];
      // Small round glazing above the handle, framed in dark oak.
      const glass = Math.hypot(x - 15.5, y - 11.5);
      if (glass < 4.2) c = DOOR_WOOD.dark_oak.dark;
      if (glass < 3.2) c = x < 15 ? [167, 206, 208] : [91, 145, 154];
      if (glass < 3.2 && (x === 15 || y === 11)) c = DOOR_WOOD.spruce.dark;
      // Two brass hinge straps and a prominent brass knob.
      if ((y === 19 || y === 23) && x >= 8 && x <= 12) c = ink;
      if ((y === 18 || y === 22) && x >= 8 && x <= 12 && r < 9.9) c = brass;
      if (x >= 20 && x <= 23 && y >= 18 && y <= 21) c = ink;
      if (x >= 20 && x <= 22 && y >= 18 && y <= 20) c = brass;
      if (x === 20 && y === 18) c = [255, 226, 137];
    }
    master.push([...c.slice(0, 3), c[3] ?? 255]);
  }
}

function scaled(size) {
  return Array.from({ length: size * size }, (_, i) =>
    master[Math.floor(Math.floor(i / size) * 32 / size) * 32 + Math.floor((i % size) * 32 / size)]);
}
const logo = png(scaled(256), 256, 256, { alpha: true });
const target = join(root, 'src/main/resources/roundwood-logo.png');
const option = process.argv[2];
if (process.argv.length > 3 || (option?.startsWith('--') && option !== '--check')) {
  throw new Error('Usage: node tools/generate-logo.mjs [--check | <artifact-directory>]');
}
if (option === '--check') {
  if (!readFileSync(target).equals(logo)) throw new Error('Logo is stale; run node tools/generate-logo.mjs');
  console.log('Logo matches deterministic 32px master (256x256 RGBA).');
} else {
  writeFileSync(target, logo);
  if (option) {
    mkdirSync(option, { recursive: true });
    writeFileSync(join(option, 'roundwood-logo.png'), logo);
    const width = 416, height = 288;
    const preview = Array.from({ length: width * height }, (_, i) => {
      const tone = (Math.floor((i % width) / 8) + Math.floor(Math.floor(i / width) / 8)) % 2 ? 43 : 49;
      return [tone, tone, tone];
    });
    for (const [size, left] of [[256, 16], [64, 288], [32, 368]]) {
      const pixels = scaled(size), top = (height - size) / 2;
      pixels.forEach((p, i) => {
        if (p[3]) preview[(top + Math.floor(i / size)) * width + left + i % size] = p;
      });
    }
    writeFileSync(join(option, 'roundwood-preview.png'), png(preview, width, height));
  }
  console.log('Wrote Roundwood logo' + (option ? ' and 256 / 64 / 32px preview.' : '.'));
}
