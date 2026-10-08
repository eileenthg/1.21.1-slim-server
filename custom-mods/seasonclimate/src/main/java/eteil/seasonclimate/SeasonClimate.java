package eteil.seasonclimate;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Cold Sweat applies Serene Seasons' temperature offset to the whole dimension. This scales that offset by
 * the player's biome, so tropical places stay stable year-round while cold ones swing harder, which is what
 * Serene Seasons already does for its own snow/crop effects.
 *
 * It only touches the value produced by Cold Sweat's Serene Seasons modifier; every other temperature source
 * (biome base temperature, altitude, blocks, armour) is left alone.
 */
@Mod(SeasonClimate.MOD_ID)
public final class SeasonClimate {
    static final String MOD_ID = "seasonclimate";
    private static final Logger LOG = LogUtils.getLogger();

    private static final TagKey<Biome> TROPICAL = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("sereneseasons", "tropical_biomes"));

    /** Biome id -> factor; cleared whenever the config is (re)loaded. */
    private static final Map<ResourceLocation, Double> CACHE = new ConcurrentHashMap<>();

    public SeasonClimate(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modBus.addListener(ModConfigEvent.Loading.class, e -> CACHE.clear());
        modBus.addListener(ModConfigEvent.Reloading.class, e -> CACHE.clear());
        NeoForge.EVENT_BUS.addListener(SeasonClimate::onSeasonTemperature);
    }

    private static void onSeasonTemperature(TempModifierEvent.Calculate.Post event) {
        if (!Config.ENABLED.get()) return;
        // Match by name so this mod still loads if Serene Seasons (and that modifier) is absent.
        if (!event.getModifier().getClass().getSimpleName().contains("SereneSeasons")) return;

        LivingEntity entity = event.getEntity();
        if (entity == null) return;
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        double factor = factorFor(biome);
        if (factor == 1.0) return;

        Function<Double, Double> original = event.getFunction();
        if (original == null) return;
        // The modifier adds an offset to its input; keep the same shape and scale only that offset.
        event.setFunction(temp -> {
            Double shifted = original.apply(temp);
            return shifted == null ? null : temp + (shifted - temp) * factor;
        });
    }

    private static double factorFor(Holder<Biome> biome) {
        ResourceLocation id = biome.unwrapKey().map(ResourceKey::location).orElse(null);
        if (id == null) return 1.0;
        Double cached = CACHE.get(id);
        if (cached != null) return cached;
        double factor = compute(biome, id);
        CACHE.put(id, factor);
        return factor;
    }

    private static double compute(Holder<Biome> biome, ResourceLocation id) {
        for (Map.Entry<String, Double> rule : overrides().entrySet()) {
            String key = rule.getKey();
            if (key.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(key.substring(1));
                if (tagId != null && biome.is(TagKey.create(Registries.BIOME, tagId))) return rule.getValue();
            } else if (key.equals(id.toString())) {
                return rule.getValue();
            }
        }

        double factor;
        if (biome.is(TROPICAL)) {
            factor = Config.TROPICAL.get();
        } else {
            float base = biome.value().getBaseTemperature();
            if (base >= 0.9f) factor = Config.WARM.get();
            else if (base >= 0.5f) factor = Config.TEMPERATE.get();
            else if (base >= 0.15f) factor = Config.COOL.get();
            else factor = Config.COLD.get();
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_BEACH)) {
            factor *= Config.COASTAL.get();
        }
        LOG.debug("[seasonclimate] {} -> seasonal factor {}", id, factor);
        return factor;
    }

    private static Map<String, Double> overrides() {
        Map<String, Double> parsed = new HashMap<>();
        for (String entry : Config.OVERRIDES.get()) {
            int split = entry.lastIndexOf('=');
            if (split <= 0) continue;
            try {
                parsed.put(entry.substring(0, split).trim(), Double.parseDouble(entry.substring(split + 1).trim()));
            } catch (NumberFormatException e) {
                LOG.warn("[seasonclimate] ignoring override \"{}\": {}", entry, e.getMessage());
            }
        }
        return parsed;
    }
}
