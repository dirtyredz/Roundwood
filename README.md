# Roundwood

Forge 1.20.1 mod: a round 2x2 wooden door, an arched 1x2 door and double arched 3x3 and 3x4 doors, a round window with arch, corner and edge profiles with optional Cross Beams attachments, three modular arch tops and a mirrored keyhole shoulder, a Universal Trim for all of them and an Arch Sill, hobbit-hole style. Each is one item you
restyle in the world: any planks (vanilla or modded) set the wood, any of the 16 dyes paints it,
and trims can also be faced with radial brick. Install on **both** the server and every client.
Generic: not tied to any modpack.

## Supported versions

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x (built and tested on 47.4.10) |
| Dependencies | none (EMI and JEI optional, client side) |

## Content

29 items: **Round Door**, **Arched Door**, **Double Arched Door**, **Tall Double Arched Door**, fourteen plain window profiles
(**Round Window**, **Round Window Arch, Left** and **Right**, **Round Window Corner, Bottom Left**
and **Bottom Right**, and since 0.10.0 **Round Window Arch, Bottom Left** and **Bottom Right**,
**Round Window Corner, Top Left** and **Top Right**, and **Round Window Edge, Top**, **Left**,
**Bottom** and **Right**), **Arch Top 1x1**, **Arch Top 1x2**, **Arch Top 2x3** and
**Keyhole Shoulder** (0.14.0), their four **Thin** counterparts (0.16.0), the **Universal Trim**, **Arch Sill** and **Cross Beams**. Creative tab order: round
door, arched door, double arched door, tall double arched door, then each plain window profile (round, arch left,
arch right, corner bottom left, corner bottom right, arch bottom left, arch bottom right, corner top
left, corner top right, edge top, left, bottom, right), the three arch tops, keyhole shoulder, Universal Trim,
Arch Sill, Cross Beams. They are crafted (and found in the creative tab) in oak;
their look is changed once placed and travels with them when picked or broken.

**Thin arches.** All four modular arch pieces also have 3/16-depth thin versions. Craft one full
piece alone to obtain its thin counterpart, preserving its appearance and supported item metadata;
there is no reverse recipe. Aim to place thin pieces at FRONT, MIDDLE or BACK. Both casing faces
follow the slab and keep independent looks. See [feature notes](docs/FEATURES.md).

**Fitted arches (0.16.0).** Ordinary-click a matching top with a Roundwood door, or a door
with its matching top. Arched 1x2 takes Top 1x1; Round 2x2 takes Top 1x2; both paired doors
take Top 2x3. The preview shows the BACK leaf and static frame. Full tops are unchanged from the
standalone arch (the swinging leaf may clip through them); thin tops fit at MIDDLE or FRONT. Frame and door appearances/casings stay independent,
and harvesting any owned cell yields separate components. Red outlines indicate refused fits.
Vanilla/other-mod doors and shoulders do not use same-cell fitting. Inspect all twelve fitted
examples with /roundwood showcase. See [0.16.0 release notes](docs/releases/0.16.0.md).

**Tall Double Arched Door (0.15.0).** The 3-wide, 4-tall variant adds a straight row below
exactly the same crown as the 3x3 double door. Match it with the 2x3 arch top starting two
blocks above the door floor, now also through same-cell fitting. Both leaves move together; all
three depths, paint, two face trims, redstone and single-item drops work as on the 3x3.
Craft it with PPP / NDN / PPP (P = planks, N = gold nugget, D = Double Arched Door).

**Door depth targeting (0.15.0).** All Roundwood doors have front, middle and back
positions. Aim along depth on a floor, ceiling or side wall; on a wall facing the door,
aim at the lower/middle/upper third for back/middle/front. The bands and full leaf preview
show the result. Hinge choice still follows across-width aim and neighbouring blocks.
Existing doors retain their original front position when depth is absent from saved state.

**Double Arched Door (0.15.0).** One item reserves a dry, replaceable 3 wide x 3 high footprint
with a solid floor beneath all three columns. Two leaves swing together on the outer hinges.
The bottom is square; the crown exactly fills the 2x3 arch opening when that arch's bottom
is one block above the door floor. Fit the matching arch with an ordinary click, or keep
it independently placed in an adjacent depth layer. The 0.17.0 crown reaches the top of its assembly; the former eight-pixel apex band is gone. Each leaf has straps and a knob on both faces, with the existing wood/paint,
camouflage, trims, redstone and projected-use behaviour. Recipe: `PNP / PPP / PNP`,
where P is any planks and N is a gold nugget.

**Round door** (one item places a 2 wide x 2 high assembly). One round leaf on one hinge, like a
hobbit door: vertical boards running the full height of the door, brass straps on the hinge side
and a prominent brass knob standing out from the middle of each face. It swings 90 degrees away
from you with a short animation; the open leaf lies along the hinge-side wall and leaves 29/16 of
clear width. Looking at any part outlines the whole door as it is: the leaf (open or closed) plus
any filled camouflage corners. The knobs and straps are decoration and do not collide. Holding the
door item (main hand, or offhand with an empty main hand) previews the placement since 0.11.0: the
leaf as it will stand (open if the spot is powered), the cells it will own and the hinge axis. It is
drawn from the same plan the server places from and shows nothing when placement would be refused.

