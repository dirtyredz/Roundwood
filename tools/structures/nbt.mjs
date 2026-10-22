import { gzipSync } from 'node:zlib';

const DATA_VERSION = 3465; // 1.20.1

// --- a minimal NBT writer: values are { type, value } ---------------------------------------------
export const TAG = { end: 0, byte: 1, int: 3, string: 8, list: 9, compound: 10 };
const u8 = (n) => Buffer.from([n]);
const u16 = (n) => { const b = Buffer.alloc(2); b.writeUInt16BE(n); return b; };
const i32 = (n) => { const b = Buffer.alloc(4); b.writeInt32BE(n); return b; };
const str = (s) => Buffer.concat([u16(Buffer.byteLength(s)), Buffer.from(s, 'utf8')]);
export const byte = (value) => ({ type: TAG.byte, value });
export const int = (value) => ({ type: TAG.int, value });
export const string = (value) => ({ type: TAG.string, value });
export const list = (itemType, items) => ({ type: TAG.list, value: { itemType, items } });
export const compound = (entries) => ({ type: TAG.compound, value: entries });

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
export function file(size, palette, blocks) {
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
