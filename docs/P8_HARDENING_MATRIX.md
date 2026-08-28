# P8 persistence, lifecycle, dimension, and multiplayer hardening matrix

This matrix converts the complete P0 lifecycle/multiplayer controls and the passing P7 target feature
evidence into adversarial P8 slices. It is not a redesign backlog. Tests use disposable runtime
copies, preserve Surveyor as the authoritative backend, and stop on unexplained S2 or any S3 result.

## Entry controls

- Approval checkpoint: `b5f9e570c` (`APPROVAL-P8-001`), YAML only.
- P7 completion control: `b7c5a06c50b13bade267836b236a142b75b7086c` on `port/26.1.2`.
- P0 behavior control: annotated tag `p0-baseline-1.21.11`, peeled target
  `7099b4df2420d5441e3b2b02e53201a0b249c8f9`.
- Roleplayer's Atlas source pin: `dab58dae0db912e91df8fbc2f6c52768e16dc742`.
- Target Surveyor source pin: `4b500855ea2e316480c1f3fb9e175ec6dc3060db`.
- Target Minecraft/Fabric/Surveyor lock: `docs/P2_BUILD_LOCK.md`; P2 lock files remain unchanged.
- Immutable controls: `compat-fixtures/p4` and `compat-fixtures/p6`. Every test verifies a source
  digest, copies the selected input, and writes only to an ignored disposable runtime.
- Existing target evidence: P4 exact codec/persistence round trips, P6 local-store and malformed-input
  probes, P7 feature probes, one basic client/server synchronization, and the complete owner-observed
  P7 feature matrix.

## Exact-source lifecycle facts

- Current Surveyor `SurveyorClient.getWorldSavePath` separates client data by biome seed and dimension.
- Integrated play resolves summaries from server worlds and uses the server-host UUID; dedicated play
  merges distinct client personal/shared exploration and obtains the exact server-visible profile UUID.
- Initial load enumerates every server-advertised dimension and invokes terrain, structure, and
  landmark updates through Surveyor's normal client event path.
- `SurveyorConfig.Networking.globalSharing` defaults to `true` at the exact target pin. The faithful
  P0 isolation control explicitly uses `false`; Atlas must not silently override this operator-owned
  setting.
- Atlas defines no independent networking protocol. Persistence, map summaries, landmarks,
  synchronization, visibility, and ownership continue through current Surveyor.
- Atlas' `WorldAtlasData.WORLDS` remains the P0-stabilized `ConcurrentHashMap`; repeated join/disconnect
  tests retain the original race reproduction as a required negative assertion.

## Bounded slices

| Slice | Control and risk | Automated evidence required | Human observation | Boundary |
| --- | --- | --- | --- | --- |
| P8-S01 immutable controls and state digests | P4/P6 data must never be migrated in place | Verify every recorded scroll/store hash and five Surveyor tree digests; create disposable runtimes and deterministic before/after manifests | None | No packaging or release audit |
| P8-S02 integrated lifecycle repetition | P0 exposed an intermittent join/disconnect `ConcurrentModificationException` | At least three fresh-JVM join/save/quit cycles plus same-process world rejoin; assert terrain, structure, landmark, annotation/store counts and no CME/protocol failure | Final focused persistence confirmation only if automated state is clean | No long-duration performance soak |
| P8-S03 dimensions and return transitions | P0 passed Overworld/Nether/End; target needs adversarial persistence | Create or identify dimension-specific terrain/marker controls, transition Overworld → Nether → Overworld → End → Overworld, save, restart, and assert each dimension retains only its own knowledge | Confirm each dimension and return map looks correct after readiness approval | No custom/WAWI dimensions |
| P8-S04 repeated import and restart | Sharing is additive and source-attributed; repeated lifecycle may duplicate or mutate | Import a copied P0/P7 scroll twice, verify stable landmark identity and source layer, grouped undo semantics, corrections/city ownership, source-byte nonmutation, then restart and reassert | Confirm preview/attribution/layer behavior after readiness approval | No hostile-size/security fuzzing beyond recorded malformed cases |
| P8-S05 dedicated restart and reconnect | P7 used one client and one server lifecycle only | Connect, synchronize, author terrain/marker, disconnect, literal server stop, restart same disposable server, reconnect same clean client, and reassert persisted server/client state | Final reconnect observation only if needed | No release deployment work |
| P8-S06 two-client isolation and hearsay | P0 Player271/Player777 isolation is the authoritative control | With `globalSharing=false`, run two distinct clean identities; prove B lacks A-only knowledge before import, then prove additive A-scroll hearsay, attribution, separate layer, reconnect, server restart, and no ownership crossover | Confirm personal versus hearsay display after readiness approval | No forced Atlas override of Surveyor policy |
| P8-S07 clean-install sharing policy | Target Surveyor defaults `globalSharing=true`, unlike the P0 generated config | Start a clean disposable server, record generated default and observable shared behavior, then run faithful isolation with explicit false; document operator policy without production mutation | None unless behavior is ambiguous | No Surveyor redesign or config coercion |
| P8-S08 final gate | P8 must close without entering P9 | Exact-lock clean build, runtime cleanup, frozen-input verification, scope/invariant audit, and zero unexplained S2/S3 | Required focused owner observations after explicit readiness confirmation | P9 and later remain approval-gated |

## Order and stop rules

1. Verify immutable controls and build independent automated harnesses before launching any runtime.
2. Use fresh ignored directories for every scored sequence. A failed harness iteration is not reused.
3. Run integrated lifecycle/dimension/import slices before dedicated multiplayer slices.
4. Start dedicated servers with retained stdin and stop them with Minecraft's literal `stop` command.
5. Use explicit `networking.globalSharing=false` for faithful isolation. Separately measure the clean
   `true` default; never change production Atlas to own that Surveyor policy.
6. Stop immediately on data loss/corruption, cross-player leakage, unrecoverable world damage, or an
   unexplained major feature/lifecycle failure. Diagnose from exact pinned source before editing.
7. Any repair is one narrow production category plus the smallest practical reproduction regression.
8. Automated clients are allowed. Before any owner-observed client launch, stop and obtain the
   owner's readiness confirmation; P8 approval alone does not waive that pause.
9. P8 completion requires the final scope/invariant audit and a clean local checkpoint, then stops at
   the explicit P9 approval gate. No push, release, upload, tag, or publication is authorized.
