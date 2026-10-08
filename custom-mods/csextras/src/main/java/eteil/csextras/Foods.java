package eteil.csextras;

import com.momosoftworks.coldsweat.core.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Hot, hearty foods (csextras:warming_foods) give Cold Sweat's Warmth effect when eaten. */
public final class Foods {
    public static final TagKey<Item> WARMING = TagKey.create(Registries.ITEM, CSExtras.id("warming_foods"));

    static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !Config.WARMING_FOODS.get()) return;
        if (event.getItem().is(WARMING)) warm(entity);
    }

    /** Returns true if the effect was applied (never shortens or weakens an existing Warmth). */
    public static boolean warm(LivingEntity entity) {
        int duration = Config.WARMING_FOOD_DURATION.get();
        int amplifier = Config.WARMING_FOOD_AMPLIFIER.get();
        MobEffectInstance current = entity.getEffect(ModEffects.WARMTH);
        if (current != null && (current.getAmplifier() > amplifier
                || current.getAmplifier() == amplifier && current.getDuration() >= duration)) return false;
        return entity.addEffect(new MobEffectInstance(ModEffects.WARMTH, duration, amplifier));
    }

    /** Client: list the effect under the food's name, like Farmer's Delight does for its own effects. */
    static void onTooltip(ItemTooltipEvent event) {
        if (!Config.WARMING_FOODS.get() || !event.getItemStack().is(WARMING)) return;
        MobEffectInstance shown = new MobEffectInstance(ModEffects.WARMTH,
                Config.WARMING_FOOD_DURATION.get(), Config.WARMING_FOOD_AMPLIFIER.get());
        Component name = Component.translatable(shown.getDescriptionId());
        if (shown.getAmplifier() > 0) {
            name = Component.translatable("potion.withAmplifier", name,
                    Component.translatable("potion.potency." + shown.getAmplifier()));
        }
        float tickRate = event.getContext().tickRate();
        event.getToolTip().add(Component.translatable("potion.withDuration", name,
                MobEffectUtil.formatDuration(shown, 1.0f, tickRate)).withStyle(ChatFormatting.BLUE));
    }

    private Foods() {
    }
}
