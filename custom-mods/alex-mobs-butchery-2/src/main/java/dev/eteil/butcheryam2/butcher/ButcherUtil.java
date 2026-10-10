package dev.eteil.butcheryam2.butcher;

import dev.eteil.butcheryam2.Config;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.Nullable;

/**
 * Cross-mod lookups and drop helpers.
 *
 * <p>Everything Butchery and Alex's Mobs related is resolved by {@link ResourceLocation} at
 * runtime, so this mod compiles against neither.
 */
public final class ButcherUtil {

    // Butchery blocks the addon interacts with.
    public static final ResourceLocation HOOK = ResourceLocation.parse("butchery:hook");
    public static final ResourceLocation ROPE = ResourceLocation.parse("butchery:rope");
    public static final ResourceLocation BLOOD_GRATE = ResourceLocation.parse("butchery:blood_grate");
    public static final ResourceLocation BLOOD_PUDDLE = ResourceLocation.parse("butchery:blood_puddle");

    // Tool tags. NeoForge standardised on the "c:" namespace; "forge:" is kept for older packs.
    private static final TagKey<net.minecraft.world.item.Item> CLEAVER_C = itemTag("c:cleaver");
    private static final TagKey<net.minecraft.world.item.Item> CLEAVER_FORGE = itemTag("forge:cleaver");
    private static final TagKey<net.minecraft.world.item.Item> KNIFE_C = itemTag("c:skinning_knives");
    private static final TagKey<net.minecraft.world.item.Item> KNIFE_FORGE = itemTag("forge:skinning_knives");

    public static TagKey<net.minecraft.world.item.Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    public static TagKey<net.minecraft.world.entity.EntityType<?>> entityTag(String id) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(id));
    }

    @Nullable
    public static Block block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.get(id);
    }

    public static boolean isBlock(LevelAccessor level, BlockPos pos, ResourceLocation id) {
        return level.getBlockState(pos).getBlock() == block(id);
    }

    public static ItemStack heldItem(@Nullable Entity entity) {
        return entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
    }

    public static boolean isCleaver(ItemStack stack) {
        return stack.is(CLEAVER_C) || stack.is(CLEAVER_FORGE);
    }

    public static boolean isSkinningKnife(ItemStack stack) {
        return stack.is(KNIFE_C) || stack.is(KNIFE_FORGE);
    }

    /**
     * Looting level of the held item, or 0 when the looting option is off.
     * In 1.21 enchantments are registry data, so the holder must be resolved through the level.
     */
    public static int lootingLevel(Level level, @Nullable Entity entity) {
        if (!Config.LOOTING_ENCHANT.get()) {
            return 0;
        }
        ItemStack held = heldItem(entity);
        if (held.isEmpty()) {
            return 0;
        }
        Holder<Enchantment> looting = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING);
        return EnchantmentHelper.getItemEnchantmentLevel(looting, held);
    }

    /** Rolls a loot table and drops the results as item entities. */
    public static void dropLoot(ServerLevel level, double x, double y, double z, String tableId) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(tableId));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        List<ItemStack> rolled = table.getRandomItems(params);
        for (ItemStack stack : rolled) {
            spawnItem(level, x, y, z, stack);
        }
    }

    /**
     * Rolls a table once, plus one extra roll per looting level — matching the original addon,
     * which duplicated its drop block for looting I, II and III.
     */
    public static void dropLootWithLooting(ServerLevel level, double x, double y, double z,
                                           String tableId, int looting) {
        for (int i = 0; i <= Math.max(0, looting); i++) {
            dropLoot(level, x, y, z, tableId);
        }
    }

    public static void spawnItem(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(level, x, y, z, stack);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
    }

    public static void playSound(Level level, BlockPos pos, String soundId, float volume, float pitch) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
        if (sound == null) {
            return;
        }
        if (!level.isClientSide()) {
            level.playSound(null, pos, sound, SoundSource.NEUTRAL, volume, pitch);
        }
    }

    /** Damages the held tool by one point, breaking it when it runs out. */
    public static void damageHeldTool(@Nullable Entity entity) {
        ItemStack held = heldItem(entity);
        if (held.isEmpty() || !(entity instanceof LivingEntity living)) {
            return;
        }
        held.hurtAndBreak(1, living, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    /**
     * Butchery's Enderman heart: the item comes off whole by chance, with a message; otherwise it
     * is lost with a sound, particles and a message, and the fallback table (if any) drops instead.
     */
    public static void rollFragile(ServerLevel level, BlockPos pos, @Nullable Entity player,
                                   dev.eteil.butcheryam2.butcher.MobDef def,
                                   dev.eteil.butcheryam2.gen.StepSpecs.Fragile fragile, int looting) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        boolean ok = level.getRandom().nextFloat() < fragile.chance();
        if (ok) {
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(fragile.item()));
            spawnItem(level, x, y, z, new ItemStack(item));
        } else {
            if (!fragile.failLoot().isEmpty()) {
                dropLootWithLooting(level, x, y, z, def.loot(fragile.failLoot()), looting);
            }
            playSound(level, pos, fragile.sound(), 1.0F, 1.0F);
            var particle = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(fragile.particle()));
            if (particle instanceof net.minecraft.core.particles.SimpleParticleType simple) {
                level.sendParticles(simple, x, y, z, 24, 0.5, 0.5, 0.5, 0.02);
            }
        }
        if (player instanceof net.minecraft.world.entity.player.Player p) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    ok ? fragile.okMessage() : fragile.failMessage()), true);
        }
    }

    private ButcherUtil() {}
}
