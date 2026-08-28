# P9 faithful compatibility release-candidate matrix

This matrix is the exhaustive release-candidate gate for the faithful Roleplayer's Atlas / Antique
Atlas 4 compatibility port to Minecraft 26.1.2. It consolidates the P0 behavioral controls and the
P3-P8 target evidence without authorizing WAWI integration, feature redesign, identity changes, a
Surveyor fork, or publication.

## Authority and immutable controls

- P9 approval checkpoint: `7db1361e10e3c78117ffc679b33c689d4ade8ecd`
  (`APPROVAL-P9-001`), YAML only.
- Exact tag authority: `APPROVAL-P9-TAG-001`, recorded after the missing criterion was discovered
  and work stopped. The only authorized candidate tag is local annotated tag
  `v82.rc.1-26.1.2` with exact message
  `Release candidate for faithful repro of AA4 on MC v26.1`.
- P8 completion control: `2a741138c9001e977e88351f3bee46c449d6b2c6` on
  `port/26.1.2`.
- P0 behavior control: annotated tag `p0-baseline-1.21.11`, peeled target
  `7099b4df2420d5441e3b2b02e53201a0b249c8f9`; it must not move.
- Locked design: `design-doc.txt`, SHA-256
  `1FC133AE82AECD1CA5728B80015C66A4D32A0C1C30F107EE73EC990520B24079`.
- Exact target lock: Java 25, Gradle 9.5.1, Loom remap 1.17.19, Modern Yarn
  26.1.2+build.4, Minecraft 26.1.2, Fabric Loader 0.19.3, Fabric API
  0.155.2+26.1.2, Surveyor 1.2.4+26.1, Kaleido 0.3.3+1.3.2, Cloth
  26.1.154, and Mod Menu 18.0.0.
- Exact source pins: Roleplayer's Atlas
  `dab58dae0db912e91df8fbc2f6c52768e16dc742`, legacy Surveyor
  `39847c28f925900ac65f12a0a22032a3150a139f`, and target Surveyor
  `4b500855ea2e316480c1f3fb9e175ec6dc3060db`.
- Frozen inputs: both P4 `.atlas` files, all five P4 Surveyor persistence trees, and all five P6
  Atlas local stores. Every runtime operates only on verified disposable copies.
- Publication boundary: no push, release, upload, publish task, or remote tag operation is
  authorized.

## Exhaustive regression matrix

| Slice | Coverage derived from P0 and P3-P8 | Required execution and evidence | Acceptance |
| --- | --- | --- | --- |
| P9-S01 entry and immutable controls | Branch/HEAD cleanliness; revision-80 P0-P8 completion; P2 lock; design; P0 tag; source pins; frozen P4/P6 controls; no upstream/publication | Recompute every file/tree digest, inspect tag object/peel, diff every P2 lock file from its lock commit, verify source objects and repository configuration | Every immutable value is exact before and after all P9 work |
| P9-S02 compile, static, resource, and mixin boundary | P3 mechanical APIs; P5 renderer/mixin changes; P6 v1 reloaders and resource inventory | Exact-lock offline `clean build`; forbidden/stale API audit; seven-mixin config/target audit; strict parse of 550 JSON and 358 metadata files; 457 PNG signatures and metadata targets | Build succeeds with tests `NO-SOURCE`; inventories and static audits contain no unexplained delta |
| P9-S03 Surveyor, serialization, persistence, and failure paths | P4 packet/persistence codecs and jigsaw shim; P6 Atlas stores and malformed import paths | Run P4 target-runtime probe and P6 server/client probes from disposable copies; require their PASS sentinels; recheck packet bytes, NBT/content round trips, local-store semantics, garbage/truncated/wrong-dimension failures, and source immutability | All positive round trips and bounded negative cases pass; frozen inputs remain exact |
| P9-S04 exploration, rendering model, annotations, and outputs | P7 terrain/biome/structure inputs, batching, markers/layers, hearth/death model, inscriptions, territories/cities, routes, undo/tracking, `.atlas`, hearsay, and PNG | Run the P7 terrain, annotation, and sharing probe modes in a fresh ignored integrated runtime; require all PASS sentinels and normal all-dimension save/stop | Target terrain/providers/batches and annotation/share/output models match controls with no temporary source mutation |
| P9-S05 integrated lifecycle and dimensions | P0 lifecycle race; P8 Overworld/Nether/End isolation, same-process reconnect, and fresh-JVM persistence | Run a fresh P8 seed sequence, same-process disconnect/rejoin, Overworld -> Nether -> Overworld -> End -> Overworld, then three independent fresh-JVM verify launches | Stable counts/identities/stores in every run; `WorldAtlasData.WORLDS` clears; no CME, protocol, codec, or dimension leak |
| P9-S06 repeated import, ownership, and hearsay | P0 Player271/Player777 control; P8 duplicate import, grouped undo/redo, attribution, source layer, corrections/cities, restart | Run fresh P8 import-seed and import-verify against hash-checked disposable copies of both P4 scrolls | One stable receiver-owned hearsay marker/layer; additive terrain; stable correction sheets; source bytes unchanged across restart |
| P9-S07 dedicated synchronization, restart, and listener lifecycle | P0 dedicated baseline; P7 basic sync; P8 Alice reconnect and server persistence | Run retained-stdin dedicated server with `globalSharing=false`, connect fresh Alice, synchronize/author/export/disconnect, literal `stop`, restart, and fresh-JVM reconnect | Server/client state and owner editability persist; every server stops through literal `stop`; Gradle succeeds and ports/processes release |
| P9-S08 two-client isolation and Surveyor policy | P0 independent knowledge/hearsay; P8 Alice/Bob isolation; clean target `globalSharing=true` default | Complete false-policy Alice/Bob import/restart/reconnect flow, then a separate clean-default true-policy A/B flow | False preserves isolation and receiver-owned hearsay; true directly shares with GROUP edit rights and no import layer; Atlas never overrides policy |
| P9-S09 runtime negative and cleanup audit | P0 CME/protocol failure; P6 malformed inputs; P8 scored-log exclusions | Search every scored log for CME, Network Protocol Error, codec/decoder/encoder failure, probe assertion, mixin/classloading failure, crash, and unclean stop; enumerate Java/javaw and test listeners | No unexplained S2/S3 signature; no test JVM or listener remains |
| P9-S10 owner-observed client release check | P0/P5-P8 rendering, input, F3+T, GUI scale, dimensions, import/hearsay, and visual fidelity | **Pause first for separate readiness confirmation.** In one disposable candidate client, observe handheld/fullscreen/closed cycle, movement/look, pan/zoom, resize, GUI scale, pause/resume, F3+T, all four Atlas reloaders, representative terrain/structure/annotations, dimensions/returns, import preview/attribution/layer, and normal save/quit | Owner reports no visual/input/reload/persistence regression; logs corroborate reload and normal all-dimension shutdown |
| P9-S11 genuinely clean-checkout build | P0/P2 reproducibility and final candidate commit | Clone the local repository into a new short temporary path without local hardlinks, check out the exact candidate commit with no generated/build/run data, set only documented Java 25 and host TCP fallback, and run offline wrapper `clean build` | Clean clone is unmodified before/after build except ignored outputs; wrapper checksum and exact lock resolve solely from documented caches/prerequisites; build succeeds |
| P9-S12 main, sources, embedded Surveyor, metadata, and legal/package audit | P0 artifact audit and RISK-007 | Hash and expand final main/sources jars; inspect duplicate/path traversal entries, `fabric.mod.json`, mixins, resources, nested Surveyor bytes/metadata, root and embedded licenses/notices/credits, source provenance, and absence of probe/test/runtime output | Main/sources metadata and content are coherent; nested Surveyor is exact locked bytes; licenses/notices/credits are distribution-complete; RISK-007 is closed or P9 stops |
| P9-S13 final scope, invariant, process, listener, and publication audit | INV-001 through INV-014; all phase boundaries | Review approval-to-candidate diff and commit categories; verify no P2 lock, identifier/format, gameplay, WAWI, Surveyor-policy, fixture, design, P0-tag, upstream, publication, process, or listener violation | Every invariant passes, all scored artifacts/hashes are recorded, repository is clean, and no unexplained S2/S3 remains |
| P9-S14 candidate tag and P10 stop | Exact owner tag authority and separate P10 gate | Only after P9-S01 through P9-S13 pass, create local annotated `v82.rc.1-26.1.2` with exact authorized message, verify tag object/message/peeled target, and do not push | Tag is local, annotated, exact, and targets the final P9 completion commit; state records one next action requiring separate P10 approval |

