# P7 feature-parity reconstruction matrix

This matrix converts the complete P0 control evidence into bounded P7 behavior slices. It is not a
redesign backlog. Each row preserves Roleplayer's Atlas identifiers, formats, resources, visual
behavior, and the current Surveyor ownership boundary unless a target-runtime defect demonstrates
that a narrow compatibility repair is required.

## Entry controls

- Approval checkpoint: `8662ac1a148a22c65f8e27392d919aabd7c3ef66` (`APPROVAL-P7-001`), YAML only.
- P6 completion control: `0b2c3a4f37d6bd95e301385cfb8e479d6c7af578` on `port/26.1.2`.
- P0 behavior control: annotated tag `p0-baseline-1.21.11`, peeled target
  `7099b4df2420d5441e3b2b02e53201a0b249c8f9`.
- Roleplayer's Atlas source pin: `dab58dae0db912e91df8fbc2f6c52768e16dc742`.
- Target Surveyor source pin: `4b500855ea2e316480c1f3fb9e175ec6dc3060db`.
- Target Minecraft/Fabric lock: `docs/P2_BUILD_LOCK.md` and unchanged P2 build files.
- Immutable data controls: `compat-fixtures/p4` and `compat-fixtures/p6`; tests operate only on copies.
- P7 entry clean build: exact-lock offline `clean build` passed in 16 seconds; test tasks are
  `NO-SOURCE`.

## Source-delta classification

The P0-to-P7 production range changes 35 Java files. The changes are already categorized by P3-P6:
Fabric key/tick/tag names, `ChunkPos` record accessors/factories, sound and message target methods,
removed `CharInput` modifier reads that fed unused locals, renderer/mixin/allocator target APIs, and
the Fabric v1 resource-loader registration. P4 proved that no Atlas-to-Surveyor call-site migration
was required.

The following feature-model implementations are Git-blob-identical to the P0 control:

- `AtlasComponents`, `AtlasTime`, `AtlasUndo`, `FeatureTiles`, and `MapAutosave`.
- `MarkerLayers`, `SpawnMarker`, and `TrackedMarkersStore`.
- `AtlasOverlay`, `ScreenshotModal`, and `RouteUtil`.

Other feature files contain only the already-evidenced target substitutions above. Therefore the
inventory does not justify a production edit by itself; live or automated evidence must first expose
a compatibility defect.

## Bounded slices

| Slice | P0 control behavior | Existing target evidence | P7 evidence still required | P8 boundary |
| --- | --- | --- | --- | --- |
| P7-S01 exploration and tiles | Newly explored terrain produces antique terrain/biome tiles; only discovered structure knowledge is shown | P4 decoded all frozen Surveyor terrain/structure data; P5/P6 joined target worlds; P6 rendered 24 tiles | Automated target-world assertions for Surveyor exploration, Atlas tile population, biome resolution, batching, and discovered-structure inputs; later owner observation of new exploration | Adversarial save/restart, portal/dimension, and long lifecycle repetition |
| P7-S02 atlas view and input | Held/fullscreen/closed cycle, pan, zoom, resize, scale, pause, and faithful rendering | Complete owner-observed P5 matrix plus P6 reload/render pass | Recheck only after a P7 defect changes relevant production code; otherwise carry forward P5/P6 evidence and perform final focused owner confirmation | Repeated transition/lifecycle hardening |
| P7-S03 annotations | Marker/layer lifecycle, hearth/death marks, inscriptions, territories/city paint, routes, undo, tracking | Core models are blob-identical; target startup already loads local stores | Automated create/edit/remove and classification assertions on disposable target state; focused owner workflow for visual/modal interactions | Restart/reconnect/dimension persistence stress |
| P7-S04 sharing and PNG | `.atlas` preview/export/import/signature/hearsay is additive; PNG view saves; source data is not mutated | P4 exact codec/packet round trips; P6 preview and failure-path probes | Successful disposable target export/import, authorship/layer/correction assertions, source-nonmutation check, and automated PNG output; owner-visible preview/result confirmation | Repeated imports, crash interruption, restart, and hostile lifecycle cases |
| P7-S05 basic ownership and synchronization | Independent personal knowledge, author attribution, imported hearsay, separate visibility | P4 exact ownership/source semantics and frozen multiplayer data; current Surveyor remains backend owner | One bounded basic target client/dedicated synchronization and ownership flow with `globalSharing=false` | Multi-client isolation matrix, reconnect/restart, server lifecycle, and global-sharing policy hardening |
| P7-S06 final gate | Every P0 feature category has a target result; no S2/S3 regression | Exact lock, tag, design, fixtures, identity, formats, and no-publication controls already exist | Exact-lock clean build, final scope/invariant audit, owner observations after readiness confirmation | All P8 work remains approval-gated |

## Test order and stop rules

1. Run non-launching static and exact-lock probes first.
2. Run automated clients only in disposable target directories; never modify frozen fixtures in place.
3. Repair only a reproduced target defect, in one categorized production commit with its smallest
   practical regression.
4. Before any user testing, stop and obtain the owner's readiness confirmation before launching the
   client. An automated client launch does not waive that pause.
5. Stop immediately on any unexplained S2 or any S3 result. Do not enter P8 to make P7 pass.
