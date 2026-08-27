# P1 API-Delta Dossier: Roleplayer's Atlas 1.21.11 to Minecraft 26.1.2

Status: complete research dossier; no production uplift edits were made

Target: Fabric / Minecraft 26.1.2 / Java 25

Authority: `WAWI_ATLAS_AGENT_STATE.yaml` and `design-doc.txt`

Prepared: 2026-08-27

## 1. Scope and conclusions

This dossier maps the 1.21.11 control implementation to exact 26.1.2 source evidence before any uplift edit. It covers the Atlas source lineage, Surveyor boundary, Minecraft/Fabric API surfaces, rendering, input, mixins, resource loading, configuration integrations, and serialized sharing.

The port is feasible without replacing or forking Surveyor. The principal changes are concentrated in three areas:

1. Fabric event/key/tag API changes.
2. The held-atlas rendering submission path and one GameRenderer mixin descriptor.
3. Mechanical Minecraft naming/signature migration, including `BufferAllocator` moving packages.

The Surveyor-facing API used by Atlas is unusually stable: all 21 imported Surveyor types exist at the pinned 26.1 head, and the Atlas-used landmark and terrain codecs remain structurally recognizable. That does **not** prove data compatibility. Existing Surveyor state and `.atlas` files are S3-sensitive and require frozen fixtures plus old-read/new-round-trip tests in the approved integration phases.

P1 did not change build metadata, dependencies, Java release, production Java/resources, or runtime state. P2 remains approval-gated.

## 2. Immutable evidence ledger

### Repository and lineage

| Subject | Immutable evidence | Role |
|---|---|---|
| P0 control | `7099b4df2420d5441e3b2b02e53201a0b249c8f9` | Annotated local tag `p0-baseline-1.21.11`; must never move |
| Roleplayer's Atlas | `dab58dae0db912e91df8fbc2f6c52768e16dc742` | Primary 1.21.11 source lineage |
| AA4 upstream 1.21 | `1821a06b0ad40b12fc1bf9943baf307fc9ed9fc3` | Exact merge base of the Roleplayer's Atlas lineage |
| AA4 upstream 1.22 | `079557da2048ca75f206664805cc43ddfb9273b9` | Later upstream reference |
| Legacy Surveyor 1.21.11 | `39847c28f925900ac65f12a0a22032a3150a139f` | Exact P0 embedded backend source |
| Surveyor upstream 1.21 | `22e24e092b22249b3601746bfaf29c838ad78a1d` | Lineage reference |
| Surveyor upstream 26.1 | `4b500855ea2e316480c1f3fb9e175ec6dc3060db` | Target API source pin |
| Fabric API 0.155.2+26.1.2 | `f9468776b662dd2ab7875e9cdcdf2b653171309d` | Exact target API source tag |
| Fabric Loader 0.19.3 | `35b0b1c0268eb5f9d377322db491b0bb436541a8` | Exact loader source tag |

The Roleplayer's Atlas source is exactly 17 commits ahead of AA4 upstream 1.21 and zero commits behind it. Its comparison to that merge base spans 1,812 files, reflecting the fork's identity/resources and accumulated features; it is not a small version-only patch.

Surveyor lineage is not linear across the local 1.21.11 fork and upstream 26.1. The legacy source and target 26.1 head have merge base `ff1c965c393495ccbfd226c72516644037d2331b` with respective ahead counts 2 and 19. Exact source comparison is therefore required; version-number inference is insufficient.

### Target artifacts and source mappings

