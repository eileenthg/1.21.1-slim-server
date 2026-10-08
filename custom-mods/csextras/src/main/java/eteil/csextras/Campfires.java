package eteil.csextras;

import com.momosoftworks.coldsweat.core.init.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Stoking a lit campfire with a log warms everything around it (Frostiful-style). */
public final class Campfires {
    public static final TagKey<Item> FUEL = TagKey.create(Registries.ITEM, CSExtras.id("campfire_fuel"));
    public static final TagKey<Block> STOKABLE = TagKey.create(Registries.BLOCK, CSExtras.id("stokable_campfires"));

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.CAMPFIRE.get()) return;
        Player player = event.getEntity();
        if (player.isSpectator() || player.isShiftKeyDown()) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        ItemStack stack = event.getItemStack();
        if (!canStoke(state, stack)) return;
        if (!event.getLevel().isClientSide()) {
            stoke(event.getLevel(), event.getPos(), player, stack);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }

    public static boolean canStoke(BlockState state, ItemStack stack) {
        return state.is(STOKABLE) && CampfireBlock.isLitCampfire(state) && stack.is(FUEL);
    }

    /** Returns how many entities were warmed. */
    public static int stoke(Level level, BlockPos pos, Player player, ItemStack stack) {
        double r = Config.CAMPFIRE_RADIUS.get();
        int duration = Config.CAMPFIRE_DURATION.get();
        int warmed = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(r))) {
            if (entity.isSpectator()) continue;
            MobEffectInstance current = entity.getEffect(ModEffects.WARMTH);
            if (current != null && (current.getAmplifier() > 0 || current.getDuration() >= duration)) continue;
            entity.addEffect(new MobEffectInstance(ModEffects.WARMTH, duration, 0, true, true));
            warmed++;
        }
        if (level instanceof ServerLevel server) {
            double x = pos.getX() + 0.5, y = pos.getY() + 0.4, z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.SMOKE, x, y, z, 15, 0.5, 0.7, 0.5, 1e-3);
            server.sendParticles(ParticleTypes.LAVA, x, y, z, 6, 0.3, 0.3, 0.3, 1e-2);
        }
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 0.6f);
        player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return warmed;
    }

    private Campfires() {
    }
}
