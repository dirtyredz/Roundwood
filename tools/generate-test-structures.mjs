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
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'src/gameTest/resources/data/roundwood/structures');
import { file } from './structures/nbt.mjs';
import { legacyBuild } from './structures/legacy-v040.mjs';
import { build070 } from './structures/door-trim-v070.mjs';
import { windowCross0120 } from './structures/window-cross-v0120.mjs';

const empty = (size) => file(size, [{ name: 'minecraft:air' }], []);

mkdirSync(outDir, { recursive: true });
for (const [name, size] of Object.entries({ empty: [9, 6, 9], strip: [17, 5, 4], showcase: [86, 10, 86] })) {
  writeFileSync(join(outDir, `${name}.nbt`), empty(size));
  console.log(`wrote ${name}.nbt ${size.join('x')}`);
}
writeFileSync(join(outDir, 'legacy_v040.nbt'), legacyBuild());
console.log('wrote legacy_v040.nbt 13x4x4');
writeFileSync(join(outDir, 'legacy_v070.nbt'), build070());
console.log('wrote legacy_v070.nbt 13x4x6');

writeFileSync(join(outDir, 'window_cross_v0120.nbt'), windowCross0120());
console.log('wrote window_cross_v0120.nbt 15x4x3');
