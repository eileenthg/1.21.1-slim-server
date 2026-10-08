package net.eileenthg.maidchef.station;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;

/**
 * Which item fills each input of a recipe. The same item is used for an input in every unit, so a station
 * slot never needs two different items (a cooking pot slot can only hold one).
 *
 * @param perInput item chosen for each entry of {@link RecipeNeed#inputs()}
 * @param perUnit  how many of each item one unit consumes (inputs plus containers)
 */
public record Alloc(List<Item> perInput, Map<Item, Integer> perUnit) {
    public Map<Item, Integer> times(int units) {
        Map<Item, Integer> out = new LinkedHashMap<>();
        perUnit.forEach((item, n) -> out.put(item, n * units));
        return out;
    }
}
