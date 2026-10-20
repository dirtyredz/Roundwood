// Generates Roundwood's five original 16x16 block textures (deterministic, no dependencies).
//
//   node tools/generate-textures.mjs
//
// Writes src/main/resources/assets/roundwood/textures/block/{panel_green,panel_yellow,panel_red,
// panel_blue,brass}.png. Painted panels are four horizontal boards with grain and staggered butt
// joints; brass is a warm metal tile for knobs and hinge straps. Re-running produces identical bytes.
import { writeFileSync, mkdirSync } from 'node:fs';
import { deflateSync } from 'node:zlib';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'src/main/resources/assets/roundwood/textures/block');
const SIZE = 16;

// Small deterministic PRNG (mulberry32) so the art is reproducible.
function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)));
const shade = ([r, g, b], f) => [clamp(r * f), clamp(g * f), clamp(b * f)];

function paintedPlanks(base, seed) {
  const rand = rng(seed);
  const px = [];
  const joints = [5, 11, 2, 9]; // butt joint column per board
  for (let y = 0; y < SIZE; y++) {
    const board = Math.floor(y / 4);
    const row = y % 4;
    const boardTone = 0.94 + rand() * 0.1;
    for (let x = 0; x < SIZE; x++) {
      let f = boardTone;
      // Grain: soft streaks that run along the board.
      f += Math.sin((x + board * 3) * 0.9 + row * 1.7) * 0.025 + (rand() - 0.5) * 0.05;
      if (row === 0) f *= 1.06; // lit top edge of each board
      if (row === 3) f *= 0.72; // shadowed seam under each board
      if (x === joints[board]) f *= 0.78; // end joint
      px.push(shade(base, f));
    }
  }
  return px;
}

function brass() {
  const rand = rng(77);
  const base = [196, 154, 62];
  const px = [];
  for (let y = 0; y < SIZE; y++) {
    for (let x = 0; x < SIZE; x++) {
      // Diagonal sheen with a little tarnish noise.
      const sheen = 0.85 + 0.3 * Math.cos(((x + y) / SIZE) * Math.PI) ** 2;
      const f = sheen + (rand() - 0.5) * 0.08;
      px.push(shade(base, (x + y) % 11 === 0 ? f * 1.12 : f));
    }
  }
  return px;
}

// --- minimal PNG writer (RGB, 8-bit) ---
const CRC_TABLE = Array.from({ length: 256 }, (_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c >>> 0;
});
function crc32(buf) {
  let c = 0xffffffff;
  for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}
function png(pixels) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(SIZE, 0);
  ihdr.writeUInt32BE(SIZE, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 2; // RGB
  const raw = Buffer.alloc(SIZE * (1 + SIZE * 3));
  for (let y = 0; y < SIZE; y++) {
    const o = y * (1 + SIZE * 3);
    raw[o] = 0; // filter: none
    for (let x = 0; x < SIZE; x++) {
      const [r, g, b] = pixels[y * SIZE + x];
      raw[o + 1 + x * 3] = r;
      raw[o + 2 + x * 3] = g;
      raw[o + 3 + x * 3] = b;
    }
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

const textures = {
  panel_green: paintedPlanks([74, 112, 58], 11),
  panel_yellow: paintedPlanks([212, 166, 52], 23),
  panel_red: paintedPlanks([150, 46, 40], 37),
  panel_blue: paintedPlanks([50, 86, 138], 41),
  brass: brass(),
};

mkdirSync(outDir, { recursive: true });
for (const [name, pixels] of Object.entries(textures)) {
  writeFileSync(join(outDir, `${name}.png`), png(pixels));
  console.log(`wrote ${name}.png`);
}
