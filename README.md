# Roundwood

Forge 1.20.1 mod: a round 2x2 wooden door and an arched 1x2 one, a round window with arch, corner and edge pieces (each
plain or with cross beams), two open 2x2 archways, a Universal Trim for all of them and an Arch Sill, hobbit-hole style. Each is one item you
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

32 items: **Round Door**, **Arched Door** (0.11.0), thirteen window pieces, each in a plain and a cross-beam version
(**Round Window**, **Round Window Arch, Left** and **Right**, **Round Window Corner, Bottom Left**
and **Bottom Right**, and since 0.10.0 **Round Window Arch, Bottom Left** and **Bottom Right**,
**Round Window Corner, Top Left** and **Top Right**, and **Round Window Edge, Top**, **Left**,
**Bottom** and **Right**; the crossed ones are named "... with Cross Beams"), the **Archway** and
**Keyhole Archway** (0.12.0), the **Universal Trim** and the **Arch Sill**. Creative tab order: round
door, arched door, then each plain window piece followed by its crossed variant (round, arch left,
arch right, corner bottom left, corner bottom right, arch bottom left, arch bottom right, corner top
left, corner top right, edge top, left, bottom, right), archway, keyhole archway, Universal Trim,
Arch Sill. They are crafted (and found in the creative tab) in oak;
their look is changed once placed and travels with them when picked or broken.

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
  it does not collide, cannot be clicked, and can overlap whatever is there.
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

**Archway and Keyhole Archway** (0.12.0, one item places a 2 wide x 2 high static opening). A
full-block-deep frame with an open passage: no leaf, glass, swing, camouflage or redstone. Both are
32 x 32 pixels outside, straight outer jambs up to half height under a round (radius 16) crown.
- *Archway*: two-pixel jambs and crown; the passage is 28 pixels wide (1.75 blocks) under a radius-14
  crown.
- *Keyhole Archway*: the same crown, but below the spring line each jamb steps in four pixels over
  eight (one pixel every two rows), leaving a 20-pixel (1.25 blocks) lower passage.
- Placement: an ordinary click; the cell you target (or the replaceable cell you click) becomes the
  **lower-left** cell seen from where you stand, and the archway extends one block to your right and
  one up, facing you (four facings, always upright, no hinge). All four cells must be empty or
  replaceable, dry, inside the world and border, loaded and buildable by you, with a solid floor
  under both lower cells, and no mob where the frame would stand (someone in the passage is fine).
  Holding the item previews the frame outline (the real opening) and the four cells; nothing is
  drawn where placement would be refused. A refused or cancelled placement keeps the item and
  restores every cell.
- You walk through the passage; the jambs and crown collide like a wall. Light and neighbours show
  through.
- Looks: any planks (stripped-log look, like the trims), any dye, or bricks, stone bricks, deepslate
  bricks or mud bricks, with brickwork that follows the contour (radial voussoirs with a keystone,
  straight jamb courses, corbelled shoulders on the keyhole). A plain click with planks, masonry or
  dye restyles it (sneaking does the same).
- Universal Trim: click either face (a plain click) to fit a casing over that whole face, one pixel
  proud, in its own look; restyle it by aiming planks, masonry or dye at that face; shears take it off.
  On a face with a casing, donors restyle the casing; to restyle the archway itself, aim at the inside
  of the passage or the crown. The Arch Sill is refused with a hint.
- Breaking any cell removes the archway and drops **one** archway in its look plus one Universal Trim
  per trimmed face, each in its own look. Losing the floor breaks it the same way. Pistons cannot move
  it. It is in `roundwood:archways`, mines with an axe, and is never a door.

**Round window** (single block). A thin square wooden pane with round tinted glass in the middle.
It can stand upright and centred, or flush against any side of its cell (four walls, floor,
ceiling). Centred, it connects like a glass pane, with a short glass arm across its plane where it
meets a pane, bars, wall or crossing window (never towards a solid block); flush, floor and ceiling
windows grow no glass, so where they meet each other their frames simply abut. Waterloggable; drops
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

**Plain or cross beams.** Each of the thirteen window pieces also exists as a separate item with cross
beams (names end "with Cross Beams"). Every crossed piece has its own one-pixel wooden cross through
the middle of its own opening, so four crossed pieces in a 2 x 2 show four crosses (a grid), not one
shared cross. The beams use the pane's wood or paint, run the full thickness of the pane, and glass
is cut away under them. Plain and crossed are chosen by the item, not by a click or toggle.

**Universal Trim** (`round_trim`, one item, one piece per face, since 0.9.0). One trim item for every
window and door; its icon is a flat square frame in the trim's look. The older Window Trim and
Door Trim items still work everywhere (see Upgrading).

*On a window* the trim fits onto any of the 26 windows (plain or crossed) and follows its shape: a
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

