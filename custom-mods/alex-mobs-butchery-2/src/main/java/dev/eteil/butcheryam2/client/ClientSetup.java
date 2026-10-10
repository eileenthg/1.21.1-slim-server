package dev.eteil.butcheryam2.client;

import dev.eteil.butcheryam2.ButcheryAlexsMobs2;
import dev.eteil.butcheryam2.gen.LargeLayers;
import dev.eteil.butcheryam2.gen.SegmentedLayers;
import dev.eteil.butcheryam2.init.ModBlockEntities;
import dev.eteil.butcheryam2.init.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Client registration.
 *
 * <p>Render layers are not set here: the block models declare their own {@code render_type}
 * ({@code cutout}), as in Part 1. Segmented and large carcasses are drawn by {@link DrawnCarcassRenderer}.
 */
@EventBusSubscriber(modid = ButcheryAlexsMobs2.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FEATHERS.get(), CarcassParticle.FeathersProvider::new);
        event.registerSpriteSet(ModParticles.BLOOD.get(), CarcassParticle.BloodProvider::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        SegmentedLayers.LOOKS.forEach((mob, look) -> look.layers().forEach((piece, layer) ->
                event.registerLayerDefinition(DrawnCarcassRenderer.layer(mob, piece), layer)));
        LargeLayers.LOOKS.forEach((mob, look) ->
                event.registerLayerDefinition(DrawnCarcassRenderer.layer(mob, "main"), look.layer()));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DRAWN.get(), DrawnCarcassRenderer::new);
    }

    private ClientSetup() {}
}
