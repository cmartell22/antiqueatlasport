package glam.ardor.roleplayers_atlas;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class P6ClientFailureProbe implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        try {
            Path scratch = Files.createTempDirectory("wawi-p6-client-failures-");
            Path atlas = Path.of(System.getProperty("wawi.p4.fixtures")).resolve("atlas/singleplayer.atlas");

            Path garbage = scratch.resolve("garbage.atlas");
            Files.writeString(garbage, "not a gzip nbt scroll");
            require("read_failed".equals(MapShare.importFile(garbage, World.OVERWORLD, null, null).error()),
                "garbage scroll did not return read_failed");

            byte[] atlasBytes = Files.readAllBytes(atlas);
            Path truncated = scratch.resolve("truncated.atlas");
            Files.write(truncated, Arrays.copyOf(atlasBytes, 64));
            require("read_failed".equals(MapShare.importFile(truncated, World.OVERWORLD, null, null).error()),
                "truncated scroll did not return read_failed");

            Path missingDimension = scratch.resolve("missing-dimension.atlas");
            try (var output = Files.newOutputStream(missingDimension)) {
                NbtIo.writeCompressed(new NbtCompound(), output);
            }
            require("wrong_dimension".equals(MapShare.importFile(missingDimension, World.OVERWORLD, null, null).error()),
                "dimensionless scroll did not return wrong_dimension");

            System.out.println("P6_CLIENT_IMPORT_FAILURE_PROBE_PASS readFailed=2 wrongDimension=1");
            System.out.flush();
            Runtime.getRuntime().halt(0);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
