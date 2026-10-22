import { byte, int, string, compound } from './nbt.mjs';
import { StructureBuilder } from './structure-builder.mjs';

export function legacyBuild() {
  const size = [13, 4, 4];
  const builder = new StructureBuilder(size);
  const put = builder.put;

  // A stone floor under everything.
  builder.floor();
  // A 0.4.0 door: four parts of one finish; the anchor (lower-left) carries the round_door entity.
  const door = (x, z, finish, options, nbt) => builder.door(x, z, `roundwood:${finish}_round_door`, options, nbt);
  // A 0.4.0 trim: four quarters of one material, facing north (no block entity in 0.4.0).
  const trim = (x, z, material) => builder.frame(x, z, `roundwood:${material}_round_trim`);
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
  return builder.file();
}
