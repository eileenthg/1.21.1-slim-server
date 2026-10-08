package net.eileenthg.maidchef;

import javax.annotation.Nullable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import vectorwing.farmersdelight.common.block.FeastBlock;

/** Farmer's Delight feast blocks (roast chicken, shepherd's pie...). Only class-loaded when FD is installed. */
final class FdFeast {
    record Feast(int servings, Item serving) {
    }

    private FdFeast() {
    }

    @Nullable
    static Feast of(Block block) {
        if (block instanceof FeastBlock feast && feast.servingItem != null) {
            return new Feast(feast.getMaxServings(), feast.servingItem.get());
        }
        return null;
    }
}
