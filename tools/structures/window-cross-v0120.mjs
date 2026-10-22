import { int, string, compound } from './nbt.mjs';
import { StructureBuilder } from './structure-builder.mjs';

// Literal saved IDs, independent of the registration code; every crossed product in 0.12.0.
const ids = ['round_window_cross', 'round_window_arch_left_cross', 'round_window_arch_right_cross',
  'round_window_bottom_left_cross', 'round_window_bottom_right_cross', 'round_window_arch_bottom_left_cross',
  'round_window_arch_bottom_right_cross', 'round_window_top_left_cross', 'round_window_top_right_cross',
  'round_window_edge_top_cross', 'round_window_edge_left_cross', 'round_window_edge_bottom_cross', 'round_window_edge_right_cross'];

export function windowCross0120() {
  const builder = new StructureBuilder([15, 4, 3]);
  builder.floor();
  const look = compound({ Version: int(1), Kind: string('wood'), Material: string('minecraft:spruce_planks') });
  for (const [index, id] of ids.entries()) {
    builder.put([index + 1, 1, 1], `roundwood:${id}`, {
      north: 'false', east: 'false', south: 'false', west: 'false', waterlogged: 'true', trimmed: 'true', face_reversed: 'true',
      ...(index === 0 ? { axis: 'x', position: 'center' } : { orientation: 'south', depth: 'center' }),
    }, compound({ id: string('roundwood:round_window'), RoundwoodAppearance: look,
      WindowGlass: compound({ Version: int(1), Color: string('cyan') }),
      // Explicitly preserve two distinct face looks and two sill records, even on historically unsupported profiles.
      WindowTrim: compound({ Version: int(2), Front: compound({}), Back: compound({ RoundwoodAppearance: look }) }),
      WindowSillTrim: compound({ Version: int(1), Front: compound({ RoundwoodAppearance: look }), Back: compound({}) }),
    }));
  }
  return builder.file();
}
