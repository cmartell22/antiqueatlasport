package glam.ardor.roleplayers_atlas;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import glam.ardor.roleplayers_atlas.reloader.MarkerTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class P7SyncProbe {
    private int joinedTicks;
    private Landmark marker;

    public boolean tick(MinecraftClient client) {
        joinedTicks++;
        require(client.getServer() == null, "P7-S05 joined an integrated server instead of the dedicated target");
        WorldSummary summary = SurveyorClient.tryGetSummary(client.world.getRegistryKey());
        WorldAtlasData atlas = WorldAtlasData.WORLDS.get(client.world.getRegistryKey());
        if (summary == null || summary.landmarks() == null || atlas == null || atlas.isLoading()) {
            require(joinedTicks < 1200, "dedicated Surveyor/Atlas state did not initialize within 60 seconds");
            return false;
        }

        if (marker == null) {
            UUID owner = SurveyorClient.getClientUuid();
            Identifier markerBase = MarkerTextures.getInstance().asMap().keySet().stream()
                .filter(id -> id.getPath().startsWith("custom/"))
                .findFirst().orElseThrow(() -> new IllegalStateException("no custom marker texture loaded for sync"));
            marker = Landmark.create(owner, markerBase.withSuffixedPath("/white/p7_sync"), b -> b
                .add(LandmarkComponentTypes.POS, client.player.getBlockPos())
                .add(LandmarkComponentTypes.NAME, Text.literal("P7 synchronized marker"))
                .add(AtlasComponents.NOTE, "dedicated synchronization probe")
                .add(AtlasComponents.SOURCE, AtlasTime.selfName()));
            summary.landmarks().put(marker);
            return false;
        }

        require(summary.landmarks().contains(marker.owner(), marker.id()),
            "client Surveyor store lost the authored marker");
        require(atlas.getEditableLandmarks().keySet().stream().anyMatch(found -> found.id().equals(marker.id())),
            "Atlas did not expose the synchronized marker as owner-editable");
        if (joinedTicks < 120) return false;

        System.out.printf(
            "P7_SYNC_PROBE_PASS owner=%s marker=%s editable=1 dedicated=1 settleTicks=%d%n",
            marker.owner(), marker.id(), joinedTicks);
        System.out.flush();
        return true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
