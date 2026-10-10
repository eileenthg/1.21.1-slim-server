package com.github.eileenthg.tamablearcadianfairy.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * How willing a tamed fairy is to pick its own fights.
 */
public enum AttackMode implements StringRepresentable {

    /** Attacks any nearby hostile mob on its own. */
    ACTIVE("active"),
    /** Only retaliates for itself or its owner. */
    PASSIVE("passive"),
    /** Never picks a target. */
    DISABLED("disabled");

    public static final Codec<AttackMode> CODEC = StringRepresentable.fromEnum(AttackMode::values);

    private final String name;

    AttackMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public AttackMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component displayName() {
        return Component.translatable("mode.tamablearcadianfairy." + name);
    }
}
