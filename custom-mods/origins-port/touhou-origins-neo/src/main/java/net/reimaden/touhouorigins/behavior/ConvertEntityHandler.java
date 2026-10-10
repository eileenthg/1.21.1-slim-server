package net.reimaden.touhouorigins.behavior;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.reimaden.touhouorigins.power.ConvertEntityPower;

import java.util.Optional;

/**
 * Drives {@code touhouorigins:convert_entity}: when the holder lands a killing blow, the victim is
 * converted instead of dying.
 *
 * <p>Voile hung this on Fabric's {@code ServerLivingEntityEvents.AllowDeath}, which returns a boolean
 * to veto the death. NeoForge's {@link LivingDeathEvent} is cancellable, which amounts to the same
 * thing: convert, then cancel so vanilla's death handling never runs. {@code Mob.convertTo} already
 * discards the original entity, so there is nothing left to kill.
 *
 * <p>Villager → zombie villager is special-cased exactly as vanilla zombies do it, carrying over
 * profession, gossip, trades and XP so an infected villager can be cured back into the same person.
 * A generic {@code convertTo} would silently lose all of that.
 */
public final class ConvertEntityHandler {

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        if (!(event.getEntity() instanceof Mob victim)) return;

        // A zombie killing a villager already infects it; converting again would double up.
        if (killer instanceof Zombie) return;

        Optional<ConvertEntityPower.Cfg> found = ConvertEntityPower.findFor(killer, victim);
        if (found.isEmpty()) return;
        ConvertEntityPower.Cfg cfg = found.get();

        if (!cfg.ignoreDifficulty() && !difficultyAllows(level)) return;

        EntityType<?> target = BuiltInRegistries.ENTITY_TYPE.get(cfg.convertTo());
        if (target == null) return;

        if (convert(level, killer, victim, target)) {
            event.setCanceled(true);
        }
    }

    /** Vanilla's own infection odds: never on Peaceful or Easy, coin-flip on Normal, always on Hard. */
    private static boolean difficultyAllows(ServerLevel level) {
        Difficulty difficulty = level.getDifficulty();
        if (difficulty == Difficulty.PEACEFUL || difficulty == Difficulty.EASY) return false;
        if (difficulty == Difficulty.NORMAL) return level.getRandom().nextBoolean();
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean convert(ServerLevel level, LivingEntity killer, Mob victim, EntityType<?> target) {
        if (victim instanceof Villager villager && target == EntityType.ZOMBIE_VILLAGER) {
            ZombieVillager zombie = villager.convertTo(EntityType.ZOMBIE_VILLAGER, false);
            if (zombie == null) return false;
            carryOverVillagerData(level, killer, villager, zombie);
            return true;
        }

        Mob converted = victim.convertTo((EntityType<? extends Mob>) target, true);
        if (converted == null) return false;
        converted.playAmbientSound();
        converted.setTarget(killer);
        return true;
    }

    /**
     * Preserves everything that makes an infected villager curable back into the same individual.
     * {@code convertTo(…, false)} does not transfer these, and vanilla's own infection path sets them
     * by hand too.
     */
    private static void carryOverVillagerData(ServerLevel level, LivingEntity killer,
                                              Villager villager, ZombieVillager zombie) {
        zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()),
            MobSpawnType.CONVERSION, new Zombie.ZombieGroupData(false, true));
        zombie.setVillagerData(villager.getVillagerData());

        DynamicOps<Tag> ops = NbtOps.INSTANCE;
        zombie.setGossips(villager.getGossips().store(ops));
        zombie.setTradeOffers(villager.getOffers());
        zombie.setVillagerXp(villager.getVillagerXp());

        if (!killer.isSilent()) {
            level.levelEvent(null, LevelEvent.SOUND_ZOMBIE_INFECTED, killer.blockPosition(), 0);
        }
    }
}
