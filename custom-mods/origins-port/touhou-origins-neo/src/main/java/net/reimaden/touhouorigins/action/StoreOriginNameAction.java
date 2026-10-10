package net.reimaden.touhouorigins.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.reimaden.touhouorigins.TouhouOrigins;

/**
 * Writes the acting entity's origin <i>display name</i> into command storage, so a {@code tellraw}
 * can show "Satori" instead of {@code touhouorigins:satori}.
 *
 * <p>Exists because there is no data-only route to the localised name. The name comes from
 * {@code Origin.name()}, which builds {@code Component.translatable("origin." + namespace + "." +
 * path + ".name")} — turning an id into that key means splicing it apart, and mcfunction macros
 * cannot do string surgery. Reading the raw id straight out of the NeoForge attachment (which is
 * what this replaces) works but renders the bare id.
 *
 * <p>The name is stored already serialised as a JSON text component, so the reading {@code tellraw}
 * uses {@code "interpret": true} and gets a real translatable component — meaning it localises to
 * whatever language the <i>reader</i> is using, rather than being baked to English here.
 *
 * <p>Ordering matters and is relied upon: in {@code apoli:raycast}, {@code bientity_action} runs
 * before {@code command_at_hit} (RaycastAction lines 330 and 400), so this writes the value in the
 * same tick, just before {@code touhouorigins:recollection} reads it.
 *
 * <p>Command storage is global rather than per-player, so two satoris reading minds on the exact
 * same tick could in principle read each other's value. Write and read happen back-to-back within
 * one command chain, so this is not reachable in practice for a single player.
 */
public final class StoreOriginNameAction implements ActionType<EntityCtx, StoreOriginNameAction.Cfg> {

    public static final ResourceLocation CANONICAL = TouhouOrigins.id("store_origin_name");

    private static final ResourceLocation DEFAULT_LAYER =
        ResourceLocation.fromNamespaceAndPath("origins", "origin");

    public record Cfg(ResourceLocation storage, String path, ResourceLocation layer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("storage").forGetter(Cfg::storage),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("path", "value").forGetter(Cfg::path),
            IdCodecs.ID.optionalFieldOf("layer", DEFAULT_LAYER).forGetter(Cfg::layer)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return;
        if (!(ctx.level() instanceof ServerLevel level)) return;

        MinecraftServer server = level.getServer();
        Component name = resolveName(player, cfg.layer());

        CompoundTag tag = server.getCommandStorage().get(cfg.storage());
        if (tag == null) tag = new CompoundTag();
        tag.put(cfg.path(), StringTag.valueOf(
            Component.Serializer.toJson(name, level.registryAccess())));
        server.getCommandStorage().set(cfg.storage(), tag);
    }

    /**
     * The origin's display name, or an empty component if the player has no origin on this layer —
     * an empty line reads better in the mind-reading output than the word "null".
     */
    private static Component resolveName(Player player, ResourceLocation layer) {
        PlayerOriginsImpl origins = PlayerOriginsAttachment.get(player);
        if (origins == null) return Component.empty();

        ResourceLocation originId = origins.getOrigin(layer);
        if (originId == null) return Component.empty();

        Origin origin = OriginRegistry.get(originId);
        // Fall back to the raw id rather than nothing, so an unregistered origin is still debuggable.
        return origin == null ? Component.literal(originId.toString()) : origin.name();
    }
}
