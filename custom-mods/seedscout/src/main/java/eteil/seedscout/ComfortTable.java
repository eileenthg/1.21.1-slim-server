package eteil.seedscout;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Safe starting temperature" as Cold Sweat sees it: its per-biome night/noon range from
 * config/coldsweat/world.toml, falling back to the biome's base temperature for biomes it doesn't list.
 */
final class ComfortTable {
    static final double MIN_NIGHT_F = 40;
    static final double MAX_NOON_F = 85;

    record Range(double lowF, double highF) {}

    private static final Pattern ROW = Pattern.compile("\\[\\s*\"([^\"]+)\"\\s*,\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*(?:,\\s*\"(\\w+)\")?");
    private static final Map<String, Range> RANGES = new HashMap<>();

    static void load(Path configDir, Logger log) {
        Path file = configDir.resolve("coldsweat").resolve("world.toml");
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            int start = text.indexOf("\"Biome Temperatures\"");
            if (start < 0) return;
            int end = text.indexOf("\n\t]", start);
            Matcher m = ROW.matcher(text.substring(start, end < 0 ? text.length() : end));
            while (m.find()) {
                String unit = m.group(4) == null ? "mc" : m.group(4).toLowerCase();
                double lo = Double.parseDouble(m.group(2));
                double hi = Double.parseDouble(m.group(3));
                if (unit.equals("c")) { lo = lo * 9 / 5 + 32; hi = hi * 9 / 5 + 32; }
                else if (!unit.equals("f")) continue; // Minecraft units: leave to the base-temperature fallback
                RANGES.put(m.group(1), new Range(lo, hi));
            }
            log.info("[seedscout] Cold Sweat biome temperatures loaded for {} biomes", RANGES.size());
        } catch (IOException e) {
            log.info("[seedscout] no Cold Sweat world.toml ({}); using base temperatures", e.getMessage());
        }
    }

    static Range range(String biomeId) {
        return RANGES.get(biomeId);
    }

    static boolean comfortable(Holder<Biome> biome, String biomeId) {
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER)
                || biome.is(BiomeTags.IS_BEACH) || biome.is(Tags.Biomes.IS_SWAMP) || biome.is(Tags.Biomes.IS_MUSHROOM)
                || biomeId.contains("shore") || biomeId.contains("beach") || biomeId.contains("river")) {
            return false;
        }
        Range r = RANGES.get(biomeId);
        if (r != null) return r.lowF() >= MIN_NIGHT_F && r.highF() <= MAX_NOON_F;
        float t = biome.value().getBaseTemperature();
        return t >= 0.5f && t <= 0.9f;
    }

    private ComfortTable() {}
}
