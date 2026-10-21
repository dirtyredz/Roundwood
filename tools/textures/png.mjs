// Minimal deterministic PNG encoder (8-bit RGB or RGBA, filter none, zlib level 9). No dependencies.
import { deflateSync } from 'node:zlib';

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

/**
 * Encodes `pixels` (row-major, top row first; each [r, g, b] or [r, g, b, a]) as a PNG of
 * `width` x `height`. RGB unless `alpha` is set. Identical input gives identical bytes.
 */
export function png(pixels, width, height = width, { alpha = false } = {}) {
  if (pixels.length !== width * height) throw new Error(`expected ${width * height} pixels, got ${pixels.length}`);
  const channels = alpha ? 4 : 3;
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = alpha ? 6 : 2; // RGBA or RGB
  const stride = 1 + width * channels;
  const raw = Buffer.alloc(height * stride);
  for (let y = 0; y < height; y++) {
    const o = y * stride;
    raw[o] = 0; // filter: none
    for (let x = 0; x < width; x++) {
      const p = pixels[y * width + x];
      for (let c = 0; c < channels; c++) raw[o + 1 + x * channels + c] = c < 3 ? p[c] : (p[3] ?? 255);
    }
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}
