package dev.eteil.butcheryam2.butcher;

import dev.eteil.butcheryam2.gen.GeneratedMobs;
import dev.eteil.butcheryam2.init.ModBlocks;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** The Alex's Mobs creatures Part 2 supports, from the generated table. */
public final class MobDefs {

    public static final List<MobDef> ALL = GeneratedMobs.ALL.stream()
            .map(e -> new MobDef(e.name(), e.name(), MobDef.Kind.valueOf(e.kind()), e.match(),
                    ModBlocks.carcass(e.name()), ModBlocks.drained(e.name()), ModBlocks.head(e.name()),
                    e.deathExtra(), e.extraWhen()))
            .toList();

    public static MobDef get(String name) {
        for (MobDef def : ALL) {
            if (def.name().equals(name)) {
                return def;
            }
        }
        throw new IllegalArgumentException("no mob " + name);
    }

    /** The definition matching this entity, or null. */
    @Nullable
    public static MobDef forEntity(Entity entity) {
        for (MobDef def : ALL) {
            if (def.matches(entity)) {
                return def;
            }
        }
        return null;
    }

    /** The definition owning this block, whether fresh carcass, drained carcass or head. */
    @Nullable
    public static MobDef forBlock(Block block) {
        for (MobDef def : ALL) {
            if (def.carcass().get() == block
                    || (def.drained() != null && def.drained().get() == block)
                    || (def.head() != null && def.head().get() == block)) {
                return def;
            }
        }
        return null;
    }

    private MobDefs() {}
}
