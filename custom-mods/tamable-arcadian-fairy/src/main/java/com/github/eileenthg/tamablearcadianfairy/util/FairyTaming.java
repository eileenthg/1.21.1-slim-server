package com.github.eileenthg.tamablearcadianfairy.util;

import com.github.eileenthg.tamablearcadianfairy.data.ActState;
import com.github.eileenthg.tamablearcadianfairy.data.ModAttachments;
import com.github.eileenthg.tamablearcadianfairy.data.TameData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.reimaden.arcadiandream.entity.custom.mob.BaseFairyEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Small helpers so the rest of the mod never has to poke at attachments directly.
 */
public final class FairyTaming {

    private FairyTaming() {}

    /** True for every Arcadian Dream fairy, including the ice and sunflower variants. */
    public static boolean isFairy(@Nullable Entity entity) {
        return entity instanceof BaseFairyEntity;
    }

    public static boolean isTamed(@Nullable Entity entity) {
        return isFairy(entity) && tameData(entity).tamed();
    }

    public static TameData tameData(Entity entity) {
        return entity.getData(ModAttachments.TAME_DATA);
    }

    public static ActState actState(Entity entity) {
        return entity.getData(ModAttachments.ACT_STATE);
    }

    public static void setActState(Entity entity, ActState state) {
        entity.setData(ModAttachments.ACT_STATE, state);
    }

    public static boolean isOwner(@Nullable Entity entity, @Nullable Player player) {
        return player != null && isFairy(entity) && tameData(entity).isOwnedBy(player.getUUID());
    }

    /** The owner, if they happen to be loaded in the same level. */
    @Nullable
    public static Player owner(Entity fairy) {
        TameData data = tameData(fairy);
        return data.owner().map(uuid -> fairy.level().getPlayerByUUID(uuid)).orElse(null);
    }

    /** True when both entities are tamed fairies belonging to the same player. */
    public static boolean shareOwner(Entity a, Entity b) {
        TameData first = tameData(a);
        TameData second = tameData(b);
        return first.tamed() && second.tamed() && first.owner().isPresent()
                && first.owner().equals(second.owner());
    }

    @Nullable
    public static UUID ownerId(Entity fairy) {
        return tameData(fairy).owner().orElse(null);
    }
}
