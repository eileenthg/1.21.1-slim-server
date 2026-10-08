package net.eileenthg.colonyexpeditions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.eileenthg.colonyexpeditions.ColonyExpeditions;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Citizens normally take at most 20% of their health per hit. MineColonies lifts that cap for nether encounters;
 * this lifts it for expedition encounters too.
 */
@Mixin(value = EntityCitizen.class, remap = false)
public abstract class EntityCitizenMixin
{
    private static final TagKey<DamageType> EXPEDITION_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, ColonyExpeditions.id("expedition"));

    @WrapOperation(
      method = "handleDamagePerformed",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/resources/ResourceKey;)Z"))
    private boolean colonyExpeditions$expeditionDamage(final Holder<DamageType> holder, final ResourceKey<DamageType> key, final Operation<Boolean> original)
    {
        return original.call(holder, key) || holder.is(EXPEDITION_DAMAGE);
    }
}
