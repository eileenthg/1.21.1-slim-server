package eteil.seedscout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.LevelStem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes the worldgen registries exactly as the running server resolved them (after Lithostitched,
 * TerraBlender and every mod's data have been applied) as a plain datapack folder.
 */
final class WorldgenDumper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    record Result(Path root, Map<String, Integer> written, List<String> errors) {}

    static Result dump(MinecraftServer server, Path root) throws IOException {
        RegistryAccess access = server.registryAccess();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        Map<String, Integer> written = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        for (RegistryDataLoader.RegistryData<?> data : RegistryDataLoader.WORLDGEN_REGISTRIES) {
            String path = data.key().location().getPath();
            if (!path.startsWith("worldgen/") && !path.equals("dimension_type")) continue;
            dumpRegistry(access, ops, data.key(), data.elementCodec(), root, written, errors, null);
        }
        // Only the overworld stem: the goal is overworld seed searching, and the other dimensions carry
        // mod-specific generators that no datapack viewer understands.
        dumpRegistry(access, ops, Registries.LEVEL_STEM, LevelStem.CODEC, root, written, errors, LevelStem.OVERWORLD);

        Files.writeString(root.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": 48,
                    "description": "Resolved overworld worldgen of the modpack (seedscout dump)"
                  }
                }
                """, StandardCharsets.UTF_8);
        Files.write(root.resolve("_dump_errors.txt"), errors, StandardCharsets.UTF_8);
        return new Result(root, written, errors);
    }

    private static <T> void dumpRegistry(RegistryAccess access, RegistryOps<JsonElement> ops,
                                         ResourceKey<? extends Registry<?>> rawKey, Codec<T> codec, Path root,
                                         Map<String, Integer> written, List<String> errors, ResourceKey<?> only) {
        @SuppressWarnings("unchecked")
        ResourceKey<? extends Registry<T>> key = (ResourceKey<? extends Registry<T>>) rawKey;
        Registry<T> registry = access.registry(key).orElse(null);
        if (registry == null) return;
        String dir = Registries.elementsDirPath(key);
        int count = 0;
        for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            if (only != null && !entry.getKey().equals(only)) continue;
            ResourceLocation id = entry.getKey().location();
            DataResult<JsonElement> result;
            try {
                result = codec.encodeStart(ops, entry.getValue());
            } catch (RuntimeException e) {
                // Some mods register codecs that throw instead of returning an error (Integrated API placements).
                errors.add(dir + " " + id + ": encoder threw " + e);
                continue;
            }
            if (result.error().isPresent()) {
                errors.add(dir + " " + id + ": " + result.error().get().message());
                continue;
            }
            Path file = root.resolve("data").resolve(id.getNamespace()).resolve(dir).resolve(id.getPath() + ".json");
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(result.getOrThrow()), StandardCharsets.UTF_8);
                count++;
            } catch (IOException | RuntimeException e) {
                errors.add(dir + " " + id + ": write failed: " + e);
            }
        }
        written.merge(dir, count, Integer::sum);
    }

    private WorldgenDumper() {}
}
