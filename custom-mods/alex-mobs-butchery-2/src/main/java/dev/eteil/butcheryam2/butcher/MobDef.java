package dev.eteil.butcheryam2.butcher;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.Config;
import dev.eteil.butcheryam2.gen.GeneratedMobs;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * One butcherable Alex's Mobs creature.
 *
 * @param name       short id, e.g. {@code "bison"}; also its config key and loot table prefix
 * @param tag        entity-type tag (in this mod's namespace) gating the death drop
 * @param kind       how it is butchered
 * @param match      for creatures split into variants (catfish size, fed mosquito, ...), which one
 * @param carcass    the carcass block a kill drops
 * @param drained    drained carcass block, or null when the mob has no bleeding stage
 * @param head       severed head block, or null
 * @param deathExtra whether {@code <name>_death_extra} also drops on death
 * @param extraWhen  when that extra applies, or null for always
 */
public record MobDef(
        String name,
        String tag,
        Kind kind,
        @Nullable GeneratedMobs.Match match,
        Supplier<Block> carcass,
        @Nullable Supplier<Block> drained,
        @Nullable Supplier<Block> head,
        boolean deathExtra,
        @Nullable GeneratedMobs.Match extraWhen
) {

    public enum Kind {
        /** Bleeds into a drained carcass, then head, skin and three cuts (Part 1's big mobs). */
        FULL,
        /** No blood: three steps on one block. */
        STAGED,
        /** One step with either tool (fish). */
        SINGLE,
        /** A list of up to five cuts from {@code StepSpecs}, each removing named parts. */
        STEPS,
        /** Butchery's Ender Dragon scheme: one piece of the body per cut. */
        SEGMENTED,
        /** Too big for a block model: drawn by a renderer, a fixed list of cuts. */
        LARGE
    }

    /** Loot table id for one of this mob's drop stages, e.g. {@code bison_cut_1_drop}. */
    public String loot(String suffix) {
        return ButcheryAlexsMobs2.MODID + ":blocks/" + name + "_" + suffix;
    }

    public boolean isEnabled() {
        return Config.enabled(name);
    }

    public boolean bleeds() {
        return drained != null;
    }

    /** Whether a dying entity is this creature (and, for split mobs, this variant). */
    public boolean matches(Entity entity) {
        return entity.getType().is(ButcherUtil.entityTag(ButcheryAlexsMobs2.MODID + ":" + tag))
                && test(match, entity);
    }

    /** Whether the death extra applies to this entity. */
    public boolean extraApplies(Entity entity) {
        return deathExtra && test(extraWhen, entity);
    }

    /**
     * Tests a variant rule: {@code nbt} reads the entity's saved NBT; {@code method} calls a getter,
     * for state Alex's Mobs keeps only in synced data (the mosquito's blood level).
     */
    public static boolean test(@Nullable GeneratedMobs.Match m, Entity entity) {
        if (m == null) {
            return true;
        }
        int value;
        if (m.source().equals("method")) {
            value = invoke(entity, m.key());
        } else {
            CompoundTag data = entity.saveWithoutId(new CompoundTag());
            value = data.getInt(m.key());
        }
        return switch (m.op()) {
            case ">" -> value > m.value();
            case ">=" -> value >= m.value();
            case "<" -> value < m.value();
            case "<=" -> value <= m.value();
            default -> value == m.value();
        };
    }

    private static int invoke(Entity entity, String getter) {
        try {
            Method method = entity.getClass().getMethod(getter);
            Object out = method.invoke(entity);
            if (out instanceof Number n) {
                return n.intValue();
            }
            return Boolean.TRUE.equals(out) ? 1 : 0;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }
}
