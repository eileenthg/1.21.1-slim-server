package net.eileenthg.maidchef;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * Items that differ only by what's in them: potions. The planner works per item, so "Prepare X" stores the variant next to the
 * item id ("minecraft:potion|effect:minecraft:speed") and only recipes making that variant count.
 */
public final class Variant {
    private Variant() {
    }

    /** "" for an ordinary item; "potion:id" or "effect:id" for a potion-like item. */
    public static String key(ItemStack stack) {
        PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
        if (pc == null) return "";
        if (pc.potion().isPresent()) {
            return pc.potion().get().unwrapKey().map(k -> "potion:" + k.location()).orElse("");
        }
        for (MobEffectInstance e : pc.customEffects()) {
            return e.getEffect().unwrapKey().map(k -> "effect:" + k.location()).orElse("");
        }
        return "";
    }

    /** What "Prepare X" stores for this stack. */
    public static String dishOf(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String k = key(stack);
        return k.isEmpty() ? id : id + "|" + k;
    }

    public static String itemPart(String dish) {
        int i = dish.indexOf('|');
        return i < 0 ? dish : dish.substring(0, i);
    }

    public static String variantPart(String dish) {
        int i = dish.indexOf('|');
        return i < 0 ? "" : dish.substring(i + 1);
    }

    /** A stack to show for a stored dish (the potion with its effect). */
    public static ItemStack display(String dish) {
        ResourceLocation rl = ResourceLocation.tryParse(itemPart(dish));
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        ItemStack stack = new ItemStack(item);
        String v = variantPart(dish);
        if (v.startsWith("potion:")) {
            ResourceLocation p = ResourceLocation.tryParse(v.substring(7));
            if (p != null) BuiltInRegistries.POTION.getHolder(p).ifPresent(h -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(h)));
        } else if (v.startsWith("effect:")) {
            ResourceLocation e = ResourceLocation.tryParse(v.substring(7));
            if (e != null) {
                BuiltInRegistries.MOB_EFFECT.getHolder(e).ifPresent(h -> stack.set(DataComponents.POTION_CONTENTS,
                        new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(h, 3600)))));
            }
        }
        return stack;
    }

    /** True if the stored variant (possibly "") accepts this result. */
    public static boolean matches(@Nullable String variant, ItemStack result) {
        return variant == null || variant.isEmpty() || variant.equals(key(result));
    }
}
