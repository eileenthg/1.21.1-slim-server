package net.eileenthg.maidchef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import net.neoforged.fml.ModList;

/** How much a food item is worth, per the weights in the config. Cached until the config reloads. */
public final class FoodValue {
    private static final Map<Item, Double> CACHE = new HashMap<>();
    private static Object cacheKey;
    /** Extra Delight feast item -> EdFeast.Info, rebuilt when the recipe manager changes. Typed loosely so EdFeast only loads with Extra Delight. */
    private static Map<Item, Object> edFeasts = Map.of();
    private static net.minecraft.world.item.crafting.RecipeManager edFeastsFor;

    /** Call with the current recipe manager before valuing (picks up Extra Delight feasts after a datapack reload). */
    public static synchronized void update(net.minecraft.server.level.ServerLevel level) {
        net.minecraft.world.item.crafting.RecipeManager rm = level.getRecipeManager();
        if (rm == edFeastsFor) return;
        edFeastsFor = rm;
        edFeasts = ModList.get().isLoaded("extradelight") ? new HashMap<>(EdFeast.index(rm, level.registryAccess())) : Map.of();
        CACHE.clear();
    }

    private FoodValue() {
    }

    public static synchronized double of(Item item) {
        Object key = List.of(ChefConfig.NUTRITION_WEIGHT.get(), ChefConfig.SATURATION_WEIGHT.get(), ChefConfig.EFFECT_WEIGHT.get(), ChefConfig.FEAST_WEIGHT.get());
        if (!key.equals(cacheKey)) {
            CACHE.clear();
            cacheKey = key;
        }
        Double v = CACHE.get(item);
        if (v == null) {
            v = compute(item, 0);
            CACHE.put(item, v);
        }
        return v;
    }

    private static double compute(Item item, int depth) {
        ItemStack stack = item.getDefaultInstance();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            double v = food.nutrition() * ChefConfig.NUTRITION_WEIGHT.get() + food.saturation() * ChefConfig.SATURATION_WEIGHT.get();
            for (FoodProperties.PossibleEffect pe : food.effects()) {
                MobEffectInstance e = pe.effect();
                if (e.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) continue;
                double minutes = Math.min(5.0, e.getDuration() / 1200.0);
                v += ChefConfig.EFFECT_WEIGHT.get() * (e.getAmplifier() + 1) * minutes * pe.probability();
            }
            return v;
        }
        if (depth == 0 && edFeasts.containsKey(item)) {
            return Math.max(0, EdFeast.value(edFeasts.get(item), it -> compute(it, 1))) * ChefConfig.FEAST_WEIGHT.get();
        }
        if (depth == 0 && item instanceof BlockItem bi && ModList.get().isLoaded("farmersdelight")) {
            FdFeast.Feast feast = FdFeast.of(bi.getBlock());
            if (feast != null) {
                return feast.servings() * compute(feast.serving(), 1) * ChefConfig.FEAST_WEIGHT.get();
            }
        }
        return 0;
    }

    /**
     * Hunger, saturation and number of beneficial effects one serving gives (a feast block counts one serving).
     * Null if the item isn't food.
     */
    public record Stats(int nutrition, float saturation, int effects) {
    }

    @Nullable
    public static synchronized Stats stats(Item item) {
        FoodProperties food = item.getDefaultInstance().get(DataComponents.FOOD);
        if (food == null && edFeasts.containsKey(item)) {
            Item serving = EdFeast.bestServing(edFeasts.get(item), it -> compute(it, 1));
            if (serving != null) food = serving.getDefaultInstance().get(DataComponents.FOOD);
        }
        if (food == null && item instanceof BlockItem bi && ModList.get().isLoaded("farmersdelight")) {
            FdFeast.Feast feast = FdFeast.of(bi.getBlock());
            if (feast != null) food = feast.serving().getDefaultInstance().get(DataComponents.FOOD);
        }
        if (food == null) return null;
        int effects = 0;
        for (FoodProperties.PossibleEffect pe : food.effects()) {
            if (pe.effect().getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) effects++;
        }
        return new Stats(food.nutrition(), food.saturation(), effects);
    }

    public static boolean matchesAny(Item item, List<? extends String> entries) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        for (String e : entries) {
            if (e.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(e.substring(1));
                if (tag != null && item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag))) return true;
            } else if (e.equals(id.toString())) {
                return true;
            }
        }
        return false;
    }
}
