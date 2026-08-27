# P4 Current Surveyor Compatibility Dossier

## Scope and immutable inputs

P4 evaluates Roleplayer's Atlas against official Surveyor `1.2.4+26.1` without forking or
recreating the backend. The exact P2 lock remains unchanged: Java 25, Gradle 9.5.1, Loom remap
1.17.19, Modern Yarn `26.1.2+build.4`, Loader 0.19.3, Fabric API `0.155.2+26.1.2`, and Minecraft
26.1.2. The official Surveyor jar SHA-256 remains
`D705677212A38B2071B621012478109ABD13E60A9678E11A6B90F1BF7A6EEFAC`.

Source comparison uses exact commits:

- legacy Surveyor: `39847c28f925900ac65f12a0a22032a3150a139f`
- target Surveyor: `4b500855ea2e316480c1f3fb9e175ec6dc3060db`
- frozen P0 inputs: the 49 files and hashes in `compat-fixtures/p4/README.md`

## Source comparison

All 21 Surveyor types imported by Atlas exist at the target commit. Twelve of their source files
are blob-identical between the two pins: `ClientSummary`, `SurveyorClientEvents`, `Landmark`,
`LandmarkComponentMap`, `LandmarkComponentType`, `PlayerSummary`, `JigsawPieceSummary`,
`StructurePieceSummary`, `StructureStartSummary`, `LayerSummary`, `RegistryPalette`, and
`WorldSummary`.

The nine changed Atlas-used files are narrowly classified:

| Surface | Exact target delta | P4 result |
|---|---|---|
| `S2CUpdateRegionPacket` | Only a `ChunkPos` log accessor changed; `CODEC` is source-identical. | Frozen packet byte round trips pass. |
| `RegionStructureSummary` | Only `ChunkPos` accessors changed in its persistence key. | Frozen structure NBT round trips pass with Atlas's existing jigsaw mixin. |
| `RegionSummary` / `RegionPos` | `ChunkPos` fields became accessors. | Region/chunk persistence round trips pass. |
| `WorldTerrain` | Mechanical accessors plus an unused `generated` callback parameter. | Atlas does not call the changed method. |
| `ChunkSummary` | Current Minecraft collision access is supplied by Surveyor's target mixin. | 8,479 persisted summaries round-trip exactly. |
| `LandmarkComponentTypes` | Current pick-stack access is supplied by Surveyor's target mixin. | Atlas landmark component codecs round-trip exactly. |
| `WorldLandmarks` | Xaero migration moved from removed `clientInitialized()` into target initial-load handling. | Atlas never called the removed method; `CODEC` round trips pass. |
| `SurveyorClient` | Mechanical tick/callback uplift, Xaero migration relocation, and expanded `SERVER` waypoint edit rules. | No Atlas call-site repair is required. |

Across all Surveyor production Java, the exact diff is 23 files, 105 insertions, and 93 deletions.
The remaining changes are target Minecraft/Fabric adaptation plus three meaningful current-backend
semantics:

- `BitSetUtil.half` now splits by set-bit cardinality instead of backing capacity, correcting
  oversized packet subdivision.
- `canModify` supports `SERVER` waypoint mode while retaining owner, operator, and group rules and
  excluding global landmarks from ordinary player ownership.
- `networking.globalSharing` defaults to `true` instead of the legacy `false`.

The generated P0 Surveyor configurations explicitly contain `globalSharing=false`, so existing P0
server behavior survives config-preserving upgrades. Atlas must not silently override an operator's
current Surveyor configuration. Clean target servers therefore require an explicit
`globalSharing=false` setting when the faithful independent-knowledge/hearsay policy is desired;
this clean-default difference remains a documented hardening-policy risk rather than a reason to
fork Surveyor.

## Runtime compatibility probe

The isolated harness at `compat-fixtures/p4/probe-project` runs under the exact target Fabric server
runtime so access wideners, mappings, mixins, registries, and item components are initialized exactly
as they are in Minecraft. It loads the existing production `MixinRegionStructureSummary` unchanged;
without that established Atlas shim, Surveyor's optional-return comparison would deserialize jigsaw
pieces generically and discard their specialized fields.

The passing offline command reported:

```text
ATLAS singleplayer landmarks=0 authored=0 regions=6 packetBytes=2733119
ATLAS multiplayer landmarks=1 authored=1 regions=4 packetBytes=421797
PERSISTENCE files=46 landmarks=5 landmarkFiles=4 chunkSummaries=8479 structureRegions=12 explorationFiles=1
P4_COMPATIBILITY_PROBE_PASS
BUILD SUCCESSFUL
```

The probe copies all frozen inputs to a temporary directory, then verifies:

- target NBT compressed read/write content equality;
- target `Landmark` and `WorldLandmarks.CODEC` NBT equality for Atlas's 14 custom components;
- target `S2CUpdateRegionPacket.CODEC` complete decode and byte-identical re-encode;
- exact `ChunkSummary` and structure-region NBT re-encode;
- personal and shared `SurveyorExploration` state preservation;
- dedicated-server classloading and all-three-dimension Surveyor startup under the exact lock.

## Migration decision and phase boundary

No P4 production migration is required. The target artifact preserves every Atlas-used codec and API
needed at the current compile boundary, while the one known jigsaw persistence defect remains covered
by Atlas's pre-existing production mixin and the frozen regression probe. Inventing a production edit
would add risk without repairing a demonstrated incompatibility.

Full Atlas client connection, rendering, and interactive synchronization cannot run until the four
deliberately deferred P5 rendering compile failures are migrated. Those later runtime checks remain
assigned to P7/P8 after P5; P4 does not cross that gate. P4 also makes no resource-loader, format,
identity, art, feature, dependency-lock, packaging, publication, or external backend change.
