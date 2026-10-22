// Generates the GameTest structure templates (gzipped NBT, no dependencies).
//
//   node tools/generate-test-structures.mjs
//
// Writes src/gameTest/resources/data/roundwood/structures/:
//   empty.nbt, strip.nbt, showcase.nbt   only a size and an air palette: GameTests build their own floor.
//                                        "strip" is wide enough (17 blocks) to always contain a chunk
//                                        boundary; "showcase" holds the whole showcase in any facing.
//   legacy_v040.nbt                      a Roundwood 0.4.0 build, written from the 0.4.0 ids and NBT
//                                        directly (independent of the mod's migration tables): a
//                                        camouflaged spruce door, an open red door, a fitted green
//                                        trim, a freestanding mud-brick trim and two windows, one wet.
//   legacy_v070.nbt                      a Roundwood 0.7.0 build, written from the 0.7.0 states and NBT
//                                        directly (0.8.0 migration fixture): trimmed windows with and
//                                        without their version-1 trim payload (0.7.0 stored none for
//                                        oak), an untrimmed window carrying stale trim data, two doors
//                                        (one open) with frames fitted in front, one oak and one spruce,
//                                        and a freestanding frame.
import { writeFileSync, mkdirSync } from 'node:fs';
import { gzipSync } from 'node:zlib';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'src/gameTest/resources/data/roundwood/structures');
const DATA_VERSION = 3465; // 1.20.1

// --- a minimal NBT writer: values are { type, value } ---------------------------------------------
const TAG = { end: 0, byte: 1, int: 3, string: 8, list: 9, compound: 10 };
const u8 = (n) => Buffer.from([n]);
const u16 = (n) => { const b = Buffer.alloc(2); b.writeUInt16BE(n); return b; };
const i32 = (n) => { const b = Buffer.alloc(4); b.writeInt32BE(n); return b; };
const str = (s) => Buffer.concat([u16(Buffer.byteLength(s)), Buffer.from(s, 'utf8')]);
const byte = (value) => ({ type: TAG.byte, value });
const int = (value) => ({ type: TAG.int, value });
const string = (value) => ({ type: TAG.string, value });
const list = (itemType, items) => ({ type: TAG.list, value: { itemType, items } });
const compound = (entries) => ({ type: TAG.compound, value: entries });

function payload({ type, value }) {
  switch (type) {
    case TAG.byte: return u8(value & 0xff);
    case TAG.int: return i32(value);
    case TAG.string: return str(value);
    case TAG.list: return Buffer.concat([u8(value.items.length ? value.itemType : TAG.end), i32(value.items.length),
      ...value.items.map(payload)]);
    case TAG.compound: return Buffer.concat([...Object.entries(value).map(([name, tag]) =>
      Buffer.concat([u8(tag.type), str(name), payload(tag)])), u8(TAG.end)]);
    default: throw new Error(`unknown tag ${type}`);
  }
}

/** A gzipped structure file; mtime 0 keeps the bytes reproducible. */
function file(size, palette, blocks) {
  const body = compound({
    DataVersion: int(DATA_VERSION),
    size: list(TAG.int, size.map(int)),
    palette: list(TAG.compound, palette.map(({ name, properties }) => compound({
      Name: string(name),
      ...(properties ? { Properties: compound(Object.fromEntries(Object.entries(properties).map(([k, v]) => [k, string(v)]))) } : {}),
    }))),
    blocks: list(TAG.compound, blocks.map(({ pos, state, nbt }) => compound({
      pos: list(TAG.int, pos.map(int)),
      state: int(state),
      ...(nbt ? { nbt } : {}),
    }))),
    entities: list(TAG.compound, []),
  });
  return gzipSync(Buffer.concat([u8(TAG.compound), str(''), payload(body)]), { level: 9 });
}

const empty = (size) => file(size, [{ name: 'minecraft:air' }], []);

// --- the 0.4.0 build -----------------------------------------------------------------------------
function legacyBuild() {
  const size = [13, 4, 4];
  const palette = [];
  const blocks = [];
  const stateIndex = (name, properties) => {
    const key = JSON.stringify([name, properties ?? null]);
    let index = palette.findIndex((p) => JSON.stringify([p.name, p.properties ?? null]) === key);
    if (index < 0) {
      palette.push({ name, properties });
      index = palette.length - 1;
    }
    return index;
  };
  const put = (pos, name, properties, nbt) => blocks.push({ pos, state: stateIndex(name, properties), nbt });

  // A stone floor under everything.
  for (let x = 0; x < size[0]; x++) {
    for (let z = 0; z < size[2]; z++) {
      put([x, 0, z], 'minecraft:stone');
    }
  }
  // A 0.4.0 door: four parts of one finish; the anchor (lower-left) carries the round_door entity.
  const door = (x, z, finish, { hinge, open }, anchorNbt) => {
    for (const [dx, column] of [[0, 'left'], [1, 'right']]) {
      for (const [dy, half] of [[0, 'lower'], [1, 'upper']]) {
        const properties = { column, facing: 'north', half, hinge, open: String(open), powered: 'false' };
        const nbt = column === 'left' && half === 'lower' ? anchorNbt : undefined;
        put([x + dx, 1 + dy, z], `roundwood:${finish}_round_door`, properties, nbt);
      }
    }
  };
  // A 0.4.0 trim: four quarters of one material, facing north (no block entity in 0.4.0).
  const trim = (x, z, material) => {
    for (const [dx, dy, part] of [[0, 0, 'lower_left'], [0, 1, 'upper_left'], [1, 0, 'lower_right'], [1, 1, 'upper_right']]) {
      put([x + dx, 1 + dy, z], `roundwood:${material}_round_trim`, { facing: 'north', part });
    }
  };
  const window = (x, z, finish, properties) => put([x, 1, z], `roundwood:${finish}_round_window`,
    { axis: 'x', north: 'false', south: 'false', east: 'false', west: 'false', waterlogged: 'false', ...properties });

  // Camouflaged spruce door (survival-paid polished granite), closed, hinge left; a green trim fitted in front of it.
  door(1, 1, 'spruce', { hinge: 'left', open: false }, compound({
    id: string('roundwood:round_door'),
    Camouflage: compound({
      Material: compound({ Name: string('minecraft:polished_granite') }),
      Refund: compound({ id: string('minecraft:polished_granite'), Count: byte(1) }),
    }),
  }));
  trim(1, 2, 'green');
  // Open red (painted) door, hinge right, no camouflage.
  door(4, 1, 'red', { hinge: 'right', open: true }, compound({ id: string('roundwood:round_door') }));
  // Two windows joined in their plane, the left one waterlogged.
  window(7, 1, 'birch', { east: 'true', waterlogged: 'true' });
  window(8, 1, 'blue', { west: 'true' });
  // A freestanding mud-brick trim.
  trim(10, 1, 'mud_brick');
  return file(size, palette, blocks);
}

