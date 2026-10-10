package net.reimaden.touhouorigins.scale;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.reimaden.touhouorigins.TouhouOrigins;
import net.reimaden.touhouorigins.power.ModifyScalePower;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleModifier;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;

import java.util.List;

/**
 * A single stateless Pehkui {@link ScaleModifier} shared by every
 * {@code touhouorigins:modify_scale} power.
 *
 * <p>Pehkui composes a final scale as {@code baseScale} passed through each base-value modifier
 * (see {@code ScaleData.getScale}). Origins: Player Scale owns the base layer via {@code /scale
 * multiply}; this modifier owns a layer on top. That is what makes the effect <i>relative</i>:
 * removing it leaves the base untouched, so Player Scale's sizing survives.
 *
 * <p>Being stateless matters — one registered instance serves any number of powers at any factors,
 * because the factor is recomputed from live power state whenever Pehkui consults it. There is no
 * per-power bookkeeping to get out of sync.
 */
public final class RelativeScaleModifier extends ScaleModifier {

    public static final ResourceLocation ID = TouhouOrigins.id("relative_scale");
    public static final ResourceLocation POWER_TYPE = TouhouOrigins.id("modify_scale");

    public static final RelativeScaleModifier INSTANCE = new RelativeScaleModifier();

    private RelativeScaleModifier() {
        // Default priority (512). Applied after Pehkui's own defaults, which is what we want:
        // we multiply whatever the base layer produced.
        super();
    }

    /** Registers the modifier with Pehkui. Must run on both sides — Pehkui syncs modifiers by id. */
    public static void register() {
        ScaleRegistries.register(ScaleRegistries.SCALE_MODIFIERS, ID, INSTANCE);
    }

    @Override
    public float modifyScale(ScaleData scaleData, float modifiedScale, float delta) {
        float factor = factorFor(scaleData);
        return factor == 1.0F ? modifiedScale : modifiedScale * factor;
    }

    @Override
    public float modifyPrevScale(ScaleData scaleData, float modifiedScale) {
        float factor = factorFor(scaleData);
        return factor == 1.0F ? modifiedScale : modifiedScale * factor;
    }

    /**
     * Product of the {@code scale} of every active {@code touhouorigins:modify_scale} power on this
     * entity that names this scale type. Returns 1.0 when there are none, which makes an
     * orphaned attachment harmless.
     */
    private static float factorFor(ScaleData scaleData) {
        Entity entity = scaleData.getEntity();
        if (entity == null) return 1.0F;

        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 1.0F;

        List<ResourceLocation> powers = container.powersOfType(POWER_TYPE);
        if (powers.isEmpty()) return 1.0F;

        ScaleType type = scaleData.getScaleType();
        if (type == null) return 1.0F;
        ResourceLocation typeId = ScaleRegistries.getId(ScaleRegistries.SCALE_TYPES, type);
        if (typeId == null) return 1.0F;

        float factor = 1.0F;
        EntityCtx ctx = null;

        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;

            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ModifyScalePower.Cfg cfg)) continue;
            if (!cfg.scaleTypes().contains(typeId)) continue;

            if (power.condition().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }

            factor *= cfg.scale();
        }
        return factor;
    }
}
