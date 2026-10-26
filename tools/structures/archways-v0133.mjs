import { int, string, compound } from './nbt.mjs';
import { StructureBuilder } from './structure-builder.mjs';

// Historical IDs, states and known payloads, independent of Java registrations.
export function archways0133() {
  const builder = new StructureBuilder([9, 4, 4]);
  const look = (kind, material) => compound({ Version: int(1), Kind: string(kind), Material: string(material) });
  builder.floor();
  builder.frame(1, 1, 'roundwood:archway', compound({
    id: string('roundwood:archway'),
    RoundwoodAppearance: look('wood', 'minecraft:spruce_planks'),
    ArchwayTrim: compound({ Version: int(1), Front: compound({}),
      Back: compound({ RoundwoodAppearance: look('masonry', 'minecraft:bricks') }) }),
  }));
  builder.frame(5, 1, 'roundwood:keyhole_archway', compound({
    id: string('roundwood:archway'),
    RoundwoodAppearance: look('masonry', 'minecraft:stone_bricks'),
  }));
  return builder.file();
}
