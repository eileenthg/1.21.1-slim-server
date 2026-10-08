package net.eileenthg.colonyexpeditions.mixin;

import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The level-aware tool check is protected; expeditions and the Netherworker both need it. */
@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public interface AbstractEntityAIBasicInvoker
{
    @Invoker("checkForToolOrWeapon")
    boolean colonyExpeditions$checkForToolOrWeapon(EquipmentTypeEntry toolType, int minimalLevel);
}
