package net.eileenthg.maidchef.station;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.eileenthg.maidchef.ChefConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.neoforged.fml.ModList;

public final class Stations {
    private static List<Station> all;
    /** Recipe lists per station, rebuilt whenever the recipe manager changes (datapack reload). */
    private static final Map<RecipeManager, Map<Station, List<RecipeNeed>>> CACHE = new WeakHashMap<>();

    private Stations() {
    }

    public static synchronized List<Station> all() {
        if (all == null) {
            List<Station> list = new ArrayList<>();
            list.add(new FurnaceStation("furnace", FurnaceBlockEntity.class, RecipeType.SMELTING));
            list.add(new FurnaceStation("smoker", SmokerBlockEntity.class, RecipeType.SMOKING));
            list.add(new CampfireStation());
            if (ModList.get().isLoaded("farmersdelight")) {
                FdStations.addTo(list);
            }
            if (ModList.get().isLoaded("extradelight")) {
                EdStations.addTo(list);
            }
            if (ModList.get().isLoaded("brewinandchewin")) {
                BncStations.addTo(list);
            }
            if (ModList.get().isLoaded("farmersrespite")) {
                FrStations.addTo(list);
            }
            if (ModList.get().isLoaded("youkaishomecoming")) {
                YhcStations.addTo(list);
            }
            if (ModList.get().isLoaded("spectrum")) {
                SpectrumStations.addTo(list);
            }
            MiscStations.addTo(list);
            list.add(new CraftingStation());
            all = List.copyOf(list);
        }
        return all;
    }

    public static boolean enabled(Station s) {
        if (s instanceof CraftingStation) return ChefConfig.USE_CRAFTING_TABLE.get();
        if (s.needsTool()) return ChefConfig.USE_CUTTING_BOARD.get();
        return true;
    }

    private static final Map<RecipeManager, java.util.Set<net.minecraft.world.item.Item>> PREPARED = new WeakHashMap<>();

    /**
     * Prepared food: anything some station recipe in the game cooks from other food. A maid never asks her owner for
     * these; she makes them (or asks for their ingredients).
     */
    public static synchronized java.util.Set<net.minecraft.world.item.Item> preparedFoods(ServerLevel level) {
        return PREPARED.computeIfAbsent(level.getRecipeManager(), rm -> {
            java.util.Set<net.minecraft.world.item.Item> out = new java.util.HashSet<>();
            // made without food somewhere (bread from wheat): still something you can just bring her
            java.util.Set<net.minecraft.world.item.Item> plain = new java.util.HashSet<>();
            // any recipe of any type (other mods' mixing bowls, sushi mats...) that turns food into something
            for (net.minecraft.world.item.crafting.RecipeHolder<?> h : rm.getRecipes()) {
                try {
                    var r = h.value();
                    if (r.isSpecial()) continue;
                    if (r.getIngredients().isEmpty()) continue;
                    boolean fromFood = r.getIngredients().stream().anyMatch(ing -> !ing.isEmpty() && java.util.Arrays.stream(ing.getItems())
                            .anyMatch(st -> net.eileenthg.maidchef.FoodValue.of(st.getItem()) > 0));
                    net.minecraft.world.item.ItemStack res = r.getResultItem(level.registryAccess());
                    if (res == null || res.isEmpty()) continue;
                    (fromFood ? out : plain).add(res.getItem());
                } catch (RuntimeException ignored) {
                    // some modded recipes can't report a result without an input
                }
            }
            List<Station> kinds = new ArrayList<>(all());
            kinds.add(net.eileenthg.maidchef.FeastServeStation.INSTANCE);
            for (Station s : kinds) {
                for (RecipeNeed need : recipes(level, s)) {
                    boolean fromFood = need.inputs().stream().anyMatch(ing -> java.util.Arrays.stream(ing.getItems())
                            .anyMatch(st -> net.eileenthg.maidchef.FoodValue.of(st.getItem()) > 0));
                    (fromFood ? out : plain).add(need.resultItem());
                }
            }
            out.removeAll(plain);
            return out;
        });
    }

    public static synchronized List<RecipeNeed> recipes(ServerLevel level, Station station) {
        RecipeManager rm = level.getRecipeManager();
        return CACHE.computeIfAbsent(rm, k -> new IdentityHashMap<>()).computeIfAbsent(station, s -> s.recipes(level));
    }
}
