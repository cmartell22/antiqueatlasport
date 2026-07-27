<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->
<center>
<b>Roleplayer's Atlas</b><br/>
A hand-drawn clientside atlas for roleplay servers – territories, roads, quill inscriptions, dated marks, and maps you sign and hand to other players.<br/>
<b>Requires <a href="https://modrinth.com/mod/surveyor">Surveyor Map Framework</a>.</b><br/>
</center>

---

A fork of [Antique Atlas 4](https://modrinth.com/mod/antique-atlas-4) by [Sisby folk](https://github.com/sisby-folk), ported to 1.21.8 and rebuilt around one idea: on a roleplay server a map isn't a HUD, it's a document. You draw it, you sign it, you date it, and you give it to someone who then knows the place only by your word until they go and see it for themselves.

Everything here is **client-side**. The server needs no mod – which is the point, since most roleplay servers run plugin cores.

**[M] opens the atlas in two stages.** Press it once and the book is drawn into your hands, held open in front of you: you keep walking, keep looking around, keep whatever is in your main hand, and the map is simply there, tilting with you. Press it again and the book is put away and the full page opens, where you drag to pan, scroll to zoom, and draw with the tools down the right edge. Press it a third time to close.

The first stage is the one you travel with. The second is the one you work on.

## What this fork adds

**Territories.** Paint an area chunk by chunk, give it a name, and it appears written across the land in the old cartographic style. Entering it can announce itself on screen.

**Roads and routes.** Click out a path and it's drawn as a curving dashed track with its name written along the curve. Optionally reports its length on hover.

**Quill inscriptions.** Free text written anywhere on the map, with no icon – for the things a place is called rather than the things that are there.

**Marker layers.** Your own layers with names and colours, filtered by tabs down the left edge. Deaths get their own automatically.

**Dating.** Every mark records the day it was drawn. The day comes from the world's *game* time, not its time of day, because a server can freeze the day cycle or hand each player their own – game time keeps ticking and is the same for everyone.

**Scrolls, signatures and hearsay.** Export any slice of your atlas – terrain, chosen markers, inscriptions, territories, routes – into an `.atlas` scroll and hand the file to another player. Signed scrolls carry your name and date. On the other side those marks arrive **faint**, filed into a layer of their own named after you, captioned *"As told by …"*. They stay faint until that player physically walks to the spot, at which point the mark firms up and records the day it was verified.

**Guide arrows.** Track any mark and an arrow points to it from your position with the distance. Arrives, drops itself, and chimes.

**The hearth.** Your respawn point, marked automatically, with its own button beside the player button. Read from the same packet the vanilla compass follows, so it works for beds, respawn anchors, `/spawnpoint` and plugins that set a real respawn point.

**Map snapshots.** Save the current view or the whole explored map – stitched from real in-game renders, so it's pixel-identical to what you see. Optionally framed like a hung item frame and signed with your name and the date.

**Notes and search.** Any mark can carry a note; the magnifier above the layer tabs searches both names and notes. The tab below it reorders the list: by kind, nearest first, newest first, or alphabetically.

**Biome corrections.** The atlas guesses at biomes it has no drawing for, and a datapack server gives it plenty to guess at. When a guess is wrong, overrule it: draw one biome as another everywhere, or correct named cells one at a time. Corrections off someone else's scroll are filed under their name, never overrule your own, and can be torn out whole.

**Hand-drawn towns.** The atlas draws villages the game tells it about; a town players built is invisible to it. So draw it yourself, from the same box of pieces the villages use: roads, crossroads, wells, houses, farms, market stalls, lamps. Nothing about the land underneath is changed.

**Undo and redo.** Twenty steps deep, across every drawing tool. Ctrl+Z and Ctrl+Y.

**Quick marks.** A key that marks a place without opening anything, either under your feet or wherever you are looking, so a peak on the horizon can be marked from the valley you are standing in.

Plus 53 marker icons and sounds for drawing, tracking and sealing.

## Configuration

`config/roleplayers-atlas.toml`, or in-game via [Mod Menu](https://modrinth.com/mod/modmenu). Death markers and grave styles, zone titles and their radius, guide arrow opacity, whether arrows drop on arrival, the hearth mark, fullscreen, remembered zoom and position, and how unexplored land is drawn.

With [Cloth Config](https://modrinth.com/mod/cloth-config) installed the settings screen is the searchable one, with a reset on every row. Both mods are optional; without them the config file still works.

Scrolls, layers and tracked marks live in `config/roleplayers-atlas/`.

Additional options are in the Surveyor config at `config/surveyor.toml`.

## Resource packs

Tiles, markers, biome detection and structure detection are fully data-driven through resource packs, unchanged from upstream. See the [resource pack tutorial](https://github.com/sisby-folk/antique-atlas/blob/1.20/RESPACKS.md).

Marker icons come in pairs – `name.png` and `name_accent.png`, where the accent layer is recoloured to whatever colour the player picked for that marker.

## Troubleshooting

Roleplayer's Atlas is a clientside frontend for [Surveyor Map Framework](https://modrinth.com/mod/surveyor). It renders Surveyor's save data as tiles and gives you the tools to draw on top.

Anything about the screen, tiles, markers, drawing tools or resource packs belongs in [this repository's issues](https://github.com/GlamArdor/roleplayers-atlas/issues). Anything about explored area, structure discovery or save data belongs in [Surveyor's](https://github.com/sisby-folk/surveyor/issues).

**Crash reports need `logs/latest.log` via [mclo.gs](https://mclo.gs/), with both mods on their latest version.**

## Credit

This is a fork, and almost all of the foundation is someone else's work.

Antique Atlas was created by [Hunternif](https://github.com/Hunternif), continued by [Kenkron](https://github.com/Kenkron), [asie](https://github.com/asiekierka) and [tyra314](https://github.com/tyra314), and rewritten as Antique Atlas 4 by [Sisby folk](https://github.com/sisby-folk). Surveyor Map Framework, which this mod cannot run without, is also theirs.

The map art is by [Hunternif](https://www.deviantart.com/hunternif) and [lumiscosity](https://lumiscosity.neocities.org/) – see [CREDITS](CREDITS) for the file-by-file breakdown, which also marks the icons drawn for this fork.

This fork is maintained by Glam_Ardor.

## Licence

LGPL-3.0-or-later for code, CC BY-NC-SA for assets – inherited from upstream and unchanged. Both are copyleft: anything adapted from this mod has to carry the same terms.
