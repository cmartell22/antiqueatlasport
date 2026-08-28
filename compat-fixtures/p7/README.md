# P7 automated feature-parity probes

These probes exercise the compiled production Atlas in disposable Minecraft 26.1.2 runtimes. They
are test harnesses, not production resources, and are pinned independently to the exact P2 lock.

`P7TerrainProbe` is the P7-S01 exploration/terrain/biome/structure-input slice. The main Atlas
development client and the probe mod run together against a disposable copy of the target P6test
world. The probe waits for a real integrated-world join and Atlas terrain completion, then checks:

- current Surveyor summary, terrain, and exploration are present;
- Atlas resolved at least sixteen explored chunks with non-null tiles, biome IDs, and provider IDs;
- the visible scope produces non-empty batched tile geometry;
- required vanilla provider looks survived resource loading;
- the blob-identical name-fallback model returns its P0-defined representative results;
- the current Surveyor structure store accepts the discovered-structure query surface.

The probe prints `P7_TERRAIN_PROBE_PASS` and requests a normal client stop. It does not score visual
appearance or user interaction. Frozen P4/P6 fixtures are never modified.

`P7AnnotationProbe` is the P7-S03 data/model slice. After the terrain assertions pass, it uses
production Atlas and Surveyor paths to create, edit, undo, redo, classify, and remove a marker;
exercise layer visibility and tracking; verify route sampling/length, inscription and territory
classification, biome correction/patch, city paint, and hearth presence; restore every captured
state; then print `P7_ANNOTATION_PROBE_PASS`.

Build the main classes and probe with the exact lock, then place the remapped probe jar in the
disposable client's `mods` directory. The ignored init script used by the recorded run resolves to
`build/p7-s01-client-r3`, supplies no program arguments, and the probe invokes Minecraft 26.1.2's
exact public `QuickPlay.startQuickPlay` path after confirming the copied `P7Auto` world exists.

```powershell
$fallback = 'C:\wawi-gradle-uds-nonexistent'
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$fallback"
.\gradlew.bat compileJava --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p7\probe-project build --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -I build\p7-tools\p7-s01.init.gradle runClient --offline --no-daemon --console=plain --no-problems-report
```
