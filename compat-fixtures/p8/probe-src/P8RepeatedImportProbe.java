package glam.ardor.roleplayers_atlas;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.QuickPlay;
import net.minecraft.client.RunArgs;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public final class P8RepeatedImportProbe implements ClientModInitializer {
    private static final String WORLD_FOLDER = "P8Import";
    private static final String MULTIPLAYER_FILE = "p8_player271_control.atlas";
    private static final String SINGLEPLAYER_FILE = "p8_singleplayer_control.atlas";
    private static final String MULTIPLAYER_SHA256 =
        "FA711A577D70FB6D2CD577855CF6FDBA0BF09471145BA7BCCB5A72CCE78D34DE";
    private static final String SINGLEPLAYER_SHA256 =
        "FAB249FCC170BBE1D50A54A96B634220133BC54C13B8FD78C4E62E853A53D9C2";

    private final String phase = System.getProperty("wawi.p8.phase", "");
    private final boolean active = phase.equals("import-seed") || phase.equals("import-verify");
    private final boolean seed = phase.equals("import-seed");
    private int ticks;
    private boolean quickPlayStarted;
    private boolean finished;

    @Override
    public void onInitializeClient() {
        if (active) ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        ticks++;
        try {
            require(ticks < 4800, "P8 repeated-import probe exceeded four minutes");
            if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
                if (!quickPlayStarted && ticks >= 100) {
                    require(client.getLevelStorage().levelExists(WORLD_FOLDER),
                        "disposable P8Import world is absent from the configured run directory");
                    quickPlayStarted = true;
                    QuickPlay.startQuickPlay(client, new RunArgs.SingleplayerQuickPlay(WORLD_FOLDER), null);
                }
                return;
            }

            WorldSummary summary = SurveyorClient.tryGetSummary(World.OVERWORLD);
            WorldAtlasData atlas = WorldAtlasData.WORLDS.get(World.OVERWORLD);
            if (client.world.getRegistryKey() != World.OVERWORLD || summary == null || summary.terrain() == null
                || summary.landmarks() == null || atlas == null || atlas.isLoading()
                || atlas.exploredChunks().size() < 1000) return;

            if (seed) seedAndVerify(client, summary, atlas);
            else verifyRestart(client, summary, atlas);
            finished = true;
            client.scheduleStop();
        } catch (Throwable throwable) {
            finished = true;
            throwable.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    private void seedAndVerify(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) throws Exception {
        Path multiplayer = scroll(MULTIPLAYER_FILE);
        Path singleplayer = scroll(SINGLEPLAYER_FILE);
        assertSources(multiplayer, singleplayer);
        require("P8Reader".equals(AtlasTime.selfName()), "import probe did not launch with fixed P8Reader identity");

        UUID owner = SurveyorClient.getClientUuid();
        MapShare.Preview multiplayerPreview = MapShare.peek(multiplayer, World.OVERWORLD);
        require(multiplayerPreview != null && multiplayerPreview.sameDimension(),
            "P0 Player271 scroll failed Overworld preview");
        require(multiplayerPreview.markers() == 1 && multiplayerPreview.labels() == 0
                && multiplayerPreview.routes() == 0 && multiplayerPreview.territories() == 0,
            "P0 Player271 scroll changed landmark classification");
        require(multiplayerPreview.regions() == 4 && multiplayerPreview.authors().contains("Player271"),
            "P0 Player271 preview changed terrain count or author");

        Set<Identifier> markerIdsBefore = new HashSet<>(summary.landmarks().asMap(owner, null).keySet());
        int terrainBefore = explored(summary);
        AtlasUndo.clear();
        MapShare.ImportResult first = MapShare.importFile(multiplayer, World.OVERWORLD, summary,
            client.world.getRegistryManager());
        require(first.error() == null && first.landmarks() == 1 && first.regions() == 4 && first.corrections() == 0,
            "first P0 Player271 import returned unexpected result " + first);
        int terrainAfterFirst = explored(summary);
        require(terrainAfterFirst >= terrainBefore, "first import removed known terrain");

        Set<Identifier> markerIdsAfter = new HashSet<>(summary.landmarks().asMap(owner, null).keySet());
        markerIdsAfter.removeAll(markerIdsBefore);
        require(markerIdsAfter.size() == 1, "first import did not add exactly one stable landmark identity");
        Identifier markerId = markerIdsAfter.iterator().next();
        Landmark firstMarker = requireMarker(summary, owner, markerId);
        assertHearsay(firstMarker, owner, "Player271", "scroll_player271");

        MapShare.ImportResult second = MapShare.importFile(multiplayer, World.OVERWORLD, summary,
            client.world.getRegistryManager());
        require(second.equals(first), "second P0 Player271 import changed its result");
        Landmark secondMarker = requireMarker(summary, owner, markerId);
        require(stableMarker(firstMarker, secondMarker), "second import changed landmark identity or content");
        require(summary.landmarks().asMap(owner, null).keySet().stream().filter(markerId::equals).count() == 1,
            "second import duplicated the Player271 landmark");
        require(MarkerLayers.all().stream().filter(layer -> layer.id().equals("scroll_player271")).count() == 1,
            "second import duplicated the Player271 source layer");

        require(AtlasUndo.undo() != null, "second landmark import had no grouped undo");
        assertHearsay(requireMarker(summary, owner, markerId), owner, "Player271", "scroll_player271");
        require(AtlasUndo.undo() != null, "first landmark import had no grouped undo");
        require(summary.landmarks().get(owner, markerId) == null, "two landmark undos retained imported marker");
        require(MarkerLayers.get("scroll_player271") == null, "two landmark undos retained imported layer");
        require(explored(summary) >= terrainAfterFirst, "grouped landmark undo incorrectly removed additive terrain");
        require(AtlasUndo.redo() != null && AtlasUndo.redo() != null, "landmark grouped redo did not replay twice");
        Landmark redoneMarker = requireMarker(summary, owner, markerId);
        assertHearsay(redoneMarker, owner, "Player271", "scroll_player271");
        require(stableMarker(firstMarker, redoneMarker), "landmark redo changed stable identity or content");

        MapShare.Preview correctionPreview = MapShare.peek(singleplayer, World.OVERWORLD);
        require(correctionPreview != null && correctionPreview.sameDimension(),
            "P0 singleplayer scroll failed Overworld preview");
        int correctionCount = correctionPreview.biomeCorrections() + correctionPreview.patches()
            + correctionPreview.towns();
        require(correctionPreview.regions() == 6 && correctionCount > 0,
            "P0 singleplayer scroll lost terrain or correction controls");
        String correctionAuthor = correctionPreview.authors().stream().filter(author -> !author.isBlank()).findFirst()
            .orElse(SINGLEPLAYER_FILE.replaceFirst("\\.atlas$", ""));
        require(!correctionAuthor.equals(AtlasTime.selfName()), "correction control unexpectedly became self-authored");

        AtlasUndo.clear();
        MapShare.ImportResult correctionsFirst = MapShare.importFile(singleplayer, World.OVERWORLD, summary,
            client.world.getRegistryManager());
        require(correctionsFirst.error() == null && correctionsFirst.landmarks() == 0
                && correctionsFirst.regions() == 6 && correctionsFirst.corrections() == correctionCount,
            "first correction import returned unexpected result " + correctionsFirst);
        assertCorrectionSheet(correctionAuthor, correctionPreview);
        MapShare.ImportResult correctionsSecond = MapShare.importFile(singleplayer, World.OVERWORLD, summary,
            client.world.getRegistryManager());
        require(correctionsSecond.equals(correctionsFirst), "second correction import changed its result");
        assertCorrectionSheet(correctionAuthor, correctionPreview);

        require(AtlasUndo.undo() != null, "second correction import had no grouped undo");
        assertCorrectionSheet(correctionAuthor, correctionPreview);
        require(AtlasUndo.undo() != null, "first correction import had no grouped undo");
        require(!BiomeOverrides.importedAuthors().contains(correctionAuthor)
                && !CityPaint.importedAuthors().contains(correctionAuthor),
            "two correction undos retained imported sheets");
        require(AtlasUndo.redo() != null && AtlasUndo.redo() != null, "correction grouped redo did not replay twice");
        assertCorrectionSheet(correctionAuthor, correctionPreview);

        assertSources(multiplayer, singleplayer);
        Properties manifest = new Properties();
        manifest.setProperty("markerId", markerId.toString());
        manifest.setProperty("markerAuthor", "Player271");
        manifest.setProperty("markerLayer", "scroll_player271");
        manifest.setProperty("correctionAuthor", correctionAuthor);
        manifest.setProperty("biomes", Integer.toString(correctionPreview.biomeCorrections()));
        manifest.setProperty("patches", Integer.toString(correctionPreview.patches()));
        manifest.setProperty("towns", Integer.toString(correctionPreview.towns()));
        manifest.setProperty("terrainBits", Integer.toString(explored(summary)));
        manifest.setProperty("atlasTiles", Integer.toString(atlas.exploredChunks().size()));
        try (OutputStream out = Files.newOutputStream(manifestPath())) {
            manifest.store(out, "P8 disposable repeated-import manifest");
        }
        AtlasUndo.clear();
        System.out.printf(
            "P8_IMPORT_CHECKPOINT phase=seed imports=4 markerId=%s author=Player271 layer=scroll_player271 regions=10 corrections=%d biomes=%d patches=%d towns=%d terrainBits=%d atlasTiles=%d sourceNonmutation=1 groupedUndoRedo=1%n",
            markerId, correctionCount, correctionPreview.biomeCorrections(), correctionPreview.patches(),
            correctionPreview.towns(), explored(summary), atlas.exploredChunks().size());
        System.out.println("P8_REPEATED_IMPORT_PASS phase=seed");
        System.out.flush();
    }

    private void verifyRestart(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) throws Exception {
        Path multiplayer = scroll(MULTIPLAYER_FILE);
        Path singleplayer = scroll(SINGLEPLAYER_FILE);
        assertSources(multiplayer, singleplayer);
        Properties manifest = new Properties();
        try (InputStream in = Files.newInputStream(manifestPath())) {
            manifest.load(in);
        }

        UUID owner = SurveyorClient.getClientUuid();
        Identifier markerId = Identifier.tryParse(requireProperty(manifest, "markerId"));
        require(markerId != null, "persisted import manifest has invalid marker id");
        String markerAuthor = requireProperty(manifest, "markerAuthor");
        String markerLayer = requireProperty(manifest, "markerLayer");
        Landmark marker = requireMarker(summary, owner, markerId);
        assertHearsay(marker, owner, markerAuthor, markerLayer);
        require(summary.landmarks().asMap(owner, null).keySet().stream().filter(markerId::equals).count() == 1,
            "restart duplicated the Player271 landmark");
        require(MarkerLayers.all().stream().filter(layer -> layer.id().equals(markerLayer)).count() == 1,
            "restart duplicated the Player271 layer");

        String correctionAuthor = requireProperty(manifest, "correctionAuthor");
        int biomes = Integer.parseInt(requireProperty(manifest, "biomes"));
        int patches = Integer.parseInt(requireProperty(manifest, "patches"));
        int towns = Integer.parseInt(requireProperty(manifest, "towns"));
        assertCorrectionSheet(correctionAuthor, biomes, patches, towns);
        int terrainBits = Integer.parseInt(requireProperty(manifest, "terrainBits"));
        int atlasTiles = Integer.parseInt(requireProperty(manifest, "atlasTiles"));
        require(explored(summary) >= terrainBits, "restart lost imported terrain bits");
        require(atlas.exploredChunks().size() >= atlasTiles, "restart lost imported Atlas tiles");

        System.out.printf(
            "P8_IMPORT_CHECKPOINT phase=restart markerId=%s author=%s layer=%s biomes=%d patches=%d towns=%d terrainBits=%d atlasTiles=%d sourceNonmutation=1 stableIdentity=1%n",
            markerId, markerAuthor, markerLayer, biomes, patches, towns, explored(summary),
            atlas.exploredChunks().size());
        System.out.println("P8_REPEATED_IMPORT_PASS phase=restart");
        System.out.flush();
    }

    private static void assertCorrectionSheet(String author, MapShare.Preview preview) {
        assertCorrectionSheet(author, preview.biomeCorrections(), preview.patches(), preview.towns());
    }

    private static void assertCorrectionSheet(String author, int biomes, int patches, int towns) {
        if (biomes + patches > 0) {
            require(BiomeOverrides.importedAuthors().stream().filter(author::equals).count() == 1,
                "correction author sheet is absent or duplicated");
            BiomeOverrides.Sheet sheet = BiomeOverrides.importedSheet(author);
            require(sheet != null && sheet.biomeCount() == biomes && sheet.patchCount() == patches,
                "correction sheet counts changed");
        }
        if (towns > 0) {
            require(CityPaint.importedAuthors().stream().filter(author::equals).count() == 1,
                "city author sheet is absent or duplicated");
            Map<Identifier, Map<net.minecraft.util.math.ChunkPos, Identifier>> byDimension =
                CityPaint.capture().shared().get(author);
            Map<net.minecraft.util.math.ChunkPos, Identifier> cells = byDimension == null
                ? null : byDimension.get(World.OVERWORLD.getValue());
            require(cells != null && cells.size() == towns, "city sheet count changed");
        }
    }

    private static void assertHearsay(Landmark marker, UUID owner, String author, String layer) {
        require(marker.owner().equals(owner), "imported marker is not owned by the receiving Surveyor identity");
        require(author.equals(marker.get(AtlasComponents.SOURCE)), "imported marker lost source attribution");
        require(layer.equals(RoleplayersAtlas.layerOf(marker)), "imported marker lost source layer");
        require(AtlasTime.isHearsay(marker), "imported foreign marker is no longer hearsay");
        require(MarkerLayers.get(layer) != null, "imported source layer is absent");
    }

    private static boolean stableMarker(Landmark first, Landmark second) {
        return first.owner().equals(second.owner()) && first.id().equals(second.id())
            && java.util.Objects.equals(first.get(LandmarkComponentTypes.POS), second.get(LandmarkComponentTypes.POS))
            && java.util.Objects.equals(first.get(LandmarkComponentTypes.NAME), second.get(LandmarkComponentTypes.NAME))
            && java.util.Objects.equals(first.get(AtlasComponents.SOURCE), second.get(AtlasComponents.SOURCE))
            && java.util.Objects.equals(first.get(AtlasComponents.LAYER), second.get(AtlasComponents.LAYER))
            && java.util.Objects.equals(first.get(AtlasComponents.NOTE), second.get(AtlasComponents.NOTE));
    }

    private static Landmark requireMarker(WorldSummary summary, UUID owner, Identifier id) {
        Landmark marker = summary.landmarks().get(owner, id);
        if (marker == null) throw new IllegalStateException("imported marker is absent: " + id);
        return marker;
    }

    private static int explored(WorldSummary summary) {
        return summary.terrain().bitSet(SurveyorClient.getExploration()).values().stream()
            .mapToInt(java.util.BitSet::cardinality).sum();
    }

    private static Path scroll(String name) {
        return FabricLoader.getInstance().getConfigDir().resolve("roleplayers-atlas").resolve("scrolls").resolve(name);
    }

    private static Path manifestPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("roleplayers-atlas").resolve("p8_import_manifest.properties");
    }

    private static void assertSources(Path multiplayer, Path singleplayer) throws Exception {
        require(MULTIPLAYER_SHA256.equals(sha256(multiplayer)), "P0 Player271 scroll copy changed");
        require(SINGLEPLAYER_SHA256.equals(sha256(singleplayer)), "P0 singleplayer scroll copy changed");
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static String requireProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("import manifest is missing " + key);
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
