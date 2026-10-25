# Roundwood

Forge 1.20.1 mod: a round 2x2 wooden door and an arched 1x2 one, a round window with arch, corner and edge profiles with optional Cross Beams attachments, two open 2x2 archways, a Universal Trim for all of them and an Arch Sill, hobbit-hole style. Each is one item you
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

20 items: **Round Door**, **Arched Door**, thirteen plain window profiles
(**Round Window**, **Round Window Arch, Left** and **Right**, **Round Window Corner, Bottom Left**
and **Bottom Right**, and since 0.10.0 **Round Window Arch, Bottom Left** and **Bottom Right**,
**Round Window Corner, Top Left** and **Top Right**, and **Round Window Edge, Top**, **Left**,
**Bottom** and **Right**), the **Archway** and
**Keyhole Archway** (0.12.0), the **Universal Trim**, **Arch Sill** and **Cross Beams**. Creative tab order: round
door, arched door, then each plain window profile (round, arch left,
arch right, corner bottom left, corner bottom right, arch bottom left, arch bottom right, corner top
left, corner top right, edge top, left, bottom, right), archway, keyhole archway, Universal Trim,
Arch Sill, Cross Beams. They are crafted (and found in the creative tab) in oak;
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
32 x 32 pixels outside, with filled square corners around the curved opening (0.13.2).
- *Archway*: two-pixel jambs and crown; the passage is 28 pixels wide (1.75 blocks) under a radius-14
  crown.
- *Keyhole Archway*: the same crown, but below the spring line each jamb steps in four pixels over
  eight (one pixel every two rows), leaving a 20-pixel (1.25 blocks) lower passage.
- Walking through (0.13.1): only the lower jambs collide, and the crown never does, so the opening is
  clear to the full two blocks for a player and for tall mobs such as villagers and zombies (the 0.12.0
  crown left 1.875 blocks, which a 1.95-block mob could not pass). The crown and filled upper corners
  remain targetable and outlined where you aim, but do not collide.
- Placement: an ordinary click; the cell you target (or the replaceable cell you click) becomes the
  **lower-left** cell seen from where you stand, and the archway extends one block to your right and
  one up, facing you (four facings, always upright, no hinge). All four cells must be empty or
  replaceable, dry, inside the world and border, loaded and buildable by you, with a solid floor
  under both lower cells, and no mob where the frame would stand (someone in the passage is fine).
  Holding the item previews the frame outline (the real opening) and the four cells; nothing is
  drawn where placement would be refused. A refused or cancelled placement keeps the item and
  restores every cell.
- You walk through the passage; only the lower jambs collide. Light and neighbours show
  through.
- Looks: any planks, any dye, or bricks, stone bricks, deepslate bricks or mud bricks. Since 0.13.3,
  the body and fitted casings tile the donor block texture on every face, including the passage;
  paint uses tiled painted boards. Vertical faces keep horizontal plank rows. A plain click with planks, masonry or
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

**Cross Beams** (`cross_beams`, 0.13.0). One attachment fits any of the thirteen window profiles.
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
layout on the ground ahead of you, centred on the way you face (90 x 8 x 28 blocks, starting three
blocks ahead; it was 82 x 8 x 28 in 0.13.0, with the samples two columns apart). It first clears that whole box to air and lays a stone
floor, so use it on open ground; it refuses if the box would leave the build limits, the world border
or loaded chunks, and never touches blocks outside it. The layout is one of each thing, not every
combination. From the front row to the back:

- **One of each placeable piece**, oak, default state: the thirteen windows, the round door, the arched
  door (both closed) and the ordinary and keyhole archways, in a single row.
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
| Archway (0.12.0) | Rows `PPP` / `P P` / `P P` (P any planks) |
| Keyhole Archway (0.12.0) | Rows `SPS` / `P P` / `P P` (S stick) |
| Cross Beams (`cross_beams`) | Five sticks: rows ` S ` / `SSS` / ` S `; unlocked by obtaining a stick |
| Universal Trim (`round_trim`) | 4 planks in the four corners of the grid |
| Arch Sill (`arch_sill_trim`) | Rows `PPP` / `S S` |

The new shapes are not the old ones turned over: a lower arch drawn like an upturned upper arch
would be a bottom corner's recipe, so each piece has its own. Shaped recipes also match mirrored, so
the left and right edges differ by where the pane sits.

Crafting always gives the oak object; restyle it in the world. With **EMI** and **JEI** each of the
20 items (two doors, 13 plain windows, two archways, Universal Trim, Arch Sill and Cross Beams) is
listed once, as its plain oak look, and nothing cycles; the retired crossed window items stay hidden.
Styled stacks in your inventory are told apart by their look (EMI comparison key, JEI subtype).
Neither viewer is required.

## Upgrading

**From 0.12.x and earlier:** crossed windows become plain profiles with one Cross Beams attachment
in their former frame look. Their frame, glass, pose, water, trims and sills survive. Old `_cross`
stacks keep their IDs, counts and saved contents in inventories and chests; placing one installs the
plain pane and beams together. Unreadable beam data keeps the old block or stack intact. Hidden
compatibility IDs remain registered, but their recipes and creative/viewer entries are retired.

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
- [ ] Fit preview equals the result on all 13 profiles and placement families, from either side.
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
