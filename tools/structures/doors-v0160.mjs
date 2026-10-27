import { byte, int, string, compound } from './nbt.mjs';
import { StructureBuilder } from './structure-builder.mjs';

// Independently recorded 0.16.0 state/NBT schema, before the crown/hardware geometry replacement.
// No runtime profile, registration list or current codec supplies these historical values.
export function doors0160() {
  const builder = new StructureBuilder([17, 6, 5]);
  builder.floor();
  const look = (kind, material) => compound({ Version: int(1), Kind: string(kind), Material: string(material) });
  const trims = compound({ Version: int(1), Front: compound({
    RoundwoodAppearance: look('masonry', 'minecraft:bricks'),
  }) });
  const samples = [
    { x: 1, id: 'round_door', width: 2, height: 2, hinge: 'right', depth: 'back', open: false },
    { x: 5, id: 'arched_door', width: 1, height: 2, hinge: 'left', depth: 'middle', open: false },
    { x: 8, id: 'double_arched_door', width: 3, height: 3, hinge: 'left', depth: 'front', open: false },
    { x: 12, id: 'tall_double_arched_door', width: 3, height: 4, hinge: 'right', depth: 'back', open: true },
  ];
  for (const sample of samples) {
    const nbt = { id: string('roundwood:round_door'),
      RoundwoodAppearance: look('wood', 'minecraft:spruce_planks'), DoorTrim: trims };
    if (sample.id === 'round_door') nbt.Camouflage = compound({
      Material: compound({ Name: string('minecraft:stone') }),
      Refund: compound({ id: string('minecraft:stone'), Count: byte(1) }),
    });
    if (sample.width === 3) nbt.DoorArch = compound({ Version: int(1), Product: string('TOP_2X3'),
      Thickness: string('FULL'), Depth: string('FRONT'),
      Appearance: look('masonry', 'minecraft:stone_bricks'), Casings: trims });
    for (let x = 0; x < sample.width; x++) for (let y = 0; y < sample.height; y++) {
      const properties = { facing: 'north', hinge: sample.hinge, depth: sample.depth,
        half: y === 0 ? 'lower' : 'upper', open: String(sample.open), powered: String(sample.open) };
      if (sample.width === 3) Object.assign(properties, { cell_x: String(x), cell_y: String(y) });
      else if (sample.width === 2) properties.column = x === 0 ? 'left' : 'right';
      builder.put([sample.x + x, 1 + y, 2], 'roundwood:' + sample.id, properties,
        x === 0 && y === 0 ? compound(nbt) : undefined);
    }
  }
  // The saved powered/open sample has a persistent redstone source beneath its anchor.
  builder.put([12, 0, 2], 'minecraft:redstone_block');
  return builder.file();
}
