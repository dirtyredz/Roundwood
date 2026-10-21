// Generates the empty GameTest structure templates (gzipped NBT, no dependencies).
//
//   node tools/generate-test-structures.mjs
//
// Writes src/gameTest/resources/data/roundwood/structures/{empty,strip,showcase}.nbt. Each template is only
// a size and an air palette: GameTests build their own floor. "strip" is wide enough (17 blocks) to
// always contain a chunk boundary.
import { writeFileSync, mkdirSync } from 'node:fs';
import { gzipSync } from 'node:zlib';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'src/gameTest/resources/data/roundwood/structures');
const DATA_VERSION = 3465; // 1.20.1

const TAG = { end: 0, int: 3, string: 8, list: 9, compound: 10 };
const u8 = (n) => Buffer.from([n]);
const u16 = (n) => { const b = Buffer.alloc(2); b.writeUInt16BE(n); return b; };
const i32 = (n) => { const b = Buffer.alloc(4); b.writeInt32BE(n); return b; };
const str = (s) => Buffer.concat([u16(Buffer.byteLength(s)), Buffer.from(s, 'utf8')]);
const named = (type, name, payload) => Buffer.concat([u8(type), str(name), payload]);

function structure([x, y, z]) {
  const size = Buffer.concat([u8(TAG.int), i32(3), i32(x), i32(y), i32(z)]);
  const air = Buffer.concat([named(TAG.string, 'Name', str('minecraft:air')), u8(TAG.end)]);
  const palette = Buffer.concat([u8(TAG.compound), i32(1), air]);
  const emptyList = Buffer.concat([u8(TAG.end), i32(0)]);
  const body = Buffer.concat([
    named(TAG.int, 'DataVersion', i32(DATA_VERSION)),
    named(TAG.list, 'size', size),
    named(TAG.list, 'palette', palette),
    named(TAG.list, 'blocks', emptyList),
    named(TAG.list, 'entities', emptyList),
    u8(TAG.end),
  ]);
  // mtime 0 keeps the gzip bytes reproducible.
  return gzipSync(named(TAG.compound, '', body), { level: 9 });
}

mkdirSync(outDir, { recursive: true });
for (const [name, size] of Object.entries({ empty: [9, 6, 9], strip: [17, 5, 4], showcase: [50, 10, 50] })) {
  writeFileSync(join(outDir, `${name}.nbt`), structure(size));
  console.log(`wrote ${name}.nbt ${size.join('x')}`);
}
