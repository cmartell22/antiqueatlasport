# P6 Atlas-owned local-store fixtures

These are LF-normalized semantic snapshots of the five JSON stores produced by the P0 integrated
control world `sp:P0 Dedicated Baseline`. Their source paths were under
`run/p0-integrated/config/roleplayers-atlas/`; the ignored runtime tree is not a durable test input, so
P6 freezes its content here before production edits.

| Fixture | P0 source bytes | P0 source SHA-256 |
| --- | ---: | --- |
| `local-stores/biomes.json` | 106 | `3A6FD2F2DEBA4D22D910DAB230A38E451A8F2E584B7708D94E13AE25A4F806FE` |
| `local-stores/cities.json` | 471 | `F14851B92531F48453A996FCD868527F62D7828D455D5EB117B1B2664BB68BC8` |
| `local-stores/hearth.json` | 63 | `EE000CB801E44D9A1AC1FCB33B3F4DD26DCA5A7C1831202B6009B1F511E37510` |
| `local-stores/layers.json` | 36 | `FBA2E7B8529B572C54430F4666D1A42219BFEDEB5961F1444F48A71DEC5A0794` |
| `local-stores/tracked_markers.json` | 36 | `FBA2E7B8529B572C54430F4666D1A42219BFEDEB5961F1444F48A71DEC5A0794` |

The byte hashes describe the CRLF-bearing P0 source files; normalized Git-blob hashes are recorded in
the table below:

| Fixture | Committed bytes | Committed SHA-256 |
| --- | ---: | --- |
| `local-stores/biomes.json` | 107 | `7AC51C9BDF0402D54B0A428EE4F78229CCA51FF13474EFE881CF12FD885AE7F4` |
| `local-stores/cities.json` | 472 | `0B1E5BC4A83C2DD991D49738623055187FF4AC4F2755216C813F3440F9E1CE2F` |
| `local-stores/hearth.json` | 64 | `7B6A3F401A6CFBE330B5897D7941C87CEB1CD81BFD835F9BFDC19772B853F4D3` |
| `local-stores/layers.json` | 37 | `B813590481756D9EFC41C86F69C0939CA2C6AE7B9B9420390ABED841A7C2BDB2` |
| `local-stores/tracked_markers.json` | 37 | `B813590481756D9EFC41C86F69C0939CA2C6AE7B9B9420390ABED841A7C2BDB2` |

Tests must copy these files to a disposable directory and must never save over the tracked fixtures.

`.atlas` and Surveyor binary fixtures are not duplicated here. P6 reuses the immutable files and
hashes already recorded in `compat-fixtures/p4/README.md`.

## Exact target serialization probe

The server-side Fabric/Loom harness in `probe-project` is independently pinned to the exact P2
Minecraft, mappings, loader, Fabric API, Surveyor, and Kaleido versions. It uses the already-compiled
target production `MapShare` class, reads only the immutable P4/P6 fixtures, and performs every write
in an operating-system temporary directory.

From the repository root, compile production first, then run:

```powershell
$fallback='C:\wawi-gradle-uds-nonexistent'
$env:JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$fallback"
.\gradlew.bat compileJava --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p6\probe-project runP6Probe --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p6\probe-project runP6ClientProbe --offline --no-daemon --console=plain --no-problems-report
```

Acceptance requires `P6_SERIALIZATION_PROBE_PASS` and Gradle exit zero. The probe directly exercises
`MapShare.peek` against the frozen P0 scroll; verifies bounded null/foreign-dimension previews for
garbage, truncated, and valid dimensionless scrolls; checks the five local-store schemas and semantic
JSON round trips; and verifies every tracked input digest is unchanged afterward. Because
`MapShare.importFile` is client-coupled through its undo/screen path, the separate minimal client probe
executes two `read_failed` cases and one `wrong_dimension` case in temporary files. It exits immediately
after the sentinel and does not enter a world or alter tracked inputs.
