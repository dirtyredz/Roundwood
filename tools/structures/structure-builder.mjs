import { file } from './nbt.mjs';

/** Ordered palette and cell builder; preserves historical fixture byte order. */
export class StructureBuilder {
  constructor(size) { this.size = size; this.palette = []; this.blocks = []; }
  put = (pos, name, properties, nbt) => {
    const key = JSON.stringify([name, properties ?? null]);
    let state = this.palette.findIndex(p => JSON.stringify([p.name, p.properties ?? null]) === key);
    if (state < 0) { state = this.palette.length; this.palette.push({ name, properties }); }
    this.blocks.push({ pos, state, nbt });
  };
  floor() {
    for (let x = 0; x < this.size[0]; x++) for (let z = 0; z < this.size[2]; z++) this.put([x, 0, z], 'minecraft:stone');
  }
  door(x, z, name, { hinge, open }, anchorNbt) {
    for (const [dx, column] of [[0, 'left'], [1, 'right']]) for (const [dy, half] of [[0, 'lower'], [1, 'upper']]) {
      this.put([x + dx, 1 + dy, z], name, { column, facing: 'north', half, hinge, open: String(open), powered: 'false' },
        column === 'left' && half === 'lower' ? anchorNbt : undefined);
    }
  }
  frame(x, z, name, anchorNbt) {
    for (const [dx, dy, part] of [[0, 0, 'lower_left'], [0, 1, 'upper_left'], [1, 0, 'lower_right'], [1, 1, 'upper_right']]) {
      this.put([x + dx, 1 + dy, z], name, { facing: 'north', part }, part === 'lower_left' ? anchorNbt : undefined);
    }
  }
  file() { return file(this.size, this.palette, this.blocks); }
}
