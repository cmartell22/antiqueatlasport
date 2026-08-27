# P4 Surveyor compatibility fixtures

These files are byte-for-byte copies of the ignored P0 control data. They are immutable inputs for
Surveyor 1.2.1+1.21.11 to Surveyor 1.2.4+26.1 compatibility probes. Tests must copy them to a
disposable directory before attempting to read, migrate, or write them.

## Atlas scrolls

| Fixture | P0 source | Bytes | SHA-256 |
|---|---|---:|---|
| `atlas/singleplayer.atlas` | `run/p0-integrated/config/roleplayers-atlas/scrolls/P0ExportTest.atlas` | 926917 | `FAB249FCC170BBE1D50A54A96B634220133BC54C13B8FD78C4E62E853A53D9C2` |
| `atlas/multiplayer-author-player271.atlas` | `run/p0-client/config/roleplayers-atlas/scrolls/p0 player a.atlas` | 131955 | `FA711A577D70FB6D2CD577855CF6FDBA0BF09471145BA7BCCB5A72CCE78D34DE` |

The singleplayer scroll is the P0 all-dimension/export-import control. The multiplayer scroll was
authored by Player271 and was imported by the clean Player777 client during the P0 authorship,
hearsay, additive-import, and layer-visibility control.

## Surveyor persistence trees

| Fixture tree | P0 source | Files | Bytes | Tree digest |
|---|---|---:|---:|---|
| `surveyor/singleplayer/overworld` | `run/p0-integrated/saves/New World/data/surveyor` | 14 | 1258004 | `3F1E8390967228BFCADFC38139E88CB91E9FA4572CF3C68944001CE74C965525` |
| `surveyor/singleplayer/nether` | `run/p0-integrated/saves/New World/DIM-1/data/surveyor` | 9 | 769823 | `60319A16482741F8529466CB8E5A314D1F3A724F010489BDC0DC44C8D328597D` |
| `surveyor/singleplayer/end` | `run/p0-integrated/saves/New World/DIM1/data/surveyor` | 9 | 64003 | `8E0F12A1A911A095721649842F4875B10F04DDFE935F4CBED8DF481E41D0DA1E` |
| `surveyor/dedicated-server/world` | `run/P0 Dedicated Baseline/data/surveyor` | 8 | 201374 | `FC58B4514E22B705E63EA9DE1A622F0C0792C0715912F396876264D4BE91B275` |
| `surveyor/multiplayer-client-b/client-data` | `run/p0-client-b/data/surveyor` | 7 | 142659 | `9C92A23AB1FF6DBC27839F0C611DCA99680362B5607C39EB134E400EB6D3C696` |

A tree digest is SHA-256 over the UTF-8, LF-separated sequence of
`SHA256  relative/path` records, sorted by relative path and with `/` separators. The copied source
and destination tree digests matched when this fixture set was created.

The singleplayer trees cover Overworld, Nether, and End exploration, structures, and landmarks. The
dedicated-server tree is the clean-stop/restart multiplayer control. The client-B tree contains the
separate Player777 client state used to verify independent knowledge plus imported Player271 hearsay.

## Safety rule

Never run a migration or round trip in place. Verify the recorded digest first, copy the selected
fixture into a disposable test directory, operate only on that copy, and verify the tracked fixture
is unchanged afterward.

## Exact target-runtime probe

`probe-project` is a dedicated-server Fabric/Loom harness pinned independently to the exact P2
lock. It copies this complete fixture tree to an operating-system temporary directory before any
decode or write, starts Minecraft 26.1.2 with Fabric Loader transformations active, and loads the
unchanged production `MixinRegionStructureSummary` compatibility shim. It does not compile or load
the rest of Atlas, whose P5 renderer is deliberately still outside the P4 boundary.

Run from the repository root with the host's documented Java socket fallback:

```powershell
$fallback='C:\wawi-gradle-uds-nonexistent'
$env:JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$fallback"
.\gradlew.bat -p compat-fixtures\p4\probe-project runP4Probe --offline --no-daemon --console=plain --no-problems-report
```

The passing probe requires the `P4_COMPATIBILITY_PROBE_PASS` sentinel and Gradle exit zero. It
target-decodes and exactly re-encodes landmark NBT and every frozen region packet, performs target
NBT read/write content round trips, exercises `WorldLandmarks.CODEC`, all persisted
`ChunkSummary` values, `RegionStructureSummary` with Atlas's existing jigsaw fix, and client
personal/shared `SurveyorExploration` state.
