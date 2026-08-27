# P6 resource and serialization inventory

## Scope and exact sources

This dossier records the P6 inventory before the first production edit. P6 is limited to resources,
reloaders, identifiers, registries, and Atlas-owned serialization. It does not authorize P7 runtime
synchronization work, a Surveyor redesign, dependency-lock changes, new art or resources, packaging,
or publication.

The comparison uses the exact P2 Fabric API lock at commit
`f9468776c4c86811775b9d46cabb4925b1d917be`:

- `fabric-resource-loader-v0-3.3.17+4fc5413f4c-sources.jar`, SHA-256
  `72C654DF5CFB0E6AEA2953793D50DC382B728B7095A22600751FD705DF66BC7D`
- `fabric-resource-loader-v1-2.0.10+7c44c7324c-sources.jar`, SHA-256
  `DA0703799205E0ABB1D514B4EADB5FACE39FB99615D237C0F04D285A781D9403`

Minecraft comparisons use the immutable 1.21.11 control and the exact 26.1.2 Modern Yarn target
locked in P2.

## Resource inventory

The production resource tree contains 1,366 files and no P6 content edit is indicated:

| Type | Count |
| --- | ---: |
| JSON | 550 |
| PNG | 457 |
| PNG metadata (`.mcmeta`) | 358 |
| TOML | 1 |

All 550 JSON files pass strict syntax parsing. The active Atlas data/reloader roots are:

| Root | Files | Consumer |
| --- | ---: | --- |
| `assets/minecraft/atlas/biome` | 64 JSON | `BiomeTileProviders` |
| `assets/roleplayers_atlas/atlas/biome` | 9 JSON | `BiomeTileProviders` |
| `assets/minecraft/atlas/structure` | 457 JSON | `StructureTileProviders` |
| `assets/roleplayers_atlas/textures/atlas/tile` | 287 PNG + 287 metadata | `TileTextures` |
| `assets/roleplayers_atlas/textures/atlas/marker` | 116 PNG + 59 metadata | `MarkerTextures` |
| `assets/minecraft/textures/atlas` | 4 PNG + 3 metadata | same tile metadata path |

The source registers an optional built-in pack at `roleplayers_atlas:shader_patch`, but no matching
`resourcepacks/shader_patch` directory exists in the control or target tree. P6 preserves that exact
registration outcome and does not invent a pack.

## Exact reloader migration

The deprecated v0 `ResourceManagerHelperImpl.registerReloadListener` is only an adapter. Its exact
implementation registers `listener.getFabricId()` with v1 `ResourceLoader`, then adds one v1 ordering
edge for each `getFabricDependencies()` entry. Therefore the bounded migration is to register the
existing Minecraft reloaders directly through v1 while preserving these identifiers and edges:

| Listener | Identifier | Must run after |
| --- | --- | --- |
| `TileTextures` | `roleplayers_atlas:tile_textures` | none |
| `MarkerTextures` | `roleplayers_atlas:marker_textures` | none |
| `StructureTileProviders` | `roleplayers_atlas:structures` | tile textures, marker textures |
| `BiomeTileProviders` | `roleplayers_atlas:tile_provider/biome` | tile textures |

The four listeners remain `SinglePreparationResourceReloader` implementations. Replacing them with
Fabric `SimpleReloadListener` would be unnecessary churn: the exact target Minecraft preparation,
apply, `ResourceManager`, metadata, and profiler signatures used here are unchanged.

The built-in pack call maps directly to v1 `ResourceLoader.registerBuiltinPack` with
`PackActivationType.NORMAL`, the same identifier, container, and display text.

## Identifier, registry, and serialization findings

Every Atlas-used `Identifier` factory, codec, and path helper is present at the target; the target only
adds `resolveInPath` among the compared helpers. `DynamicRegistryManager.getOrThrow` and Atlas-used
registry lookups retain their exact signatures. `ResourceFinder` is now a record, but its `json`,
`toResourceId`, and `findResources` surfaces are unchanged. No identifier or registry source edit is
justified by the exact comparison or by the passing target compile.

Atlas-owned persistence surfaces are:

- GZIP NBT `.atlas` files in `MapShare`, with root `Version=1`, dimension, landmarks, layers,
  Surveyor region-packet bytes, biome/chunk/city corrections, and author.
- JSON stores for biome overrides, city paint, marker layers, hearth/spawn, and tracked markers.
- Kaleido/TOML configuration in `AtlasConfig`.

The compared target keeps all Atlas-used `NbtIo`, `NbtCompound`, `ResourceMetadataSerializer`,
`Identifier.CODEC`, and registry-buffer surfaces. P4 already proved the exact two frozen P0 `.atlas`
files decode and re-encode byte/content-exactly with the target Surveyor codecs. P6 reuses those
immutable fixtures; it must never rewrite them in place. The normalized P0 local JSON snapshots in
`compat-fixtures/p6/local-stores` add coverage for every Atlas-owned JSON store.

## P6 acceptance checks

1. Migrate only the deprecated Fabric resource-loader wrapper and listener identity interface.
2. Compile under the unchanged exact P2 lock and prove no v0 resource API import remains.
3. Parse all production JSON plus every texture metadata document and verify expected file counts.
4. Re-run the immutable P4 `.atlas` target probe from disposable copies and add malformed-input
   coverage without mutating the fixtures.
5. Load the frozen P0 local stores through target Atlas code, save disposable copies, and compare
   semantic content.
6. Launch the full target client, confirm all four reloaders complete at startup, perform F3+T, and
   confirm they complete again without a project exception or missing map/marker textures.
7. Repeat the clean offline build and dedicated-server startup boundary, audit the categorized diff,
   and stop at the P7 approval gate.
