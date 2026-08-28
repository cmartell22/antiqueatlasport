package glam.ardor.roleplayers_atlas;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import glam.ardor.roleplayers_atlas.reloader.MarkerTextures;
import glam.ardor.roleplayers_atlas.reloader.TileTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.BiomeKeys;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class P7SharingProbe {
    private static final String FOREIGN_AUTHOR = "P7 Cartographer";
    private static final String SCROLL_NAME = "p7_s04_foreign";

    private P7SharingProbe() {
    }

    public static void probe(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) throws Exception {
        require(summary.landmarks() != null, "Surveyor landmark store is absent for sharing");
        UUID owner = SurveyorClient.getClientUuid();
        var dimension = summary.dimension();
        var manager = client.world.getRegistryManager();
        ChunkPos chunk = ChunkPos.fromBlockPos(client.player.getBlockPos());
        BiomeOverrides.State biomeBefore = BiomeOverrides.capture();
        CityPaint.State cityBefore = CityPaint.capture();
        Path scroll = null;
        Path signedScroll = null;
        Path png = null;

        Identifier markerBase = MarkerTextures.getInstance().asMap().keySet().stream()
            .filter(id -> id.getPath().startsWith("custom/"))
            .findFirst().orElseThrow(() -> new IllegalStateException("no custom marker texture loaded for sharing"));
        Landmark foreignMarker = Landmark.create(owner, markerBase.withSuffixedPath("/white/p7_share"), b -> b
            .add(LandmarkComponentTypes.POS, client.player.getBlockPos())
            .add(LandmarkComponentTypes.NAME, Text.literal("P7 shared marker"))
            .add(AtlasComponents.SOURCE, FOREIGN_AUTHOR)
            .add(AtlasComponents.NOTE, "sharing probe"));
        Landmark unsignedMarker = Landmark.create(owner, markerBase.withSuffixedPath("/white/p7_signing"), b -> b
            .add(LandmarkComponentTypes.POS, client.player.getBlockPos())
            .add(LandmarkComponentTypes.NAME, Text.literal("P7 signing copy")));

        Identifier syntheticBiome = id("p7_probe:shared_biome");
        BiomeOverrides.set(syntheticBiome, BiomeKeys.PLAINS.getValue());
        BiomeOverrides.setPatches(dimension, List.of(chunk), BiomeKeys.FOREST.getValue());
        Identifier cityTile = TileTextures.getInstance().getTextures().keySet().stream()
            .filter(id -> id.getPath().startsWith("structure/village/"))
            .findFirst().orElseThrow(() -> new IllegalStateException("no village tile texture loaded for sharing"));
        CityPaint.set(dimension, List.of(chunk), cityTile);

        AtlasUndo.clear();
        try {
            signedScroll = MapShare.export(dimension, summary, manager, List.of(unsignedMarker), false, false, true,
                "p7_s04_signed");
            require(unsignedMarker.get(AtlasComponents.SOURCE) == null
                    && unsignedMarker.get(AtlasComponents.DAY) == null
                    && unsignedMarker.get(AtlasComponents.REAL_TIME) == null,
                "signed export mutated the source landmark");
            MapShare.Preview signedPreview = MapShare.peek(signedScroll, dimension);
            require(signedPreview != null && signedPreview.markers() == 1
                    && signedPreview.authors().contains(AtlasTime.selfName()),
                "signed export did not stamp its copied landmark");

            scroll = MapShare.export(dimension, summary, manager, List.of(foreignMarker), true, true, false, SCROLL_NAME);
            require(Files.isRegularFile(scroll) && Files.size(scroll) > 0, "map scroll export produced no file");
            require(MapShare.listScrolls().contains(scroll), "exported scroll is absent from the scroll listing");

            MapShare.Preview preview = MapShare.peek(scroll, dimension);
            require(preview != null && preview.sameDimension(), "exported scroll preview failed its dimension check");
            require(preview.markers() == 1 && preview.labels() == 0 && preview.routes() == 0 && preview.territories() == 0,
                "exported scroll preview changed landmark classification");
            require(preview.regions() > 0, "terrain-enabled scroll exported no regions");
            require(preview.biomeCorrections() == 1 && preview.patches() == 1 && preview.towns() == 1,
                "correction-enabled scroll exported unexpected counts");
            require(preview.authors().contains(FOREIGN_AUTHOR), "scroll preview lost landmark attribution");

            BiomeOverrides.restore(biomeBefore);
            CityPaint.restore(cityBefore);
            MapShare.ImportResult imported = MapShare.importFile(scroll, dimension, summary, manager);
            require(imported.error() == null, "valid scroll import returned " + imported.error());
            require(imported.landmarks() == 1 && imported.regions() == preview.regions() && imported.corrections() == 3,
                "valid scroll import returned unexpected counts");

            Landmark received = summary.landmarks().get(owner, foreignMarker.id());
            require(received != null, "imported marker is absent from Surveyor");
            require(FOREIGN_AUTHOR.equals(received.get(AtlasComponents.SOURCE)), "imported marker lost source attribution");
            String importedLayer = RoleplayersAtlas.layerOf(received);
            require(importedLayer.equals("scroll_p7_cartographer"), "foreign marker did not enter its source layer");
            require(MarkerLayers.get(importedLayer) != null, "foreign marker source layer was not created");
            require(BiomeOverrides.importedAuthors().contains(SCROLL_NAME), "imported biome sheet lost scroll ownership");
            require(CityPaint.importedAuthors().contains(SCROLL_NAME), "imported city sheet lost scroll ownership");
            require(AtlasUndo.canUndo(), "successful scroll import did not register undo");

            ParchmentExport.setDecoration(true, true, true);
            NativeImage map = ParchmentExport.composeFullMap(atlas, true, true, true, true);
            require(map != null && map.getWidth() > 32 && map.getHeight() > 32,
                "full-map PNG composition produced no useful image");
            NativeImage decorated = ParchmentExport.decorate(map);
            try {
                require(decorated.getWidth() > map.getWidth() && decorated.getHeight() > map.getHeight(),
                    "decorated PNG did not preserve map content inside a larger frame");
                png = ParchmentExport.save(decorated, "p7_s04");
            } finally {
                if (decorated != map) decorated.close();
                map.close();
            }
            byte[] signature = Arrays.copyOf(Files.readAllBytes(png), 8);
            require(Arrays.equals(signature, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
                "saved full-map image has an invalid PNG signature");

            require(AtlasUndo.undo() != null, "scroll import undo returned no description");
            require(summary.landmarks().get(owner, foreignMarker.id()) == null, "scroll undo retained the imported marker");
            require(MarkerLayers.get(importedLayer) == null, "scroll undo retained the imported source layer");
            require(!BiomeOverrides.importedAuthors().contains(SCROLL_NAME), "scroll undo retained imported biome data");
            require(!CityPaint.importedAuthors().contains(SCROLL_NAME), "scroll undo retained imported city data");

            System.out.printf(
                "P7_SHARING_PROBE_PASS scrollBytes=%d regions=%d landmarks=1 corrections=3 attribution=1 sourceLayer=1 undo=1 sourceNonmutation=1 pngBytes=%d pngWidth=%d pngHeight=%d%n",
                Files.size(scroll), preview.regions(), Files.size(png), decoratedWidth(png), decoratedHeight(png));
            System.out.flush();
        } finally {
            summary.landmarks().removeLocal(owner, foreignMarker.id());
            MarkerLayers.remove("scroll_p7_cartographer");
            BiomeOverrides.restore(biomeBefore);
            CityPaint.restore(cityBefore);
            ParchmentExport.setDecoration(false, false, false);
            AtlasUndo.clear();
            if (scroll != null) Files.deleteIfExists(scroll);
            if (signedScroll != null) Files.deleteIfExists(signedScroll);
            if (png != null) Files.deleteIfExists(png);
        }
    }

    private static int decoratedWidth(Path png) throws Exception {
        try (NativeImage image = NativeImage.read(Files.newInputStream(png))) {
            return image.getWidth();
        }
    }

    private static int decoratedHeight(Path png) throws Exception {
        try (NativeImage image = NativeImage.read(Files.newInputStream(png))) {
            return image.getHeight();
        }
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
