package net.eileenthg.maidchef.station;

import com.lance5057.extradelight.util.BottleFluidRegistry;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

/** Extra Delight's bottle ↔ fluid registry. Only class-loaded when Extra Delight is installed. */
final class EdBottles {
    private EdBottles() {
    }

    static Set<Item> bottlesFor(Predicate<FluidStack> test) {
        Set<Item> out = new LinkedHashSet<>();
        for (BottleFluidRegistry.BottleFluid bf : BottleFluidRegistry.registry) {
            boolean any = false;
            for (FluidStack f : bf.fluid.getFluids()) any |= test.test(f.copyWithAmount(FluidItems.BOTTLE));
            if (!any) continue;
            for (ItemStack s : bf.bottle.getItems()) {
                // water's "bottle" is a potion item; plain items only
                if (!s.isEmpty() && !s.is(Items.POTION)) out.add(s.getItem());
            }
        }
        return out;
    }

    static FluidStack fluidOf(ItemStack stack) {
        if (stack.is(Items.POTION)) return FluidStack.EMPTY;
        FluidStack f = BottleFluidRegistry.getFluidFromBottle(stack);
        return f.isEmpty() ? FluidStack.EMPTY : f.copyWithAmount(FluidItems.BOTTLE);
    }

    static ItemStack bottleOf(FluidStack fluid) {
        return BottleFluidRegistry.getBottleFromFluid(fluid.copyWithAmount(Math.max(fluid.getAmount(), FluidItems.BOTTLE)));
    }
}
