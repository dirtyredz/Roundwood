import { byte, int, string, compound } from './nbt.mjs';
import { StructureBuilder } from './structure-builder.mjs';

export function build070() {
  const size = [13, 4, 6];
  const builder = new StructureBuilder(size);
  const put = builder.put;
  const look = (kind, material) => compound({ Version: int(1), Kind: string(kind), Material: string(material) });

  builder.floor();
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
  const door = (x, z, options, nbt) => builder.door(x, z, 'roundwood:round_door', options, nbt);
  const frame = (x, z, nbt) => builder.frame(x, z, 'roundwood:round_trim', nbt);
  door(1, 3, { hinge: 'left', open: false }, compound({ id: string('roundwood:round_door') }));
  frame(1, 4, compound({ id: string('roundwood:round_trim'), RoundwoodAppearance: look('wood', 'minecraft:spruce_planks') }));
  door(5, 3, { hinge: 'right', open: true }, compound({ id: string('roundwood:round_door') }));
  frame(5, 4, compound({ id: string('roundwood:round_trim') }));
  // A freestanding frame: stays a frame.
  frame(9, 3, compound({ id: string('roundwood:round_trim'), RoundwoodAppearance: look('masonry', 'minecraft:mud_bricks') }));
  return builder.file();
}
