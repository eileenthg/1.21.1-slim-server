package eteil.drinkfix;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.common.Mod;

/**
 * Some mods (Compat Delight) register their juices, shakes and teas as plain food items, so they are eaten with
 * the chewing animation and the bottle they were brewed into is lost. Items in {@code #drinkfix:drinks} are drunk
 * instead; items also in a {@code #drinkfix:returns/*} tag give that container back.
 */
@Mod(DrinkFix.MOD_ID)
public class DrinkFix {
    public static final String MOD_ID = "drinkfix";
    public static final TagKey<Item> DRINKS = tag("drinks");
    private static final TagKey<Item> RETURNS_BOTTLE = tag("returns/glass_bottle");
    private static final TagKey<Item> RETURNS_BOWL = tag("returns/bowl");
    private static final TagKey<Item> RETURNS_CUP = tag("returns/compatdelight_cup");
    private static final ResourceLocation CUP = ResourceLocation.fromNamespaceAndPath("compatdelight", "cup");

    public DrinkFix() {
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
    }

    /** The container a drink hands back, or empty if its recipe didn't use one. */
    public static ItemStack containerFor(ItemStack drink) {
        if (drink.is(RETURNS_BOTTLE)) return new ItemStack(Items.GLASS_BOTTLE);
        if (drink.is(RETURNS_BOWL)) return new ItemStack(Items.BOWL);
        if (drink.is(RETURNS_CUP)) {
            Item cup = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(CUP);
            return cup == Items.AIR ? ItemStack.EMPTY : new ItemStack(cup);
        }
        return ItemStack.EMPTY;
    }
}
