package eteil.csextras;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;

/** What the entity is wearing or holding, as far as weather is concerned. */
public final class Gear {
    public static final TagKey<Item> RAINCOATS = tag("raincoats");
    public static final TagKey<Item> RAIN_HATS = tag("rain_hats");
    public static final TagKey<Item> UMBRELLAS = tag("umbrellas");
    public static final TagKey<Item> SUN_HATS = tag("sun_hats");

    private static final boolean CURIOS = ModList.get().isLoaded("curios");

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, CSExtras.id(path));
    }

    /** Convenient Decor keeps these flags in plain custom_data. */
    private static boolean flag(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(key) && data.copyTag().getBoolean(key);
    }

    public static boolean isOpenUmbrella(LivingEntity entity, ItemStack stack) {
        if (!stack.is(UMBRELLAS)) return false;
        if (flag(stack, "folded")) return false;                           // Convenient Decor: right-click folds
        return !(entity.isUsingItem() && entity.getUseItem() == stack);    // Artifacts: held as a shield
    }

    public static boolean holdingOpenUmbrella(LivingEntity entity) {
        return isOpenUmbrella(entity, entity.getItemInHand(InteractionHand.MAIN_HAND))
                || isOpenUmbrella(entity, entity.getItemInHand(InteractionHand.OFF_HAND));
    }

    public static boolean wearingRaincoat(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(RAINCOATS);
    }

    public static boolean hoodUp(LivingEntity entity) {
        ItemStack coat = entity.getItemBySlot(EquipmentSlot.CHEST);
        return coat.is(RAINCOATS) && flag(coat, "hooded");
    }

    public static boolean wearingRainHat(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(RAIN_HATS);
    }

    /** Raincoat plus a covered head (hood up or rain hat). */
    public static boolean fullRainCover(LivingEntity entity) {
        return wearingRaincoat(entity) && (hoodUp(entity) || wearingRainHat(entity));
    }

    /** Full cover for Origins purposes (optionally an open umbrella too). */
    public static boolean originsRainCover(LivingEntity entity) {
        return fullRainCover(entity) || (Config.UMBRELLA_ORIGINS_RAIN.get() && holdingOpenUmbrella(entity));
    }

    /** Fraction of rain soaking kept off, 0-1. */
    public static double rainProtection(LivingEntity entity) {
        double clothes;
        if (fullRainCover(entity)) clothes = Config.FULL_COVER.get();
        else if (wearingRaincoat(entity)) clothes = Config.RAINCOAT.get();
        else if (wearingRainHat(entity)) clothes = Config.RAIN_HAT.get();
        else clothes = 0;
        double umbrella = holdingOpenUmbrella(entity) ? Config.UMBRELLA_RAIN.get() : 0;
        return 1 - (1 - clothes) * (1 - umbrella);
    }

    /** How much artificial shade the entity carries, 0-1. */
    public static double carriedShade(LivingEntity entity) {
        double shade = holdingOpenUmbrella(entity) ? Config.UMBRELLA_SHADE.get() : 0;
        if (shade < 1 && wearingSunHat(entity)) shade = Math.max(shade, Config.SUN_HAT_SHADE.get());
        return shade;
    }

    public static boolean wearingSunHat(LivingEntity entity) {
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(SUN_HATS)) return true;
        return CURIOS && CuriosCompat.isEquipped(entity, s -> s.is(SUN_HATS));
    }

    private Gear() {
    }
}
