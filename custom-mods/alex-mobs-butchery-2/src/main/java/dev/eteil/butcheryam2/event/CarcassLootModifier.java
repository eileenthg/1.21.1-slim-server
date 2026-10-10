package dev.eteil.butcheryam2.event;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eteil.butcheryam2.Config;
import dev.eteil.butcheryam2.butcher.ButcherUtil;
import dev.eteil.butcheryam2.butcher.MobDef;
import dev.eteil.butcheryam2.butcher.MobDefs;
import dev.eteil.butcheryam2.gen.SegmentedSpecs;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Swaps a butchered creature's death loot for its carcass.
 *
 * <p>Working on the loot table, rather than clearing every drop on death, leaves equipment and
 * inventories alone: a laviathan's saddle and helmet, a potted flutter's pot, an elephant's chest
 * and anything a mob was holding still drop.
 */
public class CarcassLootModifier extends LootModifier {

    public static final MapCodec<CarcassLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, CarcassLootModifier::new));

    public CarcassLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof LivingEntity living) || living.isAlive()
                || !context.getQueriedLootTableId().equals(living.getLootTable().location())) {
            return loot; // only a dying creature's own death table
        }
        MobDef def = MobDefs.forEntity(living);
        if (def == null || !def.isEnabled() || living.isBaby()) {
            return loot;
        }
        Entity killer = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
        boolean cleaver = ButcherUtil.isCleaver(ButcherUtil.heldItem(killer)) || DeathHandler.killedWithCleaver(living);
        if (Config.SPECIFIC_TOOLS.get() && !cleaver) {
            return loot;
        }

        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        out.add(carcassFor(def, living));
        if (def.extraApplies(living)) {
            LootTable extra = context.getLevel().getServer().reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(def.loot("death_extra"))));
            out.addAll(extra.getRandomItems(new LootParams.Builder(context.getLevel()).create(LootContextParamSets.EMPTY)));
        }
        return out;
    }

    /** The carcass item, carrying a segmented body's length and variant into its block entity. */
    public static ItemStack carcassFor(MobDef def, LivingEntity entity) {
        ItemStack carcass = new ItemStack(def.carcass().get());
        SegmentedSpecs.Spec spec = SegmentedSpecs.SPECS.get(def.name());
        if (spec != null) {
            CompoundTag nbt = entity.saveWithoutId(new CompoundTag());
            int count = spec.countNbt() != null ? nbt.getInt(spec.countNbt())
                    : spec.countMethod() != null ? callInt(entity, spec.countMethod()) : 0;
            carcass = DeathHandler.withBody(carcass, count > 0 ? Math.max(1, count + spec.countOffset()) : 0,
                    spec.variantNbt() != null && nbt.getBoolean(spec.variantNbt()));
        }
        return carcass;
    }

    /** A length kept only in synced entity data (the void worm's), read through its getter. */
    private static int callInt(Entity entity, String method) {
        try {
            return ((Number) entity.getClass().getMethod(method).invoke(entity)).intValue();
        } catch (ReflectiveOperationException | ClassCastException e) {
            return 0;
        }
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
