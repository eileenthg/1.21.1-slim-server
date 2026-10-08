package com.brass_amber.ba_bt.item.item;

import com.brass_amber.ba_bt.init.BTExtras;
import com.brass_amber.ba_bt.util.GolemType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * 1.21: RecordItem is gone; the stone is a plain item made jukebox-playable via its properties
 * (see data/ba_bt/jukebox_song). Enchantments are data-driven, so they are looked up by key.
 */
public class ResonanceStoneItem extends Item {

    private final GolemType golemType;
    public boolean effectOn;

    public ResonanceStoneItem(GolemType golemType, Properties properties) {
        super(properties);
        this.golemType = golemType;
        this.effectOn = false;
    }

    public boolean isEnchantable(ItemStack itemStack) {
        return itemStack.getCount() == 1;
    }

    private ResourceKey<Enchantment> enchantmentKey() {
        return this.golemType == GolemType.OCEAN ? BTExtras.DEPTH_DROPPER : null;
    }

    public void addEnchantment(Level level, ItemStack stackInUse) {
        ResourceKey<Enchantment> key = enchantmentKey();
        if (key != null) {
            level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key)
                    .ifPresent(holder -> stackInUse.enchant(holder, 1));
        }
    }

    @Override
    public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int i, boolean b) {
        super.inventoryTick(itemStack, level, entity, i, b);
        ResourceKey<Enchantment> key = enchantmentKey();
        if (key == null || level.isClientSide() || !this.effectOn) {
            return;
        }
        Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        if (holder != null && EnchantmentHelper.getItemEnchantmentLevel(holder, itemStack) > 0 && entity instanceof LivingEntity living) {
            living.forceAddEffect(new MobEffectInstance(BTExtras.DEPTH_DROPPER_EFFECT, 80, 3), null);
        }
    }
}
