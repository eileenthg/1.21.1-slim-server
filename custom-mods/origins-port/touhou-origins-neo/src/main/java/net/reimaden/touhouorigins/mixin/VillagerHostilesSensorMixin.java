package net.reimaden.touhouorigins.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraft.world.entity.player.Player;
import net.reimaden.touhouorigins.behavior.BehaviorLookup;
import net.reimaden.touhouorigins.power.ModifyBehaviorPower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes villagers actually flee a {@code hostile} holder — the other half of {@code common/feared}.
 *
 * <p>Villagers have no attack goals, so the targeting goal added by
 * {@link net.reimaden.touhouorigins.behavior.HostileTargetingHandler} cannot reach them. Their fear
 * runs on the brain instead: {@code VillagerHostilesSensor} tests each nearby entity against a static
 * map of scary types (zombie, husk, illager, ravager…) with a per-type radius, and writes any match
 * into the {@code NEAREST_HOSTILE} memory, which the panic behaviour then reads. Players are not in
 * that map, so villagers ignore them however aggressive they are.
 *
 * <p>Answering true here inserts the player into that existing pipeline — no new behaviour needed,
 * the vanilla panic activity does the rest.
 *
 * <p>The 8-block radius matches Voile's choice, and sits in the same range as the vanilla entries.
 * Creative players are skipped, as villagers already ignore them.
 */
@Mixin(VillagerHostilesSensor.class)
public class VillagerHostilesSensorMixin {

    @Unique
    private static final double TOUHOUORIGINS$FEAR_RADIUS_SQR = 8.0D * 8.0D;

    @Inject(method = "isMatchingEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
        at = @At("HEAD"), cancellable = true)
    private void touhouorigins$fearHostileHolders(LivingEntity villager, LivingEntity target,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (!(target instanceof Player player) || player.isCreative() || player.isSpectator()) return;
        if (player.distanceToSqr(villager) > TOUHOUORIGINS$FEAR_RADIUS_SQR) return;

        if (BehaviorLookup.behaviorFor(player, villager) == ModifyBehaviorPower.Behavior.HOSTILE) {
            cir.setReturnValue(true);
        }
        // Otherwise fall through: vanilla decides, and neutral/passive never suppress fear here
        // because players are not in the sensor's map to begin with.
    }
}
