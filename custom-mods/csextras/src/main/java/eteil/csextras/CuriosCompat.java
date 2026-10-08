package eteil.csextras;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Predicate;

/** Only loaded when Curios is present (Artifacts hats sit in curio slots). */
final class CuriosCompat {
    static boolean isEquipped(LivingEntity entity, Predicate<ItemStack> test) {
        return CuriosApi.getCuriosInventory(entity).map(inv -> inv.isEquipped(test)).orElse(false);
    }

    private CuriosCompat() {
    }
}
