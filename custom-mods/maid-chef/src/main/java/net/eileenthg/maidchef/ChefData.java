package net.eileenthg.maidchef;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-maid Chef settings and state, stored through TLM's task data (saved with the maid, synced to clients).
 *
 * @param radius        kitchen radius around the maid's home/work point, in blocks
 * @param target        how many of the chosen dish to make per round
 * @param recentSize    how many recent dishes she remembers (and skips while other dishes are possible)
 * @param minNutrition  dishes must restore at least this much hunger
 * @param minSaturation dishes must give at least this much saturation
 * @param minEffects    dishes must give at least this many beneficial effects
 * @param blacklist     item ids (or #tags) she never makes
 * @param serveFeasts   serve feasts out into portions (the feast is just a step) instead of delivering feast blocks
 * @param dish          item id she makes in "Prepare X" mode, or "" for "best food"
 * @param recent        item ids of the last dishes made, newest first
 * @param status        one-line English status shown in the Chef config screen
 * @param brewing       slow stations she started and will come back to: "x,y,z|recipe id|dish id|units"
 */
public record ChefData(int radius, int target, int recentSize, int minNutrition, float minSaturation, int minEffects,
                       List<String> blacklist, boolean serveFeasts, String dish, List<String> recent, String status,
                       List<String> brewing) {
    public static final int MAX_RECENT = 32;
    public static final int MAX_BLACKLIST = 128;

    public static final Codec<ChefData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("Radius", 8).forGetter(ChefData::radius),
            Codec.INT.optionalFieldOf("Target", 8).forGetter(ChefData::target),
            Codec.INT.optionalFieldOf("RecentSize", 3).forGetter(ChefData::recentSize),
            Codec.INT.optionalFieldOf("MinNutrition", 0).forGetter(ChefData::minNutrition),
            Codec.FLOAT.optionalFieldOf("MinSaturation", 0f).forGetter(ChefData::minSaturation),
            Codec.INT.optionalFieldOf("MinEffects", 0).forGetter(ChefData::minEffects),
            Codec.STRING.listOf().optionalFieldOf("Blacklist", List.of()).forGetter(ChefData::blacklist),
            Codec.BOOL.optionalFieldOf("ServeFeasts", true).forGetter(ChefData::serveFeasts),
            Codec.STRING.optionalFieldOf("Dish", "").forGetter(ChefData::dish),
            Codec.STRING.listOf().optionalFieldOf("Recent", List.of()).forGetter(ChefData::recent),
            Codec.STRING.optionalFieldOf("Status", "").forGetter(ChefData::status),
            Codec.STRING.listOf().optionalFieldOf("Brewing", List.of()).forGetter(ChefData::brewing)
    ).apply(i, ChefData::new));

    public static ChefData defaults() {
        return new ChefData(ChefConfig.DEFAULT_RADIUS.get(), ChefConfig.DEFAULT_TARGET.get(), ChefConfig.RECENT_MEMORY.get(),
                0, 0f, 0, List.of(), true, "", List.of(), "", List.of());
    }

    public ChefData withSettings(int radius, int target, int recentSize, int minNutrition, float minSaturation, int minEffects,
                                 List<String> blacklist, boolean serveFeasts) {
        return withSettings(radius, target, recentSize, minNutrition, minSaturation, minEffects, blacklist, serveFeasts, this.dish);
    }

    public ChefData withSettings(int radius, int target, int recentSize, int minNutrition, float minSaturation, int minEffects,
                                 List<String> blacklist, boolean serveFeasts, String newDish) {
        List<String> r = recent.size() > recentSize ? List.copyOf(recent.subList(0, recentSize)) : recent;
        return new ChefData(radius, target, recentSize, minNutrition, minSaturation, minEffects, List.copyOf(blacklist), serveFeasts, newDish, r, status, brewing);
    }

    public ChefData withStatus(String status) {
        return new ChefData(radius, target, recentSize, minNutrition, minSaturation, minEffects, blacklist, serveFeasts, dish, recent, status, brewing);
    }

    private ChefData withRecentList(List<String> list) {
        return new ChefData(radius, target, recentSize, minNutrition, minSaturation, minEffects, blacklist, serveFeasts, dish, List.copyOf(list), status, brewing);
    }

    /** "Prepare X" mode: the item she makes, or null in "best food" mode. */
    @javax.annotation.Nullable
    public net.minecraft.world.item.Item targetItem() {
        if (dish.isEmpty()) return null;
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(Variant.itemPart(dish));
        return rl == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
    }

    /** Which potion (etc.) "Prepare X" means, or "" for any. */
    public String targetVariant() {
        return Variant.variantPart(dish);
    }

    /** Put a dish at the front of the recent list (moving it if it was already there), trimmed to recentSize. */
    public ChefData withRecent(String itemId) {
        if (recentSize <= 0) return withRecentList(List.of());
        List<String> list = new ArrayList<>();
        list.add(itemId);
        for (String s : recent) {
            if (list.size() >= recentSize) break;
            if (!s.equals(itemId)) list.add(s);
        }
        return withRecentList(list);
    }

    /** Drop a dish from the recent list (its fallback attempt failed). */
    public ChefData withoutRecent(String itemId) {
        List<String> list = new ArrayList<>(recent);
        list.remove(itemId);
        return withRecentList(list);
    }

    public ChefData withBrewing(List<String> list) {
        return new ChefData(radius, target, recentSize, minNutrition, minSaturation, minEffects, blacklist, serveFeasts, dish, recent, status,
                List.copyOf(list));
    }
}
