package net.eileenthg.colonyexpeditions;

import com.minecolonies.api.crafting.RecipeStorage;
import com.minecolonies.api.items.component.AdventureData;
import com.minecolonies.core.items.ItemAdventureToken;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Estimates how often a worker comes back alive, by replaying the fight loop of EntityAIWorkNether.stayInNether
 * against a destination's real trip loot table. Used to pick the minimum gear tier each destination asks for.
 */
public final class ExpeditionSimulator
{
    /** Armour points and toughness per tier: leather, gold, chain, iron, diamond, netherite. */
    private static final int[] ARMOR_POINTS = {7, 11, 12, 15, 20, 20};
    private static final float[] ARMOR_TOUGHNESS = {0, 0, 0, 0, 2, 3};
    private static final net.minecraft.world.item.Item[] SWORDS =
      {Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.NETHERITE_SWORD};

    private ExpeditionSimulator()
    {
    }

    /** Fraction of simulated trips the worker survives, for one gear tier. */
    public static double survivalRate(
      final ServerLevel level, final net.minecraft.world.entity.Entity stand, final Destination dest, final int tier, final int trials,
      final int secondarySkill, final float maxHealth)
    {
        final LootTable table = level.getServer().reloadableRegistries()
                                  .getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                                    ColonyExpeditions.id("recipes/" + dest.workName() + "/trip1")));
        final ItemStack sword = new ItemStack(SWORDS[Math.min(tier, SWORDS.length - 1)]);
        final LootParams params = new LootParams.Builder(level)
                                    .withParameter(LootContextParams.ORIGIN, stand.position())
                                    .withParameter(LootContextParams.THIS_ENTITY, stand)
                                    .withParameter(LootContextParams.TOOL, sword)
                                    .withLuck(1.0F)
                                    .create(RecipeStorage.recipeLootParameters);
        final float swordDamage = sword.getItem() instanceof SwordItem s ? s.getDamage(sword) : 0;
        final int armor = ARMOR_POINTS[Math.min(Math.max(tier, 0), ARMOR_POINTS.length - 1)];
        final float toughness = ARMOR_TOUGHNESS[Math.min(Math.max(tier, 0), ARMOR_TOUGHNESS.length - 1)];
        final RandomSource rnd = level.random;
        final double damageMultiplier = ExpeditionConfig.trips(dest).damageMultiplier.get();

        int survived = 0;
        for (int trip = 0; trip < trials; trip++)
        {
            float health = maxHealth;
            for (final AdventureData token : tokens(table, params))
            {
                final LivingEntity mob = mob(level, token.entityType());
                if (mob == null)
                {
                    continue;
                }
                float mobHealth = mob.getHealth();
                float incoming = (float) (token.damage() * damageMultiplier);
                incoming -= incoming * secondarySkill * 0.005F;
                for (int hit = 0; mobHealth > 0 && health > 0; hit++)
                {
                    if (rnd.nextBoolean())
                    {
                        mobHealth -= 3.0F + swordDamage;
                    }
                    if (rnd.nextBoolean())
                    {
                        health -= afterArmor(incoming, armor, toughness);
                    }
                    if (hit % 2 == 0)
                    {
                        health = Math.min(maxHealth, health + 2.0F);
                    }
                }
                if (health <= 0)
                {
                    break;
                }
            }
            if (health > 0)
            {
                survived++;
            }
        }
        return (double) survived / trials;
    }

    /** Vanilla armour reduction (CombatRules.getDamageAfterAbsorb, inlined because its signature needs a live entity). */
    private static float afterArmor(final float damage, final int armor, final float toughness)
    {
        final float reduction = Math.min(20.0F, Math.max(armor / 5.0F, armor - damage / (2.0F + toughness / 4.0F)));
        return damage * (1.0F - reduction / 25.0F);
    }

    private static List<AdventureData> tokens(final LootTable table, final LootParams params)
    {
        final List<AdventureData> tokens = new ArrayList<>();
        for (final ItemStack stack : table.getRandomItems(params))
        {
            if (stack.getItem() instanceof ItemAdventureToken)
            {
                final AdventureData data = AdventureData.readFromItemStack(stack);
                if (data != null)
                {
                    tokens.add(data);
                }
            }
        }
        return tokens;
    }

    private static LivingEntity mob(final ServerLevel level, final EntityType<?> type)
    {
        return type.create(level) instanceof LivingEntity living ? living : null;
    }
}
