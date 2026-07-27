# Roleplayer's Atlas

**A hand-drawn atlas for roleplay servers.** Territories, roads, quill inscriptions, dated marks, and maps you sign and hand to another player.

A fork of [Antique Atlas 4](https://modrinth.com/mod/antique-atlas-4) by [Sisby folk](https://modrinth.com/user/sisby), rebuilt around one idea: on a roleplay server a map isn't a HUD, it's a document. You draw it, you sign it, you date it, and you give it to someone who then knows the place only by your word – until they go and see it for themselves.

**Client-side only.** The server needs no mod, which is the point: most roleplay servers run plugin cores you can't add mods to. [Surveyor Map Framework](https://modrinth.com/mod/surveyor) rides inside the jar, so there is nothing else to install.

---

## The book opens in two stages

Press **[M]** once and the book is drawn into your hands, held open in front of you. You keep walking, keep looking around, keep whatever is in your main hand, and the map is simply there, tilting as you turn. This is the stage you travel with.

Press it again and the book is put away and the full page opens, where you drag to pan, scroll to zoom, and draw with the tools down the right edge. This is the stage you work on.

A third press closes it.

---

## Drawing on the world

**Territories.** Paint an area cell by cell, name it, and the name appears written across the land in the old cartographic style. Walking in can announce it on screen. Redraw the bounds later without starting again.

**Roads.** Click out a path and it becomes a curving dashed track with its name written along the curve, and its length on hover. Pick an old road back up at its far end and carry it on.

**Quill inscriptions.** Free text anywhere on the map, no icon – for what a place is called rather than what stands there.

**Hand-drawn towns.** The atlas draws villages the game tells it about. A town players built is invisible to it, so draw it yourself, from the same box of pieces the villages use: roads, crossroads, wells, houses, farms, market stalls, lamps. Nothing about the land underneath is changed, and any of it lifts again.

**Biome corrections.** The atlas guesses at biomes it has no drawing for, and a datapack server gives it plenty to guess at. When a guess is wrong, overrule it – draw one biome as another everywhere, or correct named cells one at a time.

**Undo and redo,** twenty steps deep, across every drawing tool.

---

## Marks that are worth something

**Dating.** Every mark records the day it was drawn. The day comes from the world's *game* time rather than its time of day, because a server can freeze the day cycle or hand each player their own. Game time keeps ticking and reads the same for everyone.

**Layers.** Your own layers with names and colours, filtered by tabs down the left edge. Deaths get one automatically.

**Notes and search.** Any mark can carry a note, and the magnifier searches names and notes alike. The tab below reorders the list: by kind, nearest first, newest first, or alphabetically.

**Place names on screen.** Come near a named mark and its name fades in over the world, the way vanilla announces a biome. How near is set per mark or once for all of them, with an optional chime. Territories announce by standing inside them, and the smallest one containing you wins – walk into a duchy inside a kingdom and it is the duchy you are told you are in. Marks on hidden layers stay quiet, and any mark can have it switched off on its own.

**Quick marks.** One key marks a place without opening anything – under your feet, or wherever you are looking, so a peak on the horizon can be marked from the valley you stand in.

**Guide arrows.** Track a mark and an arrow points at it with the distance. It drops itself when you arrive.

**The hearth.** Your respawn point, marked automatically, read from the same packet the vanilla compass follows. Works with beds, respawn anchors, `/spawnpoint`, and plugins that set a real respawn point.

---

## Maps you hand over

Export a slice of your atlas – terrain, chosen marks, inscriptions, territories, roads, your biome corrections and your towns – into an `.atlas` scroll, and give the file to another player.

Signed scrolls carry your name and the date. On the other side those marks arrive **faint**, filed into a layer named after you, captioned *"As told by …"*. They stay faint until that player has physically walked to the spot, at which point the mark firms up and records the day it was seen.

Your corrections and towns land as a sheet under your name. That sheet never overrules anything the reader drew themselves, and it can be torn out whole. Before any of it is written, the reader sees exactly what the scroll holds and can decline.

**Snapshots to a PNG.** The map also comes out of the game as an image, written to your `screenshots` folder like any other. Two kinds: the page as you are looking at it, or the whole explored world stitched together at full tile resolution – and that second one is a poster rather than a screenshot, running to tens of thousands of pixels on a side for a well-travelled world. Either can be framed like a hung item frame and signed with your name and the date. Post it, print it, hang it in a Discord.

---

## Settings

`config/roleplayers-atlas.toml`, or in-game through [Mod Menu](https://modrinth.com/mod/modmenu). With [Cloth Config](https://modrinth.com/mod/cloth-config) the settings screen is the searchable one; without either, the file still works and a plainer screen is built in.

Elevation tiers, unexplored land, death markers and their wording, place names on screen, guide arrows, the hearth, the quick-mark key and its icon, fullscreen, remembered zoom and position.

Scrolls, layers, corrections and tracked marks live in `config/roleplayers-atlas/`.

---

## Credit

This is a fork, and almost all of the foundation is someone else's work.

Antique Atlas was created by [Hunternif](https://github.com/Hunternif), continued by [Kenkron](https://github.com/Kenkron), [asie](https://github.com/asiekierka) and [tyra314](https://github.com/tyra314), and rewritten as Antique Atlas 4 by [Sisby folk](https://modrinth.com/user/sisby). [Surveyor Map Framework](https://modrinth.com/mod/surveyor), which this cannot run without, is theirs as well.

The map art is by Hunternif and [lumiscosity](https://lumiscosity.neocities.org/) – the [CREDITS](https://github.com/GlamArdor/roleplayers-atlas/blob/main/CREDITS) file names every texture and who drew it.

LGPL-3.0-or-later for code, CC BY-NC-SA for assets, inherited from upstream and unchanged. Both are copyleft: anything adapted from this has to carry the same terms.

[Source and issues on GitHub.](https://github.com/GlamArdor/roleplayers-atlas)