- Placement: the cell you target becomes the **lower-left** part (seen from where you stand); the
  door extends one block to your right and one up. All four cells must be empty or replaceable,
  dry, free of mobs, inside the world, and both columns need a solid floor. If anything is in the
  way nothing is placed and the item is kept.
- Hinge: chosen like a vanilla door, judged at the door's two outer edges. Next to another round
  door it hinges on the far side (a pair opens from the middle); otherwise the side with more solid
  blocks beside it; otherwise the half of the targeted block you aimed at.
- Use: clicking any part, a villager, or any mod calling `setOpen` moves the whole door. Redstone on
  any of the four cells opens it; removing the last signal closes it.
- The open leaf's outer half sticks out one block in front of the hinge column. It is only drawn:
  it does not collide and can overlap whatever is there. An unobstructed projected leaf can be
  clicked to close the door; mining and picking still require an occupied cell.
- Camouflage: the four square corners around the round leaf can take one block's look. Right-click
  a corner holding a full, ordinary block (stone, planks, bricks...) to fill all four; another block
  swaps it (you get the old one back); sneak-right-click a filled corner with both hands empty to
  clear it. Survival uses one block, returned when you clear, swap or break the door.
- Breaking any part removes the whole door and drops **one** door in its look (none in creative),
  plus the stored camouflage block. Pistons cannot move it. To the game it is a normal wooden door
  (villagers, pathfinding, `minecraft:doors`, fuel, axe).

**Arched door** (0.11.0, one item places a 1 wide x 2 high assembly). A single round-headed leaf,
16 x 32 pixels: straight sides, a flat bottom and a semicircular crown, with the same boards, brass
straps (two, on both faces) and brass knobs as the round door. It swings 90 degrees with the same
short animation; the knobs and straps do not collide.

- Placement: the cell you target (or the replaceable cell you click) becomes the **lower** half and
  the door extends one block up. Both cells must be empty or replaceable, dry, free of mobs, inside
  the world and border, loaded and buildable by you, with a solid floor under the lower half. A
  powered spot places it open. If anything is in the way nothing is placed and the item is kept; a
  placement cancelled by a protection mod rolls back both cells and the item.
- Hinge: chosen like the round door but at the single column's two outer edges. Next to another door
  of the same block it hinges on the far side (a pair opens from the middle); otherwise the side with
  more full blocks beside it; otherwise the half of the clicked cell you aimed at.
- Use: clicking either half, a villager, or any mod calling `setOpen` moves both halves. Redstone on
  either half opens it; removing the last signal closes it.
