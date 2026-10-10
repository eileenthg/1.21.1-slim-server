package dev.eteil.butcheryam.butcher;

import dev.eteil.butcheryam.Config;
import dev.eteil.butcheryam.init.ModBlocks;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** The seven Alex's Mobs creatures this addon supports. */
public final class MobDefs {

    public static final MobDef BALD_EAGLE = new MobDef(
            "bald_eagle", "eagle", "alexsmobs:bald_eagle", Config.BALD_EAGLE,
            ModBlocks.BALD_EAGLE_CARCASS, ModBlocks.DRAINED_BALD_EAGLE_CARCASS, ModBlocks.BALD_EAGLE_HEAD);

    public static final MobDef BLOBFISH = new MobDef(
            "blobfish", "blobfish", "alexsmobs:blobfish", Config.BLOBFISH,
            ModBlocks.BLOBFISH_CARCASS, null, null);

    public static final MobDef CAIMAN = new MobDef(
            "caiman", "caiman", "alexsmobs:caiman", Config.CAIMAN,
            ModBlocks.CAIMAN_CARCASS, ModBlocks.DRAINED_CAIMAN_CARCASS, ModBlocks.CAIMAN_HEAD);

    // The Alex's Mobs entity is capuchin_monkey; the original addon's tag said "capuchin",
    // which silently never matched. The tag file ships the corrected entity id.
    public static final MobDef CAPUCHIN = new MobDef(
            "capuchin", "capuchin", "alexsmobs:capuchin", Config.CAPUCHIN,
            ModBlocks.CAPUCHIN_CARCASS, ModBlocks.DRAINED_CAPUCHIN_CARCASS, ModBlocks.CAPUCHIN_HEAD);

    public static final MobDef CROW = new MobDef(
            "crow", "crow", "alexsmobs:crow", Config.CROW,
            ModBlocks.CROW_CARCASS, null, ModBlocks.CROW_HEAD);

    public static final MobDef EMU = new MobDef(
            "emu", "emu", "alexsmobs:emu", Config.EMU,
            ModBlocks.EMU_CARCASS, ModBlocks.DRAINED_EMU_CARCASS, ModBlocks.EMU_HEAD);

    public static final MobDef FLYING_FISH = new MobDef(
            "flying_fish", "flying_fish", "alexsmobs:flying_fish", Config.FLYING_FISH,
            ModBlocks.FLYING_FISH_CARCASS, null, null);

    public static final List<MobDef> ALL =
            List.of(BALD_EAGLE, BLOBFISH, CAIMAN, CAPUCHIN, CROW, EMU, FLYING_FISH);

    /** The definition whose entity tag this entity carries, or null. */
    @Nullable
    public static MobDef forEntity(Entity entity) {
        for (MobDef def : ALL) {
            if (entity.getType().is(ButcherUtil.entityTag(def.entityTag()))) {
                return def;
            }
        }
        return null;
    }

    /** The definition owning this block, whether fresh carcass, drained carcass or head. */
    @Nullable
    public static MobDef forBlock(net.minecraft.world.level.block.Block block) {
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
