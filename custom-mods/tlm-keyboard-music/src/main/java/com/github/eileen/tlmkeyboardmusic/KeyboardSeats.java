package com.github.eileen.tlmkeyboardmusic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * Recognises Touhou Little Maid's keyboard seat using only registry names and saved data, so the addon
 * has no compile-time dependency on TLM.
 */
public final class KeyboardSeats {
    private static final ResourceLocation KEYBOARD_BLOCK = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "keyboard");
    private static final ResourceLocation SIT_ENTITY = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "sit");
    private static final ResourceLocation MAID_ENTITY = ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "maid");
    /** TLM stores the maid's model id under this key (InitDataComponent.MODEL_ID_TAG_NAME). */
    private static final String MODEL_ID_TAG = "model_id";

    private KeyboardSeats() {
    }

    public static boolean isSit(Entity entity) {
        return SIT_ENTITY.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    /** TLM spawns the keyboard seat 0.625 above the block's floor, so the seat's own block is the keyboard. */
    public static boolean isKeyboardSeat(Entity sit, Level level) {
        return isSit(sit) && KEYBOARD_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(sit.blockPosition()).getBlock()));
    }

    public static boolean isMaid(Entity entity) {
        return MAID_ENTITY.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    /**
     * Reads the maid's model id from her saved data. Saving a whole maid is not cheap, but this runs
     * only when a new track is picked, which is minutes apart.
     */
    @Nullable
    public static String maidModelId(Entity maid) {
        String id = maid.saveWithoutId(new CompoundTag()).getString(MODEL_ID_TAG);
        return id.isEmpty() ? null : id;
    }
}
