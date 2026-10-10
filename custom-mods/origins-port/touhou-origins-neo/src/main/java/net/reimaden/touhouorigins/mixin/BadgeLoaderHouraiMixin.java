package net.reimaden.touhouorigins.mixin;

import dev.overgrown.origins.badge.Badge;
import dev.overgrown.origins.badge.BadgeLoader;
import dev.overgrown.origins.badge.BadgeManager;
import dev.overgrown.origins.badge.TooltipBadge;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import net.reimaden.touhouorigins.TouhouOrigins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Warns fairy players about the Hourai Elixir, but only when Arcadian Dream is actually installed.
 *
 * <p>The elixir's second draught cures all negative effects, and vanilla classes
 * {@code minecraft:levitation} as {@link net.minecraft.world.effect.MobEffectCategory#HARMFUL} —
 * so it strips the effect Fluttering Wings climbs with, as fast as the power reapplies it. The
 * fairy keeps the drift down (slow falling is beneficial) and silently loses the ability to rise,
 * which is impossible to diagnose from inside the game.
 *
 * <p>Rather than change the mechanic to dodge another mod, say so on the origin screen. Doing it
 * here rather than in the datapack because neither Apoli's nor Origins' loaders honour
 * {@code neoforge:conditions}, so a data file cannot be made conditional on a mod being present —
 * and an unconditional warning about a mod most players will not have is worse than none.
 *
 * <p>Appends at the tail of the badge reload rather than through {@code AddReloadListenerEvent},
 * whose ordering against Origins' own listener is not guaranteed. Server side, because whether the
 * elixir exists at all is a property of the server, and badges are synced to clients from here.
 */
@Mixin(BadgeLoader.class)
public abstract class BadgeLoaderHouraiMixin {

    private static final String ARCADIAN_DREAM = "arcadiandream";

    private static final ResourceLocation FLUTTERING_WINGS =
        TouhouOrigins.id("fairy/fluttering_wings");

    private static final ResourceLocation INFO_SPRITE =
        ResourceLocation.fromNamespaceAndPath("origins", "textures/gui/badge/isaacfanta/info.png");

    @Inject(
        method = "apply(Ldev/overgrown/origins/badge/BadgeLoader$Prepared;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("TAIL")
    )
    private void touhouorigins$warnAboutHouraiElixir(BadgeLoader.Prepared prepared, ResourceManager rm,
                                                     ProfilerFiller profiler, CallbackInfo ci) {
        if (!ModList.get().isLoaded(ARCADIAN_DREAM)) return;

        Badge warning = new TooltipBadge(INFO_SPRITE,
            Component.translatable("power.touhouorigins.fairy/fluttering_wings.hourai_warning"));

        // fluttering_wings already declares its own badges, so add to them rather than replace.
        // A fresh list because the loader's is not documented as mutable.
        List<Badge> existing = BadgeManager.BY_POWER.get(FLUTTERING_WINGS);
        List<Badge> merged = existing == null ? new ArrayList<>(1) : new ArrayList<>(existing);
        merged.add(warning);
        BadgeManager.BY_POWER.put(FLUTTERING_WINGS, merged);

        // Announced because this cannot be verified in a dev run: the warning only attaches when
        // Arcadian Dream is present, and a badge that quietly failed to attach looks exactly like
        // one that was never meant to.
        TouhouOrigins.LOGGER.info(
            "[Touhou Origins] Arcadian Dream detected; added the Hourai Elixir warning to {}.",
            FLUTTERING_WINGS);
    }
}
