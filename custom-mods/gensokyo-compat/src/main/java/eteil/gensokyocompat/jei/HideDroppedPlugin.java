package eteil.gensokyocompat.jei;

import eteil.gensokyocompat.Bridge;
import eteil.gensokyocompat.GensokyoCompat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Hides the dropped duplicate items from JEI. */
@JeiPlugin
public final class HideDroppedPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(GensokyoCompat.MOD_ID, "hide_dropped");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var stacks = Bridge.hiddenItems().stream().map(ItemStack::new).toList();
        if (!stacks.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
    }
}
