package glam.ardor.roleplayers_atlas;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import glam.ardor.roleplayers_atlas.reloader.MarkerTextures;
import glam.ardor.roleplayers_atlas.reloader.TileTextures;
import glam.ardor.roleplayers_atlas.util.RouteUtil;
import glam.ardor.roleplayers_atlas.util.TerritoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.BiomeKeys;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class P7AnnotationProbe {
    private P7AnnotationProbe() {
    }

    public static void probe(MinecraftClient client, WorldSummary summary, WorldAtlasData atlas) {
        require(summary.landmarks() != null, "Surveyor landmark store is absent");
        UUID owner = SurveyorClient.getClientUuid();
        var dimension = summary.dimension();
        BlockPos anchor = client.player.getBlockPos();
        ChunkPos chunk = ChunkPos.fromBlockPos(anchor);

        MarkerLayers.MapLayer layer = new MarkerLayers.MapLayer("p7_probe", "P7 Probe", 0x55AA77);
        MarkerLayers.put(layer);
        require(layer.equals(MarkerLayers.get(layer.id())), "custom marker layer did not round trip");
        require(MarkerLayers.all().stream().anyMatch(found -> found.id().equals(layer.id())),
            "custom marker layer is absent from ordered layers");

        Identifier markerBase = MarkerTextures.getInstance().asMap().keySet().stream()
            .filter(id -> id.getPath().startsWith("custom/"))
            .findFirst().orElseThrow(() -> new IllegalStateException("no custom marker texture loaded"));
        Landmark marker = Landmark.create(owner, markerBase.withSuffixedPath("/white/p7_probe"), b -> b
            .add(LandmarkComponentTypes.POS, anchor)
            .add(LandmarkComponentTypes.NAME, Text.literal("P7 marker"))
            .add(AtlasComponents.LAYER, layer.id())
            .add(AtlasComponents.NOTE, "annotation probe")
            .add(AtlasComponents.OPACITY, 75)
            .add(AtlasComponents.DAY, 7L));

        AtlasUndo.clear();
        WorldAtlasData.swapLandmark(dimension, null, marker, Text.literal("P7 marker add"));
        require(summary.landmarks().contains(owner, marker.id()), "marker was not written through Surveyor");
        require(atlas.getEditableLandmarks().keySet().stream().anyMatch(found -> found.id().equals(marker.id())),
            "marker was not reflected in Atlas editable landmarks");
        require(AtlasUndo.canUndo(), "marker creation did not register undo");
        require(RoleplayersAtlas.layerVisible(marker), "new marker layer is unexpectedly hidden");
        RoleplayersAtlas.hiddenLayers.add(layer.id());
        require(!RoleplayersAtlas.layerVisible(marker), "hidden marker layer remained visible");
        RoleplayersAtlas.hiddenLayers.remove(layer.id());

        String trackKey = RoleplayersAtlas.trackKey(marker);
        RoleplayersAtlas.trackedMarkers.add(trackKey);
        require(RoleplayersAtlas.trackedMarkers.contains(trackKey), "marker tracking key was not retained");
        RoleplayersAtlas.trackedMarkers.remove(trackKey);

        Landmark edited = WorldAtlasData.copyLandmarkWith(marker, marker.id(), c -> c.set(AtlasComponents.NOTE, "edited"));
        WorldAtlasData.swapLandmark(dimension, marker, edited, Text.literal("P7 marker edit"));
        require("edited".equals(summary.landmarks().get(owner, marker.id()).get(AtlasComponents.NOTE)),
            "marker edit did not reach Surveyor");
        require(AtlasUndo.undo() != null, "marker edit undo returned no description");
        require("annotation probe".equals(summary.landmarks().get(owner, marker.id()).get(AtlasComponents.NOTE)),
            "marker edit undo did not restore the prior note");
        require(AtlasUndo.redo() != null, "marker edit redo returned no description");
        require("edited".equals(summary.landmarks().get(owner, marker.id()).get(AtlasComponents.NOTE)),
            "marker edit redo did not restore the edited note");

        List<BlockPos> routePoints = List.of(anchor, anchor.add(3, 0, 4));
        require(Math.abs(RouteUtil.length(routePoints) - 5.0) < 0.0001, "route length changed");
        require(RouteUtil.sample(new double[][] {{0, 0}, {2, 1}, {4, 0}, {6, 2}}, false).size() == 25,
            "curved route sampling changed");
        Landmark route = Landmark.create(owner, RoleplayersAtlas.id("route/p7_probe"), b -> b
            .add(LandmarkComponentTypes.POS, anchor)
            .add(LandmarkComponentTypes.NAME, Text.literal("P7 route"))
            .add(AtlasComponents.ROUTE, routePoints)
            .add(AtlasComponents.LAYER, layer.id()));

        Landmark label = Landmark.create(owner, RoleplayersAtlas.id("label/p7_probe"), b -> b
            .add(LandmarkComponentTypes.POS, anchor.add(1, 0, 1))
            .add(LandmarkComponentTypes.NAME, Text.literal("P7 inscription"))
            .add(AtlasComponents.PEN_LABEL, true)
            .add(AtlasComponents.LAYER, layer.id()));

        List<ChunkPos> territoryChunks = List.of(chunk, new ChunkPos(chunk.x() + 1, chunk.z()));
        var territoryShape = TerritoryUtil.chunksToRegions(territoryChunks);
        require(TerritoryUtil.contains(territoryShape, territoryChunks.getFirst()),
            "territory omitted its first chunk");
        require(TerritoryUtil.contains(territoryShape, territoryChunks.getLast()),
            "territory omitted its second chunk");
        var center = TerritoryUtil.centroid(territoryShape);
        require(center.x() == chunk.getStartX() + 16 && center.z() == chunk.getStartZ() + 8,
            "territory centroid changed");
        Landmark territory = Landmark.create(owner, RoleplayersAtlas.id("territory/p7_probe"), b -> b
            .add(LandmarkComponentTypes.CHUNKS, territoryShape)
            .add(LandmarkComponentTypes.NAME, Text.literal("P7 territory"))
            .add(AtlasComponents.LAYER, layer.id()));

        summary.landmarks().putLocal(route);
        summary.landmarks().putLocal(label);
        summary.landmarks().putLocal(territory);
        Map<Landmark, MarkerTexture> markers = atlas.getAllMarkers(1);
        require(markers.keySet().stream().anyMatch(found -> found.id().equals(route.id())),
            "route was not classified as an Atlas marker");
        require(markers.keySet().stream().anyMatch(found -> found.id().equals(label.id())),
            "inscription was not classified as an Atlas marker");
        require(markers.keySet().stream().anyMatch(found -> found.id().equals(territory.id())),
            "territory was not classified as an Atlas marker");

        BiomeOverrides.State biomeBefore = BiomeOverrides.capture();
        Identifier syntheticBiome = id("p7_probe:unknown_biome");
        BiomeOverrides.set(syntheticBiome, BiomeKeys.PLAINS.getValue());
        BiomeOverrides.setPatches(dimension, List.of(chunk), BiomeKeys.FOREST.getValue());
        require(BiomeKeys.PLAINS.getValue().equals(BiomeOverrides.get(syntheticBiome)),
            "biome correction did not round trip");
        require(BiomeKeys.FOREST.getValue().equals(BiomeOverrides.patch(dimension, chunk)),
            "biome patch did not round trip");

        CityPaint.State cityBefore = CityPaint.capture();
        Identifier cityTile = TileTextures.getInstance().getTextures().keySet().stream()
            .filter(id -> id.getPath().startsWith("structure/village/"))
            .findFirst().orElseThrow(() -> new IllegalStateException("no village tile texture loaded"));
        CityPaint.set(dimension, List.of(chunk), cityTile);
        require(cityTile.equals(CityPaint.at(dimension, chunk)), "city paint did not round trip");

        require(SpawnMarker.get(dimension) != null, "hearth/spawn marker is absent after world join");

        CityPaint.restore(cityBefore);
        BiomeOverrides.restore(biomeBefore);
        for (Landmark landmark : List.of(edited, route, label, territory)) {
            summary.landmarks().removeLocal(landmark.owner(), landmark.id());
        }
        MarkerLayers.remove(layer.id());
        RoleplayersAtlas.hiddenLayers.remove(layer.id());
        RoleplayersAtlas.trackedMarkers.remove(trackKey);
        AtlasUndo.clear();

        System.out.printf(
            "P7_ANNOTATION_PROBE_PASS markers=1 layers=1 edits=1 undo=1 redo=1 routes=1 inscriptions=1 territories=1 biomeCorrections=2 cityCells=1 hearth=1%n");
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
