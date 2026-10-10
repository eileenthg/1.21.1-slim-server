package net.reimaden.touhouorigins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * Runs an entity action when the origin-selection screen opens.
 *
 * <p>Used once, by the {@code unlink} half of {@code powers/global/boundless_servitude}: re-rolling
 * an origin has to clear the shikigami entity set and reset the link resource, or stale links
 * survive the change. Not decorative — this is why the power was kept rather than dropped.
 *
 * <p>Fabric Touhou Origins fired this from two mixins with byte-identical bodies, one on
 * {@code OrbOfOriginItem#use} and one on {@code OriginCommand#openLayerScreen}. Overgrown's Origins
 * has no {@code OriginCommand}, but every path that opens the screen — orb, command, and first join
 * — funnels through {@code OriginsServerNetwork.openChooseScreen}, so a single injection there
 * replaces both and additionally covers the first-join path the original missed.
 * See {@code net.reimaden.touhouorigins.mixin.OriginsServerNetworkMixin}.
 */
public final class ActionOnOriginGuiPower extends PowerType<ActionOnOriginGuiPower.Config> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("action_on_origin_gui");

    public record Config(EntityAction entityAction) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }

    /** Invoked from the mixin when the choose screen is about to open for {@code entity}. */
    public static void onGuiOpen(LivingEntity entity) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return;
        if (container.powersOfType(CANONICAL).isEmpty()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        EntityCtx ctx = new EntityCtx(entity, level);
        // forEach already skips suppressed powers and ones whose condition fails.
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> cfg.entityAction().run(ctx));
    }
}
