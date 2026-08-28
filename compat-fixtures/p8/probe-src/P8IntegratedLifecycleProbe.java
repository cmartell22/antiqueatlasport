package glam.ardor.roleplayers_atlas;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import glam.ardor.roleplayers_atlas.reloader.TileTextures;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.QuickPlay;
import net.minecraft.client.RunArgs;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class P8IntegratedLifecycleProbe implements ClientModInitializer {
    private static final String WORLD_FOLDER = "P8Auto";
    private static final String LAYER_ID = "p8_lifecycle";
    private static final Identifier SYNTHETIC_BIOME = RoleplayersAtlas.id("p8_lifecycle_unknown");
    private static final ChunkPos PATCH_CHUNK = new ChunkPos(200, 200);
    private static final ChunkPos CITY_CHUNK = new ChunkPos(201, 200);

    private final boolean seed = "seed".equals(System.getProperty("wawi.p8.phase"));
    private Stage stage = Stage.MENU_INITIAL;
    private int totalTicks;
    private int stageTicks;
    private boolean transitionRequested;
    private boolean finished;
    private Identifier cityTile;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        totalTicks++;
        stageTicks++;
        try {
            require(totalTicks < 12000, "P8 integrated lifecycle exceeded ten minutes");
            switch (stage) {
                case MENU_INITIAL -> startInitial(client);
                case WAIT_OVERWORLD_INITIAL -> initialOverworld(client);
                case WAIT_OVERWORLD_SEEDED -> seededOverworld(client);
                case WAIT_NETHER -> dimensionStage(client, World.NETHER, Stage.WAIT_OVERWORLD_RETURN_ONE);
                case WAIT_OVERWORLD_RETURN_ONE -> dimensionStage(client, World.OVERWORLD, Stage.WAIT_END);
                case WAIT_END -> dimensionStage(client, World.END, Stage.WAIT_OVERWORLD_RETURN_TWO);
                case WAIT_OVERWORLD_RETURN_TWO -> finishTransitions(client);
                case WAIT_DISCONNECT -> waitForDisconnect(client);
                case WAIT_REJOIN -> verifyRejoin(client);
            }
        } catch (Throwable throwable) {
            finished = true;
            throwable.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    private void startInitial(MinecraftClient client) {
        if (client.world != null || client.player != null) return;
        if (stageTicks < 100) return;
        require(client.getLevelStorage().levelExists(WORLD_FOLDER),
            "disposable P8Auto world is absent from the configured run directory");
        QuickPlay.startQuickPlay(client, new RunArgs.SingleplayerQuickPlay(WORLD_FOLDER), null);
        advance(Stage.WAIT_OVERWORLD_INITIAL);
    }

    private void initialOverworld(MinecraftClient client) {
        if (!ready(client, World.OVERWORLD, seed ? 1000 : 1)) return;
        if (!seed) {
            verifyAll(client, "fresh-jvm");
            passAndStop(client, "verify");
            return;
        }

        seedLocalStores(client);
        seedMarker(client, World.OVERWORLD);
        advance(Stage.WAIT_OVERWORLD_SEEDED);
    }

    private void seededOverworld(MinecraftClient client) {
        if (!ready(client, World.OVERWORLD, 1000)) return;
        verifyDimension(World.OVERWORLD, 1000);
        requestTransition(client, World.NETHER);
        advance(Stage.WAIT_NETHER);
    }

    private void dimensionStage(MinecraftClient client, RegistryKey<World> dimension, Stage next) {
        if (!ready(client, dimension, 1)) return;
        if (seed && dimension != World.OVERWORLD) seedMarker(client, dimension);
        verifyDimension(dimension, dimension == World.OVERWORLD ? 1000 : 1);

        RegistryKey<World> target = next == Stage.WAIT_OVERWORLD_RETURN_ONE
            || next == Stage.WAIT_OVERWORLD_RETURN_TWO ? World.OVERWORLD
            : next == Stage.WAIT_END ? World.END : World.OVERWORLD;
        requestTransition(client, target);
        advance(next);
    }

    private void finishTransitions(MinecraftClient client) {
        if (!ready(client, World.OVERWORLD, 1000)) return;
        verifyAll(client, "post-transitions");
        client.disconnectWithSavingScreen();
        advance(Stage.WAIT_DISCONNECT);
    }

    private void waitForDisconnect(MinecraftClient client) {
        if (client.world != null || client.player != null || client.getServer() != null) return;
        if (stageTicks < 40) return;
        require(WorldAtlasData.WORLDS.isEmpty(), "Atlas retained world data after integrated disconnect");
        QuickPlay.startQuickPlay(client, new RunArgs.SingleplayerQuickPlay(WORLD_FOLDER), null);
        advance(Stage.WAIT_REJOIN);
    }

    private void verifyRejoin(MinecraftClient client) {
        if (!ready(client, World.OVERWORLD, 1000)) return;
        verifyAll(client, "same-process-rejoin");
        passAndStop(client, "seed-and-rejoin");
    }

    private void seedLocalStores(MinecraftClient client) {
        MarkerLayers.put(new MarkerLayers.MapLayer(LAYER_ID, "P8 Lifecycle", 0x4477AA));
        require(MarkerLayers.get(LAYER_ID) != null, "P8 marker layer did not seed");

        BiomeOverrides.set(SYNTHETIC_BIOME, BiomeKeys.PLAINS.getValue());
        BiomeOverrides.setPatches(World.OVERWORLD, List.of(PATCH_CHUNK), BiomeKeys.FOREST.getValue());

        cityTile = TileTextures.getInstance().getTextures().keySet().stream()
            .filter(id -> id.getPath().startsWith("structure/village/"))
            .sorted(java.util.Comparator.comparing(Identifier::toString))
            .findFirst().orElseThrow(() -> new IllegalStateException("no village tile is loaded"));
        CityPaint.set(World.OVERWORLD, List.of(CITY_CHUNK), cityTile);
        require(cityTile.equals(CityPaint.at(World.OVERWORLD, CITY_CHUNK)), "P8 city cell did not seed");
    }

    private void seedMarker(MinecraftClient client, RegistryKey<World> dimension) {
        WorldSummary summary = requireSummary(dimension);
        UUID owner = SurveyorClient.getClientUuid();
        Identifier id = markerId(dimension);
        if (!summary.landmarks().contains(owner, id)) {
            // Keep the tracked control well outside the default arrival radius. A
            // marker at the player position is intentionally cleared by Atlas.
            BlockPos pos = new BlockPos(640, 80, 640);
            Landmark marker = Landmark.create(owner, id, b -> b
                .add(LandmarkComponentTypes.POS, pos)
                .add(LandmarkComponentTypes.NAME, Text.literal("P8 " + dimension.getValue().getPath()))
                .add(AtlasComponents.LAYER, LAYER_ID)
                .add(AtlasComponents.NOTE, "persistent lifecycle marker"));
            WorldAtlasData.swapLandmark(dimension, null, marker, Text.literal("P8 seed marker"));
            require(summary.landmarks().contains(owner, id), "P8 marker did not enter Surveyor for " + dimension.getValue());
            if (dimension == World.OVERWORLD) {
                RoleplayersAtlas.trackedMarkers.add(RoleplayersAtlas.trackKey(marker));
                TrackedMarkersStore.save();
            }
        }
    }

    private void verifyAll(MinecraftClient client, String checkpoint) {
        require(client.isInSingleplayer() && client.getServer() != null, "P8 lifecycle is not integrated singleplayer");
        verifyDimension(World.OVERWORLD, 1000);
        verifyDimension(World.NETHER, 1);
        verifyDimension(World.END, 1);

        require(MarkerLayers.get(LAYER_ID) != null, "P8 marker layer did not persist at " + checkpoint);
        require(BiomeKeys.PLAINS.getValue().equals(BiomeOverrides.get(SYNTHETIC_BIOME)),
            "P8 biome override did not persist at " + checkpoint);
        require(BiomeKeys.FOREST.getValue().equals(BiomeOverrides.patch(World.OVERWORLD, PATCH_CHUNK)),
            "P8 chunk patch did not persist at " + checkpoint);

        Identifier expectedCity = cityTile != null ? cityTile : TileTextures.getInstance().getTextures().keySet().stream()
            .filter(id -> id.getPath().startsWith("structure/village/"))
            .sorted(java.util.Comparator.comparing(Identifier::toString))
            .findFirst().orElseThrow();
        require(expectedCity.equals(CityPaint.at(World.OVERWORLD, CITY_CHUNK)),
            "P8 city cell did not persist at " + checkpoint);

        Landmark overworldMarker = requireSummary(World.OVERWORLD).landmarks().get(
            SurveyorClient.getClientUuid(), markerId(World.OVERWORLD));
        require(overworldMarker != null, "P8 Overworld marker missing at " + checkpoint);
        require(RoleplayersAtlas.trackedMarkers.contains(RoleplayersAtlas.trackKey(overworldMarker)),
            "P8 tracked marker did not persist at " + checkpoint);

        System.out.printf(
            "P8_INTEGRATED_CHECKPOINT checkpoint=%s overworldTiles=%d netherTiles=%d endTiles=%d markers=3 layer=1 biome=1 patch=1 city=1 tracked=1 worlds=%d%n",
            checkpoint,
            WorldAtlasData.WORLDS.get(World.OVERWORLD).exploredChunks().size(),
            WorldAtlasData.WORLDS.get(World.NETHER).exploredChunks().size(),
            WorldAtlasData.WORLDS.get(World.END).exploredChunks().size(),
            WorldAtlasData.WORLDS.size());
        System.out.flush();
    }

    private void verifyDimension(RegistryKey<World> dimension, int minimumTiles) {
        WorldSummary summary = requireSummary(dimension);
        require(summary.terrain() != null, "Surveyor terrain absent for " + dimension.getValue());
        require(summary.structures() != null, "Surveyor structures absent for " + dimension.getValue());
        require(summary.landmarks() != null, "Surveyor landmarks absent for " + dimension.getValue());

        UUID owner = SurveyorClient.getClientUuid();
        Identifier expected = markerId(dimension);
        require(summary.landmarks().contains(owner, expected), "P8 marker absent for " + dimension.getValue());
        long ownMatches = summary.landmarks().asMap(owner, null).keySet().stream().filter(expected::equals).count();
        require(ownMatches == 1, "P8 marker duplicated for " + dimension.getValue());
        for (RegistryKey<World> other : List.of(World.OVERWORLD, World.NETHER, World.END)) {
            if (other != dimension) require(!summary.landmarks().contains(owner, markerId(other)),
                "P8 marker leaked from " + other.getValue() + " into " + dimension.getValue());
        }

        WorldAtlasData atlas = WorldAtlasData.WORLDS.get(dimension);
        require(atlas != null && !atlas.isLoading(), "Atlas data unavailable for " + dimension.getValue());
        require(atlas.exploredChunks().size() >= minimumTiles,
            "Atlas retained fewer than " + minimumTiles + " tiles for " + dimension.getValue());
    }

    private boolean ready(MinecraftClient client, RegistryKey<World> dimension, int minimumTiles) {
        if (client.world == null || client.player == null || client.getNetworkHandler() == null) return false;
        if (client.world.getRegistryKey() != dimension) return false;
        WorldSummary summary = SurveyorClient.tryGetSummary(dimension);
        WorldAtlasData atlas = WorldAtlasData.WORLDS.get(dimension);
        if (summary == null || summary.landmarks() == null || atlas == null || atlas.isLoading()
            || atlas.exploredChunks().size() < minimumTiles) {
            require(stageTicks < 2400, "P8 stage did not initialize within 120 seconds: " + stage);
            return false;
        }
        return true;
    }

    private void requestTransition(MinecraftClient client, RegistryKey<World> targetKey) {
        require(!transitionRequested, "duplicate P8 transition request from " + stage);
        transitionRequested = true;
        MinecraftServer server = client.getServer();
        require(server != null, "integrated server absent during P8 transition");
        UUID playerId = client.player.getUuid();
        server.execute(() -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            ServerWorld target = server.getWorld(targetKey);
            require(player != null, "server player absent during P8 transition");
            require(target != null, "target world absent during P8 transition: " + targetKey.getValue());
            player.changeGameMode(GameMode.SPECTATOR);
            double x = targetKey == World.END ? 100.5 : 0.5;
            double y = targetKey == World.NETHER ? 128.0 : 80.0;
            double z = 0.5;
            require(player.teleport(target, x, y, z, Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false),
                "server rejected P8 transition to " + targetKey.getValue());
        });
    }

    private WorldSummary requireSummary(RegistryKey<World> dimension) {
        WorldSummary summary = SurveyorClient.tryGetSummary(dimension);
        if (summary == null) throw new IllegalStateException("Surveyor summary absent for " + dimension.getValue());
        return summary;
    }

    private void passAndStop(MinecraftClient client, String phase) {
        System.out.printf("P8_INTEGRATED_LIFECYCLE_PASS phase=%s totalTicks=%d%n", phase, totalTicks);
        System.out.flush();
        finished = true;
        client.scheduleStop();
    }

    private void advance(Stage next) {
        stage = next;
        stageTicks = 0;
        transitionRequested = false;
    }

    private static Identifier markerId(RegistryKey<World> dimension) {
        return RoleplayersAtlas.id("custom/skull/white/p8_" + dimension.getValue().getPath());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private enum Stage {
        MENU_INITIAL,
        WAIT_OVERWORLD_INITIAL,
        WAIT_OVERWORLD_SEEDED,
        WAIT_NETHER,
        WAIT_OVERWORLD_RETURN_ONE,
        WAIT_END,
        WAIT_OVERWORLD_RETURN_TWO,
        WAIT_DISCONNECT,
        WAIT_REJOIN
    }
}
