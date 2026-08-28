# P8 integrated lifecycle probe

This independently locked client-side probe exercises the P8 integrated persistence and dimension
matrix without modifying production source or the P2 dependency lock. Its project repeats the exact
Minecraft 26.1.2, Modern Yarn, Loom, Fabric Loader/API, Kaleido, and Surveyor versions from
`docs/P2_BUILD_LOCK.md` and compiles against the main project's already-built classes.

The scored `seed` phase must run in a newly created ignored runtime copied from a verified control.
It joins `P8Auto`, waits for the existing Overworld data, creates dimension-scoped markers and
deterministic Atlas local-store controls, transitions Overworld to Nether to Overworld to End to
Overworld, disconnects, rejoins in the same process, and asserts persistence and isolation. It also
requires `WorldAtlasData.WORLDS` to clear between connections and emits stable checkpoint/pass
sentinels.

The `verify` phase starts a fresh JVM against the persisted disposable runtime and reasserts all
three dimension summaries, terrain counts, markers, local stores, and tracking before a normal stop.
P8-S02 requires three independent verify launches after the successful seed sequence.

The marker tracked by the probe is deliberately placed outside Atlas's arrival radius. A marker at
the player's feet is correctly untracked by the production `clearTrackingOnArrival` behavior and is
therefore not a valid persistence control.

Build the main project first, then build this probe offline:

```powershell
.\gradlew.bat -p compat-fixtures\p8\probe-project build --offline --no-daemon --console=plain --no-problems-report
```

The Loom run-directory override, runtime copies, compiled probe artifact, logs, and world output all
belong under ignored `build/` paths. Never point the probe at `compat-fixtures/p4` or
`compat-fixtures/p6`, and never treat an unscored failed harness runtime as input to a scored rerun.