| Artifact | Exact version/hash |
|---|---|
| Minecraft release | 26.1.2, released 2026-04-09 |
| Minecraft client SHA-1 | `4e618f09a0c649dde3fdf829df443ce0b8831e65` |
| Minecraft server SHA-1 | `97ccd4c0ed3f81bbb7bfacddd1090b0c56f9bc51` |
| Minecraft common deobfuscated jar SHA-256 | `43DD08265C7361A8203DAF2C0A95F41B2355FFCA0C48B677637E0F66DBE12558` |
| Minecraft client-only deobfuscated jar SHA-256 | `56943FC9456558BC252753AE6E4CC22C714FF78B0356C2097331E98C28185366` |
| Minecraft common source jar SHA-256 | `9C97F8A43C728BC3C0C02C8C684AB5F192157C29A2CD544D403591CDAC4C0145` |
| Minecraft client-only source jar SHA-256 | `AE7EBDDB71E1FEEBF69EF52DF2D3E3A40B0B1C16B59880EEE43912F0F7156469` |
| Modern Yarn research mapping | `26.1.2+build.4`, v2 jar SHA-256 `B1CCDDD70ED300644F1637EB19CF9041D7854F2EE2CC285A7E0DFE906C59BA7E` |
| Fabric API binary | 0.155.2+26.1.2, SHA-256 `7196F2D8700CF906676DB98CC13CD4B98267C4CA4BA3E8C516E62070B21A5A3A` |
| Fabric Loader binary | 0.19.3, SHA-256 `73EED8C34BBAD0320A2A3CBA5346351E822F74F82B3F3C060574068474132958` |
| Loom reference resolution | 1.17.19, SHA-256 `AD331736D7EE6CD5F21C45B19584B951C716BA5DE8ACE8662B42813D110452B8` |

Modern Yarn build 4 is a research pin, not an approved production dependency decision. P2 must choose and lock the production mapping/toolchain deliberately.

## 3. Atlas touchpoint inventory

The control tree contains 92 Java files and 1,366 resources. Static import inventory identifies:

| Surface | Files | Notes |
|---|---:|---|
| Minecraft classes | 78 | GUI, rendering, world/registry, NBT/codec, resources, input |
| Surveyor classes | 24 | 21 unique imported Surveyor types |
| Fabric/Loader APIs | 15 | lifecycle, keys, HUD, resources, tags, loader |
| Mixin implementations | 7 | all declared in the client-only mixin list |
| Direct Fabric networking | 0 | only connection lifecycle events; transport/data synchronization is Surveyor-owned |

Surveyor is correctly preserved as the authoritative backend. Atlas consumes client/world summaries, terrain regions/chunks/layers, landmarks/components, structure summaries, palettes/region positions, and the Surveyor region packet codec. Atlas owns presentation, annotation, local corrections, export/import, and interaction.

## 4. Surveyor API delta

All Atlas-imported Surveyor types exist at `4b500855ea2e316480c1f3fb9e175ec6dc3060db`:

`ClientSummary`, `SurveyorClient`, `SurveyorClientEvents`, `PlayerSummary`, `WorldSummary`, `WorldTerrain`, `RegionSummary`, `ChunkSummary`, `LayerSummary`, `RegionPos`, `RegistryPalette`, `Landmark`, `WorldLandmarks`, `LandmarkComponentMap`, `LandmarkComponentType`, `LandmarkComponentTypes`, `S2CUpdateRegionPacket`, `RegionStructureSummary`, `StructureStartSummary`, `StructurePieceSummary`, and `JigsawPieceSummary`.

| Surface | Evidence-backed delta | Port consequence |
|---|---|---|
| Client permission | `SurveyorClient.canModify(UUID)` remains; target semantics add server/group behavior | Keep the call; verify multiplayer ownership/visibility in P4/P7 |
| Client tick | Surveyor internally moved from end-world to end-level terminology | Atlas's own Fabric callback must migrate independently |
| Terrain ingestion | `WorldTerrain.onChunkLoad(World, WorldChunk)` gained a `generated` argument | Atlas does not call it; no direct production edit |
| Region/chunk positions | Target source uses accessors where older code used fields | Review mechanical access at compile time; no backend redesign |
| Landmarks | Target exposes Xaero migration differently and removes an internal initialization flag | Atlas does not call the removed member |
| Packet/terrain codec | `S2CUpdateRegionPacket.CODEC` terrain structure remains recognizable | Preserve use, then prove with fixtures; do not assume wire/file compatibility |
| Landmark component codecs | Atlas-used codec registration and component shapes remain recognizable | Preserve custom components; verify encode/decode and ownership |
| Structure NBT | Target Surveyor still compares `CompoundTag.getString("id")` directly with a string | P0's `MixinRegionStructureSummary` workaround remains necessary because 26.1.2 returns `Optional<String>` |

