package net.eileenthg.colonyexpeditions.mixin;

import com.minecolonies.core.colony.buildings.AbstractBuildingContainer;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractBuildingContainer.class, remap = false)
public interface AbstractBuildingContainerAccessor
{
    @Accessor("keepX")
    Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> colonyExpeditions$getKeepX();
}