Holding a window item (any of the 26 window pieces) and aiming at a block face draws an overlay on
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
- Junctions: only a centred window grows glass, an arm across its plane towards a pane, bars, a
  wall or a centred window crossing it. Flush, floor and ceiling windows never connect, so two of
  them meeting at a corner or edge just meet frame to frame. Panes never link up or down.

## Restyling

| Aim at | Holding | Result | Cost |
|---|---|---|---|
| Door leaf | **Sneak** + planks | That wood; any paint removed | 1 plank |
| Door leaf | Dye (sneaking or not) | Painted, the wood kept underneath | 1 dye |
| Door corner | Block, not sneaking (planks included) | Camouflage, as above | 1 block, refunded later |
| Door corner | Sneak + planks or dye | Refused with a hint to aim at the leaf | nothing |
| Window frame or cross beam | Planks | That wood for the frame and beams; glass and trims kept | 1 plank |
| Window frame or cross beam | Dye | Frame and beams painted | 1 dye |
| Window glass (including the glass arms) | Dye | The whole window's glass tinted that dye (another dye replaces the tint) | 1 dye |
| Window trim band (the ring under a trim) | Planks | That wood for that face's trim (paint removed) | 1 plank |
| Window trim band | Bricks, stone bricks, deepslate bricks or mud bricks | Brickwork for that face's trim | 1 block |
| Window trim band | Dye | Painted trim (refused on brickwork, free) | 1 dye |
| Window trim band | Shears | That face's trim removed and returned as a Universal Trim; shears take one wear | none |
| Window face | Universal Trim (or an older trim item) | Fits a trim on the clicked face (nothing happens if it has one) | 1 trim |
| Arch sill | Planks, masonry or dye | Restyles that face's sill, like a trim band | 1 item |
| Arch sill | Shears | That face's sill removed and returned as an Arch Sill; shears take one wear | none |
| Upper arch face | Arch Sill | Fits a sill on the clicked face (nothing happens if it has one) | 1 sill |
| Any other window | Arch Sill | Refused with a hint | nothing |
| Window, anywhere but a trim band or sill | Bricks, stone bricks, deepslate bricks or mud bricks | Refused with a hint to aim at the trim | nothing |
| Door trim ring | Planks | That wood for that face's ring (also replaces masonry) | 1 plank |
| Door trim ring | Bricks, stone bricks, deepslate bricks or mud bricks | Radial brickwork, paint cleared | 1 block |
| Door trim ring | Dye | Painted (refused on brickwork, free) | 1 dye |
| Door trim ring | Shears | That face's ring removed and returned as an item; shears take one wear | none |
| Door | Universal Trim (or an older trim item) | Attaches a ring to the clicked face (nothing happens if it has one) | 1 trim |
| Archway (passage side, crown, or a face without a casing) | Planks, masonry or dye | That look for the whole archway (dye refused on brickwork, free) | 1 item |
| Archway face with a casing | Planks, masonry or dye | Restyles that face's casing | 1 item |
| Archway face | Shears | That face's casing removed and returned as a Universal Trim; shears take one wear | none |
| Archway | Universal Trim (or an older trim item) | Fits a casing on the nearer face (nothing happens if it has one) | 1 trim |
| Archway | Arch Sill | Refused with a hint | nothing |
| Freestanding frame (older saves) | **Sneak** + planks | That wood (also replaces masonry) | 1 plank |
| Freestanding frame | **Sneak** + bricks, stone bricks, deepslate bricks or mud bricks | Radial brickwork, paint cleared | 1 block |
| Wooden freestanding frame | Dye | Painted | 1 dye |
| Brick freestanding frame | Dye | Refused (masonry is not painted) | nothing |

- Windows, archways and door trims need no sneaking: a plain click does the same as a sneaking one.
  Only the door leaf and freestanding frames keep their sneak gestures.
