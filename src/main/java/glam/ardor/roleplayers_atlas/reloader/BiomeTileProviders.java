package glam.ardor.roleplayers_atlas.reloader;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import glam.ardor.roleplayers_atlas.BiomeOverrides;
import glam.ardor.roleplayers_atlas.RoleplayersAtlas;
import glam.ardor.roleplayers_atlas.AtlasConfig;
import glam.ardor.roleplayers_atlas.TerrainTileProvider;
import glam.ardor.roleplayers_atlas.TileElevation;
import glam.ardor.roleplayers_atlas.TileTexture;
import glam.ardor.roleplayers_atlas.util.ForgeTags;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import glam.ardor.roleplayers_atlas.util.LegacyJsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BiomeTileProviders extends LegacyJsonDataLoader implements IdentifiableResourceReloadListener {
	public static final BiomeTileProviders INSTANCE = new BiomeTileProviders();
	public static final Identifier ID = RoleplayersAtlas.id("tile_provider/biome");

	public static BiomeTileProviders getInstance() {
		return INSTANCE;
	}

	protected final Map<Identifier, TerrainTileProvider> tileProviders = new HashMap<>();
	protected final Map<Identifier, Identifier> biomeFallbacks = new HashMap<>();
	protected boolean hasFallbacks = false;

	public BiomeTileProviders() {
		super("atlas/biome");
	}

	/** Where a biome's look came from, so the player can see what is guesswork. */
	public enum Source {
		EXACT,
		TAGS,
		NAME,
		NONE,
		MANUAL,
		SHARED
	}

	protected final Map<Identifier, Source> sources = new HashMap<>();

	/** Every biome this world has, and how each one ended up looking as it does. */
	public Map<Identifier, Source> sources() {
		Map<Identifier, Source> out = new HashMap<>(sources);
		BiomeOverrides.importedBiomes().forEach(id -> out.put(id, Source.SHARED));
		BiomeOverrides.all().keySet().forEach(id -> out.put(id, Source.MANUAL));
		return out;
	}

	/** What a biome is drawn as right now, correction and all. */
	public Identifier drawnAs(Identifier biomeId) {
		Identifier manual = BiomeOverrides.get(biomeId);
		if (manual != null && tileProviders.containsKey(manual)) return manual;
		if (tileProviders.containsKey(biomeId)) return biomeId;
		Identifier guessed = biomeFallbacks.get(biomeId);
		return guessed != null ? guessed : null;
	}

	/** The looks a correction can choose from — every biome the atlas has a picture for. */
	public java.util.Set<Identifier> availableLooks() {
		return java.util.Collections.unmodifiableSet(tileProviders.keySet());
	}

	public TerrainTileProvider getTileProvider(Identifier providerId) {
		// The player's own word comes before anything the atlas worked out,
		// including an exact match — a server can put anything in any biome.
		Identifier manual = BiomeOverrides.get(providerId);
		if (manual != null) {
			TerrainTileProvider corrected = tileProviders.get(manual);
			if (corrected != null) return corrected;
		}
		TerrainTileProvider exact = tileProviders.get(providerId);
		if (exact != null) return exact;
		TerrainTileProvider guessed = tileProviders.get(biomeFallbacks.get(providerId));
		if (guessed != null) return guessed;
		if (RoleplayersAtlas.CONFIG.fallbackFailHandling == AtlasConfig.FallbackHandling.PLAINS && !providerId.equals(BiomeKeys.PLAINS.getValue())) {
			return getTileProvider(BiomeKeys.PLAINS.getValue());
		}
		return TerrainTileProvider.fallback();
	}

	/**
	 * Register fallbacks for any biomes present in the client world that don't have explicit sets.
	 * Doing this on world join catches data-biomes that might not be registered in other worlds.
	 */
	public void registerFallbacks(Registry<Biome> biomeRegistry) {
		for (Biome biome : biomeRegistry) {
			Identifier biomeId = biomeRegistry.getId(biome);
			if (tileProviders.containsKey(biomeId)) {
				sources.put(biomeId, Source.EXACT);
				continue;
			}
			Identifier fallbackBiome = getFallbackBiome(biomeRegistry.getEntry(biome));
			if (fallbackBiome != null && tileProviders.containsKey(fallbackBiome)) {
				biomeFallbacks.put(biomeId, fallbackBiome);
				sources.put(biomeId, Source.TAGS);
				RoleplayersAtlas.LOGGER.info("[Roleplayer's Atlas] Set fallback biome for {} to {}. You can set a more fitting texture using a resource pack!", biomeId, fallbackBiome);
			} else if (fallbackBiome != null) {
				sources.put(biomeId, Source.NONE);
				RoleplayersAtlas.LOGGER.error("[Roleplayer's Atlas] Fallback biome for {} is {}, which has no defined tile provider.", biomeId, fallbackBiome);
			} else {
				// Tags said nothing. Datapack biomes on a plugin server routinely
				// carry none at all, so read the name before giving up.
				Identifier byName = BiomeNames.guess(biomeId);
				if (byName != null && tileProviders.containsKey(byName)) {
					biomeFallbacks.put(biomeId, byName);
					sources.put(biomeId, Source.NAME);
					RoleplayersAtlas.LOGGER.info("[Roleplayer's Atlas] {} has no biome tags; drawing it as {} going by its name.", biomeId, byName);
					continue;
				}
				sources.put(biomeId, Source.NONE);
				RoleplayersAtlas.LOGGER.warn("[Roleplayer's Atlas] No fallback could be found for {} — no tags, and nothing recognisable in the name.", biomeId);
				if (RoleplayersAtlas.CONFIG.fallbackFailHandling == AtlasConfig.FallbackHandling.CRASH) throw new IllegalStateException("Roleplayer's Atlas fallback biome registration failed! Fix the missing biome or change fallbackFailHandling in roleplayers-atlas.toml");
			}
		}
		hasFallbacks = true;
	}

	public void clearFallbacks() {
		hasFallbacks = false;
		biomeFallbacks.clear();
		sources.clear();
	}

	public boolean hasFallbacks() {
		return hasFallbacks;
	}

	public static Identifier getFallbackBiome(RegistryEntry<Biome> biome) {
		if (biome.isIn(ConventionalBiomeTags.IS_VOID) || biome.isIn(ForgeTags.Biomes.IS_VOID)) {
			return BiomeKeys.THE_VOID.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_END) || biome.isIn(ConventionalBiomeTags.IS_OUTER_END_ISLAND) || biome.isIn(BiomeTags.IS_END)) {
			if (biome.isIn(ConventionalBiomeTags.IS_VEGETATION_SPARSE) || biome.isIn(ConventionalBiomeTags.IS_VEGETATION_DENSE) || biome.isIn(ForgeTags.Biomes.IS_LUSH)) return BiomeKeys.END_HIGHLANDS.getValue();
			return BiomeKeys.END_BARRENS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_NETHER_FOREST)) {
			return BiomeKeys.WARPED_FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_NETHER) || biome.isIn(BiomeTags.IS_NETHER)) {
			return BiomeKeys.SOUL_SAND_VALLEY.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_SWAMP) || biome.isIn(ForgeTags.Biomes.IS_SWAMP)) {
			return BiomeKeys.SWAMP.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_AQUATIC) || biome.isIn(ConventionalBiomeTags.IS_OCEAN) || biome.isIn(ConventionalBiomeTags.IS_DEEP_OCEAN) || biome.isIn(ConventionalBiomeTags.IS_SHALLOW_OCEAN) || biome.isIn(ConventionalBiomeTags.IS_RIVER) || biome.isIn(ConventionalBiomeTags.IS_AQUATIC_ICY) || biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN) || biome.isIn(BiomeTags.IS_RIVER) || biome.isIn(ForgeTags.Biomes.IS_WATER)) {
			if (biome.isIn(ConventionalBiomeTags.IS_AQUATIC_ICY) || biome.isIn(ConventionalBiomeTags.IS_ICY)) return BiomeKeys.FROZEN_RIVER.getValue();
			return BiomeKeys.RIVER.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_STONY_SHORES)) {
			return BiomeKeys.STONY_SHORE.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_BEACH) || biome.isIn(BiomeTags.IS_BEACH)) {
			return BiomeKeys.BEACH.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_JUNGLE_TREE) || biome.isIn(ConventionalBiomeTags.IS_JUNGLE) || biome.isIn(BiomeTags.IS_JUNGLE)) {
			return BiomeKeys.JUNGLE.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_FLOWER_FOREST) || biome.isIn(ConventionalBiomeTags.IS_FLORAL)) {
			return BiomeKeys.FLOWER_FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_SAVANNA_TREE) || biome.isIn(ConventionalBiomeTags.IS_SAVANNA) || biome.isIn(BiomeTags.IS_SAVANNA)) {
			return BiomeKeys.SAVANNA.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_BADLANDS) || biome.isIn(BiomeTags.IS_BADLANDS)) {
			return BiomeKeys.BADLANDS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_CONIFEROUS_TREE) || biome.isIn(ConventionalBiomeTags.IS_TAIGA) || biome.isIn(ForgeTags.Biomes.IS_CONIFEROUS) || biome.isIn(BiomeTags.IS_TAIGA)) {
			if (biome.isIn(ConventionalBiomeTags.IS_SNOWY) || biome.isIn(ConventionalBiomeTags.IS_ICY)) return BiomeKeys.SNOWY_TAIGA.getValue();
			return BiomeKeys.TAIGA.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_OLD_GROWTH)) {
			return BiomeKeys.BIRCH_FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_BIRCH_FOREST) || biome.isIn(ConventionalBiomeTags.IS_DECIDUOUS_TREE)) {
			return BiomeKeys.BIRCH_FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_FOREST) || biome.isIn(BiomeTags.IS_FOREST)) {
			return BiomeKeys.FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_SNOWY_PLAINS) || biome.isIn(ConventionalBiomeTags.IS_PLAINS) || biome.isIn(ForgeTags.Biomes.IS_PLAINS) || biome.isIn(ConventionalBiomeTags.IS_SNOWY) || biome.isIn(ForgeTags.Biomes.IS_SNOWY)) {
			if (biome.isIn(ConventionalBiomeTags.IS_ICY) || biome.isIn(ConventionalBiomeTags.IS_SNOWY_PLAINS) || biome.isIn(ConventionalBiomeTags.IS_SNOWY)) return BiomeKeys.SNOWY_PLAINS.getValue();
			return BiomeKeys.PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_WASTELAND) || biome.isIn(ConventionalBiomeTags.IS_DEAD) || biome.isIn(ConventionalBiomeTags.IS_DESERT) || biome.isIn(ForgeTags.Biomes.IS_SANDY) || biome.isIn(ForgeTags.Biomes.IS_DESERT) || biome.isIn(ForgeTags.Biomes.IS_DEAD) || biome.isIn(ForgeTags.Biomes.IS_WASTELAND)) {
			return BiomeKeys.DESERT.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_ICY)) {
			return BiomeKeys.FROZEN_OCEAN.getValue();
		} else if (biome.isIn(ForgeTags.Biomes.IS_PLATEAU)) {
			return BiomeKeys.MEADOW.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_WINDSWEPT)) {
			return BiomeKeys.WINDSWEPT_HILLS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_MOUNTAIN_PEAK) || biome.isIn(ForgeTags.Biomes.IS_PEAK)) {
			return BiomeKeys.JAGGED_PEAKS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_MOUNTAIN_SLOPE) || biome.isIn(ConventionalBiomeTags.IS_MOUNTAIN) || biome.isIn(BiomeTags.IS_MOUNTAIN) || biome.isIn(ForgeTags.Biomes.IS_SLOPE) || biome.isIn(ForgeTags.Biomes.IS_MOUNTAIN)) {
			return BiomeKeys.STONY_PEAKS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_MUSHROOM) || biome.isIn(ForgeTags.Biomes.IS_MUSHROOM)) {
			return BiomeKeys.MUSHROOM_FIELDS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_HILL) || biome.isIn(BiomeTags.IS_HILL)) {
			return BiomeKeys.WINDSWEPT_GRAVELLY_HILLS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_UNDERGROUND) || biome.isIn(ConventionalBiomeTags.IS_CAVE) || biome.isIn(ForgeTags.Biomes.IS_UNDERGROUND) || biome.isIn(ForgeTags.Biomes.IS_CAVE)) {
			return BiomeKeys.DRIPSTONE_CAVES.getValue();
		} else if (biome.isIn(ForgeTags.Biomes.IS_SPOOKY)) {
			return BiomeKeys.DARK_FOREST.getValue();
		} else if (biome.isIn(ForgeTags.Biomes.IS_MAGICAL)) {
			return BiomeKeys.MUSHROOM_FIELDS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_VEGETATION_DENSE) || biome.isIn(ForgeTags.Biomes.IS_DENSE)) {
			return BiomeKeys.FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_VEGETATION_SPARSE) || biome.isIn(ForgeTags.Biomes.IS_SPARSE)) {
			return BiomeKeys.PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_HOT) || biome.isIn(ForgeTags.Biomes.IS_HOT)) {
			return BiomeKeys.DESERT.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_COLD) || biome.isIn(ForgeTags.Biomes.IS_COLD)) {
			return BiomeKeys.SNOWY_PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_TEMPERATE)) {
			return BiomeKeys.PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_DRY) || biome.isIn(ForgeTags.Biomes.IS_DRY)) {
			return BiomeKeys.BADLANDS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.IS_WET) || biome.isIn(ForgeTags.Biomes.IS_WET)) {
			return BiomeKeys.SWAMP.getValue();
		}
		return null;
	}

	public static TileTexture getTexture(Map<Identifier, TileTexture> textures, Identifier id) {
		if (textures.containsKey(id)) {
			return textures.get(id);
		} else {
			throw new IllegalStateException("texture %s is not present!".formatted(id));
		}
	}

	public static @Nullable List<TileTexture> resolveTextureJson(Map<Identifier, TileTexture> textures, JsonElement textureJson) {
		if (textureJson instanceof JsonPrimitive texturePrimitive && texturePrimitive.isString()) {
			return List.of(getTexture(textures, Identifier.tryParse(texturePrimitive.getAsString())));
		} else if (textureJson instanceof JsonArray textureArray) {
			return textureArray.asList().stream().map(je -> getTexture(textures, Identifier.tryParse(je.getAsString()))).toList();
		} else if (textureJson instanceof JsonObject textureObject && textureObject.keySet().stream().allMatch(k -> textureObject.get(k) instanceof JsonPrimitive jp && jp.isNumber())) {
			Multiset<TileTexture> outList = HashMultiset.create();
			textureObject.entrySet().forEach(e -> outList.add(getTexture(textures, Identifier.tryParse(e.getKey())), e.getValue().getAsInt()));
			return outList.stream().toList();
		}
		return null;
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		RoleplayersAtlas.LOGGER.info("[Roleplayer's Atlas] Reloading Biome Tile Providers...");
		Map<Identifier, TileTexture> textures = TileTextures.getInstance().getTextures();
		Set<TileTexture> unusedTextures = new HashSet<>(textures.values().stream().filter(t -> t.id().getPath().startsWith("biome")).toList());
		Map<Identifier, Identifier> providerParents = new HashMap<>();
		for (Map.Entry<Identifier, JsonElement> fileEntry : prepared.entrySet()) {
			Identifier fileId = fileEntry.getKey();
			try {
				JsonObject fileJson = fileEntry.getValue().getAsJsonObject();
				if (fileJson.has("parent")) {
					Identifier parentId = Identifier.tryParse(fileJson.getAsJsonPrimitive("parent").getAsString());
					providerParents.put(fileId, parentId);
					continue;
				}
				JsonElement textureJson = fileJson.get("textures");
				List<TileTexture> defaultTextures = resolveTextureJson(textures, textureJson);
				if (defaultTextures != null) {
					defaultTextures.forEach(unusedTextures::remove);
					tileProviders.put(fileId, new TerrainTileProvider(fileId, defaultTextures));
				} else {
					JsonObject textureObject = textureJson.getAsJsonObject();
					Map<TileElevation, List<TileTexture>> textureElevations = new HashMap<>();
					Set<TileElevation> skippedElevations = new HashSet<>();
					List<TileTexture> elevationTextures = null;
					for (TileElevation elevation : TileElevation.values()) {
						if (textureObject.has(elevation.getName())) {
							elevationTextures = resolveTextureJson(textures, textureObject.get(elevation.getName()));
							if (elevationTextures == null) throw new IllegalStateException("Malformed object %s in textures object!".formatted(elevation.getName()));
							elevationTextures.forEach(unusedTextures::remove);
							textureElevations.put(elevation, elevationTextures);
							for (TileElevation skipped : skippedElevations) {
								textureElevations.put(skipped, elevationTextures);
							}
							skippedElevations.clear();
						} else {
							skippedElevations.add(elevation);
						}
					}
					if (textureElevations.isEmpty()) {
						throw new IllegalStateException("No elevation keys were found in the textures object!");
					}
					for (TileElevation elevation : skippedElevations) {
						textureElevations.put(elevation, elevationTextures);
					}
					tileProviders.put(fileId, new TerrainTileProvider(fileId, textureElevations));
				}
			} catch (Exception e) {
				RoleplayersAtlas.LOGGER.error("[Roleplayer's Atlas] Error reading biome tile provider {}!", fileId, e);
			}
		}
		providerParents.forEach((id, parentId) -> {
			if (tileProviders.containsKey(parentId)) {
				tileProviders.put(id, tileProviders.get(parentId));
			} else {
				RoleplayersAtlas.LOGGER.error("[Roleplayer's Atlas] Error reading biome tile provider {}!", id, new IllegalStateException("Parent id %s doesn't exist".formatted(parentId)));
			}
		});

		for (TileTexture texture : unusedTextures) {
			if (texture.displayId().startsWith("test") || texture.displayId().startsWith("base")) continue;
			RoleplayersAtlas.LOGGER.warn("[Roleplayer's Atlas] Tile texture {} isn't referenced by any biome tile provider!", texture.displayId());
		}
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return List.of(TileTextures.ID);
	}
}
