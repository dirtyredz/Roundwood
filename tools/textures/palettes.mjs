// Artwork parameters keyed by artwork name. These are NOT a content list: what a door, window or trim
// can look like is decided in Java (appearance/, client/material/BuiltinWoodStyles, PaintStyles);
// GeneratedTexturesTest checks the exact texture manifest independently.
//
// Order matters: each door palette's seed comes from its position, so the eleven woods and the four
// 0.4.0 paints keep their places (their door boards stay byte-identical) and new entries only append.
import { PAINT } from './legacy.mjs';
import { NEW_PAINT } from './paint-panels.mjs';
import { shade } from './noise.mjs';

const painted = (base) => ({ dark: shade(base, 0.68), mid: base, light: shade(base, 1.14) });

/** The neutral grey boards tinted at runtime with an unfamiliar wood's average colour. */
export const NEUTRAL = painted([196, 196, 196]);

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
  ...Object.fromEntries(Object.entries(PAINT).map(([id, base]) => [id, painted(base)])),
  ...Object.fromEntries(Object.entries(NEW_PAINT).map(([id, base]) => [id, painted(base)])),
  neutral: NEUTRAL,
};

/** Radial masonry: brick body, its darker and lighter tones, and the mortar between bricks. */
export const MASONRY = {
  brick: { brick: [150, 74, 58], dark: [116, 56, 44], light: [172, 92, 72], mortar: [170, 160, 152] },
  stone_brick: { brick: [126, 126, 126], dark: [104, 104, 104], light: [146, 145, 146], mortar: [84, 84, 86] },
  deepslate_brick: { brick: [74, 74, 77], dark: [56, 56, 59], light: [92, 92, 95], mortar: [36, 36, 38] },
  mud_brick: { brick: [146, 110, 80], dark: [124, 92, 66], light: [164, 126, 94], mortar: [100, 78, 66] },
};

// 0.8.0: the sixteen dye tints for dyed window glass, close to vanilla stained glass, in DyeColor order.
export const STAINED_GLASS = {
  white: [255, 255, 255], orange: [216, 127, 51], magenta: [178, 76, 216], light_blue: [102, 153, 216],
  yellow: [229, 229, 51], lime: [127, 204, 25], pink: [242, 127, 165], gray: [76, 76, 76],
  light_gray: [153, 153, 153], cyan: [76, 127, 153], purple: [127, 63, 178], blue: [51, 76, 178],
  brown: [102, 76, 51], green: [102, 127, 51], red: [153, 51, 51], black: [25, 25, 25],
};