- Open, the leaf lies along the hinge-side wall inside its own column and leaves 13/16 of the doorway
  clear. Nothing projects outside the two cells (unlike the round door's open leaf), and collision is
  clipped to them.
- Camouflage: only the two upper corners beside the crown can take one block's look, with the round
  door's gestures and one-block payment and refund. The lower cell has no corner.
- Universal Trim: fits the door's face and adds a static two-pixel casing following the arched
  outline (both jambs and the crown, no threshold across the floor), one pixel proud of the face. It
  restyles (planks, brickwork, dye) and comes off with shears like the round door's ring.
- Breaking either half removes the door and drops **one** door in its look (none in creative), plus
  its trims and the stored camouflage block. To the game it is a normal wooden door
  (`minecraft:doors`, `minecraft:wooden_doors`, fuel, axe, villagers). The same placement preview as
  the round door's is drawn while you hold it. The item icon is the closed left-hinge leaf.

**Modular arch pieces** (0.14.0). Names are **height x width**: 1x1, 1x2 and 2x3 tops,
plus one Keyhole Shoulder item that can face LEFT or RIGHT. All are one block deep. The tops
have true semicircular openings, square upper corners and no lower posts. The 1x1 is only
the two upper corners above a radius-8 round door opening, with nothing below y=8 or above its apex. The wider tops have radii 16 and 24
with spring heights 0 and 8 respectively. The 2x3 apex reaches the top of its footprint.
The shoulder extends a four-pixel-wide base down to the floor beneath the stepped transition (widths 4,3,3,2,2,1,1,0 in rows 8..15): the original keyhole corbel shifted two pixels outward to meet the flush arch spring.

- Place with an ordinary click. The landing cell is the lower-left anchor; tops extend right
  and up. Shoulders use four aimed-face wedges: on floors/ceilings choose the corresponding edge;
  on walls choose the left/right edge or lower = touching / upper = far edge. This reaches left,
  right, front and back without sneaking. The preview shows that same choice and every
  occupied cell. The 2x3 top reserves all six cells, including its visually empty lower centre.
- Every cell must be loaded, dry, replaceable, inside the border and editable. No supporting floor
  is required. Refused or cancelled placement keeps the item and restores the cells.
- Tops have no collision. Shoulders collide with their full visible body. Compose an
  ordinary opening from a top, or a keyhole opening from a top and two shoulders directly below.
  Put full-block columns **outside** the opening; full blocks beneath it reduce walking clearance.

```text
Ordinary             Keyhole              Taller ordinary
C [  top  ] C        C [  top  ] C         C [   2x3 top   ] C
C           C        C [L]   [R] C         C                 C
                                           C                 C
C = ordinary full-block column; L/R = independent shoulders
```

- Planks, paint and the four masonry materials restyle each piece. Wood/paint tile donor grain or
  painted boards; masonry adds contour mortar (5/9/13 sectors by top size, centred around the keystone axis).
  The 1x1/1x2 apex stays open; no extra keystone band is added.
- Universal Trim fits either face of each piece, one per face in an independent look. Donors on an
  installed casing restyle it; donors on the reveal restyle the body. Shears refund the casing.
  Arch Sill and Cross Beams are refused without payment.
- Breaking any owned cell removes only that piece, dropping one body and its two optional casings
  in their own looks. Neighbouring pieces and ordinary columns remain. All four products belong
  to `roundwood:arch_pieces`, mine with an axe and are never doors.

**Round window** (single block). A thin square wooden pane with round tinted glass in the middle.
It can stand upright and centred, or flush against any side of its cell (four walls, floor,
ceiling). A window never connects (0.13.1): where it meets a pane, bars, a wall, another window or a
corner, no glass bridge is drawn and its frames simply abut (the 0.5.0 glass arm across a centred
window's plane is gone; a vanilla pane beside a window still draws its own arm). Waterloggable; drops
itself with its frame look and its glass tint. The glass is clear unless you dye it (much more
transparent since 0.7.0, with the same diagonal glints) and shows no horizontal seam lines. See
"Placing windows".

**Round window arches** (two single blocks, left and right). The upper half of a round window two
blocks wide, split into two independent pieces: place them side by side and the glazed half circle
runs across both, with square wooden corners and nothing below the diameter. Left and right are
judged from the front: the left piece has its glass in its bottom right corner, the right piece in
its bottom left, so they meet at the circle's centre. The back shows the mirrored shape. Each piece
is a 16 x 16 pane 2 pixels thick; there is no partner lookup, so a single piece stands fine alone.
They share the round window's restyling, waterlogging and drops.

**Round window corners** (two single blocks, bottom left and bottom right). The lower corners of a
2 x 2 arched window: a two-pixel outer jamb and sill meeting square around glass, with no top or
inner border. Put a corner under each arch piece (bottom left under the left arch, bottom right
under the right arch) and the four pieces form a 32 x 32 window with one continuous frame, an arch
over a square-cornered lower opening and no divider where the pieces meet. Corners are independent
pieces like the arches and share their targeted placement, overlay, restyling, waterlogging and
drops.

**Lower arches** (0.10.0, two single blocks, bottom left and bottom right). The upper arches turned
upside down: put them under the upper arches and the four pieces glaze one full circle (a 28-pixel
round opening in a 32 x 32 window of four square panes). Breaking one quarter leaves the others.

**Top corners** (0.10.0, two single blocks, top left and top right). The bottom corners turned upside
down: an outer jamb and a two-pixel head. Over bottom corners they make a square-cornered 2 x 2
window; with edges between them, any rectangle.

**Edges** (0.10.0, four single blocks: top, left, bottom, right). A pane glazed everywhere except one
two-pixel border on the named side. Runs of them, between the corner pieces, make windows of any
width or height: a 3 x 2 window is bottom left corner, bottom edge, bottom right corner under top
left corner, top edge, top right corner.

**Cross Beams** (`cross_beams`, 0.13.0). One attachment fits any of the fourteen window profiles.
Hold it and aim at a pane to preview the clipped cross; click to fit it. One item spans the whole
pane, visible and removable from either side. Every pane in a larger window has its own cross.
Beams have their own wood, paint or masonry look: aim at a beam with planks, dye or one of the four
masonry blocks to restyle it; aim with shears to remove it and recover one styled item. Sneaking
changes nothing. An occupied fit is free. Beams run the full slab depth, with glazing cut away
beneath them. Sills cover beam ends and take clicks where they overlap. A broken window drops its
plain pane and one styled Cross Beams item, plus its trims and sills. Pick-block returns only the
plain pane with its frame look and glass tint.

**Universal Trim** (`round_trim`, one item, one piece per face, since 0.9.0). One trim item for every
window and door; its icon is a flat square frame in the trim's look. The older Window Trim and
Door Trim items still work everywhere (see Upgrading).

*On a window* the trim fits onto any of the 13 window profiles and follows its shape: a
ring round the round window, the matching quarter of the big ring on an arch (upper or lower, so four
arches carry one full ring), the outer jamb and sill or head on a corner, and only the framed side of
an edge piece (beams are never trimmed). Click a window
face with it (a plain click, no sneaking): it trims the face you clicked, so a window takes up to two
trims, each with its own look. A face that already has a trim just ignores the click and keeps the
item. Every trim stands one pixel proud of its face. On a window set flush against a wall, floor or
ceiling, the wall-side trim therefore extends one pixel outside the window's block; it is drawn only
(no collision, support or connection) and decorative. A trimmed window drops its pane and one trim
per trimmed face as separate items; picking the block gives the pane (with its glass tint) only.

**Arch Sill** (`arch_sill_trim`, 0.10.0, one item, one sill per face). A two-pixel bar across the
bottom of an upper arch's opening, one pixel proud, on the face you click (plain click, no
sneaking). It sits inside the opening, so it never overlaps the arch's trim: the same face can carry
a trim and a sill in different looks, and a window up to two trims and two sills. It lies over the
glass and the beam ends without changing the glass or its tint. Restyle it like a trim (planks,
masonry, dye aimed at the sill), take it off with shears (it comes back as an Arch Sill), and a
broken window drops each sill separately. Only the upper arches take one: on any other window it is
refused with a hint and kept.

*On a round door* it is a flat ring, open in the middle, purely decorative. Click a round door with
it (a plain click) and it attaches to the face you clicked, as a static casing: it never swings, collides or changes redstone. The front ring stands at
the leaf's front face, one pixel into the cells in front of the door; the back ring sits just behind
the leaf. A closed door takes a click anywhere inside the ring's outer edge; an open door is fitted
by aiming at the ring itself (it is outlined while you hold the trim). A face that already has a
trim ignores the click and keeps the item. The ring stays put while the door swings, and a door
drops its trims, one per face, when broken. The old 2 x 2 frame block is still there for saves made
before 0.8.0 (see Upgrading).

## Placing windows

Holding a window item (any of the 13 window profiles) and aiming at a block face draws an overlay on
that face: a centre square and four edge trapezoids, with the zone you aim at picked out in white
and the pane that would be placed outlined in light blue at its destination. The outline is hidden
when placement would be refused. The main hand's window wins; the offhand window is used only when
the main hand is empty. Sneaking changes nothing.

| Zone | Where on the face | Result |
|---|---|---|
| Centre | The middle square, from 1/4 to 3/4 across and up (edges included) | Pane upright and centred in the destination cell, facing you |
| Edge | One of four trapezoids along the face edges | Pane flush against that side of the destination cell |

- Face coordinates: on a floor or ceiling face u runs east and v south; on a north or south face u
  runs east and v up; on an east or west face u runs south and v up. Outside the centre square, the
  larger distance from the face middle picks the edge (an exact diagonal picks the u edge). An edge
  zone names the world direction of that edge, so the top and bottom zones of a wall face mean up
  and down: the six flush sides are the four walls, the floor and the ceiling.
- The destination cell is vanilla's (a replaceable clicked block is replaced); the zone is read on
  the face you clicked. Interactive blocks (doors, chests, levers) can still take the click first,
  so the overlay is not a promise.
- Arch, corner and edge pieces: the centre zone places them upright with the front towards you (a left
  piece on your left); an edge zone places them flush with the front towards you; on a floor or
  ceiling the top is the side you see as up, so a left piece looks left to whoever placed it.
- **To move a placed window, break it and place it again**; its look survives in the item (a fitted
  trim comes back as a Universal Trim, and a glass tint survives in the item). Windows saved by 0.5.0 or 0.6.x load unchanged and
  untrimmed, with their position, connections, water and look as they were.
- No junctions (0.13.1): no window grows glass towards a neighbour, centred or not, so two windows
  meeting at a corner or edge, or a window beside a pane, bars or a wall, just meet frame to frame.
  Planks, dye and the attachments act on a window exactly as before.

## Restyling

| Aim at | Holding | Result | Cost |
|---|---|---|---|
| Door leaf | **Sneak** + planks | That wood; any paint removed | 1 plank |
| Door leaf | Dye (sneaking or not) | Painted, the wood kept underneath | 1 dye |
| Door corner | Block, not sneaking (planks included) | Camouflage, as above | 1 block, refunded later |
| Door corner | Sneak + planks or dye | Refused with a hint to aim at the leaf | nothing |
| Window frame | Planks | That wood for the frame; glass and attachments kept | 1 plank |
| Window frame | Dye | Frame painted | 1 dye |
| Window face | Cross Beams | Fits one whole-pane cross; occupied fits are free | 1 Cross Beams |
| Fitted beam | Planks, dye or masonry | Restyles beams only; masonry refuses dye | 1 donor |
| Fitted beam | Shears | Removes and returns one styled Cross Beams item; one wear | none |
| Window glass | Dye | The whole window's glass tinted that dye (another dye replaces the tint) | 1 dye |
| Window trim band (the ring under a trim) | Planks | That wood for that face's trim (paint removed) | 1 plank |
| Window trim band | Bricks, stone bricks, deepslate bricks or mud bricks | Brickwork for that face's trim | 1 block |
| Window trim band | Dye | Painted trim (refused on brickwork, free) | 1 dye |
| Window trim band | Shears | That face's trim removed and returned as a Universal Trim; shears take one wear | none |
| Window face | Universal Trim (or an older trim item) | Fits a trim on the clicked face (nothing happens if it has one) | 1 trim |
| Arch sill | Planks, masonry or dye | Restyles that face's sill, like a trim band | 1 item |
| Arch sill | Shears | That face's sill removed and returned as an Arch Sill; shears take one wear | none |
| Upper arch face | Arch Sill | Fits a sill on the clicked face (nothing happens if it has one) | 1 sill |
| Any other window | Arch Sill | Refused with a hint | nothing |
| Window, anywhere but a trim band, sill or fitted beam | Bricks, stone bricks, deepslate bricks or mud bricks | Refused with a hint to aim at the trim | nothing |
| Door trim ring | Planks | That wood for that face's ring (also replaces masonry) | 1 plank |
| Door trim ring | Bricks, stone bricks, deepslate bricks or mud bricks | Radial brickwork, paint cleared | 1 block |
| Door trim ring | Dye | Painted (refused on brickwork, free) | 1 dye |
| Door trim ring | Shears | That face's ring removed and returned as an item; shears take one wear | none |
| Door | Universal Trim (or an older trim item) | Attaches a ring to the clicked face (nothing happens if it has one) | 1 trim |
| Arch piece (reveal, crown, or a face without a casing) | Planks, masonry or dye | That look for the whole arch piece (dye refused on brickwork, free) | 1 item |
| Archway face with a casing | Planks, masonry or dye | Restyles that face's casing | 1 item |
| Archway face | Shears | That face's casing removed and returned as a Universal Trim; shears take one wear | none |
| Archway | Universal Trim (or an older trim item) | Fits a casing on the nearer face (nothing happens if it has one) | 1 trim |
| Archway | Arch Sill | Refused with a hint | nothing |
| Freestanding frame (older saves) | **Sneak** + planks | That wood (also replaces masonry) | 1 plank |
| Freestanding frame | **Sneak** + bricks, stone bricks, deepslate bricks or mud bricks | Radial brickwork, paint cleared | 1 block |
| Wooden freestanding frame | Dye | Painted | 1 dye |
| Brick freestanding frame | Dye | Refused (masonry is not painted) | nothing |

- Windows, arch pieces and door trims need no sneaking: a plain click does the same as a sneaking one.
  Only the door leaf and freestanding frames keep their sneak gestures.
- Aim decides on a window: installed perimeter trim, installed sill, fitted beam, frame, then glass.
  Each attachment keeps its own look. A sill takes priority over the beam end underneath it.
- Nothing is refunded when you restyle; using the same planks or dye again does nothing and costs
  nothing; re-planking a painted object with its own wood strips the paint (one plank). Creative
  costs nothing, and shears in creative return nothing and take no wear. A plain click (or a plain
  click with planks) still opens the door; buckets still work on windows.
- There is no way back to clear glass except a new window.
- The offhand works only when the main hand is empty.
- **Planks** are any item tagged `minecraft:planks`, or added by a datapack to
  `roundwood:appearance_planks`. Modded woods show their own plank texture on windows, arches and
  trims and
  Roundwood's full-height boards tinted to that wood's colour on doors. A wood whose mod is later
  removed shows as oak and comes back when the mod returns. Dyeing the glass uses the dye's
  stained-glass colour.
- The look is cosmetic: every door, window piece and trim behaves like the oak one (an axe mines all
  of them, nether woods do not change fire behaviour, brick trims do not need a pickaxe).
- Brickwork has thinner mortar since 0.7.0, on door trims, freestanding frames and window trims;
  saved brick looks pick it up automatically.

## Showcase command

`/roundwood showcase` (permission level 2: ops, or single-player with cheats) builds a labelled test
layout on the ground ahead of you, centred on the way you face (90 x 8 x 40 blocks, starting three
blocks ahead; it was 82 x 8 x 28 in 0.13.0, with the samples two columns apart). It first clears that whole box to air and lays a stone
floor, so use it on open ground; it refuses if the box would leave the build limits, the world border
or loaded chunks, and never touches blocks outside it. The layout is one of each thing, not every
combination. From the front row to the back:

- **One of each placeable piece**, oak, default state: the fourteen windows, the round door, the arched
  door (both closed), in a single row.
- **The materials wall** (0.13.1; it was a row of one archway per look): the actual restyling source
  blocks, each with a wall sign on its front face, for you to take and use. The bottom row is the planks
  of the eleven vanilla woods (and up to eight installed modded planks); the row above is the four
  masonry blocks, then a barrel holding a stack of each of the sixteen dyes (paint is a dye on any
  look).
- **Window configurations**, built in oak only: the seven positions (centred, and flush on each side,
  ceiling and floor) with neighbour and practice-block examples; arch pairs (centred, flush, floor,
  ceiling) and single arch pieces; stacked, waterlogged and stone-behind neighbours; 2 x 2
  arched windows (centred, flush, floor, ceiling) and each bottom corner alone; a full circle; a 3 x 2
  and a 2 x 3 window from top corners and edges, and each edge alone. (The junction demo is gone with
  the junction pane.)
- **Attachments**, one fitted example each: a Universal Trim (brick front, spruce back) on a round
  window, an Arch Sill under an arched window, Cross Beams on a round window, stone-brick camouflage on
  a round door, and a lever-driven round door (redstone).

- **Arch pieces**: each top and a paired shoulder example, with cherry front/blue back casings
  and a brick contour example.
- **Arch compositions**: ordinary, keyhole and taller three-wide openings with outside columns.

Every sample stands four clear blocks from the next (along the row and between rows), so you can walk between them. Every label sign stands beside the piece it names, never in front of it: in the piece's own row, two columns to the left of its lower-left cell, turned to face you, so it reads from where you stand to look at the piece and does not cover it, and it is two clear blocks from any other sample or sign. The build reports a placement problem if a sign would replace, or be replaced by, another sample's block.

## Recipes

| Result | Recipe |
|---|---|
| Round door | 8 planks (any, mixed allowed) around 1 gold nugget |
| Arched door (0.11.0) | Rows ` P ` / `PNP` / `PPP` (P any planks, N gold nugget) |
| Round window | 4 planks around 1 glass pane (plus shape) |
| Round window arch, left | Rows `PP` / `PG` / `P ` (P planks, G glass pane) |
| Round window arch, right | Rows `PPP` / ` G ` / ` P ` (P planks, G glass pane) |
| Round window corner, bottom left | Rows `P ` / `PG` / `PP` (P planks, G glass pane) |
| Round window corner, bottom right | Rows ` P ` / ` G ` / `PPP` (P planks, G glass pane) |
| Arch, bottom left (0.10.0) | Rows `PP` / `PG` / `PP` |
| Arch, bottom right | Rows `PPP` / ` G ` / `PPP` |
| Corner, top left | Rows `PPP` / `PG ` / `P  ` |
| Corner, top right | Rows `PPP` / ` G ` / `  P` |
| Edge, top | Rows `PPP` / ` G ` |
| Edge, left | Rows `P ` / `PG` / `P ` |
| Edge, bottom | Rows ` G ` / `PPP` |
| Edge, right | Rows `GP` / ` P` / ` P` |
| Arch Top 1x1 | Rows `PPP` / `P P` (P any planks) |
| Arch Top 1x2 | Rows `PPP` / `P P` / ` S ` (S stick) |
| Arch Top 2x3 | Rows `PPP` / `P P` / `S S` |
| Keyhole Shoulder | Rows `PP` / ` P` |
| Cross Beams (`cross_beams`) | Five sticks: rows ` S ` / `SSS` / ` S `; unlocked by obtaining a stick |
| Universal Trim (`round_trim`) | 4 planks in the four corners of the grid |
| Arch Sill (`arch_sill_trim`) | Rows `PPP` / `S S` |

The new shapes are not the old ones turned over: a lower arch drawn like an upturned upper arch
would be a bottom corner's recipe, so each piece has its own. Shaped recipes also match mirrored, so
the left and right edges differ by where the pane sits.

Crafting always gives the oak object; restyle it in the world. With **EMI** and **JEI** each of the
24 items (four doors, 13 plain windows, four arch pieces, Universal Trim, Arch Sill and Cross Beams) is
listed once, as its plain oak look, and nothing cycles; retired crossed windows and old archways stay hidden.
Styled stacks in your inventory are told apart by their look (EMI comparison key, JEI subtype).
Neither viewer is required.

## Upgrading

**To 0.14.0 from a world with old archways:** loaded ordinary archways become a 1x2 top in the
upper row and air below. The crown opening widens from radius 14 to 16. Keyhole archways become
that top plus full-height LEFT/RIGHT shoulders below, preserving their old lower shape. Facing
and body look survive; both old casings move to the same faces of the top, in their own looks.
Shoulders start untrimmed. Conversion is atomic and repeat-safe. Unloaded partners defer it;
damaged assemblies use the established integrity cleanup. Unknown trim versions defer conversion.

Old archway stacks become the same number of 1x2 tops; each keyhole item also gives two shoulders.
Player/ender inventories, loaded vanilla block containers, container entities and dropped items
are converted. Excess shoulders drop at the holder; refused drops leave the source unchanged.
The top preserves source stack metadata and every output keeps the body look. Nested item NBT
and private mod inventories are not recursively scanned; unopened loot tables are not rolled. Old IDs remain migration sources only:
no placement, recipes, creative tab or viewer entries.

**From 0.12.x and earlier:** crossed windows become plain profiles with one Cross Beams attachment
in their former frame look. Their frame, glass, pose, water, trims and sills survive. Old `_cross`
stacks keep their IDs, counts and saved contents in inventories and chests; placing one installs the
plain pane and beams together. Unreadable beam data keeps the old block or stack intact. Hidden
compatibility IDs remain registered, but their recipes and creative/viewer entries are retired.

**From 0.10.0 or 0.11.0:** the later archway changes above apply only if your world contains archways.

**From 0.9.0:** saves load unchanged. Windows gain an optional sill payload
(`WindowSillTrim`) only when a sill is fitted; nothing converts.

**From 0.8.0:** the Window Trim and Door Trim become one **Universal Trim** (`round_trim`, the old
Door Trim id, now with a square-frame icon). Old stacks are not rewritten: Window Trim and Door
Trim stacks in chests and inventories keep their id, count and look, and both fit windows and doors
straight away; a trim you take off with shears, or that drops when its window or door breaks, comes
back as a Universal Trim in the same look. The Window Trim recipe is gone (the four-corner planks
recipe makes the Universal Trim). Placed windows and doors keep every trim exactly as saved.

**From 0.7.0:** window trims become two trims, one on each face, in the look they had (a 0.7.0 trim
fitted both faces); this is a one-time uplift in material, since each face now drops its own trim.
Fitted door frames (a round trim standing in the four cells in front of a door) become that door's
front trim, once the chunk loads, when the frame and door match exactly and the door's front has
no trim yet; a door whose front already has a trim keeps its frame. A freestanding frame exactly in
front of a door is indistinguishable from a fitted one and converts too. Other freestanding frames
remain as legacy content: they still render, restyle with the sneak gestures, break normally and
drop a Universal Trim in their look. Nothing else changes.

**From 0.6.x:** saves load unchanged. Placed windows keep their position, connections, water and
look and come back untrimmed (a missing `trimmed` means no trim); they are redrawn with the
clearer glass and without seams, and saved brick looks get the thinner mortar. Nothing converts
and nothing drops.

**From 0.4.0:** the 49 old door, window and trim ids still load. Placed doors, windows and trims (fitted or
freestanding) turn into the new blocks shortly after their chunk loads, keeping their wood or paint,
facing, hinge, open state, water and camouflage (with its refund); nothing drops. Old items in chests
keep their names and looks and place the new blocks. Mirrored structure copies keep the 0.4.0
limitation (camouflage is lost when mirroring).

## Build, datagen, test

```
node tools/generate-textures.mjs        # original textures (deterministic)
node tools/generate-test-structures.mjs # GameTest templates incl. the 0.4.0 fixture
./gradlew runData                       # regenerate src/generated/resources (checked in)
./gradlew test build                    # unit tests + reobfuscated release jar
./gradlew --offline runGameTestServer -PdoorTargets # bounded door/arch placement gate
./gradlew runGameTestServer             # headless Forge server GameTests (fresh world each run)
git diff --check
```

`-PwithViewers=emi|jei|jei,emi` puts the optional viewers on the dev runtime; `-PpackModsDir=<mods
folder>` adds Double Doors, Collective and Dramatic Doors from a pack instance for compatibility runs.
The release jar is `build/libs/roundwood-1.20.1-<version>.jar`.

## Release

Bump `mod_version` in `gradle.properties` and commit non-empty `docs/releases/<version>.md`.
Push, land on main, and wait for green CI (tests, build, final-jar guard and datagen).
With the owner's go-ahead, the agent runs `gh workflow run release.yml --ref main`.
The release workflow verifies again, gates on GameTests, builds the release jar, creates its
version tag, uploads a draft, reads back and hashes every asset, then **publishes** the release
with the jar, its `.sha256` and `roundwood.pw.toml`. The pack session consumes the published
`.pw.toml` asset. The owner does not publish by hand. Versions starting with `0.` or containing
`-` are prereleases. Existing releases and tags are refused, published bytes are never replaced,
and stable latest never moves backward. A failed draft remains private for deliberate recovery.
Pushes and release dispatch still need the owner's go-ahead.

Not released yet. 0.5.0 is the baseline; 0.6.0 (targeted window placement and the arch pieces),
0.6.1 (no glass bridges on positioned windows, the door highlight follows the swing) and 0.7.0
(seamless clearer window glass, corner pieces, cross beams, window trims, thinner mortar) were
trialled by the owner, and so was 0.8.0 (a trim per window face, trim standing proud of flush
windows, tinted window glass, Door Trim attached to doors, plain-click gestures).

0.9.0 to 0.12.0 are built and tested headless (unit tests and server GameTests) and await one owner
trial together. Use a copy of a world saved in 0.8.0, run `/roundwood showcase` on open ground, and
check in game (the client-side retexturing and rendering have no automated test):

*0.14.0 trial checklist*
- [ ] Load a world saved in 0.13.x that has archways. Check ordinary/keyhole replacement positions,
  facings, body looks and both casing looks; reload and confirm there are no duplicate pieces.
- [ ] Check old chest and player stacks, including a full inventory: counts and looks survive,
  with excess shoulders dropped at the holder. Old items cannot place an old opening.
- [ ] Check the 1x1 behind/above a one-wide round-topped door: only upper corners, no crown band.
- [ ] Check 1x2/2x3 beside full blocks: no two-pixel spring ledge; shoulders reach the floor and their corbel steps continue the arch curve with no ledge or gap at the seam.
- [ ] Build all three tops and both shoulder sides in all facings. Check preview, six-cell
  obstruction and ownership, ordinary/keyhole clearance, and taller outside-column compositions.
- [ ] Check cherry and another wood, paint, all four masonry looks, front/back casings, reveals,
  icons, keystone/joint continuity, F3+T and chunk-edge rendering.
- [ ] Run `/roundwood showcase`: four new advertised products, composition rows, labels and spacing.
  EMI/JEI each show the 22 active items once; no old archway recipes remain.
- [ ] Close open round and arched doors from their free edges, both sides/hinges/facings,
  with camouflage and trims; repeat on a second client.

*Historical trial checklists (the 0.14.0 replacement policy supersedes old archway checks)*

*0.13.4, open-door targeting*
- [ ] Close round and arched doors by clicking the outlined free edge from either side, with both
  hinges and all four facings; reopen them, and repeat with camouflage, trims and a second client.

*0.13.3, archway plank textures*
- [ ] Cherry (and one other wood) archways, ordinary and keyhole, in all four facings: every face shows
  real plank grain at block scale (no smeared or sideways stripes), including the inner steps and jambs.
- [ ] Front and back casings, each masonry material, and the archway icon in the inventory and in hand.

*0.13.1, owner feedback fixes*
- [ ] `/roundwood showcase`: pieces and signs are well apart, each sign beside its piece and readable
  on its own; the materials wall holds every plank, the four masonry blocks and the dye barrel, and the
  blocks can be taken and used on a look.
- [ ] No glass pane bridges windows meeting at a corner or junction (the owner's screenshot 3), and
  plain windows look otherwise unchanged.
- [ ] Both archways are comfortable to walk through, a villager and a zombie included, while the frame
  looks as before and the crown is still outlined.

*0.13.0, Cross Beams*
- [ ] Fit preview equals the result on all 14 profiles and placement families, from either side.
- [ ] Beam wood/paint/masonry stays independent of frame and tint; sills win overlapping targets.
- [ ] Mixed fitted/plain neighbours have no seams or missing beam ends, including chunk edges,
  save/reload, F3+T and a second client.
- [ ] Hidden crossed inventory icons show the fitted result; viewers expose only plain profiles and
  Cross Beams; old saved builds and chest stacks retain all contents.

*0.9.0, Universal Trim*
- [ ] The square-frame icon in oak, another wood, an unfamiliar modded wood, a paint and each of the
  four masonry materials.
- [ ] Old Window Trim and Door Trim stacks from a chest fit both windows and doors, plain click and
  sneaking; shears give back a Universal Trim in the same look.
- [ ] Two faces with different looks survive save and reload; 0.7.0 fitted frames convert only as
  documented in Upgrading.

*0.10.0, window shapes and the Arch Sill*
- [ ] Plain and crossed full circles from four arches: front and back seams, the ring trim's quarters
  meeting.
- [ ] Top corners over bottom corners, runs of each edge piece (a 3 x 2 window), each edge alone.
- [ ] Centred, flush, floor, ceiling and waterlogged placement, and the preview matching the result.
- [ ] Sills fitted, restyled and removed independently of trims on both faces; glass tint under a
  sill; a window with two trims and two sills drops five items.
- [ ] Masonry art: radial on lower arches, head on top corners, straight runs on edges, one course on
  sills; the Arch Sill icon; F3+T reload keeps every look.

*0.11.0, the arched door and the door preview*
- [ ] Place both door designs on both hinges; the preview before placing (open when powered, nothing
  shown where refused); refusals keep the item.
- [ ] The open arched leaf's 13/16 clearance with nothing outside its cells; straps and knobs; the
  swing and its highlight.
- [ ] Static trims on the arched outline; upper-corner camouflage; redstone from either half;
  villagers walking through; save and reload; both item icons.

*0.12.0, the archways*
- [ ] Hold each archway: the preview shows the frame's real opening (ordinary, keyhole) and the four
  cells, matches what is placed, and shows nothing where placement is refused (blocked cell, water,
  missing floor, a mob in a jamb). Sneaking changes nothing.
- [ ] Depth (full block), the open floor (no threshold), the crown, and the keyhole's shoulders and
  20-pixel lower passage; walk through both; the jambs and crown stop you.
- [ ] Every look: oak, other woods, an unfamiliar wood, a paint, each masonry material (contour
  brickwork: keystone on top, joints on the spring lines, straight jamb courses, corbelled shoulders);
  the styled item icon in the inventory and in hand.
- [ ] Universal Trim on the front and the back separately, in different looks, one pixel proud;
  restyling a casing vs the archway (aim at the passage side); shears; the Arch Sill refused.
- [ ] Break each cell: one archway in its look plus each trim in its own; remove the floor under a
  lower cell; save and reload; F3+T; rendering from a distance and with the archway across a chunk
  edge (it is drawn whenever any part is in view).
- [ ] Known gap shared with doors and frames: the mining crack does not show on archways. The flow follows DankRecipeTransfer: an immutable
GitHub prerelease asset, then a direct-URL `mods/roundwood.pw.toml` (with SHA-256, `side = "both"`)
in the pack repo. Both steps need the owner's go-ahead.

See [STRUCTURE.md](STRUCTURE.md) for the code map and [docs/](docs/) for architecture, decisions
and gotchas.

## License

MIT. Textures under `src/main/resources` are original; vanilla textures are referenced, never copied.

The **Frameless Window Pane** (`frameless_window`) fills the centre of a larger window without a wooden border. Craft one from one glass pane. It supports the usual aimed placement, glass dyes and optional Cross Beams, but no perimeter trim or sill.

Block-surface finishes (0.17.0) apply to doors, window frames, casings and arch bodies: iron/gold/diamond/emerald/netherite blocks; full copper at every oxidation stage, including waxed donors; quartz block, smooth quartz, chiseled quartz, quartz bricks and quartz pillar. They use donor textures at block scale and do not oxidize or accept dye. Cut copper, stairs and slabs are not donors. Existing donor gestures and one-item payment rules apply.
