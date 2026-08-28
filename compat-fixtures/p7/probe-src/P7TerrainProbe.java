package glam.ardor.roleplayers_atlas;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.util.RegionPos;
import glam.ardor.roleplayers_atlas.reloader.BiomeNames;
import glam.ardor.roleplayers_atlas.reloader.BiomeTileProviders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.QuickPlay;
import net.minecraft.client.RunArgs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import java.util.BitSet;
import java.util.Map;

public final class P7TerrainProbe implements ClientModInitializer {
    private int ticks;
    private boolean finished;
    private boolean quickPlayStarted;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        ticks++;
        try {
            if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
                if (!quickPlayStarted && ticks >= 100) {
                    require(client.getLevelStorage().levelExists("P7Auto"),
                        "disposable P7Auto world is absent from the configured run directory");
                    quickPlayStarted = true;
                    QuickPlay.startQuickPlay(client, new RunArgs.SingleplayerQuickPlay("P7Auto"), null);
                }
                require(ticks < 2400, "client did not join the disposable world within 120 seconds");
                return;
            }
            if (!BiomeTileProviders.getInstance().hasFallbacks()) {
                require(ticks < 2400, "biome fallback registration did not complete within 120 seconds");
                return;
            }

            WorldSummary summary = SurveyorClient.tryGetSummary(client.world.getRegistryKey());
            WorldAtlasData atlas = WorldAtlasData.WORLDS.get(client.world.getRegistryKey());
            if (summary == null || atlas == null || atlas.isLoading() || atlas.exploredChunks().size() < 16) {
                require(ticks < 2400, "Atlas did not resolve sixteen target terrain tiles within 120 seconds");
                return;
            }

            probe(summary, atlas);
            P7AnnotationProbe.probe(client, summary, atlas);
            P7SharingProbe.probe(client, summary, atlas);
            finished = true;
            client.scheduleStop();
        } catch (Throwable throwable) {
            finished = true;
            throwable.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    private static void probe(WorldSummary summary, WorldAtlasData atlas) {
        require(summary.dimension() == World.OVERWORLD, "disposable probe world is not the Overworld");
        require(summary.terrain() != null, "Surveyor terrain is absent");
        Map<RegionPos, BitSet> explored = summary.terrain().bitSet(SurveyorClient.getExploration());
        require(!explored.isEmpty(), "Surveyor exploration is empty");

        int tiles = atlas.exploredChunks().size();
        int biomeIds = 0;
        int providerIds = 0;
        for (ChunkPos pos : atlas.exploredChunks()) {
            require(atlas.getTile(pos) != null, "resolved explored chunk has no tile at " + pos);
            if (atlas.biomeAt(pos) != null) biomeIds++;
            if (atlas.getProvider(pos) != null) providerIds++;
        }
        require(biomeIds >= 16, "fewer than sixteen explored chunks retained biome identity");
        require(providerIds >= 16, "fewer than sixteen explored chunks retained provider identity");

        var scope = atlas.getScope();
        require(scope.minX <= scope.maxX && scope.minY <= scope.maxY, "Atlas tile scope is inverted");
        var batches = atlas.getTileBatches(scope.minX, scope.minY, scope.maxX + 1, scope.maxY + 1, 1);
        require(!batches.isEmpty(), "resolved Atlas scope produced no tile batches");
        int subTiles = batches.values().stream().mapToInt(java.util.List::size).sum();
        require(subTiles > 0, "resolved Atlas scope produced zero subtiles");

        var looks = BiomeTileProviders.getInstance().availableLooks();
        require(looks.contains(BiomeKeys.PLAINS.getValue()), "plains tile provider is absent");
        require(looks.contains(BiomeKeys.FOREST.getValue()), "forest tile provider is absent");
        require(looks.contains(BiomeKeys.RIVER.getValue()), "river tile provider is absent");
        require(looks.contains(BiomeKeys.NETHER_WASTES.getValue()), "Nether tile provider is absent");
        require(looks.contains(BiomeKeys.THE_END.getValue()), "End tile provider is absent");

        require(BiomeKeys.CHERRY_GROVE.getValue().equals(BiomeNames.guess(id("test:sakura_forest"))),
            "sakura forest name fallback changed");
        require(BiomeKeys.DEEP_COLD_OCEAN.getValue().equals(BiomeNames.guess(id("test:deep_cold_ocean"))),
            "cold deep ocean name fallback changed");
        require(BiomeKeys.END_HIGHLANDS.getValue().equals(BiomeNames.guess(id("test:outer_end_mountain"))),
            "outer End mountain name fallback changed");
        require(BiomeKeys.WARPED_FOREST.getValue().equals(BiomeNames.guess(id("test:nether_pine_forest"))),
            "Nether forest name fallback changed");
        require(BiomeNames.guess(id("test:featureless_unknown")) == null,
            "unknown biome name unexpectedly predicts a look");

        int discoveredStructures = summary.structures() == null ? 0
            : summary.structures().keySet(SurveyorClient.getExploration()).size();
        System.out.printf(
            "P7_TERRAIN_PROBE_PASS tiles=%d biomeIds=%d providerIds=%d batches=%d subTiles=%d explorationRegions=%d discoveredStructures=%d looks=%d%n",
            tiles, biomeIds, providerIds, batches.size(), subTiles, explored.size(), discoveredStructures, looks.size());
        System.out.flush();
    }

    private static Identifier id(String value) {
        Identifier id = Identifier.tryParse(value);
        if (id == null) throw new IllegalArgumentException(value);
        return id;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
