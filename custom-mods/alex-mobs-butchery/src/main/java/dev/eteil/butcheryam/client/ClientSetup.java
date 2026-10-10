package dev.eteil.butcheryam.client;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import dev.eteil.butcheryam.init.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Client registration.
 *
 * <p>Render layers are not set here: the block models declare their own {@code render_type}
 * ({@code cutout} / {@code cutout_mipped}), exactly as in the original addon.
 */
@EventBusSubscriber(modid = ButcheryAlexsMobs.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FEATHERS.get(), CarcassParticle.FeathersProvider::new);
        event.registerSpriteSet(ModParticles.BLOOD.get(), CarcassParticle.BloodProvider::new);
    }

    private ClientSetup() {}
}
