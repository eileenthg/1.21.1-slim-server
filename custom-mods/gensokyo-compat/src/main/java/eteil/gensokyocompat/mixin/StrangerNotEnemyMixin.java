package eteil.gensokyocompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Legacy characters treat any player with reputation 0 or less as an enemy and attack on sight, and every new
 * player starts at 0, so you could never get close enough to gift or feed them. Read 0 as a neutral stranger
 * (as Youkai's Homecoming did); a player who hurts a character still drops below 0 and is attacked.
 */
@Mixin(targets = "dev.xkmc.gensokyolegacy.content.entity.behavior.combat.DefaultCombatManager", remap = false)
public abstract class StrangerNotEnemyMixin {
    @ModifyVariable(method = "targetKind", at = @At("STORE"), ordinal = 0)
    private int gensokyocompat$strangerIsNeutral(int reputation) {
        return reputation == 0 ? 1 : reputation;
    }
}
