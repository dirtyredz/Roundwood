// Writes the registry and blockstate contract of one historical Forge 1.20.1 Roundwood release as the
// version-labelled manifest the save-compatibility GameTests read.
//
//   node tools/compat/historical-manifest.mjs <extracted-release-jar-dir> <version> <tag-commit> <jar-sha256>
//
// Block ids come from the release jar's blockstates, item ids from its item models (template_* models are
// not items). Block-entity, loot-condition and recipe-serializer ids, and the property contract per family,
// are transcribed from the release's registration and block classes (see SAVE-COMPATIBILITY.md); the GameTests
// compare the running port against this file, never against the port's own registrations.
// Writes src/gameTest/resources/compatibility/forge-1.20.1/<version>/manifest.json and prints OK/FAILED.
import { readdirSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const [jarDir, version, commit, sha256] = process.argv.slice(2);
if (!jarDir || !version || !commit || !sha256) {
  console.error('usage: historical-manifest.mjs <extracted-jar-dir> <version> <tag-commit> <jar-sha256>');
  console.log('FAILED exit=2');
  process.exit(2);
}
const ids = (dir, keep) => readdirSync(join(resolve(jarDir), 'assets/roundwood', dir))
  .filter(f => f.endsWith('.json')).map(f => f.slice(0, -5)).filter(keep).sort();
const blocks = ids('blockstates', () => true);
const items = ids('models/item', id => !id.startsWith('template_'));
const modern = version !== '0.13.3';

const WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped',
  'green', 'yellow', 'red', 'blue'];
const bool = ['true', 'false'];
const horizontal = ['north', 'south', 'west', 'east'];
const vanillaDoor = { facing: horizontal, half: ['upper', 'lower'], hinge: ['left', 'right'], open: bool, powered: bool };
const doorDepth = modern ? { depth: ['front', 'middle', 'back'] } : {};
const pane = { north: bool, east: bool, south: bool, west: bool, waterlogged: bool, trimmed: bool, face_reversed: bool };
const fourParts = ['lower_left', 'upper_left', 'lower_right', 'upper_right'];
const roundWindows = ['round_window', 'round_window_cross', ...WOODS.map(w => `${w}_round_window`)];
const armIds = new Set(['round_door', 'arched_door', 'double_arched_door', 'tall_double_arched_door', 'round_trim', 'archway', 'keyhole_archway']);

const families = {
  round_door: { members: ['round_door', ...WOODS.map(w => `${w}_round_door`)],
    properties: { ...vanillaDoor, column: ['left', 'right'], ...doorDepth } },
  arched_door: { members: ['arched_door'], properties: { ...vanillaDoor, ...doorDepth } },
  round_window: { members: roundWindows, properties: { ...pane, axis: ['x', 'z'],
    position: ['center', 'north', 'east', 'south', 'west', 'up', 'down'] } },
  oriented_window: { members: blocks.filter(id => (id.startsWith('round_window_') || id === 'frameless_window') && !roundWindows.includes(id)),
    properties: { ...pane, orientation: ['north', 'east', 'south', 'west', 'up_north', 'up_east', 'up_south', 'up_west',
      'down_north', 'down_east', 'down_south', 'down_west'], depth: ['center', 'negative', 'positive'] } },
  frame: { members: blocks.filter(id => id === 'round_trim' || id.endsWith('_round_trim')),
    properties: { facing: horizontal, part: fourParts } },
  archway: { members: ['archway', 'keyhole_archway'], properties: { facing: horizontal, part: fourParts } },
};
if (modern) {
  families.paired_door = { members: ['double_arched_door', 'tall_double_arched_door'],
    properties: { ...vanillaDoor, cell_x: ['0', '1', '2'], cell_y: ['0', '1', '2', '3'], ...doorDepth } };
  const cells = ['lower_left', 'upper_left', 'lower_middle', 'upper_middle', 'lower_right', 'upper_right'];
  const arches = ['arch_top_1x1', 'arch_top_1x2', 'arch_top_2x3', 'keyhole_shoulder'];
  families.arch_piece = { members: arches, properties: { facing: horizontal, cell: cells, side: ['left', 'right'] } };
  families.thin_arch_piece = { members: arches.map(id => `${id}_thin`),
    properties: { facing: horizontal, cell: cells, side: ['left', 'right'], depth: ['front', 'middle', 'back'] } };
}
const covered = new Set(Object.values(families).flatMap(f => f.members));
const missing = blocks.filter(id => !covered.has(id));
const phantom = [...covered].filter(id => !blocks.includes(id));
if (missing.length || phantom.length) {
  console.error(`family contract mismatch: uncovered ${missing.join(',')} / not in jar ${phantom.join(',')}`);
  console.log('FAILED exit=1');
  process.exit(1);
}

const manifest = {
  source: { version, tag: `v${version}`, commit, jar: `roundwood-1.20.1-${version}.jar`, jarSha256: sha256, minecraft: '1.20.1',
    dataVersion: 3465, captured: 'registry ids from the release jar; contracts from the tagged registration and block classes' },
  blocks, items,
  blockEntities: modern ? ['arch_piece', 'archway', 'round_door', 'round_trim', 'round_window'] : ['archway', 'round_door', 'round_trim', 'round_window'],
  lootConditions: ['trim_face'],
  recipeSerializers: modern ? ['arch_thinning'] : [],
  creativeTabs: ['roundwood'],
  // Properties a 0.13.3 save never wrote: reading such a state must land on this value.
  addedSince: modern ? {} : { round_door: { depth: 'front' }, arched_door: { depth: 'front' } },
  families,
};
const outDir = join(root, 'src/gameTest/resources/compatibility/forge-1.20.1', version);
if (!existsSync(outDir)) mkdirSync(outDir, { recursive: true });
writeFileSync(join(outDir, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
console.log(`wrote ${version}: ${blocks.length} blocks, ${items.length} items, ${Object.keys(families).length} families`);
console.log('OK exit=0');
