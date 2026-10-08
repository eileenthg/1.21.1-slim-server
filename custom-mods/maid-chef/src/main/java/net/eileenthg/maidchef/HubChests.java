package net.eileenthg.maidchef;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.ModList;

/**
 * Chest positions bound to a Maidsoul Kitchen Culinary Hub sitting in the maid's 5th backpack slot.
 * Red (Ingredient) chests are where she takes ingredients from, green (Output) chests where she delivers dishes.
 */
public final class HubChests {
    public record Bound(List<BlockPos> ingredient, List<BlockPos> output) {
        public static final Bound NONE = new Bound(List.of(), List.of());

        public boolean hasHub() {
            return this != NONE;
        }
    }

    private HubChests() {
    }

    public static Bound of(EntityMaid maid) {
        if (!ModList.get().isLoaded("maidsoulkitchen")) return Bound.NONE;
        return MskHub.read(maid);
    }
}