The target Surveyor branch declares Minecraft 26.1, Java 25, Fabric Loader 0.18.4, Fabric API 0.144.3+26.1, and a 26.1 Modern Yarn build. Its head is not an exact published 26.1.2 compatibility claim. P2 must prove that the pinned source builds against the exact Atlas target lock before it can be accepted as an embedded dependency.

## 5. Fabric API delta

| Current control API | Exact 26.1.2 source finding | Category |
|---|---|---|
| `ClientTickEvents.END_WORLD_TICK` | Target exposes `END_LEVEL_TICK` | Required mechanical/event semantic migration |
| `client.keybinding.v1.KeyBindingHelper.registerKeyBinding` | Target API is `client.keymapping.v1.KeyMappingHelper.registerKeyMapping` | Required compile migration |
| Convention biome tags v1 | Target convention tags are under v2; v1 references are absent | Required data/tag migration |
| `HudElementRegistry.addLast` | Present | Retain and compile-verify |
| `CommonLifecycleEvents.TAGS_LOADED` | Present | Retain and runtime-verify reload behavior |
| `ClientPlayConnectionEvents.JOIN` / `DISCONNECT` | Present | Retain and verify lifecycle race regression |
| `IdentifiableResourceReloadListener`, `ResourceManagerHelper`, `ResourcePackActivationType` | Present only through the deprecated Fabric API module; target recommends resource API v1 | Available but deprecated; migrate in P6, not a P3 blocker |

Biome providers already contain v2 convention-tag handling plus older fallbacks. The uplift should remove or replace the unavailable v1 symbols without collapsing the data-driven fallback order. `TerrainTiling` has a separate v1 swamp-tag reference that must be included in the same categorized change.

## 6. Minecraft, rendering, screen, and input delta

The exact Modern Yarn 26.1.2 mapping contains 99 of the 100 unique imported Minecraft classes. The sole missing import is `net.minecraft.client.util.BufferAllocator`; the target class is `com.mojang.blaze3d.buffers.BufferAllocator`, with the needed integer constructor and immediate vertex-consumer integration still present.

### GUI map rendering

The custom GUI path remains structurally available: draw context/state, GUI render pipelines, simple GUI element state, texture setup, transformed rectangles, and scissor/intersection concepts all exist. This is a compile-and-visual-verification migration, not evidence for a renderer rewrite.

### Held-atlas rendering

The old `OrderedRenderCommandQueue.submitCustom(MatrixStack, RenderLayer, Custom)` surface is gone. The exact target source routes custom geometry through ordered submit collectors (`submitCustomGeometry` in the target naming surface) and batching queues. `HandheldAtlasRenderer` must migrate its custom solid/translucent submissions while preserving order, matrix state, and translucency. This is the highest-risk production rendering change and belongs in P5 with direct handheld/fullscreen visual checkpoints.

### Screen and input

The imported screen/input classes remain in the target mapping, including the event-object input model already used by the 1.21.11 control. No class-level hard break was found. Exact handler overrides must still be compile-checked because inherited method and record component signatures can move without an import disappearing. Preserve the P0 transition, pan/zoom, resize, GUI-scale, pause, and resource-reload matrix.

## 7. Mixin target audit

| Mixin | Exact target result | Required action |
|---|---|---|
| `MixinDrawContext` | Accessed draw-state field survives | Compile/mixin audit |
| `MixinHeldItemRenderer` | Referenced fields and five render methods survive structurally | Recompile descriptors; visual held-map tests |
| `MixinMinecraftClient` | `openGameMenu` survives | Compile and pause/resume regression |
| `MixinScreen` | `applyBlur` and `renderDarkening` survive | Compile and screen-background regression |
| `MixinGameRenderer` | Hard descriptor change: `bobView(MatrixStack,float)` becomes camera-state plus matrix; `renderHand(float,boolean,Matrix4f)` becomes camera-state, float, and `Matrix4fc` | Retarget WrapOperation in P5; verify steady-atlas behavior |
| `MixinRegionStructureSummary` | Target Surveyor defect still exists | Retain `remap=false` workaround and add structure discovery test |
| `MixinPunchyConfig` | `@Pseudo`, `remap=false`, and `require=0` confirm intentional fail-soft behavior when Punchy is absent | Keep startup regression; RISK-008 archaeology question is resolved |

