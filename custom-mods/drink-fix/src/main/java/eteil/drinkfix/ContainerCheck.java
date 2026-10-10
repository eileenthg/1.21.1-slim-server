package eteil.drinkfix;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Objects;

/**
 * Extra Delight's mixing bowl only compares the container's item, so any potion passes for "a purified water
 * bottle". This adds the two checks that matter: the same potion, and Thirst Was Taken's purity at least as
 * high as the recipe's container asks for. Containers without those components are not affected.
 */
public final class ContainerCheck {
    private static final ResourceLocation PURITY_ID = ResourceLocation.fromNamespaceAndPath("thirst", "purity");
    /** Thirst Was Taken treats a bottle without a purity as its configured default; 2 ("acceptable") unless changed. */
    private static final int FALLBACK_DEFAULT_PURITY = 2;

    private ContainerCheck() {
    }

    public static boolean accepts(ItemStack wanted, ItemStack offered) {
        PotionContents wantPotion = wanted.get(DataComponents.POTION_CONTENTS);
        if (wantPotion != null && wantPotion.potion().isPresent()) {
            PotionContents have = offered.get(DataComponents.POTION_CONTENTS);
            if (have == null || !Objects.equals(have.potion(), wantPotion.potion())) return false;
        }
        DataComponentType<?> purityType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(PURITY_ID);
        if (purityType != null && wanted.get(purityType) instanceof Integer required) {
            return purity(offered, purityType) >= required;
        }
        return true;
    }

    private static int purity(ItemStack stack, DataComponentType<?> type) {
        if (stack.get(type) instanceof Integer value) return value;
        return defaultPurity();
    }

    private static int defaultPurity() {
        try {
            Object value = Class.forName("dev.ghen.thirst.foundation.config.CommonConfig")
                    .getField("DEFAULT_PURITY").get(null);
            Object number = value.getClass().getMethod("get").invoke(value);
            return ((Number) number).intValue();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return FALLBACK_DEFAULT_PURITY;
        }
    }
}
