// Complete paint artwork: a base colour for every one of the 16 dyes, and the painted window boards
// (16x16, panel_<dye>) for the twelve dyes 0.4.0 did not have. The four 0.4.0 paints keep their
// legacy colours and byte-identical window panels (legacy.mjs); their door boards come from the
// door-panel generator like every other paint. Colours are chosen to stay legible as opaque paint
// (black is charcoal, white is off-white), never a donor wood multiplied by a dye.
import { PAINT, paintedPlanks } from './legacy.mjs';

/** Base colours for the twelve dyes added in 0.5.0, in vanilla dye order. */
export const NEW_PAINT = {
  white: [222, 220, 210],
  orange: [214, 118, 42],
  magenta: [176, 72, 164],
  light_blue: [96, 152, 204],
  lime: [118, 176, 52],
  pink: [222, 136, 164],
  gray: [96, 100, 106],
  light_gray: [154, 154, 150],
  cyan: [42, 128, 136],
  purple: [116, 62, 156],
  brown: [120, 82, 52],
  black: [54, 52, 58],
};

/** Every dye's base colour: the four legacy paints first (their artwork order), then the new ones. */
export const ALL_PAINT = { ...PAINT, ...NEW_PAINT };

/** name -> pixels: painted window boards for the new dyes (the legacy four come from legacy.mjs). */
export function newWindowPanels() {
  return Object.fromEntries(Object.entries(NEW_PAINT).map(([dye, base], i) =>
    [`panel_${dye}`, paintedPlanks(base, 53 + i * 6)]));
}