The mixin configuration remains client-only. The compatibility level is currently Java 21 and must be considered together with the Java 25 build lock in P2, without changing mixin behavior there.

## 8. Resources, registries, configuration, and serialization

Atlas has 1,366 resources, dominated by data-driven biome/structure/terrain mappings and textures. The four control reloaders—marker textures, tile textures, structure providers, and biome providers—must remain reloadable. Target resource finder/file-to-identifier concepts and preparable reloader signatures exist, while the Fabric registration wrapper is deprecated as described above.

Configuration boundaries are intentionally isolated:

- Kaleido Config is the core persisted configuration dependency.
- Mod Menu is an optional client entry point.
- Cloth Config is optional and isolated to one compatibility class; failure falls back to the built-in Atlas screen.
- Exact 26.1.2-compatible versions for Kaleido, Cloth Config, and Mod Menu are not yet locked. That is P2 work.

Atlas does not define a custom Fabric networking channel. `.atlas` sharing uses GZIP NBT and serializes terrain through `S2CUpdateRegionPacket.CODEC` and landmarks through Surveyor codecs. There is no explicit Atlas file-format version. This makes compatibility fixture work mandatory before accepting serialization or Surveyor integration changes:

1. Freeze representative P0 singleplayer and multiplayer/hearsay `.atlas` files and persisted Surveyor state.
2. Assert old files load without mutation or silent loss.
3. Round-trip through the target and compare terrain, corrections, landmarks, components, authorship, and layer visibility.
4. Exercise malformed/incompatible input and confirm bounded failure rather than partial corruption.

## 9. Categorized port map

| Phase | Change category | Evidence-driven scope |
|---|---|---|
| P2 | Build/dependency lock | Java 25; exact Loom/mappings/MC/Loader/FAPI; build pinned Surveyor; resolve optional config integrations; make no behavior edits |
| P3 | Mechanical Minecraft/Fabric uplift | Key mapping API, end-level tick event, convention tags v2, imports/names/accessors, low-risk signature repairs |
| P4 | Surveyor integration | Embed exact current Surveyor; component/terrain/landmark compatibility; ownership/sync; frozen fixtures and round trips |
| P5 | Rendering/input/mixins | Buffer allocator package, ordered custom geometry submission, GameRenderer descriptor, held/fullscreen visual matrix |
| P6 | Resources/serialization | Resource API v1 migration, reloaders, registries/identifiers, resource validation, `.atlas` compatibility proofs |
| P7+ | Behavioral reconstruction | Re-run P0 parity slices one at a time; no WAWI feature work before compatibility acceptance |

## 10. Gate risks and required probes

The following findings are blockers to claiming a completed port, not blockers to entering an explicitly approved P2:

- **S3:** Surveyor persistence and `.atlas` compatibility are unproven across the version jump.
- **S2:** Surveyor 26.1 head is not itself an exact 26.1.2 lock; build/runtime proof is required.
- **S2:** Held custom-geometry submission and GameRenderer mixin descriptors changed materially.
- **S2:** Fabric key mapping, tick event, and convention-tag APIs contain confirmed compile breaks.
- **S2:** Dedicated-server/client-only safety must be rechecked after embedding target Surveyor.
- **S2:** RISK-007 license/credits packaging remains unresolved and prohibits distribution.
- **S1:** Resource registration APIs compile only through a deprecated module and should be migrated deliberately.
- **S1:** Optional configuration integration versions are not yet selected for 26.1.2.

P2 acceptance must produce an exact, reproducible dependency lock and clean build-system-only diff. It must not mix production API repairs into the dependency commit. P3 and later remain separately gated by the durable phase plan.

## 11. P1 exit decision

P1's research objective is satisfied:

- Exact upstream Atlas, legacy Surveyor, target Surveyor, Fabric API, Loader, Minecraft source/artifact, and mapping evidence is pinned.
- Atlas-to-Surveyor, Fabric, Minecraft, rendering, input, mixin, resource, configuration, and serialization surfaces are inventoried.
- Known hard breaks, compatible surfaces, deprecations, uncertainty boundaries, and required acceptance probes are separated.
- No production uplift or dependency change was made.

The only authorized next action is to wait for explicit P2 approval to perform the 26.1.2 build-system and dependency lock. That approval does not implicitly authorize P3 production API edits.