// --- the 0.7.0 build -----------------------------------------------------------------------------
function build070() {
  const size = [13, 4, 6];
  const palette = [];
  const blocks = [];
  const stateIndex = (name, properties) => {
    const key = JSON.stringify([name, properties ?? null]);
    let index = palette.findIndex((p) => JSON.stringify([p.name, p.properties ?? null]) === key);
    if (index < 0) {
      palette.push({ name, properties });
      index = palette.length - 1;
    }
    return index;
  };
  const put = (pos, name, properties, nbt) => blocks.push({ pos, state: stateIndex(name, properties), nbt });
  const look = (kind, material) => compound({ Version: int(1), Kind: string(kind), Material: string(material) });

  for (let x = 0; x < size[0]; x++) {
    for (let z = 0; z < size[2]; z++) {
      put([x, 0, z], 'minecraft:stone');
    }
  }
  const pane = { north: 'false', south: 'false', east: 'false', west: 'false', waterlogged: 'false' };
  const windowNbt = (extra) => compound({ id: string('roundwood:round_window'), ...extra });
  // Trimmed, no payload (an oak trim in 0.7.0): oak on both faces after loading.
  put([1, 1, 1], 'roundwood:round_window', { ...pane, axis: 'x', position: 'center', trimmed: 'true' }, windowNbt({}));
  // Trimmed flush arch with a brick trim and a spruce frame: bricks on both faces.
  put([3, 1, 1], 'roundwood:round_window_arch_left', { ...pane, orientation: 'south', depth: 'negative', trimmed: 'true' },
    windowNbt({ RoundwoodAppearance: look('wood', 'minecraft:spruce_planks'),
      WindowTrim: compound({ Version: int(1), RoundwoodAppearance: look('masonry', 'minecraft:bricks') }) }));
  // Untrimmed with stale data: still untrimmed.
  put([5, 1, 1], 'roundwood:round_window', { ...pane, axis: 'z', position: 'center', trimmed: 'false' },
    windowNbt({ WindowTrim: compound({ Version: int(1), RoundwoodAppearance: look('masonry', 'minecraft:bricks') }) }));

  // Canonical doors facing north (front = south) with frames fitted in front (z + 1).
  const door = (x, z, { hinge, open }, anchorNbt) => {
    for (const [dx, column] of [[0, 'left'], [1, 'right']]) {
      for (const [dy, half] of [[0, 'lower'], [1, 'upper']]) {
        const properties = { column, facing: 'north', half, hinge, open: String(open), powered: 'false' };
        put([x + dx, 1 + dy, z], 'roundwood:round_door', properties, column === 'left' && half === 'lower' ? anchorNbt : undefined);
      }
    }
  };
  const frame = (x, z, anchorNbt) => {
    for (const [dx, dy, part] of [[0, 0, 'lower_left'], [0, 1, 'upper_left'], [1, 0, 'lower_right'], [1, 1, 'upper_right']]) {
      put([x + dx, 1 + dy, z], 'roundwood:round_trim', { facing: 'north', part }, part === 'lower_left' ? anchorNbt : undefined);
    }
  };
  door(1, 3, { hinge: 'left', open: false }, compound({ id: string('roundwood:round_door') }));
  frame(1, 4, compound({ id: string('roundwood:round_trim'), RoundwoodAppearance: look('wood', 'minecraft:spruce_planks') }));
  door(5, 3, { hinge: 'right', open: true }, compound({ id: string('roundwood:round_door') }));
  frame(5, 4, compound({ id: string('roundwood:round_trim') }));
  // A freestanding frame: stays a frame.
  frame(9, 3, compound({ id: string('roundwood:round_trim'), RoundwoodAppearance: look('masonry', 'minecraft:mud_bricks') }));
  return file(size, palette, blocks);
}

mkdirSync(outDir, { recursive: true });
for (const [name, size] of Object.entries({ empty: [9, 6, 9], strip: [17, 5, 4], showcase: [86, 10, 86] })) {
  writeFileSync(join(outDir, `${name}.nbt`), empty(size));
  console.log(`wrote ${name}.nbt ${size.join('x')}`);
}
writeFileSync(join(outDir, 'legacy_v040.nbt'), legacyBuild());
console.log('wrote legacy_v040.nbt 13x4x4');
writeFileSync(join(outDir, 'legacy_v070.nbt'), build070());
console.log('wrote legacy_v070.nbt 13x4x6');
