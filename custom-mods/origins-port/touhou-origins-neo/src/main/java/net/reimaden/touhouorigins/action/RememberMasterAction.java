package net.reimaden.touhouorigins.action;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.reimaden.touhouorigins.TouhouOrigins;
import net.reimaden.touhouorigins.shikigami.ShikigamiPact;

import java.util.Optional;

/**
 * Records the target as the actor's master, so the pact survives what the entity set does not.
 *
 * <p>Runs alongside {@code apoli:add_to_set} when the contract is sealed. The set is the thing the
 * powers actually read; this is the durable copy that {@link
 * net.reimaden.touhouorigins.shikigami.ShikigamiPactHandler} restores it from after a death or a
 * restart.
 *
 * <p>Direction matters and matches {@code add_to_set}: the actor is the servant, the one who held
 * the {@code Contract} book, and the target is their master.
 */
public final class RememberMasterAction implements ActionType<BiEntityCtx, Object> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("remember_master");

    @Override
    public MapCodec<Object> codec() {
        return MapCodec.unit(new Object());
    }

    @Override
    public void run(Object cfg, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof ServerPlayer servant)) return;
        if (!(ctx.target() instanceof ServerPlayer master)) return;
        if (servant == master) return;
        servant.setData(ShikigamiPact.PACT.get(),
            new ShikigamiPact.Data(Optional.of(master.getUUID())));
    }
}