## Command families

All Gradle commands use the installed Microsoft OpenJDK 25.0.4+7 and the documented host fallback:

```powershell
$fallback = 'C:\wawi-gradle-uds-nonexistent'
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$fallback"
.\gradlew.bat clean build --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p4\probe-project runP4Probe --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p6\probe-project runP6Probe --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p6\probe-project runP6ClientProbe --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p7\probe-project build --offline --no-daemon --console=plain --no-problems-report
.\gradlew.bat -p compat-fixtures\p8\probe-project build --offline --no-daemon --console=plain --no-problems-report
```

Integrated and dedicated client/server launches use ignored early Loom init scripts and fresh short
run directories under `build/`. Dedicated servers retain stdin and terminate only through
Minecraft's literal `stop` command. The exact launch command, phase/mode, run directory, identity,
port, elapsed time, sentinel, log path/hash, and exit result are recorded in durable state.

The genuinely clean-checkout gate uses a local no-hardlink clone at the final candidate commit and
does not copy `build/`, `run/`, IDE state, Gradle project state, or any prior runtime. Network access
is not permitted; only the documented installed JDK, wrapper distribution, and already-resolved
locked caches may be used.

## RISK-007 disposition rule

Artifact compliance is assessed before changing packaging. If the final artifacts omit a required
license, credit, notice, or embedded-dependency attribution, P9 may change only the narrowest archive
packaging configuration needed to include the existing repository legal files. No legal text,
dependency, source behavior, gameplay, identifier, format, art, resource content, or publication
configuration may be redesigned. The correction receives its own commit, clean build, expanded-jar
inspection, and clean-checkout reproduction. Any broader requirement stops for owner approval.

## Stop rules

1. Stop immediately on data loss/corruption, cross-player leakage outside the selected Surveyor
   policy, unrecoverable world damage, or any unexplained S2/S3 result.
2. Diagnose from exact pinned source before any repair. One demonstrated defect permits only one
   narrow categorized correction plus its smallest practical regression.
3. Never write to frozen P4/P6 controls. Discard an entire runtime after an unscored harness error.
4. Do not change the P2 lock. A demonstrated lock defect is recorded and work stops for separate
   approval.
5. Automated clients are allowed. Stop before P9-S10 until the owner separately confirms readiness.
6. Do not create the RC tag until every prior row passes and the final completion commit is clean.
7. Do not enter P10/P11 or push, release, upload, or publish anything.
