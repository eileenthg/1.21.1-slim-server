package com.github.eileenthg.tamablearcadianfairy.data;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {

    /** When false, an owner cannot damage their own fairy by accident. */
    public static GameRules.Key<GameRules.BooleanValue> ALLOW_OWNER_HURT_FAIRY;

    private ModGameRules() {}

    public static void register() {
        ALLOW_OWNER_HURT_FAIRY = GameRules.register("allowOwnerHurtArcadianFairy",
                GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
    }
}
