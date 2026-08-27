package glam.ardor.roleplayers_atlas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.world.World;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class P6SerializationProbe implements ModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> runAndExit());
    }

    private static void runAndExit() {
        try {
            run(Path.of(System.getProperty("wawi.p6.fixtures")), Path.of(System.getProperty("wawi.p4.fixtures")));
            System.out.flush();
            Runtime.getRuntime().halt(0);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    private static void run(Path p6Root, Path p4Root) throws Exception {
        Path localRoot = p6Root.resolve("local-stores");
        Path atlas = p4Root.resolve("atlas/singleplayer.atlas");
        Map<Path, byte[]> frozenDigests = captureDigests(localRoot, atlas);
        Path scratch = Files.createTempDirectory("wawi-p6-serialization-");

        probeAtlas(atlas, scratch);
        probeLocalStores(localRoot, scratch);

        Map<Path, byte[]> finalDigests = captureDigests(localRoot, atlas);
        require(digestsEqual(frozenDigests, finalDigests), "tracked fixture bytes changed during probe");
        System.out.println("P6_SERIALIZATION_PROBE_PASS atlas=1 localStores=5 malformedAtlas=3 malformedJson=1");
    }

    private static void probeAtlas(Path atlas, Path scratch) throws Exception {
        MapShare.Preview preview = MapShare.peek(atlas, World.OVERWORLD);
        require(preview != null, "target MapShare.peek rejected the frozen P0 scroll");
        require(preview.sameDimension(), "frozen P0 scroll did not match the Overworld");
        require("minecraft:overworld".equals(preview.dimension()), "unexpected frozen scroll dimension");
        require(preview.regions() == 6, "unexpected frozen scroll terrain-region count");
        require(preview.markers() == 0 && preview.labels() == 0 && preview.routes() == 0 && preview.territories() == 0,
            "unexpected frozen singleplayer landmark counts");

        MapShare.Preview wrongDimension = MapShare.peek(atlas, World.NETHER);
        require(wrongDimension != null && !wrongDimension.sameDimension(), "wrong-dimension preview was not bounded");

        try (InputStream input = Files.newInputStream(atlas)) {
            NbtCompound root = NbtIo.readCompressed(input, NbtSizeTracker.ofUnlimitedBytes());
            require(root.getInt("Version").orElse(-1) == 1, "frozen scroll version changed");
            require("minecraft:overworld".equals(root.getString("Dimension").orElse("")), "frozen scroll NBT dimension changed");
        }

        Path garbage = scratch.resolve("garbage.atlas");
        Files.writeString(garbage, "not a gzip nbt scroll");
        require(MapShare.peek(garbage, World.OVERWORLD) == null, "garbage scroll produced a preview");

        byte[] atlasBytes = Files.readAllBytes(atlas);
        Path truncated = scratch.resolve("truncated.atlas");
        Files.write(truncated, Arrays.copyOf(atlasBytes, 64));
        require(MapShare.peek(truncated, World.OVERWORLD) == null, "truncated scroll produced a preview");

        Path missingDimension = scratch.resolve("missing-dimension.atlas");
        NbtCompound emptyRoot = new NbtCompound();
        try (var output = Files.newOutputStream(missingDimension)) {
            NbtIo.writeCompressed(emptyRoot, output);
        }
        MapShare.Preview missingPreview = MapShare.peek(missingDimension, World.OVERWORLD);
        require(missingPreview != null && !missingPreview.sameDimension(), "missing dimension preview was not bounded");
    }

    private static void probeLocalStores(Path localRoot, Path scratch) throws Exception {
        JsonObject biomes = readObject(localRoot.resolve("biomes.json"));
        JsonObject biomeWorld = biomes.getAsJsonObject("sp:P0 Dedicated Baseline");
        require(biomeWorld != null, "biome world key missing");
        require("minecraft:plains".equals(biomeWorld.getAsJsonObject("biomes").get("minecraft:plains").getAsString()),
            "biome override changed");

        JsonObject cities = readObject(localRoot.resolve("cities.json"));
        JsonObject cityWorld = cities.getAsJsonObject("sp:P0 Dedicated Baseline");
        JsonObject cityCells = cityWorld.getAsJsonObject("chunks").getAsJsonObject("minecraft:overworld");
        require(cityCells.size() == 5, "city cell count changed");
        require("roleplayers_atlas:structure/village/taiga/large_house".equals(cityCells.get("-4,-13").getAsString()),
            "city tile changed");

        JsonObject hearth = readObject(localRoot.resolve("hearth.json"));
        require("minecraft:overworld 7 66 -5".equals(hearth.get("sp:P0 Dedicated Baseline").getAsString()),
            "hearth position changed");

        JsonObject layers = readObject(localRoot.resolve("layers.json"));
        require(layers.getAsJsonArray("sp:P0 Dedicated Baseline").isEmpty(), "baseline layers are not empty");
        JsonObject tracked = readObject(localRoot.resolve("tracked_markers.json"));
        require(tracked.getAsJsonArray("sp:P0 Dedicated Baseline").isEmpty(), "baseline tracked markers are not empty");

        for (String name : new String[] {"biomes.json", "cities.json", "hearth.json", "layers.json", "tracked_markers.json"}) {
            JsonElement original = JsonParser.parseString(Files.readString(localRoot.resolve(name)));
            Path copy = scratch.resolve(name);
            Files.writeString(copy, GSON.toJson(original));
            JsonElement roundTripped = JsonParser.parseString(Files.readString(copy));
            require(original.equals(roundTripped), name + " semantic JSON round trip changed content");
        }

        boolean malformedRejected = false;
        try {
            JsonParser.parseString("{\"sp:P0 Dedicated Baseline\":");
        } catch (Exception expected) {
            malformedRejected = true;
        }
        require(malformedRejected, "malformed local-store JSON was accepted");
    }

    private static JsonObject readObject(Path path) throws Exception {
        JsonElement parsed = JsonParser.parseString(Files.readString(path));
        require(parsed.isJsonObject(), path.getFileName() + " is not an object");
        return parsed.getAsJsonObject();
    }

    private static Map<Path, byte[]> captureDigests(Path localRoot, Path atlas) throws Exception {
        Map<Path, byte[]> out = new LinkedHashMap<>();
        try (var paths = Files.list(localRoot)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                out.put(path, java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
            }
        }
        out.put(atlas, java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(atlas)));
        return out;
    }

    private static boolean digestsEqual(Map<Path, byte[]> first, Map<Path, byte[]> second) {
        if (!first.keySet().equals(second.keySet())) return false;
        return first.entrySet().stream().allMatch(entry -> Arrays.equals(entry.getValue(), second.get(entry.getKey())));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
