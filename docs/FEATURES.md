# Roundwood feature notes

The complete content and controls are in [README](../README.md).

## Static chunk rendering

Static frames, casings, camouflage, windows' attachments and compatibility pieces use cached chunk geometry exclusively.
Chunk geometry takes owning-cell lighting, including proud bands.
Mining cracks resolve satellite model data. Door leaves continue to animate through their renderer.
Headless quad counts and timings do not establish an in-game FPS gain.

## Thin arch pieces

All three arch tops and the mirrored keyhole shoulder have separate thin versions, each 3/16
of a block deep. Craft one full piece alone to obtain one matching thin piece. Its wood, paint
or masonry appearance and supported item metadata are preserved. The conversion is one-way;
unknown payloads refuse crafting rather than discard data.

Ordinary aimed placement selects FRONT, MIDDLE or BACK. Shoulder orientation is chosen first,
then depth; the overlay shows the resulting regions and the complete proposed shape. Tops
remain non-colliding, while shoulders collide through their thin body. Universal Trims fit
independently on both faces and drop separately with their own appearances. The showcase
includes each thin product at all three depths.

## Doors fitted inside arches

Roundwood doors fit matching arch tops with an ordinary click in either placement order. The fitted leaf keeps its aimed FRONT/MIDDLE/BACK depth. Click the floor under a compatible top or click the top itself; the preview shows the same fitted result. Fitting an arch onto an existing door also works through its normal landing cell and keeps the door pose. Full tops match standalone geometry. Thin tops accept any depth except the same closed plane as the door (red refusal preview); either thickness may intersect the swinging leaf. Frame body, frame casings, door and door casings remain independently styled. Aim at the leaf to toggle or pick the door; aim at the frame to style or pick the arch. Harvesting any owned cell yields each paid component once.

Arched 1x2 uses Top 1x1 at y+1; Round 2x2 uses Top 1x2 at y+1. Double arched 3x3 uses Top 2x3 at y+1, and tall double arched 3x4 uses it at y+2. Shoulders and vanilla/other-mod same-cell doors are unsupported. The showcase contains twelve fitted full/thin examples spanning all three leaf depths. Visible middle/free-edge clicks toggle open and closed leaves, including their out-of-cell projection; a nearer frame or block still takes the ray.

The Frameless Window Pane fills a full cell face with recessed glazing. Craft one from one vanilla glass pane; place it upright, on floors or ceilings with the existing aiming preview. Dye tints its glass and Cross Beams are optional. Perimeter trims, sills and invisible frame restyling are refused.

Round doors accept lower-corner camouflage before or after fitting an arch. Fitting hides upper fill and preserves the existing donor and single refund; clearing the lower corners leaves the fitted frame unchanged.

The paired doors now reach their assembly tops with the raised 2x3 crown. Every door has edge-contact spear straps on both faces and knobs centred one block above the floor. Existing saved assemblies adopt the geometry without replacement.

Block-surface finishes support iron, gold, diamond, emerald, netherite, every full copper oxidation stage (including waxed donors), quartz block, smooth quartz, chiseled quartz, quartz bricks and quartz pillar. Use the existing body/frame/trim donor gestures. They stay fixed and cannot be dyed; unchanged applications and creative use are free.

Doors have independent assembly-wide hinge and knob finishes. Ordinary click either component with an accepted plank or block-surface donor, from either face or paired leaf; sneaking is optional. One donor changes that slot, with unchanged/creative/refused actions free. Wait for the eight-tick swing to settle before editing. Picked and harvested door items keep both finishes; there is no separate donor refund.

0.17.0 upgrades existing saved assemblies in place. The showcase adds a frameless centre, fitted lower camouflage, all four independently finished hardware examples, and a third material-wall row for the fourteen distinct surface looks. Waxed copper donors normalize to those same four copper textures.
