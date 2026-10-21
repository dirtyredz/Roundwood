// Artwork parameters keyed by Roundwood's content ids. These are NOT a second catalogue: the finish
// and trim-material lists live only in variant/FinishCatalog and variant/TrimMaterial (Java), and
// GeneratedTexturesTest checks that every id there has exactly one generated texture here.
import { PAINT } from './legacy.mjs';
import { shade } from './noise.mjs';

/** Door panel wood tones (dark grain/seams, mid board, light highlights), close to each vanilla plank. */
export const DOOR_WOOD = {
  oak: { dark: [109, 85, 47], mid: [170, 140, 83], light: [194, 157, 98] },
  spruce: { dark: [86, 61, 32], mid: [119, 88, 50], light: [136, 101, 57] },
  birch: { dark: [160, 141, 99], mid: [195, 179, 121], light: [215, 203, 141] },
  jungle: { dark: [108, 74, 49], mid: [167, 119, 81], light: [191, 142, 107] },
  acacia: { dark: [138, 72, 41], mid: [170, 91, 49], light: [193, 108, 62] },
  dark_oak: { dark: [43, 27, 13], mid: [70, 45, 22], light: [83, 56, 26] },
  mangrove: { dark: [95, 30, 31], mid: [118, 55, 53], light: [138, 76, 58] },
  cherry: { dark: [209, 140, 134], mid: [231, 184, 178], light: [238, 205, 199] },
  bamboo: { dark: [147, 129, 60], mid: [199, 179, 77], light: [228, 205, 107] },
  crimson: { dark: [64, 31, 46], mid: [102, 51, 72], light: [134, 62, 90] },
  warped: { dark: [21, 59, 56], mid: [42, 107, 96], light: [58, 141, 140] },
  ...Object.fromEntries(Object.entries(PAINT).map(([id, base]) => [id,
    { dark: shade(base, 0.68), mid: base, light: shade(base, 1.14) }])),
};

/** Radial masonry: brick body, its darker and lighter tones, and the mortar between bricks. */
export const MASONRY = {
  brick: { brick: [150, 74, 58], dark: [116, 56, 44], light: [172, 92, 72], mortar: [170, 160, 152] },
  stone_brick: { brick: [126, 126, 126], dark: [104, 104, 104], light: [146, 145, 146], mortar: [84, 84, 86] },
  deepslate_brick: { brick: [74, 74, 77], dark: [56, 56, 59], light: [92, 92, 95], mortar: [36, 36, 38] },
  mud_brick: { brick: [146, 110, 80], dark: [124, 92, 66], light: [164, 126, 94], mortar: [100, 78, 66] },
};