- Aim decides on a window. There are four targets: the trim band (the ring under a trim, on the face
  you aim at), the arch sill, the frame (the wooden border, including the cross beams) and the
  glass. A trim band or sill only counts on a face that has one; the cross beams are frame, never
  glass or trim (under a sill they are the sill).
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
layout on the ground ahead of you, centred on the way you face (78 x 8 x 82 blocks, starting three
blocks ahead). It first clears that whole box to air and lays a stone floor, so use it on open
ground; it refuses if the box would leave the build limits, the world border or loaded chunks, and
never touches blocks outside it. The layout: every vanilla wood (and up to eight installed modded
planks) as doors closed and open on both hinges and as one connected window row; camouflaged doors;
window junction, stacked, waterlogged and stone-behind demos; a window positions section (centred and
the six flush positions, connected junctions, a solid-neighbour example, waterlogging) and a window
arches section (centred, flush, floor and ceiling pairs, single pieces, wood and paint examples);
a window corners section (2 x 2 arched windows, centred and flush, corner singles), a cross-beam
section (crossed pieces alone and in a grid) and a window trims section (every wood, paint and
masonry trim on each piece, both faces, front only, back only and differently styled faces, centred
and flush, plus a row of round windows flush against all six sides); since 0.10.0 full circles
(plain, crossed, trimmed), arch sills in several looks beside trims, and rectangles of top corners
and edges plus each edge alone; a lever-driven door; every
paint on a door with a painted door trim, on a tinted-glass window and on a painted window; every
wood and masonry door trim on a blue, a camouflaged oak and an open red door; and the legacy
freestanding frames (older saves). Since 0.11.0 an arched door section (oak closed and open on both
hinges, a lever-driven door, other woods and a paint, trims and camouflage) stands at r 40..49,
f 0..7. Since 0.12.0 an archway section: a row of archways and a row of keyhole archways behind it
(oak, spruce, dark oak, cherry, blue paint, the four masonry materials, a stone-brick front casing, and
different front and back casings), at r 26..58, f 35..41, between the round doors and the corners.

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
| Round window with cross beams | Rows `SPS` / `PGP` / `SPS` (S stick) |
| Arch with cross beams, left | Rows `PPS` / `PGS` / `P  ` |
| Arch with cross beams, right | Rows `PPP` / `SGS` / ` P ` |
| Corner with cross beams, bottom left | Rows `P  ` / `PGS` / `PPS` |
| Corner with cross beams, bottom right | Rows ` P ` / `SGS` / `PPP` |
| Arch, bottom left (0.10.0) | Rows `PP` / `PG` / `PP` |
| Arch, bottom right | Rows `PPP` / ` G ` / `PPP` |
| Corner, top left | Rows `PPP` / `PG ` / `P  ` |
| Corner, top right | Rows `PPP` / ` G ` / `  P` |
| Edge, top | Rows `PPP` / ` G ` |
| Edge, left | Rows `P ` / `PG` / `P ` |
| Edge, bottom | Rows ` G ` / `PPP` |
| Edge, right | Rows `GP` / ` P` / ` P` |
| Arch with cross beams, bottom left | Rows `PP ` / `PGS` / `PPS` |
| Arch with cross beams, bottom right | Rows `PPP` / `SGS` / `PPP` |
| Corner with cross beams, top left | Rows `PPP` / `PGS` / `PS ` |
| Corner with cross beams, top right | Rows `PPP` / `SGS` / `  P` |
| Edge with cross beams, top | Rows `PPP` / `SGS` |
| Edge with cross beams, left | Rows `PS` / `PG` / `PS` |
| Edge with cross beams, bottom | Rows `SGS` / `PPP` |
| Edge with cross beams, right | Rows `GP` / `SP` / `SP` |
| Archway (0.12.0) | Rows `PPP` / `P P` / `P P` (P any planks) |
| Keyhole Archway (0.12.0) | Rows `SPS` / `P P` / `P P` (S stick) |
| Universal Trim (`round_trim`) | 4 planks in the four corners of the grid |
| Arch Sill (`arch_sill_trim`) | Rows `PPP` / `S S` |

The new shapes are not the old ones turned over: a lower arch drawn like an upturned upper arch
would be a bottom corner's recipe, so each piece has its own. Shaped recipes also match mirrored, so
the left and right edges differ by where the pane sits.

Crafting always gives the oak object; restyle it in the world. With **EMI** and **JEI** each of the
32 items is listed once, as its plain oak look; styled stacks in your inventory are told apart by
their look (EMI comparison key, JEI subtype).
Neither viewer is required.

## Upgrading

**From 0.10.0 or 0.11.0:** saves load unchanged; the archways are new content. Nothing converts.

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
./gradlew runGameTestServer             # headless Forge server GameTests (fresh world each run)
git diff --check
```

`-PwithViewers=emi|jei|jei,emi` puts the optional viewers on the dev runtime; `-PpackModsDir=<mods
folder>` adds Double Doors, Collective and Dramatic Doors from a pack instance for compatibility runs.
The release jar is `build/libs/roundwood-1.20.1-<version>.jar`.

## Release

Not released yet. 0.5.0 is the baseline; 0.6.0 (targeted window placement and the arch pieces),
0.6.1 (no glass bridges on positioned windows, the door highlight follows the swing) and 0.7.0
(seamless clearer window glass, corner pieces, cross beams, window trims, thinner mortar) were
trialled by the owner, and so was 0.8.0 (a trim per window face, trim standing proud of flush
windows, tinted window glass, Door Trim attached to doors, plain-click gestures).

0.9.0 to 0.12.0 are built and tested headless (unit tests and server GameTests) and await one owner
trial together. Use a copy of a world saved in 0.8.0, run `/roundwood showcase` on open ground, and
check in game (the client-side retexturing and rendering have no automated test):

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
