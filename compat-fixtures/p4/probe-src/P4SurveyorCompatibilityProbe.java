package folk.sisby.surveyor.structure;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.serialization.Codec;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentType;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.packet.S2CUpdateRegionPacket;
import folk.sisby.surveyor.structure.RegionStructureSummary;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.util.RegionPos;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class P4SurveyorCompatibilityProbe implements ModInitializer {
    private static final UUID PROBE_OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static LandmarkComponentType<String> sourceComponent;

    public P4SurveyorCompatibilityProbe() {
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> runAndExit());
    }

    private static void runAndExit() {
        try {
            main(new String[] {System.getProperty("wawi.p4.fixtures")});
            System.out.flush();
            Runtime.getRuntime().halt(0);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = copyFixtureTree(Path.of(args[0]));
        initializeLoaderForIsolatedProbe();
        registerLandmarkComponents();

        AtlasStats singleplayer = probeAtlas(root.resolve("atlas/singleplayer.atlas"));
        AtlasStats multiplayer = probeAtlas(root.resolve("atlas/multiplayer-author-player271.atlas"));
        PersistenceStats persistence = probePersistence(root.resolve("surveyor"));

        require(singleplayer.regions > 0, "singleplayer scroll contains no terrain regions");
        require(multiplayer.regions > 0, "multiplayer scroll contains no terrain regions");
        require(multiplayer.landmarks > 0, "multiplayer scroll contains no landmarks");
        require(multiplayer.authoredLandmarks > 0, "multiplayer scroll contains no authored landmarks");
        require(persistence.landmarkFiles > 0, "no persisted landmark files were decoded");
        require(persistence.chunkSummaries > 0, "no persisted chunk summaries were decoded");
        require(persistence.structureRegions > 0, "no persisted structure regions were decoded");
        require(persistence.explorationFiles > 0, "no client exploration files were decoded");

        System.out.printf("ATLAS singleplayer landmarks=%d authored=%d regions=%d packetBytes=%d%n",
            singleplayer.landmarks, singleplayer.authoredLandmarks, singleplayer.regions, singleplayer.packetBytes);
        System.out.printf("ATLAS multiplayer landmarks=%d authored=%d regions=%d packetBytes=%d%n",
            multiplayer.landmarks, multiplayer.authoredLandmarks, multiplayer.regions, multiplayer.packetBytes);
        System.out.printf("PERSISTENCE files=%d landmarks=%d landmarkFiles=%d chunkSummaries=%d structureRegions=%d explorationFiles=%d%n",
            persistence.files, persistence.landmarks, persistence.landmarkFiles, persistence.chunkSummaries,
            persistence.structureRegions, persistence.explorationFiles);
        System.out.println("P4_COMPATIBILITY_PROBE_PASS");
    }

    private static Path copyFixtureTree(Path frozenRoot) throws Exception {
        Path copyRoot = Files.createTempDirectory("wawi-p4-fixtures-");
        copyFixtureDirectory(frozenRoot, copyRoot, "atlas");
        copyFixtureDirectory(frozenRoot, copyRoot, "surveyor");
        return copyRoot;
    }

    private static void copyFixtureDirectory(Path frozenRoot, Path copyRoot, String directory) throws Exception {
        Path sourceRoot = frozenRoot.resolve(directory);
        try (var paths = Files.walk(sourceRoot)) {
            for (Path source : paths.sorted().toList()) {
                Path destination = copyRoot.resolve(frozenRoot.relativize(source).toString());
                if (Files.isDirectory(source)) Files.createDirectories(destination);
                else Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    private static void initializeLoaderForIsolatedProbe() throws Exception {
        if (FabricLoaderImpl.INSTANCE.getGameDir() != null) return;
        Path gameDir = Files.createTempDirectory("wawi-p4-probe-");
        Field gameDirField = FabricLoaderImpl.class.getDeclaredField("gameDir");
        gameDirField.setAccessible(true);
        gameDirField.set(FabricLoaderImpl.INSTANCE, gameDir);
        Field configDirField = FabricLoaderImpl.class.getDeclaredField("configDir");
        configDirField.setAccessible(true);
        configDirField.set(FabricLoaderImpl.INSTANCE, gameDir.resolve("config"));
    }

    private static void registerLandmarkComponents() {
        LandmarkComponentTypes.touch();
        register("zone_title", Codec.BOOL);
        register("zone_radius", Codec.INT);
        register("opacity", Codec.INT);
        register("hide_label", Codec.BOOL);
        register("pen_label", Codec.BOOL);
        register("layer", Codec.STRING);
        register("note", Codec.STRING);
        register("route", Codec.list(BlockPos.CODEC));
        register("show_distance", Codec.BOOL);
        register("day", Codec.LONG);
        register("real_time", Codec.LONG);
        register("show_date", Codec.BOOL);
        sourceComponent = register("source", Codec.STRING);
        register("confirmed_day", Codec.LONG);
    }

    private static <T> LandmarkComponentType<T> register(String path, Codec<T> codec) {
        return LandmarkComponentTypes.register(
            Identifier.of("roleplayers_atlas", path), codec, value -> Text.literal(String.valueOf(value))
        );
    }

    private static AtlasStats probeAtlas(Path path) throws Exception {
        NbtCompound root = readAndRoundTrip(path);
        require(root.getInt("Version").orElseThrow() == 1, path + " has an unexpected format version");
        require(!root.getString("Dimension").orElseThrow().isBlank(), path + " has no dimension");

        int landmarkCount = 0;
        int authoredCount = 0;
        NbtList landmarks = root.getList("Landmarks").orElseThrow();
        for (NbtElement element : landmarks) {
            require(element instanceof NbtCompound, path + " has a non-compound landmark entry");
            NbtCompound entry = (NbtCompound) element;
            Identifier id = Identifier.tryParse(entry.getString("Id").orElseThrow());
            require(id != null, path + " has an invalid landmark id");
            NbtElement data = entry.get("Data");
            require(data != null, path + " landmark has no data");
            Landmark landmark = Landmark.createCodec(PROBE_OWNER, id).parse(NbtOps.INSTANCE, data)
                .resultOrPartial(System.err::println).orElseThrow();
            NbtElement encoded = Landmark.createCodec(PROBE_OWNER, id).encodeStart(NbtOps.INSTANCE, landmark)
                .resultOrPartial(System.err::println).orElseThrow();
            require(data.equals(encoded), path + " landmark target-codec round trip changed NBT for " + id);
            if (landmark.get(sourceComponent) != null) authoredCount++;
            landmarkCount++;
        }

        int regionCount = 0;
        int packetBytes = 0;
        NbtList terrain = root.getList("Terrain").orElseThrow();
        for (NbtElement element : terrain) {
            require(element instanceof NbtCompound, path + " has a non-compound terrain entry");
            NbtCompound entry = (NbtCompound) element;
            require(entry.get("Data") instanceof NbtByteArray, path + " terrain entry has no packet bytes");
            byte[] bytes = ((NbtByteArray) entry.get("Data")).getByteArray();
            RegistryByteBuf input = new RegistryByteBuf(Unpooled.wrappedBuffer(bytes), DynamicRegistryManager.EMPTY);
            S2CUpdateRegionPacket packet = S2CUpdateRegionPacket.CODEC.decode(input);
            require(input.readableBytes() == 0, path + " target packet codec left unread bytes");
            require(packet.regionPos().x() == entry.getInt("X").orElseThrow(), path + " packet X mismatch");
            require(packet.regionPos().z() == entry.getInt("Z").orElseThrow(), path + " packet Z mismatch");
            require(packet.set().cardinality() == packet.chunks().size(), path + " packet set/chunk count mismatch");

            RegistryByteBuf output = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
            S2CUpdateRegionPacket.CODEC.encode(output, packet);
            byte[] encoded = new byte[output.readableBytes()];
            output.readBytes(encoded);
            require(Arrays.equals(bytes, encoded), path + " target packet round trip changed bytes at " + packet.regionPos());
            packetBytes += bytes.length;
            regionCount++;
        }
        return new AtlasStats(landmarkCount, authoredCount, regionCount, packetBytes);
    }

    private static PersistenceStats probePersistence(Path root) throws Exception {
        AtomicInteger files = new AtomicInteger();
        AtomicInteger landmarks = new AtomicInteger();
        AtomicInteger landmarkFiles = new AtomicInteger();
        AtomicInteger chunks = new AtomicInteger();
        AtomicInteger structureRegions = new AtomicInteger();
        AtomicInteger explorationFiles = new AtomicInteger();

        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList()) {
                if (!path.getFileName().toString().endsWith(".dat")) continue;
                files.incrementAndGet();
                NbtCompound nbt = readAndRoundTrip(path);
                String name = path.getFileName().toString();

                if (name.equals("landmarks.dat")) {
                    NbtCompound encodedLandmarks = nbt.getCompound("landmarks").orElse(new NbtCompound());
                    Table<UUID, Identifier, Landmark> table = WorldLandmarks.CODEC.parse(NbtOps.INSTANCE, encodedLandmarks)
                        .resultOrPartial(System.err::println).orElseThrow();
                    NbtElement encoded = WorldLandmarks.CODEC.encodeStart(NbtOps.INSTANCE, table)
                        .resultOrPartial(System.err::println).orElseThrow();
                    require(encodedLandmarks.equals(encoded), path + " landmark codec round trip changed NBT");
                    landmarks.addAndGet(table.size());
                    landmarkFiles.incrementAndGet();
                } else if (name.startsWith("c.")) {
                    NbtCompound chunkMap = nbt.getCompound("chunks").orElseThrow();
                    for (String key : chunkMap.getKeys()) {
                        NbtCompound encodedChunk = chunkMap.getCompound(key).orElseThrow();
                        ChunkSummary summary = new ChunkSummary(encodedChunk);
                        NbtCompound encoded = summary.writeNbt(new NbtCompound());
                        require(encodedChunk.equals(encoded), path + " chunk codec round trip changed NBT at " + key);
                        chunks.incrementAndGet();
                    }
                } else if (name.startsWith("s.")) {
                    RegionStructureSummary summary = RegionStructureSummary.readNbt(nbt);
                    NbtCompound encoded = summary.writeNbt(new NbtCompound());
                    require(nbt.equals(encoded), path + " structure-region round trip changed NBT at " + firstDifference(nbt, encoded, "$"));
                    structureRegions.incrementAndGet();
                } else if (nbt.contains(SurveyorExploration.KEY_EXPLORED_TERRAIN)) {
                    ProbeExploration personal = new ProbeExploration();
                    personal.read(nbt);
                    NbtCompound encoded = personal.write(new NbtCompound());
                    require(nbt.get(SurveyorExploration.KEY_EXPLORED_TERRAIN).equals(encoded.get(SurveyorExploration.KEY_EXPLORED_TERRAIN)), path + " personal terrain exploration changed");
                    require(nbt.get(SurveyorExploration.KEY_EXPLORED_STRUCTURES).equals(encoded.get(SurveyorExploration.KEY_EXPLORED_STRUCTURES)), path + " personal structure exploration changed");
                    if (nbt.getCompound("shared").isPresent()) {
                        NbtCompound sharedNbt = nbt.getCompound("shared").orElseThrow();
                        ProbeExploration shared = new ProbeExploration();
                        shared.read(sharedNbt);
                        NbtCompound sharedEncoded = shared.write(new NbtCompound());
                        require(sharedNbt.get(SurveyorExploration.KEY_EXPLORED_TERRAIN).equals(sharedEncoded.get(SurveyorExploration.KEY_EXPLORED_TERRAIN)), path + " shared terrain exploration changed");
                        require(sharedNbt.get(SurveyorExploration.KEY_EXPLORED_STRUCTURES).equals(sharedEncoded.get(SurveyorExploration.KEY_EXPLORED_STRUCTURES)), path + " shared structure exploration changed");
                    }
                    explorationFiles.incrementAndGet();
                }
            }
        }
        return new PersistenceStats(files.get(), landmarks.get(), landmarkFiles.get(), chunks.get(), structureRegions.get(), explorationFiles.get());
    }

    private static NbtCompound readAndRoundTrip(Path path) throws Exception {
        NbtCompound original = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
        Path temp = Files.createTempFile("p4-surveyor-roundtrip-", ".dat");
        try {
            NbtIo.writeCompressed(original, temp);
            NbtCompound roundTripped = NbtIo.readCompressed(temp, NbtSizeTracker.ofUnlimitedBytes());
            require(original.equals(roundTripped), path + " target NBT round trip changed content");
        } finally {
            Files.deleteIfExists(temp);
        }
        return original;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static String firstDifference(NbtElement left, NbtElement right, String path) {
        if (left.equals(right)) return "<none>";
        if (left instanceof NbtCompound leftCompound && right instanceof NbtCompound rightCompound) {
            Set<String> leftKeys = leftCompound.getKeys();
            Set<String> rightKeys = rightCompound.getKeys();
            if (!leftKeys.equals(rightKeys)) {
                Set<String> onlyLeft = new HashSet<>(leftKeys);
                onlyLeft.removeAll(rightKeys);
                Set<String> onlyRight = new HashSet<>(rightKeys);
                onlyRight.removeAll(leftKeys);
                return path + " keys onlyLeft=" + onlyLeft + " onlyRight=" + onlyRight;
            }
            for (String key : leftKeys.stream().sorted().toList()) {
                String difference = firstDifference(leftCompound.get(key), rightCompound.get(key), path + "." + key);
                if (!difference.equals("<none>")) return difference;
            }
        } else if (left instanceof NbtList leftList && right instanceof NbtList rightList) {
            if (leftList.size() != rightList.size()) return path + " list sizes " + leftList.size() + " != " + rightList.size();
            for (int i = 0; i < leftList.size(); i++) {
                String difference = firstDifference(leftList.get(i), rightList.get(i), path + "[" + i + "]");
                if (!difference.equals("<none>")) return difference;
            }
        }
        return path + " values " + left + " != " + right;
    }

    private record AtlasStats(int landmarks, int authoredLandmarks, int regions, int packetBytes) {
    }

    private record PersistenceStats(int files, int landmarks, int landmarkFiles, int chunkSummaries,
                                    int structureRegions, int explorationFiles) {
    }

    private static final class ProbeExploration implements SurveyorExploration {
        private final Table<RegistryKey<World>, RegionPos, BitSet> chunks = HashBasedTable.create();
        private final Table<RegistryKey<World>, RegistryKey<Structure>, LongSet> starts = HashBasedTable.create();

        @Override
        public Table<RegistryKey<World>, RegionPos, BitSet> chunks() {
            return chunks;
        }

        @Override
        public Table<RegistryKey<World>, RegistryKey<Structure>, LongSet> starts() {
            return starts;
        }

        @Override
        public Set<UUID> sharedPlayers() {
            return new HashSet<>();
        }

        @Override
        public boolean personal() {
            return true;
        }
    }
}
