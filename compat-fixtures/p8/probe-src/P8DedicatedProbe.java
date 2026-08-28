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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class P8DedicatedProbe implements ClientModInitializer {
    private static final String FALSE_ADDRESS = "127.0.0.1:25578";
    private static final String TRUE_ADDRESS = "127.0.0.1:25579";
    private static final String P0_TERRAIN_FILE = "p8_p0_terrain_control.atlas";
    private static final String P0_TERRAIN_SHA256 =
        "FAB249FCC170BBE1D50A54A96B634220133BC54C13B8FD78C4E62E853A53D9C2";
    private static final Identifier A_MARKER_ID = RoleplayersAtlas.id("custom/skull/white/p8_dedicated_a");
    private static final Identifier B_MARKER_ID = RoleplayersAtlas.id("custom/skull/white/p8_dedicated_b");
    private static final Identifier TRUE_MARKER_ID = RoleplayersAtlas.id("custom/skull/white/p8_global_true_a");

    private final String phase = System.getProperty("wawi.p8.phase", "");
    private final boolean active = phase.startsWith("dedicated-") || phase.startsWith("global-");
    private int ticks;
    private int joinedTicks;
    private int mutationTick;
    private boolean quickPlayStarted;
    private boolean mutated;
    private boolean finished;
    private Landmark stagedMarker;
    private Path stagedScroll;
    private int stagedTerrain;

    @Override
    public void onInitializeClient() {
        if (active) ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (finished) return;
        ticks++;
        try {
            require(ticks < 6000, "P8 dedicated probe exceeded five minutes: " + phase);
            if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
                if (!quickPlayStarted && ticks >= 100) {
                    quickPlayStarted = true;
                    String address = phase.startsWith("global-") ? TRUE_ADDRESS : FALSE_ADDRESS;
                    QuickPlay.startQuickPlay(client, new RunArgs.MultiplayerQuickPlay(address), null);
                }
                return;
            }

            joinedTicks++;
            require(client.getServer() == null, "P8 dedicated probe joined an integrated server");
            WorldSummary summary = SurveyorClient.tryGetSummary(World.OVERWORLD);
            WorldAtlasData atlas = WorldAtlasData.WORLDS.get(World.OVERWORLD);
            if (client.world.getRegistryKey() != World.OVERWORLD || summary == null || summary.terrain() == null
                || summary.landmarks() == null || atlas == null || atlas.isLoading()
                || atlas.exploredChunks().size() < 16 || joinedTicks < 120) return;

            switch (phase) {
                case "dedicated-a-seed" -> runSeed(client, summary, atlas, this::mutateA, this::finishA);
                case "dedicated-a-verify" -> passAfter(() -> verifyA(client, summary, atlas), client,
                    "P8_DEDICATED_A_PASS phase=restart-reconnect");
                case "dedicated-b-seed" -> runSeed(client, summary, atlas, this::mutateB, this::finishB);
                case "dedicated-b-verify" -> passAfter(() -> verifyB(client, summary, atlas), client,
                    "P8_DEDICATED_B_PASS phase=restart-reconnect-isolation");
                case "dedicated-a-isolation" -> passAfter(() -> verifyAIsolation(client, summary, atlas), client,
                    "P8_DEDICATED_A_PASS phase=post-b-isolation");
                case "global-a-seed" -> runSeed(client, summary, atlas, this::mutateTrueA, this::finishTrueA);
                case "global-b-verify" -> passAfter(() -> verifyTrueB(client, summary, atlas), client,
                    "P8_GLOBAL_SHARING_PASS phase=clean-default-visible");
                default -> throw new IllegalStateException("unknown P8 dedicated phase " + phase);
            }
        } catch (Throwable throwable) {
            finished = true;
            throwable.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    private void runSeed(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas,
                         CheckedAction mutation, CheckedAction verification) throws Exception {
        if (!mutated) {
            mutation.run();
            mutated = true;
            mutationTick = joinedTicks;
            return;
        }
        if (atlas.isLoading() || joinedTicks - mutationTick < 120) return;
        verification.run();
        passAndStop(client, phase.startsWith("global-")
            ? "P8_GLOBAL_SHARING_PASS phase=seed"
            : "P8_DEDICATED_" + (phase.contains("-a-") ? "A" : "B") + "_PASS phase=seed");
    }

    private void passAfter(CheckedAction verification, MinecraftClient client, String sentinel) throws Exception {
        verification.run();
        passAndStop(client, sentinel);
    }

    private void mutateA() throws Exception {
        require("P8Alice".equals(AtlasTime.selfName()), "dedicated A did not use P8Alice identity");
        MinecraftClient client = MinecraftClient.getInstance();
        WorldSummary summary = requireSummary();
        Path terrainControl = scroll(P0_TERRAIN_FILE);
        require(P0_TERRAIN_SHA256.equals(sha256(terrainControl)), "P0 terrain control copy changed");
        int before = explored(summary);
        AtlasUndo.clear();
        MapShare.ImportResult terrain = MapShare.importFile(terrainControl, World.OVERWORLD, summary,
            client.world.getRegistryManager(), false);
        require(terrain.error() == null && terrain.landmarks() == 0 && terrain.regions() == 6,
            "dedicated A terrain seed import changed: " + terrain);
        require(AtlasUndo.undo() != null, "dedicated A terrain seed had no grouped correction undo");
        AtlasUndo.clear();
        stagedTerrain = explored(summary);
        require(stagedTerrain > before, "dedicated A did not gain private terrain from the control scroll");

        UUID owner = SurveyorClient.getClientUuid();
        stagedMarker = Landmark.create(owner, A_MARKER_ID, builder -> builder
            .add(LandmarkComponentTypes.POS, new BlockPos(640, 80, 640))
            .add(LandmarkComponentTypes.NAME, Text.literal("P8 Alice marker"))
            .add(AtlasComponents.NOTE, "dedicated restart and isolation control")
            .add(AtlasComponents.DAY, AtlasTime.gameDay()));
        summary.landmarks().put(stagedMarker);
        stagedScroll = MapShare.export(World.OVERWORLD, summary, client.world.getRegistryManager(),
            List.of(stagedMarker), true, false, true, "p8_alice");
        require(stagedMarker.get(AtlasComponents.SOURCE) == null,
            "signed Alice export mutated its source landmark");
        MapShare.Preview preview = MapShare.peek(stagedScroll, World.OVERWORLD);
        require(preview != null && preview.sameDimension() && preview.markers() == 1
                && preview.regions() > 0 && preview.authors().contains("P8Alice"),
            "Alice export preview lost terrain, marker, or author");
        require(P0_TERRAIN_SHA256.equals(sha256(terrainControl)), "P0 terrain control mutated during A seed");
    }

    private void finishA() throws Exception {
        WorldSummary summary = requireSummary();
        WorldAtlasData atlas = requireAtlas();
        UUID owner = SurveyorClient.getClientUuid();
        Landmark marker = requireMarker(summary, owner, A_MARKER_ID);
        require(marker.get(AtlasComponents.SOURCE) == null && !AtlasTime.isHearsay(marker),
            "Alice source marker changed ownership semantics");
        require(atlas.getEditableLandmarks().keySet().stream()
            .anyMatch(found -> found.owner().equals(owner) && found.id().equals(A_MARKER_ID)),
            "Alice marker is not owner-editable");
        require(explored(summary) >= stagedTerrain, "Alice private terrain regressed before disconnect");

        Properties manifest = new Properties();
        manifest.setProperty("owner", owner.toString());
        manifest.setProperty("markerId", A_MARKER_ID.toString());
        manifest.setProperty("terrainBits", Integer.toString(explored(summary)));
        manifest.setProperty("atlasTiles", Integer.toString(atlas.exploredChunks().size()));
        manifest.setProperty("scrollFile", stagedScroll.getFileName().toString());
        manifest.setProperty("scrollSha256", sha256(stagedScroll));
        MapShare.Preview preview = MapShare.peek(stagedScroll, World.OVERWORLD);
        manifest.setProperty("scrollRegions", Integer.toString(preview.regions()));
        store(aManifest(), manifest);
        System.out.printf(
            "P8_DEDICATED_CHECKPOINT phase=a-seed owner=%s marker=%s terrainBits=%d atlasTiles=%d scrollRegions=%d scrollSha=%s editable=1 sourceNonmutation=1%n",
            owner, A_MARKER_ID, explored(summary), atlas.exploredChunks().size(), preview.regions(),
            manifest.getProperty("scrollSha256"));
        System.out.flush();
    }

    private void verifyA(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) throws Exception {
        require("P8Alice".equals(AtlasTime.selfName()), "dedicated A restart identity changed");
        Properties manifest = load(aManifest());
        UUID owner = UUID.fromString(required(manifest, "owner"));
        require(owner.equals(SurveyorClient.getClientUuid()), "dedicated A Surveyor UUID changed on reconnect");
        Landmark marker = requireMarker(summary, owner, A_MARKER_ID);
        require(marker.get(AtlasComponents.SOURCE) == null && !AtlasTime.isHearsay(marker),
            "dedicated A marker became hearsay after restart");
        require(atlas.getEditableLandmarks().keySet().stream()
            .anyMatch(found -> found.owner().equals(owner) && found.id().equals(A_MARKER_ID)),
            "dedicated A marker is not editable after restart");
        require(explored(summary) >= Integer.parseInt(required(manifest, "terrainBits")),
            "dedicated A lost synchronized terrain after restart");
        require(atlas.exploredChunks().size() >= Integer.parseInt(required(manifest, "atlasTiles")),
            "dedicated A lost Atlas tiles after restart");
        Path export = scroll(required(manifest, "scrollFile"));
        require(required(manifest, "scrollSha256").equals(sha256(export)), "Alice scroll changed across restart");
        System.out.printf("P8_DEDICATED_CHECKPOINT phase=a-reconnect owner=%s marker=%s terrainBits=%d atlasTiles=%d persisted=1 editable=1%n",
            owner, A_MARKER_ID, explored(summary), atlas.exploredChunks().size());
        System.out.flush();
    }

    private void mutateB() throws Exception {
        require("P8Bob".equals(AtlasTime.selfName()), "dedicated B did not use P8Bob identity");
        MinecraftClient client = MinecraftClient.getInstance();
        WorldSummary summary = requireSummary();
        Properties alice = load(importedAManifest());
        UUID aliceOwner = UUID.fromString(required(alice, "owner"));
        UUID bobOwner = SurveyorClient.getClientUuid();
        require(!bobOwner.equals(aliceOwner), "dedicated A and B resolved to the same Surveyor UUID");
        require(summary.landmarks().get(aliceOwner, A_MARKER_ID) == null,
            "B received Alice's server marker before import with globalSharing=false");
        int aliceTerrain = Integer.parseInt(required(alice, "terrainBits"));
        require(explored(summary) < aliceTerrain,
            "B already had Alice's extended terrain before import with globalSharing=false");

        stagedMarker = Landmark.create(bobOwner, B_MARKER_ID, builder -> builder
            .add(LandmarkComponentTypes.POS, new BlockPos(-640, 80, -640))
            .add(LandmarkComponentTypes.NAME, Text.literal("P8 Bob marker"))
            .add(AtlasComponents.NOTE, "independent B control")
            .add(AtlasComponents.DAY, AtlasTime.gameDay()));
        summary.landmarks().put(stagedMarker);

        Path aliceScroll = scroll("p8_alice_control.atlas");
        require(required(alice, "scrollSha256").equals(sha256(aliceScroll)),
            "Alice scroll copy changed before B import");
        MapShare.Preview preview = MapShare.peek(aliceScroll, World.OVERWORLD);
        require(preview != null && preview.sameDimension() && preview.markers() == 1
                && preview.regions() == Integer.parseInt(required(alice, "scrollRegions"))
                && preview.authors().contains("P8Alice"),
            "B preview lost Alice attribution or data");
        MapShare.ImportResult imported = MapShare.importFile(aliceScroll, World.OVERWORLD, summary,
            client.world.getRegistryManager());
        require(imported.error() == null && imported.landmarks() == 1 && imported.regions() == preview.regions(),
            "B import of Alice scroll changed: " + imported);
        require(summary.landmarks().get(aliceOwner, A_MARKER_ID) == null,
            "B import crossed into Alice's server-owned marker identity");
        Landmark hearsay = requireMarker(summary, bobOwner, A_MARKER_ID);
        require("P8Alice".equals(hearsay.get(AtlasComponents.SOURCE)) && AtlasTime.isHearsay(hearsay)
                && "scroll_p8alice".equals(RoleplayersAtlas.layerOf(hearsay)),
            "B import lost Alice hearsay attribution or layer");
        require(required(alice, "scrollSha256").equals(sha256(aliceScroll)),
            "Alice scroll copy mutated during B import");
        stagedTerrain = aliceTerrain;
    }

    private void finishB() throws Exception {
        WorldSummary summary = requireSummary();
        WorldAtlasData atlas = requireAtlas();
        Properties alice = load(importedAManifest());
        UUID aliceOwner = UUID.fromString(required(alice, "owner"));
        UUID bobOwner = SurveyorClient.getClientUuid();
        require(summary.landmarks().get(aliceOwner, A_MARKER_ID) == null,
            "B received Alice's original marker after import settle");
        Landmark bob = requireMarker(summary, bobOwner, B_MARKER_ID);
        require(!AtlasTime.isHearsay(bob), "B's own marker became hearsay");
        Landmark hearsay = requireMarker(summary, bobOwner, A_MARKER_ID);
        require("P8Alice".equals(hearsay.get(AtlasComponents.SOURCE)) && AtlasTime.isHearsay(hearsay)
                && "scroll_p8alice".equals(RoleplayersAtlas.layerOf(hearsay)),
            "B's Alice hearsay changed after settle");
        require(explored(summary) >= stagedTerrain, "B did not retain additive Alice terrain");

        Properties manifest = new Properties();
        manifest.setProperty("owner", bobOwner.toString());
        manifest.setProperty("ownMarkerId", B_MARKER_ID.toString());
        manifest.setProperty("aliceMarkerId", A_MARKER_ID.toString());
        manifest.setProperty("aliceOwner", aliceOwner.toString());
        manifest.setProperty("terrainBits", Integer.toString(explored(summary)));
        manifest.setProperty("atlasTiles", Integer.toString(atlas.exploredChunks().size()));
        store(bManifest(), manifest);
        System.out.printf(
            "P8_DEDICATED_CHECKPOINT phase=b-post-import bobOwner=%s aliceOwner=%s ownMarker=1 hearsayMarker=1 originalAliceVisible=0 terrainBits=%d atlasTiles=%d sourceLayer=scroll_p8alice%n",
            bobOwner, aliceOwner, explored(summary), atlas.exploredChunks().size());
        System.out.flush();
    }

    private void verifyB(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) throws Exception {
        require("P8Bob".equals(AtlasTime.selfName()), "dedicated B restart identity changed");
        Properties manifest = load(bManifest());
        UUID bobOwner = UUID.fromString(required(manifest, "owner"));
        UUID aliceOwner = UUID.fromString(required(manifest, "aliceOwner"));
        require(bobOwner.equals(SurveyorClient.getClientUuid()), "dedicated B Surveyor UUID changed on reconnect");
        require(summary.landmarks().get(aliceOwner, A_MARKER_ID) == null,
            "B received Alice's original marker after server restart");
        requireMarker(summary, bobOwner, B_MARKER_ID);
        Landmark hearsay = requireMarker(summary, bobOwner, A_MARKER_ID);
        require("P8Alice".equals(hearsay.get(AtlasComponents.SOURCE)) && AtlasTime.isHearsay(hearsay)
                && "scroll_p8alice".equals(RoleplayersAtlas.layerOf(hearsay)),
            "B lost Alice hearsay after restart");
        require(explored(summary) >= Integer.parseInt(required(manifest, "terrainBits"))
                && atlas.exploredChunks().size() >= Integer.parseInt(required(manifest, "atlasTiles")),
            "B lost additive terrain after restart");
        System.out.printf(
            "P8_DEDICATED_CHECKPOINT phase=b-reconnect bobOwner=%s aliceOwner=%s ownMarker=1 hearsayMarker=1 originalAliceVisible=0 terrainBits=%d atlasTiles=%d%n",
            bobOwner, aliceOwner, explored(summary), atlas.exploredChunks().size());
        System.out.flush();
    }

    private void verifyAIsolation(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) throws Exception {
        require("P8Alice".equals(AtlasTime.selfName()), "dedicated A isolation identity changed");
        Properties alice = load(aManifest());
        Properties bob = load(importedBManifest());
        UUID aliceOwner = UUID.fromString(required(alice, "owner"));
        UUID bobOwner = UUID.fromString(required(bob, "owner"));
        require(aliceOwner.equals(SurveyorClient.getClientUuid()), "A isolation reconnect changed UUID");
        requireMarker(summary, aliceOwner, A_MARKER_ID);
        require(summary.landmarks().get(bobOwner, B_MARKER_ID) == null
                && summary.landmarks().get(bobOwner, A_MARKER_ID) == null,
            "A received Bob-owned personal or imported markers with globalSharing=false");
        require(explored(summary) >= Integer.parseInt(required(alice, "terrainBits")),
            "A lost private terrain during B lifecycle");
        System.out.printf(
            "P8_DEDICATED_CHECKPOINT phase=a-post-b aliceOwner=%s bobOwner=%s ownMarker=1 bobOwnedVisible=0 terrainBits=%d atlasTiles=%d%n",
            aliceOwner, bobOwner, explored(summary), atlas.exploredChunks().size());
        System.out.flush();
    }

    private void mutateTrueA() {
        require("P8TrueA".equals(AtlasTime.selfName()), "global-sharing A identity changed");
        WorldSummary summary = requireSummary();
        UUID owner = SurveyorClient.getClientUuid();
        stagedMarker = Landmark.create(owner, TRUE_MARKER_ID, builder -> builder
            .add(LandmarkComponentTypes.POS, new BlockPos(960, 80, 960))
            .add(LandmarkComponentTypes.NAME, Text.literal("P8 global default marker"))
            .add(AtlasComponents.NOTE, "clean-install globalSharing=true control"));
        summary.landmarks().put(stagedMarker);
    }

    private void finishTrueA() throws Exception {
        WorldSummary summary = requireSummary();
        UUID owner = SurveyorClient.getClientUuid();
        requireMarker(summary, owner, TRUE_MARKER_ID);
        Properties manifest = new Properties();
        manifest.setProperty("owner", owner.toString());
        manifest.setProperty("markerId", TRUE_MARKER_ID.toString());
        store(trueAManifest(), manifest);
        System.out.printf("P8_GLOBAL_SHARING_CHECKPOINT phase=a-seed owner=%s marker=%s%n", owner, TRUE_MARKER_ID);
        System.out.flush();
    }

    private void verifyTrueB(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) throws Exception {
        require("P8TrueB".equals(AtlasTime.selfName()), "global-sharing B identity changed");
        Properties manifest = load(importedTrueAManifest());
        UUID aliceOwner = UUID.fromString(required(manifest, "owner"));
        UUID bobOwner = SurveyorClient.getClientUuid();
        require(!aliceOwner.equals(bobOwner), "global-sharing clients resolved the same UUID");
        Landmark shared = requireMarker(summary, aliceOwner, TRUE_MARKER_ID);
        require(SurveyorClient.canModify(aliceOwner),
            "global-sharing B did not receive Surveyor GROUP modification rights for A");
        require(atlas.getEditableLandmarks().keySet().stream()
            .anyMatch(found -> found.owner().equals(aliceOwner) && found.id().equals(TRUE_MARKER_ID)),
            "Atlas did not preserve Surveyor's global GROUP modification rights");
        require(shared.get(AtlasComponents.LAYER) == null,
            "direct global sharing unexpectedly created an Atlas import layer");
        System.out.printf(
            "P8_GLOBAL_SHARING_CHECKPOINT phase=b-visible aliceOwner=%s bobOwner=%s marker=%s visible=1 editable=1 importLayer=0%n",
            aliceOwner, bobOwner, TRUE_MARKER_ID);
        System.out.flush();
    }

    private static WorldSummary requireSummary() {
        WorldSummary summary = SurveyorClient.tryGetSummary(World.OVERWORLD);
        if (summary == null || summary.terrain() == null || summary.landmarks() == null) {
            throw new IllegalStateException("dedicated Surveyor summary is unavailable");
        }
        return summary;
    }

    private static WorldAtlasData requireAtlas() {
        WorldAtlasData atlas = WorldAtlasData.WORLDS.get(World.OVERWORLD);
        if (atlas == null || atlas.isLoading()) throw new IllegalStateException("dedicated Atlas state is unavailable");
        return atlas;
    }

    private static Landmark requireMarker(WorldSummary summary, UUID owner, Identifier id) {
        Landmark marker = summary.landmarks().get(owner, id);
        if (marker == null) throw new IllegalStateException("dedicated marker is absent: " + owner + "/" + id);
        return marker;
    }

    private static int explored(WorldSummary summary) {
        return summary.terrain().bitSet(SurveyorClient.getExploration()).values().stream()
            .mapToInt(java.util.BitSet::cardinality).sum();
    }

    private static Path atlasConfig() {
        return FabricLoader.getInstance().getConfigDir().resolve("roleplayers-atlas");
    }

    private static Path scroll(String file) {
        return atlasConfig().resolve("scrolls").resolve(file);
    }

    private static Path aManifest() {
        return atlasConfig().resolve("p8_dedicated_a.properties");
    }

    private static Path importedAManifest() {
        return atlasConfig().resolve("p8_dedicated_a_control.properties");
    }

    private static Path bManifest() {
        return atlasConfig().resolve("p8_dedicated_b.properties");
    }

    private static Path importedBManifest() {
        return atlasConfig().resolve("p8_dedicated_b_control.properties");
    }

    private static Path trueAManifest() {
        return atlasConfig().resolve("p8_global_a.properties");
    }

    private static Path importedTrueAManifest() {
        return atlasConfig().resolve("p8_global_a_control.properties");
    }

    private static void store(Path file, Properties properties) throws Exception {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, "P8 disposable dedicated manifest");
        }
    }

    private static Properties load(Path file) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        }
        return properties;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("dedicated manifest is missing " + key);
        return value;
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private void passAndStop(MinecraftClient client, String sentinel) {
        System.out.println(sentinel);
        System.out.flush();
        finished = true;
        client.scheduleStop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
